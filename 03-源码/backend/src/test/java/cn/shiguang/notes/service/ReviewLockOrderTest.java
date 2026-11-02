package cn.shiguang.notes.service;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import cn.shiguang.notes.common.contract.EventPort;
import cn.shiguang.notes.common.util.Data;
import cn.shiguang.notes.converter.HttpBoundaryConverter;
import cn.shiguang.notes.dto.Actor;
import cn.shiguang.notes.dto.request.ReviewDecisionRequest;
import cn.shiguang.notes.repository.query.PublicQueryRepository;
import cn.shiguang.notes.service.application.GovernanceApplicationService;
import cn.shiguang.notes.service.support.Commands;
import cn.shiguang.notes.service.support.Pages;
import com.fasterxml.jackson.databind.ObjectMapper;
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
        new GovernanceApplicationService(
            content,
            collection,
            mock(SocialService.class),
            governance,
            mock(EventPort.class),
            commands,
            mock(Pages.class),
            mock(PublicQueryRepository.class),
            new HttpBoundaryConverter(new ObjectMapper()));
    use.decideReview(
        new Actor(5, Set.of("REVIEWER")),
        4,
        new ReviewDecisionRequest(0, "APPROVE", null),
        "integration-review-key");
    var order = inOrder(content, collection, governance);
    order.verify(content).requirePublic(1, true);
    order.verify(collection).practice(2, true);
    order.verify(governance).decide(any(), eq(4L), eq(0), eq("APPROVE"), anyString());
  }
}
