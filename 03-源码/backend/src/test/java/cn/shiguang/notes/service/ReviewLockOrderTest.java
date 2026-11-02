package cn.shiguang.notes.service;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import cn.shiguang.notes.common.contract.EventPort;
import cn.shiguang.notes.common.util.Data;
import cn.shiguang.notes.dto.Actor;
import cn.shiguang.notes.repository.query.PublicQueryRepository;
import cn.shiguang.notes.service.support.Commands;
import java.util.*;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

class ReviewLockOrderTest {
  @Test
  void feedbackReviewLocksSourceThenPracticeBeforeReviewRow() {
    var content = mock(ContentService.class);
    var collection = mock(CollectionService.class);
    var governance = mock(GovernanceService.class);
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
        new CommunityService(
            mock(IdentityService.class),
            content,
            collection,
            mock(SocialService.class),
            governance,
            mock(DiscoveryService.class),
            mock(NotificationService.class),
            mock(StatisticsService.class),
            mock(EventPort.class),
            commands,
            mock(PublicQueryRepository.class));
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
