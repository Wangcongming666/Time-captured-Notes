package cn.shiguang.notes.controller;

import cn.shiguang.notes.dto.*;
import cn.shiguang.notes.dto.request.*;
import cn.shiguang.notes.service.application.PracticeApplicationService;
import cn.shiguang.notes.vo.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/** Practice HTTP boundary. */
@RestController
@RequestMapping("/api/v1")
public class PracticeController extends ControllerSupport {

  private final PracticeApplicationService use;

  public PracticeController(PracticeApplicationService use) {
    this.use = use;
  }

  @PostMapping("/practices")
  public ApiResponse<VersionVO> createPractice(
      @Valid @RequestBody CreatePracticeRequest body, HttpServletRequest r) {
    return ApiResponse.ok(use.createPractice(actor(r), body, key(r)));
  }

  @GetMapping("/practices")
  public ApiResponse<PageVO<PracticeVO>> practices(
      @RequestParam(required = false) String cursor,
      @RequestParam(defaultValue = "20") int size,
      HttpServletRequest r) {
    return ApiResponse.ok(use.practices(actor(r), cursor, size));
  }

  @GetMapping("/practices/{id}")
  public ApiResponse<PracticeVO> practice(@PathVariable long id, HttpServletRequest r) {
    return ApiResponse.ok(use.practice(actor(r), id));
  }

  @PostMapping("/practices/{id}/transition")
  public ApiResponse<VersionVO> transition(
      @PathVariable long id,
      @Valid @RequestBody PracticeTransitionRequest body,
      HttpServletRequest r) {
    return ApiResponse.ok(use.transition(actor(r), id, body, key(r)));
  }

  @GetMapping("/practices/{id}/history")
  public ApiResponse<PageVO<HistoryVO>> history(
      @PathVariable long id,
      @RequestParam(required = false) String cursor,
      @RequestParam(defaultValue = "20") int size,
      HttpServletRequest r) {
    return ApiResponse.ok(use.history(actor(r), id, cursor, size));
  }

  @PutMapping("/practices/{id}/result")
  public ApiResponse<VersionVO> result(
      @PathVariable long id, @Valid @RequestBody PracticeResultRequest body, HttpServletRequest r) {
    return ApiResponse.ok(use.result(actor(r), id, body));
  }

  @PostMapping("/practices/{id}/publish-request")
  public ApiResponse<VersionVO> publish(
      @PathVariable long id, @Valid @RequestBody VersionRequest body, HttpServletRequest r) {
    return ApiResponse.ok(use.publishFeedback(actor(r), id, body, key(r)));
  }

  @PostMapping("/practices/{id}/unpublish")
  public ApiResponse<VersionVO> unpublish(
      @PathVariable long id, @Valid @RequestBody VersionRequest body, HttpServletRequest r) {
    return ApiResponse.ok(use.unpublishFeedback(actor(r), id, body, key(r)));
  }

  @GetMapping("/notes/{id}/feedback")
  public ApiResponse<PageVO<FeedbackVO>> feedback(
      @PathVariable long id,
      @RequestParam(required = false) String cursor,
      @RequestParam(defaultValue = "20") int size,
      HttpServletRequest r) {
    return ApiResponse.ok(use.feedback(actor(r), id, cursor, size));
  }
}
