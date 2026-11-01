package cn.shiguang.notes.integration;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.CookieManager;
import java.net.URI;
import java.net.http.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Exercises actual HTTP, MySQL transactions and RabbitMQ, never substitutes an in-memory store. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ApiFlowIT {
  @Test
  @Order(16)
  void feedbackApprovalIsInvalidatedBySourceRevisionWithoutLosingPrivateRecord() throws Exception {
    login("demo_user");
    String pid =
        database.one("SELECT id FROM t_practice ORDER BY id DESC LIMIT 1").get("id").toString();
    var practice = (Map<?, ?>) call("GET", "/practices/" + pid, null, 200).get("data");
    String note = practice.get("noteId").toString();
    call(
        "POST",
        "/practices/" + pid + "/publish-request",
        Map.of("version", practice.get("version")),
        200);
    login("demo_reviewer");
    approveTarget(pid, "PRACTICE");
    login("demo_user");
    var feedback = (Map<?, ?>) call("GET", "/notes/" + note + "/feedback", null, 200).get("data");
    assertTrue(
        ((List<?>) feedback.get("items"))
            .stream().anyMatch(x -> pid.equals(((Map<?, ?>) x).get("id"))));
    var source = (Map<?, ?>) call("GET", "/me/notes/" + note, null, 200).get("data");
    call(
        "PUT",
        "/notes/" + note,
        Map.of(
            "title",
            source.get("title"),
            "body",
            "源笔记更新后旧反馈不能自动恢复公开",
            "version",
            source.get("version"),
            "mediaIds",
            source.get("mediaIds"),
            "topicIds",
            source.get("topicIds")),
        200);
    call("GET", "/notes/" + note, null, 404);
    call("GET", "/practices/" + pid, null, 200);
    login("demo_reviewer");
    approveTarget(note, "NOTE");
    login("demo_user");
    var after = (Map<?, ?>) call("GET", "/notes/" + note + "/feedback", null, 200).get("data");
    assertTrue(((List<?>) after.get("items")).isEmpty());
  }

  void approveTarget(String target, String type) throws Exception {
    var queue = (Map<?, ?>) call("GET", "/reviews?size=50", null, 200).get("data");
    var review =
        (Map<?, ?>)
            ((List<?>) queue.get("items"))
                .stream()
                    .filter(
                        x ->
                            target.equals(((Map<?, ?>) x).get("targetId"))
                                && type.equals(((Map<?, ?>) x).get("targetType")))
                    .findFirst()
                    .orElseThrow();
    call(
        "POST",
        "/reviews/" + review.get("id") + "/decision",
        Map.of("version", review.get("version"), "decision", "APPROVE"),
        200);
  }

  @org.springframework.beans.factory.annotation.Autowired
  cn.shiguang.notes.platform.application.MediaMaintenance maintenance;

  @org.springframework.beans.factory.annotation.Value("${app.media-root}")
  String mediaRoot;

  @Test
  @Order(14)
  void cleanupRemovesOnlyUnboundExpiredMedia() throws Exception {
    long owner =
        Long.parseLong(
            database.one("SELECT id FROM t_user WHERE username='demo_user'").get("id").toString());
    long id =
        database.insert(
            "t_media",
            Map.of(
                "owner_id",
                owner,
                "storage_key",
                "cleanup-test.png",
                "purpose",
                "NOTE",
                "mime",
                "image/png",
                "size_bytes",
                10,
                "width",
                1,
                "height",
                1,
                "sha256",
                "b".repeat(64)));
    database.update(
        "UPDATE t_media SET created_at=DATE_SUB(UTC_TIMESTAMP(),INTERVAL 25 HOUR) WHERE id=?", id);
    var file = java.nio.file.Path.of(mediaRoot).resolve("cleanup-test.png");
    java.nio.file.Files.createDirectories(file.getParent());
    java.nio.file.Files.write(file, new byte[] {1, 2});
    maintenance.cleanup();
    assertEquals(0, database.count("SELECT COUNT(*) FROM t_media WHERE id=?", id));
    assertFalse(java.nio.file.Files.exists(file));
    assertTrue(database.count("SELECT COUNT(*) FROM t_media WHERE status='BOUND'") > 0);
  }

  @Test
  @Order(15)
  void rolledBackBusinessTransactionDoesNotLeakOutboxEvents() {
    transactions.execute(
        s -> {
          events.append("RollbackTest", 1, "USER", 1, 0, Map.of(), "notification");
          s.setRollbackOnly();
          return null;
        });
    assertEquals(
        0,
        database.count(
            "SELECT COUNT(*) FROM t_outbox WHERE routing_key='RollbackTest.notification'"));
  }

  @org.springframework.beans.factory.annotation.Autowired
  org.springframework.data.redis.core.StringRedisTemplate redis;

  @BeforeEach
  void clearRequestQuotas() {
    var keys = redis.keys("sg:limit:*");
    if (keys != null && !keys.isEmpty()) redis.delete(keys);
  }

  @org.springframework.beans.factory.annotation.Autowired
  cn.shiguang.notes.platform.persistence.Database database;

  @org.springframework.beans.factory.annotation.Autowired
  org.springframework.transaction.support.TransactionTemplate transactions;

  @org.springframework.beans.factory.annotation.Autowired
  cn.shiguang.notes.shared.contract.EventPort events;

  @Container
  static final MySQLContainer<?> MYSQL =
      new MySQLContainer<>("mysql:8.4.13").withDatabaseName("shiguang");

  @Container
  static final RabbitMQContainer RABBIT =
      new RabbitMQContainer("rabbitmq:4.4.0-management")
          .withAdminUser("sg_test")
          .withAdminPassword("IntegrationBroker123")
          .withEnv("RABBITMQ_DEFAULT_VHOST", "shiguang");

  @Container
  static final GenericContainer<?> REDIS =
      new GenericContainer<>("redis:8.2.11").withExposedPorts(6379);

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry r) {
    r.add("spring.datasource.url", MYSQL::getJdbcUrl);
    r.add("spring.datasource.username", MYSQL::getUsername);
    r.add("spring.datasource.password", MYSQL::getPassword);
    r.add("spring.rabbitmq.host", RABBIT::getHost);
    r.add("spring.rabbitmq.port", RABBIT::getAmqpPort);
    r.add("spring.rabbitmq.username", () -> "sg_test");
    r.add("spring.rabbitmq.password", () -> "IntegrationBroker123");
    r.add("spring.data.redis.host", REDIS::getHost);
    r.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    r.add("app.cursor-key", () -> "integration-test-cursor-key-32-bytes-minimum");
    r.add("app.seed-demo", () -> true);
    r.add("app.demo-password", () -> "IntegrationPass123!");
    r.add("app.media-root", () -> "target/test-media");
    r.add("management.server.port", () -> 0);
  }

  @LocalServerPort int port;
  final ObjectMapper json = new ObjectMapper();
  HttpClient client;
  String csrf;

  @BeforeEach
  void prepare() {
    client = HttpClient.newBuilder().cookieHandler(new CookieManager()).build();
  }

  @AfterAll
  static void stopJobs(org.springframework.context.ConfigurableApplicationContext context) {
    context
        .getBean(
            org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor.class)
        .destroy();
    context
        .getBeanProvider(
            org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry.class)
        .ifAvailable(org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry::stop);
  }

  Map<?, ?> call(String method, String path, Object body, int status) throws Exception {
    var b = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1" + path));
    if (csrf != null) b.header("X-CSRF-TOKEN", csrf);
    b.header("Idempotency-Key", UUID.randomUUID().toString());
    if (body != null) b.header("Content-Type", "application/json");
    b.method(
        method,
        body == null
            ? HttpRequest.BodyPublishers.noBody()
            : HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)));
    var response = client.send(b.build(), HttpResponse.BodyHandlers.ofString());
    assertEquals(status, response.statusCode(), response.body());
    return json.readValue(response.body(), Map.class);
  }

  void login(String user) throws Exception {
    client = HttpClient.newBuilder().cookieHandler(new CookieManager()).build();
    csrf = null;
    csrf = (String) ((Map<?, ?>) call("GET", "/auth/csrf", null, 200).get("data")).get("token");
    call("POST", "/auth/login", Map.of("username", user, "password", "IntegrationPass123!"), 200);
    csrf = (String) ((Map<?, ?>) call("GET", "/auth/csrf", null, 200).get("data")).get("token");
  }

  @Test
  @Order(1)
  void publicAuthenticationBootstrapWorks() throws Exception {
    assertNotNull(call("GET", "/auth/csrf", null, 200).get("data"));
  }

  @Test
  @Order(2)
  void privateDataCannotBeReadWithoutLogin() throws Exception {
    call("GET", "/folders", null, 401);
  }

  @Test
  @Order(3)
  void ordinaryUserCannotManageAccounts() throws Exception {
    login("demo_user");
    call("PUT", "/admin/users/1/status", Map.of("status", "BANNED", "reason", "越权测试"), 403);
  }

  @Test
  @Order(4)
  void logoutRevokesExistingSession() throws Exception {
    login("demo_user");
    call("POST", "/auth/logout", null, 200);
    call("GET", "/me", null, 401);
  }

  @Test
  @Order(5)
  void privateFolderPersistsAndIsNotReadableByOtherUser() throws Exception {
    login("demo_user");
    var id =
        ((Map<?, ?>) call("POST", "/folders", Map.of("name", "旅行计划"), 201).get("data")).get("id");
    assertNotNull(id);
    call("POST", "/auth/logout", null, 200);
    login("demo_reviewer");
    call("GET", "/folders/" + id + "/notes", null, 404);
  }

  @Test
  @Order(6)
  void writeWithoutCsrfIsRejected() throws Exception {
    login("demo_user");
    csrf = null;
    call("POST", "/folders", Map.of("name", "错误请求"), 403);
  }

  @Test
  @Order(7)
  void createPublishInteractAndPracticeWithRealPersistence() throws Exception {
    login("demo_user");
    var image = new java.awt.image.BufferedImage(20, 20, java.awt.image.BufferedImage.TYPE_INT_RGB);
    var bytes = new java.io.ByteArrayOutputStream();
    javax.imageio.ImageIO.write(image, "png", bytes);
    String boundary = "test-boundary";
    var upload = new java.io.ByteArrayOutputStream();
    upload.write(
        ("--"
                + boundary
                + "\r\nContent-Disposition: form-data; name=\"purpose\"\r\n\r\nNOTE\r\n--"
                + boundary
                + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\"photo.png\"\r\n"
                + "Content-Type: image/png\r\n\r\n")
            .getBytes());
    upload.write(bytes.toByteArray());
    upload.write(("\r\n--" + boundary + "--\r\n").getBytes());
    var response =
        client.send(
            HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/media"))
                .header("X-CSRF-TOKEN", csrf)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(upload.toByteArray()))
                .build(),
            HttpResponse.BodyHandlers.ofString());
    assertEquals(200, response.statusCode(), response.body());
    String media =
        ((Map<?, ?>) json.readValue(response.body(), Map.class).get("data")).get("id").toString();
    var topics = (Map<?, ?>) call("GET", "/topics", null, 200).get("data");
    String topic = ((Map<?, ?>) ((List<?>) topics.get("items")).getFirst()).get("id").toString();
    var note =
        (Map<?, ?>)
            call(
                    "POST",
                    "/notes",
                    Map.of(
                        "title",
                        "真实集成测试笔记",
                        "body",
                        "明确标注为测试内容",
                        "mediaIds",
                        List.of(media),
                        "topicIds",
                        List.of(topic)),
                    200)
                .get("data");
    String id = note.get("id").toString();
    call("GET", "/notes/" + id, null, 404);
    var pending =
        (Map<?, ?>)
            call("POST", "/notes/" + id + "/submit", Map.of("version", note.get("version")), 200)
                .get("data");
    var userClient = client;
    String userCsrf = csrf;
    login("demo_reviewer");
    var queue = (Map<?, ?>) call("GET", "/reviews", null, 200).get("data");
    var review =
        (Map<?, ?>)
            ((List<?>) queue.get("items"))
                .stream()
                    .filter(x -> id.equals(((Map<?, ?>) x).get("targetId")))
                    .findFirst()
                    .orElseThrow();
    call("GET", "/reviews/" + review.get("id"), null, 200);
    call(
        "POST",
        "/reviews/" + review.get("id") + "/decision",
        Map.of("version", review.get("version"), "decision", "APPROVE"),
        200);
    client = userClient;
    csrf = userCsrf;
    var publicNote = (Map<?, ?>) call("GET", "/notes/" + id, null, 200).get("data");
    assertFalse(publicNote.containsKey("version"));
    call("PUT", "/notes/" + id + "/like", null, 200);
    call("PUT", "/notes/" + id + "/like", null, 200);
    call("POST", "/notes/" + id + "/comments", Map.of("body", "一次真实评论"), 201);
    String folder =
        ((Map<?, ?>) call("POST", "/folders", Map.of("name", "测试实践"), 201).get("data"))
            .get("id")
            .toString();
    call("PUT", "/folders/" + folder + "/notes/" + id, null, 200);
    var practice = (Map<?, ?>) call("POST", "/practices", Map.of("noteId", id), 200).get("data");
    String pid = practice.get("id").toString();
    call(
        "POST",
        "/practices/" + pid + "/transition",
        Map.of("version", practice.get("version"), "status", "DONE"),
        409);
    var doing =
        (Map<?, ?>)
            call(
                    "POST",
                    "/practices/" + pid + "/transition",
                    Map.of("version", practice.get("version"), "status", "DOING"),
                    200)
                .get("data");
    var done =
        (Map<?, ?>)
            call(
                    "POST",
                    "/practices/" + pid + "/transition",
                    Map.of("version", doing.get("version"), "status", "DONE"),
                    200)
                .get("data");
    call(
        "PUT",
        "/practices/" + pid + "/result",
        Map.of(
            "version",
            done.get("version"),
            "resultText",
            "完成后的真实反馈",
            "helpfulness",
            "HELPFUL",
            "mediaIds",
            List.of()),
        200);
    call("GET", "/statistics/me", null, 200);
  }

  @Test
  @Order(8)
  void committedOutboxEventuallyCreatesExactlyOneNotification() throws Exception {
    long recipient =
        Long.parseLong(
            database.one("SELECT id FROM t_user WHERE username='demo_user'").get("id").toString());
    long sender =
        Long.parseLong(
            database
                .one("SELECT id FROM t_user WHERE username='demo_reviewer'")
                .get("id")
                .toString());
    transactions.execute(
        s -> {
          events.append(
              "TestNotice",
              sender,
              "USER",
              sender,
              0,
              Map.of("recipientId", Long.toString(recipient), "notificationType", "FOLLOW"),
              "notification");
          return null;
        });
    org.awaitility.Awaitility.await()
        .atMost(java.time.Duration.ofSeconds(12))
        .untilAsserted(
            () ->
                assertEquals(
                    1,
                    database.count(
                        "SELECT COUNT(*) FROM t_notification WHERE recipient_id=? AND"
                            + " type='FOLLOW'",
                        recipient)));
    assertEquals(
        1,
        database.count(
            "SELECT COUNT(*) FROM t_inbox WHERE consumer='notification' AND event_id IN (SELECT"
                + " event_id FROM t_outbox WHERE routing_key='TestNotice.notification')"));
  }

  @Test
  @Order(11)
  void unsupportedMethodHas405RatherThanInternalError() throws Exception {
    login("demo_user");
    call("GET", "/reports", null, 405);
  }

  @Test
  @Order(12)
  void successfulAuthenticatedRequestsRecordOneDailyActiveUser() throws Exception {
    login("demo_user");
    call("GET", "/me", null, 200);
    call("GET", "/me", null, 200);
    assertEquals(
        1,
        database.count(
            "SELECT COUNT(*) FROM t_daily_activity WHERE user_id=(SELECT id FROM t_user WHERE"
                + " username='demo_user')"));
  }

  @Test
  @Order(13)
  void trustedProxySeparatesClientRateQuotas() throws Exception {
    for (int i = 0; i < 121; i++) {
      var response =
          client.send(
              HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/topics"))
                  .header("X-Forwarded-For", "198.51.100.10")
                  .GET()
                  .build(),
              HttpResponse.BodyHandlers.discarding());
      assertEquals(i < 120 ? 200 : 429, response.statusCode());
    }
    var other =
        client.send(
            HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/topics"))
                .header("X-Forwarded-For", "198.51.100.11")
                .GET()
                .build(),
            HttpResponse.BodyHandlers.discarding());
    assertEquals(200, other.statusCode());
  }

  @Test
  @Order(10)
  void rankingOrdersByEffectiveLikesInsteadOfCreationOrder() throws Exception {
    login("demo_admin");
    long oldest =
        Long.parseLong(
            database
                .one("SELECT MIN(id) AS id FROM t_note WHERE status='PUBLISHED'")
                .get("id")
                .toString());
    database.update("UPDATE t_note_metric SET like_count=9999 WHERE note_id=?", oldest);
    var page = (Map<?, ?>) call("GET", "/admin/statistics/ranking", null, 200).get("data");
    assertEquals(
        Long.toString(oldest),
        ((Map<?, ?>) ((List<?>) page.get("items")).getFirst()).get("subjectId"));
  }

  @Test
  @Order(9)
  void foreignPrivateMediaCannotBecomeAnAvatar() throws Exception {
    long owner =
        Long.parseLong(
            database.one("SELECT id FROM t_user WHERE username='demo_user'").get("id").toString());
    long media =
        transactions.execute(
            s ->
                database.insert(
                    "t_media",
                    Map.of(
                        "owner_id",
                        owner,
                        "storage_key",
                        "private-avatar-security-test.png",
                        "purpose",
                        "NOTE",
                        "mime",
                        "image/png",
                        "size_bytes",
                        100,
                        "width",
                        10,
                        "height",
                        10,
                        "sha256",
                        "a".repeat(64))));
    login("demo_reviewer");
    call(
        "PUT",
        "/me/profile",
        Map.of("nickname", "内容审核员", "bio", "", "avatarMediaId", Long.toString(media)),
        403);
  }
}
