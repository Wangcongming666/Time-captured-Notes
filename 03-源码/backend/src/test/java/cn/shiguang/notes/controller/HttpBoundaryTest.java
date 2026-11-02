package cn.shiguang.notes.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cn.shiguang.notes.controller.advice.GlobalExceptionHandler;
import cn.shiguang.notes.converter.HttpBoundaryConverter;
import cn.shiguang.notes.dto.Actor;
import cn.shiguang.notes.dto.request.*;
import cn.shiguang.notes.security.*;
import cn.shiguang.notes.service.*;
import cn.shiguang.notes.service.application.*;
import cn.shiguang.notes.vo.*;
import com.fasterxml.jackson.databind.*;
import jakarta.validation.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * Contract tests exercise validation, public projections, route registration and credential
 * handling.
 */
class HttpBoundaryTest {
  private final ObjectMapper json = new ObjectMapper();
  private final HttpBoundaryConverter views = new HttpBoundaryConverter(json);
  private ValidatorFactory factory;
  private Validator validator;

  @BeforeEach
  void validation() {
    factory = Validation.buildDefaultValidatorFactory();
    validator = factory.getValidator();
  }

  @AfterEach
  void closeValidation() {
    factory.close();
  }

  @Test
  void statisticsProjectionsPreserveDecimalValuesAndTopicCounts() {
    var metric =
        views.view(
            Map.of(
                "day",
                "2026-11-02",
                "metric",
                "REVIEW_DURATION_MS",
                "value",
                new java.math.BigDecimal("1234.567")),
            MetricVO.class);
    assertEquals(new java.math.BigDecimal("1234.567"), metric.value());
    var rank =
        views.view(
            Map.of("subjectId", "1", "label", "旅行", "metric", "VIEW", "value", 5, "noteCount", 3),
            RankVO.class);
    assertEquals(3L, rank.noteCount());
  }

  @Test
  void draftAllowsEmptyTextButRequiresRealMediaAndTopicLists() throws Exception {
    var draft =
        json.readValue(
            "{\"mediaIds\":[],\"topicIds\":[],\"ignoredLegacyField\":true}",
            CreateNoteRequest.class);
    assertTrue(validator.validate(draft).isEmpty());
    assertFalse(validator.validate(new CreateNoteRequest(null, null, null, List.of())).isEmpty());
    assertTrue(
        validator
            .validate(new CreateNoteRequest("😀".repeat(40), "", List.of(), List.of()))
            .isEmpty());
    assertFalse(
        validator
            .validate(new CreateNoteRequest("😀".repeat(41), "", List.of(), List.of()))
            .isEmpty());
    assertFalse(
        validator.validate(new EditNoteRequest(null, "", "", List.of(), List.of())).isEmpty());
  }

  @Test
  void passwordUsesUtf8BytesAndApprovalDoesNotRequireReason() {
    assertTrue(validator.validate(new RegisterRequest("tester", "😀".repeat(18), "测试")).isEmpty());
    assertFalse(validator.validate(new RegisterRequest("tester", "😀".repeat(19), "测试")).isEmpty());
    assertTrue(validator.validate(new ReviewDecisionRequest(1, "APPROVE", null)).isEmpty());
    assertFalse(validator.validate(new ReviewDecisionRequest(1, "REJECT", " ")).isEmpty());
    assertTrue(validator.validate(new CommentRequest("😀".repeat(500), null)).isEmpty());
  }

  @Test
  void optimisticVersionRejectsDecimalsAndOverflowRatherThanRounding() throws Exception {
    assertEquals(3, json.readValue("{\"version\":\"3\"}", VersionRequest.class).version());
    assertThrows(Exception.class, () -> json.readValue("{\"version\":1.9}", VersionRequest.class));
    assertThrows(
        Exception.class, () -> json.readValue("{\"version\":2147483648}", VersionRequest.class));
  }

