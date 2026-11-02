package cn.shiguang.notes.service.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import cn.shiguang.notes.common.contract.EventPort;
import cn.shiguang.notes.common.util.Data;
import cn.shiguang.notes.converter.HttpBoundaryConverter;
import cn.shiguang.notes.dto.Actor;
import cn.shiguang.notes.dto.request.*;
import cn.shiguang.notes.repository.query.PublicQueryRepository;
import cn.shiguang.notes.service.*;
import cn.shiguang.notes.service.support.*;
import cn.shiguang.notes.vo.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

class ApplicationServiceContractTest {
  private final ObjectMapper json = new ObjectMapper();
  private final HttpBoundaryConverter views = new HttpBoundaryConverter(json);

  @Test
  void createNoteOwnsCommandPayloadReviewAndEventOrchestration() {
    var content = mock(ContentService.class);
    var governance = mock(GovernanceService.class);
    var events = mock(EventPort.class);
    var commands = executingCommands();
    var actor = new Actor(7, Set.of("USER"));
    var request = new CreateNoteRequest("标题", "正文", List.of("23"), List.of("24"));
    var input =
        Map.<String, Object>of(
            "title", "标题", "body", "正文", "mediaIds", List.of("23"), "topicIds", List.of("24"));
    when(content.save(actor, null, input))
        .thenReturn(
            Data.map(
                "id", "19", "authorId", "7", "version", 1, "revision", 3, "status", "PENDING"));
    var use =
        new ContentApplicationService(
            content,
            governance,
            mock(StatisticsService.class),
            events,
            commands,
            mock(Pages.class),
            mock(PublicQueryRepository.class),
            views);
    assertEquals(
        new VersionVO("19", 1, "PENDING"), use.createNote(actor, request, "repeat-command-key"));
    verify(commands)
        .run(eq(actor), eq("note:save:null"), eq("repeat-command-key"), eq(input), any());
    var order = inOrder(content, governance, events);
    order.verify(content).save(actor, null, input);
    order.verify(governance).cancelReviews("NOTE", 19);
    order.verify(governance).openReview("NOTE", 19, 3, 7);
    order
        .verify(events)
        .append(
            "NoteVisibilityChanged",
            7,
            "NOTE",
            19,
            0,
            Data.map("noteId", "19", "authorId", "7"),
            "statistics",
            "cache");
  }

  @Test
  void commandReplayStillProjectsStoredSnapshotWithoutRepeatingDomainEffects() {
    var content = mock(ContentService.class);
    var governance = mock(GovernanceService.class);
    var events = mock(EventPort.class);
    var commands = mock(Commands.class);
    var actor = new Actor(7, Set.of("USER"));
    when(commands.run(
            eq(actor),
            eq("note:submit:19"),
            eq("repeat-command-key"),
            eq(Map.of("version", 3)),
            any()))
        .thenReturn(Map.of("id", "19", "version", 4, "status", "PENDING"));
    var use =
        new ContentApplicationService(
            content,
            governance,
            mock(StatisticsService.class),
            events,
            commands,
            mock(Pages.class),
            mock(PublicQueryRepository.class),
            views);
    assertEquals(
        new VersionVO("19", 4, "PENDING"),
        use.submit(actor, 19, new VersionRequest(3), "repeat-command-key"));
    verifyNoInteractions(content, governance, events);
  }

  @Test
  void searchKeepsCanonicalQueryValuesAndOverridesOnlyMode() {
    var discovery = mock(DiscoveryService.class);
    when(discovery.feed(any(), anyMap()))
        .thenReturn(Data.map("items", List.of(), "nextCursor", null, "hasMore", false));
    var use =
        new DiscoveryApplicationService(
            mock(ContentService.class),
            mock(GovernanceService.class),
            discovery,
            mock(Pages.class),
            views);
    var actor = new Actor(8, Set.of("USER"));
    var request =
        new FeedQueryRequest(
            "discover", "cursor", "12", "饮食", "咖啡", null, "9223372036854775807", "hot");
    assertEquals(new PageVO<CardVO>(List.of(), null, false), use.search(actor, request));
    verify(discovery)
        .feed(
            actor,
            Map.of(
                "mode",
                "search",
                "cursor",
                "cursor",
                "size",
                "12",
                "category",
                "饮食",
                "q",
                "咖啡",
                "authorId",
                "9223372036854775807",
                "sort",
                "hot"));
  }

