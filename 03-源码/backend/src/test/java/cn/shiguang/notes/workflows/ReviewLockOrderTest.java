package cn.shiguang.notes.workflows;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import cn.shiguang.notes.modules.collection.api.CollectionApi;
import cn.shiguang.notes.modules.content.api.ContentApi;
import cn.shiguang.notes.modules.discovery.api.DiscoveryApi;
import cn.shiguang.notes.modules.governance.api.GovernanceApi;
import cn.shiguang.notes.modules.identity.api.IdentityApi;
import cn.shiguang.notes.modules.notification.api.NotificationApi;
import cn.shiguang.notes.modules.social.api.SocialApi;
import cn.shiguang.notes.modules.statistics.api.StatisticsApi;
import cn.shiguang.notes.platform.application.Commands;
import cn.shiguang.notes.platform.readmodel.PublicReadModel;
import cn.shiguang.notes.shared.contract.*;
import java.util.*;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

class ReviewLockOrderTest {
  @Test
  void feedbackReviewLocksSourceThenPracticeBeforeReviewRow() {
    var content = mock(ContentApi.class);
    var collection = mock(CollectionApi.class);
    var governance = mock(GovernanceApi.class);
    var commands = mock(Commands.class);
    when(commands.run(any(), anyString(), anyString(), anyMap(), any()))
        .thenAnswer(i -> ((Supplier<?>) i.getArgument(4)).get());
    when(governance.review(4, false))
        .thenReturn(
            Data.map(
                "id",
                "4",
                "targetType",
                "PRACTICE",
                "targetId",
                "2",
                "targetRevision",
                1,
                "applicantId",
                "3",
                "version",
                0,
                "status",
                "PENDING"));
    when(collection.practice(eq(2L), anyBoolean()))
        .thenReturn(Data.map("noteId", "1", "sourceRevision", 1));
    when(content.requirePublic(1, true))
        .thenReturn(Data.map("id", "1", "authorId", "3", "revision", 1));
    var use =
        new CommunityUseCases(
            mock(IdentityApi.class),
            content,
            collection,
            mock(SocialApi.class),
            governance,
            mock(DiscoveryApi.class),
            mock(NotificationApi.class),
            mock(StatisticsApi.class),
            mock(EventPort.class),
            commands,
            mock(PublicReadModel.class));
    use.decideReview(
        new Actor(5, Set.of("REVIEWER")),
        4,
        Data.map("version", 0, "decision", "APPROVE"),
        "integration-review-key");
    var order = inOrder(content, collection, governance);
    order.verify(content).requirePublic(1, true);
    order.verify(collection).practice(2, true);
    order.verify(governance).decide(any(), eq(4L), eq(0), eq("APPROVE"), anyString());
  }
}