  @Test
  void projectionsPreserveBigIdsNullableContractAndRemoveSecrets() throws Exception {
    var raw = new LinkedHashMap<String, Object>();
    raw.put("id", Long.MAX_VALUE);
    raw.put("nickname", "测试");
    raw.put("bio", "");
    raw.put("avatarMediaId", null);
    raw.put("passwordHash", "secret-hash");
    raw.put("sessionToken", "secret-token");
    raw.put("csrfHash", "secret-csrf");
    raw.put("authVersion", 99);
    JsonNode user = json.valueToTree(views.view(raw, UserVO.class));
    assertEquals(Long.toString(Long.MAX_VALUE), user.path("id").textValue());
    assertTrue(user.has("avatarMediaId"));
    assertTrue(user.path("avatarMediaId").isNull());
    assertFalse(user.has("roles"));
    assertFalse(user.has("status"));
    assertEquals(Set.of("id", "nickname", "bio", "avatarMediaId"), fieldNames(user));
    var note = new LinkedHashMap<String, Object>();
    note.put("id", "1");
    note.put("authorId", "2");
    note.put("reason", null);
    note.put("publishedAt", "2026-11-01T00:00:00Z");
    JsonNode owner = json.valueToTree(views.view(note, OwnedNoteVO.class));
    assertTrue(owner.has("reason"));
    assertTrue(owner.path("reason").isNull());
    assertEquals("2026-11-01T00:00:00Z", owner.path("publishedAt").textValue());
    JsonNode publicNote = json.valueToTree(views.view(note, NoteVO.class));
    assertFalse(publicNote.has("reason"));
    assertFalse(publicNote.has("status"));
    JsonNode page =
        json.valueToTree(views.page(Map.of("items", List.of(raw), "hasMore", false), UserVO.class));
    assertTrue(page.has("nextCursor"));
    assertTrue(page.path("nextCursor").isNull());
    assertFalse(page.path("items").get(0).has("passwordHash"));
    assertEquals("{}", json.writeValueAsString(new EmptyVO()));
  }