  @Test
  void accountLoginProducesTypedUserAndNonSerializableCredential() {
    var identity = mock(IdentityService.class);
    when(identity.verify("tester", "password")).thenReturn(7L);
    when(identity.session(7)).thenReturn("session-secret");
    when(identity.profile(7, true))
        .thenReturn(
            Data.map(
                "id",
                "7",
                "nickname",
                "测试",
                "bio",
                "",
                "avatarMediaId",
                null,
                "roles",
                List.of("USER"),
                "status",
                "ACTIVE",
                "passwordHash",
                "must-not-leak"));
    var use =
        new AccountService(
            identity,
            views,
            mock(ContentService.class),
            mock(GovernanceService.class),
            mock(EventPort.class),
            mock(PublicQueryRepository.class),
            mock(StatisticsService.class));
    var result = use.login(new LoginRequest("tester", "password"));
    assertEquals("session-secret", result.token());
    assertEquals("7", result.user().id());
    var encoded = json.valueToTree(result);
    assertFalse(encoded.has("token"));
    assertFalse(encoded.path("user").has("passwordHash"));
    var order = inOrder(identity);
    order.verify(identity).verify("tester", "password");
    order.verify(identity).session(7);
    order.verify(identity).profile(7, true);
  }

  @Test
  void publicApplicationSignaturesContainNoMapOrUntypedObjectAndTransactionsStayOnEntrypoints() {
    Map<Class<?>, Set<String>> writes =
        Map.of(
            ContentApplicationService.class,
                Set.of("createNote", "editNote", "submit", "withdraw", "deleteNote", "note"),
            SocialApplicationService.class, Set.of("like", "comment", "deleteComment", "follow"),
            CollectionApplicationService.class,
                Set.of("createFolder", "renameFolder", "deleteFolder", "favorite"),
            PracticeApplicationService.class,
                Set.of(
                    "createPractice",
                    "transition",
                    "result",
                    "publishFeedback",
                    "unpublishFeedback"),
            GovernanceApplicationService.class,
                Set.of(
                    "decideReview",
                    "offlineNote",
                    "hideComment",
                    "offlineFeedback",
                    "report",
                    "decideReport"),
            AdministrationApplicationService.class, Set.of("createTopic", "updateTopic"),
            DiscoveryApplicationService.class, Set.of("preferences", "hide", "resetPreferences"),
            NotificationApplicationService.class, Set.of("readNotification", "readAll"),
            StatisticsApplicationService.class, Set.of());
    writes.forEach(
        (type, transactionalNames) -> {
          for (var method : type.getDeclaredMethods()) {
            if (!Modifier.isPublic(method.getModifiers())) continue;
            assertNotEquals(Object.class, method.getReturnType(), method.toString());
            assertFalse(Map.class.isAssignableFrom(method.getReturnType()), method.toString());
            for (var parameter : method.getParameterTypes())
              assertFalse(Map.class.isAssignableFrom(parameter), method.toString());
            boolean mutation = transactionalNames.contains(method.getName());
            if (type == DiscoveryApplicationService.class
                && method.getName().equals("preferences")
                && method.getParameterCount() == 1) mutation = false;
            if (mutation)
              assertTrue(method.isAnnotationPresent(Transactional.class), method.toString());
          }
        });
  }

  private Commands executingCommands() {
    var commands = mock(Commands.class);
    when(commands.run(any(), anyString(), anyString(), anyMap(), any()))
        .thenAnswer(invocation -> ((Supplier<?>) invocation.getArgument(4)).get());
    return commands;
  }
}
