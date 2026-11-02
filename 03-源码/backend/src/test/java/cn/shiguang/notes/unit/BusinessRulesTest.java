package cn.shiguang.notes.unit;

import static org.junit.jupiter.api.Assertions.*;

import cn.shiguang.notes.common.exception.BizException;
import cn.shiguang.notes.validation.IdentityRules;
import cn.shiguang.notes.validation.NoteRules;
import cn.shiguang.notes.validation.PracticeRules;
import cn.shiguang.notes.validation.RecommendationPolicy;
import org.junit.jupiter.api.Test;

/** Literal business cases; catches illegal state, boundary, and ranking changes. */
class BusinessRulesTest {
  @Test
  void draftCannotBePublic() {
    assertFalse(NoteRules.isPublic("DRAFT"));
  }

  @Test
  void offlineCannotBePublic() {
    assertFalse(NoteRules.isPublic("OFFLINE"));
  }

  @Test
  void publishedIsPublic() {
    assertTrue(NoteRules.isPublic("PUBLISHED"));
  }

  @Test
  void emptySubmissionIsRejected() {
    assertThrows(BizException.class, () -> NoteRules.validateSubmission("", "正文", 1, 1));
  }

  @Test
  void fortyOneCodepointsAreRejected() {
    assertThrows(
        BizException.class, () -> NoteRules.validateSubmission("字".repeat(41), "正文", 1, 1));
  }

  @Test
  void emojiCountsAsOneCodepoint() {
    assertDoesNotThrow(() -> NoteRules.validateSubmission("🌿".repeat(40), "正文", 1, 1));
  }

  @Test
  void noteMustHaveMedia() {
    assertThrows(BizException.class, () -> NoteRules.validateSubmission("标题", "正文", 0, 1));
  }

  @Test
  void tooManyTopicsAreRejected() {
    assertThrows(BizException.class, () -> NoteRules.validateSubmission("标题", "正文", 1, 6));
  }

  @Test
  void pendingNoteCannotBeEditedDirectly() {
    assertThrows(BizException.class, () -> NoteRules.afterEdit("PENDING"));
  }

  @Test
  void publishedEditRequiresReview() {
    assertEquals("PENDING", NoteRules.afterEdit("PUBLISHED"));
  }

  @Test
  void deletedNoteCannotBeRevived() {
    assertThrows(BizException.class, () -> NoteRules.afterEdit("DELETED"));
  }

  @Test
  void plannedCannotSkipDoing() {
    assertThrows(BizException.class, () -> PracticeRules.transition("PLANNED", "DONE"));
  }

  @Test
  void doingCanFinish() {
    assertDoesNotThrow(() -> PracticeRules.transition("DOING", "DONE"));
  }

  @Test
  void doneCannotReopen() {
    assertThrows(BizException.class, () -> PracticeRules.transition("DONE", "DOING"));
  }

  @Test
  void plannedCanBeAbandoned() {
    assertDoesNotThrow(() -> PracticeRules.transition("PLANNED", "ABANDONED"));
  }

  @Test
  void usernameMustBeNormalizedAscii() {
    assertThrows(
        BizException.class,
        () -> IdentityRules.validateRegistration("A User", "password123", "昵称"));
  }

  @Test
  void bcryptMustNotSilentlyTruncateUtf8() {
    assertThrows(
        BizException.class,
        () -> IdentityRules.validateRegistration("valid_user", "密".repeat(25), "昵称"));
  }

  @Test
  void shortPasswordIsRejected() {
    assertThrows(
        BizException.class, () -> IdentityRules.validateRegistration("valid_user", "short", "昵称"));
  }

  @Test
  void matchingInterestRanksAbovePlainPopular() {
    assertTrue(
        RecommendationPolicy.score(true, false, 0.2, 0.5, false)
            > RecommendationPolicy.score(false, false, 1, 1, false));
  }

  @Test
  void reducedTopicIsDownranked() {
    assertEquals(10.0, RecommendationPolicy.score(true, true, 1, 1, true), 0.001);
  }
}