  @Test
  void invalidBodyReturns400AndValidBodyPreservesActorAndIdempotencyHeader() throws Exception {
    var write = mock(ContentApplicationService.class);
    var actor = new Actor(7, Set.of("USER"));
    when(write.createNote(eq(actor), any(CreateNoteRequest.class), eq("retry-key")))
        .thenReturn(new VersionVO("19", 1, "DRAFT"));
    var mvc = mvc(new ContentController(write));
    mvc.perform(post("/api/v1/notes").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    verifyNoInteractions(write);
    mvc.perform(
            post("/api/v1/notes")
                .requestAttr(RequestSecurityFilter.ACTOR, actor)
                .header("Idempotency-Key", "retry-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"mediaIds\":[],\"topicIds\":[],\"legacyExtra\":1}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.id").value("19"));
    verify(write)
        .createNote(
            eq(actor),
            eq(new CreateNoteRequest(null, null, List.of(), List.of())),
            eq("retry-key"));
  }

  @Test
  void loginTokenOnlyAppearsInHttpOnlyCookie() throws Exception {
    var accounts = mock(AccountService.class);
    var profile = new LinkedHashMap<String, Object>();
    profile.put("id", "3");
    profile.put("nickname", "测试");
    profile.put("bio", "");
    profile.put("sessionToken", "cookie-secret");
    profile.put("passwordHash", "must-not-leak");
    when(accounts.login(new LoginRequest("tester", "password")))
        .thenReturn(new LoginSession(views.view(profile, UserVO.class), "cookie-secret"));
    var mvc =
        mvc(
            new IdentityController(
                accounts, mock(RequestSecurityFilter.class), mock(RateGuard.class), true));
    mvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"tester\",\"password\":\"password\"}"))
        .andExpect(status().isOk())
        .andExpect(cookie().value("SG_SESSION", "cookie-secret"))
        .andExpect(cookie().httpOnly("SG_SESSION", true))
        .andExpect(cookie().secure("SG_SESSION", true))
        .andExpect(jsonPath("$.data.sessionToken").doesNotExist())
        .andExpect(jsonPath("$.data.passwordHash").doesNotExist());
    assertFalse(
        json.valueToTree(new LoginSession(views.view(profile, UserVO.class), "cookie-secret"))
            .has("token"));
  }

  @Test
  void allEightyOneRoutesRegisterWithoutOverlapAndKeepCreatedStatus() throws Exception {
    var social = mock(SocialApplicationService.class);
    Object[] controllers = {
      new ContentController(mock(ContentApplicationService.class)),
      new MediaController(mock(MediaApplicationService.class)),
      new DiscoveryController(mock(DiscoveryApplicationService.class)),
      new SocialController(social),
      new CollectionController(mock(CollectionApplicationService.class)),
      new PracticeController(mock(PracticeApplicationService.class)),
      new GovernanceController(mock(GovernanceApplicationService.class)),
      new AdministrationController(mock(AdministrationApplicationService.class)),
      new NotificationController(mock(NotificationApplicationService.class)),
      new StatisticsController(mock(StatisticsApplicationService.class)),
      new IdentityController(
          mock(AccountService.class),
          mock(RequestSecurityFilter.class),
          mock(RateGuard.class),
          false)
    };
    var context = new StaticWebApplicationContext();
    context.setServletContext(new MockServletContext());
    for (int i = 0; i < controllers.length; i++)
      context.getBeanFactory().registerSingleton("controller" + i, controllers[i]);
    var mapping = new RequestMappingHandlerMapping();
    mapping.setApplicationContext(context);
    assertDoesNotThrow(mapping::afterPropertiesSet);
    assertEquals(81, mapping.getHandlerMethods().size());
    var routes = new HashSet<String>();
    mapping
        .getHandlerMethods()
        .forEach(
            (info, handler) -> {
              info.getPatternValues()
                  .forEach(
                      path ->
                          info.getMethodsCondition()
                              .getMethods()
                              .forEach(method -> assertTrue(routes.add(method + " " + path))));
              assertFalse(handler.getMethod().getGenericReturnType().getTypeName().contains("<?>"));
              for (var parameter : handler.getMethod().getParameters()) {
                if (parameter.isAnnotationPresent(
                    org.springframework.web.bind.annotation.RequestBody.class)) {
                  assertTrue(parameter.isAnnotationPresent(Valid.class));
                  assertTrue(parameter.getType().getPackageName().endsWith("dto.request"));
                }
              }
            });
    assertEquals(
        Set.of(
            "DELETE /api/v1/comments/{id}",
            "DELETE /api/v1/folders/{folder}/notes/{note}",
            "DELETE /api/v1/folders/{id}",
            "DELETE /api/v1/me/hidden-notes/{id}",
            "DELETE /api/v1/me/preferences",
            "DELETE /api/v1/notes/{id}",
            "DELETE /api/v1/notes/{id}/like",
            "DELETE /api/v1/users/{id}/follow",
            "GET /api/v1/admin/audits",
            "GET /api/v1/admin/reports",
            "GET /api/v1/admin/statistics/governance",
            "GET /api/v1/admin/statistics/platform",
            "GET /api/v1/admin/statistics/ranking",
            "GET /api/v1/admin/topics",
            "GET /api/v1/admin/users",
            "GET /api/v1/auth/csrf",
            "GET /api/v1/comments/{id}/replies",
            "GET /api/v1/feed",
            "GET /api/v1/folders",
            "GET /api/v1/folders/{id}/notes",
            "GET /api/v1/me",
            "GET /api/v1/me/notes",
            "GET /api/v1/me/notes/{id}",
            "GET /api/v1/me/preferences",
            "GET /api/v1/me/reports",
            "GET /api/v1/media/capabilities",
            "GET /api/v1/media/{id}/content",
            "GET /api/v1/media/{id}/poster",
            "GET /api/v1/notes/{id}",
            "GET /api/v1/notes/{id}/comments",
            "GET /api/v1/notes/{id}/feedback",
            "GET /api/v1/notifications",
            "GET /api/v1/notifications/unread",
            "GET /api/v1/practices",
            "GET /api/v1/practices/{id}",
            "GET /api/v1/practices/{id}/history",
            "GET /api/v1/reviews",
            "GET /api/v1/reviews/{id}",
            "GET /api/v1/search",
            "GET /api/v1/statistics/me",
            "GET /api/v1/statistics/me/ranking",
            "GET /api/v1/topics",
            "GET /api/v1/users/{id}",
            "GET /api/v1/users/{id}/followers",
            "GET /api/v1/users/{id}/following",
            "GET /api/v1/users/{id}/summary",
            "POST /api/v1/admin/comments/{id}/hide",
            "POST /api/v1/admin/notes/{id}/offline",
            "POST /api/v1/admin/practices/{id}/offline",
            "POST /api/v1/admin/reports/{id}/decision",
            "POST /api/v1/admin/topics",
            "POST /api/v1/auth/login",
            "POST /api/v1/auth/logout",
            "POST /api/v1/auth/register",
            "POST /api/v1/folders",
            "POST /api/v1/media",
            "POST /api/v1/media/videos",
            "POST /api/v1/notes",
            "POST /api/v1/notes/{id}/comments",
            "POST /api/v1/notes/{id}/submit",
            "POST /api/v1/notes/{id}/withdraw",
            "POST /api/v1/notifications/read-all",
            "POST /api/v1/practices",
            "POST /api/v1/practices/{id}/publish-request",
            "POST /api/v1/practices/{id}/transition",
            "POST /api/v1/practices/{id}/unpublish",
            "POST /api/v1/reports",
            "POST /api/v1/reviews/{id}/decision",
            "PUT /api/v1/admin/topics/{id}",
            "PUT /api/v1/admin/users/{id}/roles",
            "PUT /api/v1/admin/users/{id}/status",
            "PUT /api/v1/folders/{folder}/notes/{note}",
            "PUT /api/v1/folders/{id}",
            "PUT /api/v1/me/hidden-notes/{id}",
            "PUT /api/v1/me/preferences",
            "PUT /api/v1/me/profile",
            "PUT /api/v1/notes/{id}",
            "PUT /api/v1/notes/{id}/like",
            "PUT /api/v1/notifications/{id}/read",
            "PUT /api/v1/practices/{id}/result",
            "PUT /api/v1/users/{id}/follow"),
        routes);
    assertTrue(routes.contains("GET /api/v1/admin/statistics/governance"));
    assertTrue(routes.contains("GET /api/v1/media/{id}/content"));
    assertTrue(routes.contains("POST /api/v1/practices/{id}/publish-request"));
    when(social.comment(any(), eq(5L), any(CommentRequest.class), isNull()))
        .thenReturn(new IdVO("6"));
    mvc(controllers)
        .perform(
            post("/api/v1/notes/5/comments")
                .requestAttr(RequestSecurityFilter.ACTOR, Actor.guest())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"body\":\"评论\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.data.id").value("6"));
    context.close();
  }

  @Test
  void discoveryBindsNamedQueryForTypedApplicationService() throws Exception {
    var query = mock(DiscoveryApplicationService.class);
    var actor = new Actor(8, Set.of("USER"));
    when(query.search(eq(actor), any(FeedQueryRequest.class)))
        .thenReturn(new PageVO<>(List.of(), null, false));
    mvc(new DiscoveryController(query))
        .perform(
            get("/api/v1/search")
                .requestAttr(RequestSecurityFilter.ACTOR, actor)
                .param("q", "咖啡")
                .param("size", "12")
                .param("mode", "discover")
                .param("authorId", "9223372036854775807"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.items").isEmpty())
        .andExpect(jsonPath("$.data.hasMore").value(false));
    verify(query)
        .search(
            actor,
            new FeedQueryRequest(
                "discover", null, "12", null, "咖啡", null, "9223372036854775807", null));
  }

  @Test
  void mediaHeadPreservesRangeHeadersActorAndStreamingStatus() throws Exception {
    var media = mock(MediaApplicationService.class);
    var actor = new Actor(9, Set.of("USER"));
    when(media.stream(actor, 17, false, "bytes=3-7", "etag", true))
        .thenReturn(org.springframework.http.ResponseEntity.status(206).build());
    mvc(new MediaController(media))
        .perform(
            head("/api/v1/media/17/content")
                .requestAttr(RequestSecurityFilter.ACTOR, actor)
                .header("Range", "bytes=3-7")
                .header("If-Range", "etag"))
        .andExpect(status().isPartialContent());
    verify(media).stream(actor, 17, false, "bytes=3-7", "etag", true);
  }

  private MockMvc mvc(Object... controllers) {
    return MockMvcBuilders.standaloneSetup(controllers)
        .setControllerAdvice(new GlobalExceptionHandler())
        .setMessageConverters(new MappingJackson2HttpMessageConverter(json))
        .build();
  }

  private Set<String> fieldNames(JsonNode node) {
    var names = new HashSet<String>();
    node.fieldNames().forEachRemaining(names::add);
    return names;
  }
}
