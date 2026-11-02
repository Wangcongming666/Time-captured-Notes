package cn.shiguang.notes.controller;

import cn.shiguang.notes.dto.*;
import cn.shiguang.notes.dto.request.*;
import cn.shiguang.notes.service.application.GovernanceApplicationService;
import cn.shiguang.notes.vo.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

/** Governance HTTP boundary. */
@RestController
@RequestMapping("/api/v1")
public class GovernanceController extends ControllerSupport {

  private final GovernanceApplicationService use;

  public GovernanceController(GovernanceApplicationService use) {
    this.use = use;
  }

  @GetMapping("/reviews")
  public ApiResponse<PageVO<ReviewVO>> reviews(
      @RequestParam(defaultValue = "PENDING") String status,
      @RequestParam(required = false) String cursor,
      @RequestParam(defaultValue = "20") int size,
      HttpServletRequest r) {
    return ApiResponse.ok(use.reviews(actor(r), status, cursor, size));
  }

  @GetMapping("/reviews/{id}")
  public ApiResponse<ReviewDetailVO> review(@PathVariable long id, HttpServletRequest r) {
    return ApiResponse.ok(use.review(actor(r), id));
  }

  @PostMapping("/reviews/{id}/decision")
  public ApiResponse<VersionVO> decide(
      @PathVariable long id, @Valid @RequestBody ReviewDecisionRequest body, HttpServletRequest r) {
    return ApiResponse.ok(use.decideReview(actor(r), id, body, key(r)));
  }

  @PostMapping("/reports")
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<IdVO> report(@Valid @RequestBody ReportRequest body, HttpServletRequest r) {
    return ApiResponse.ok(use.report(actor(r), body, key(r)));
  }

  @GetMapping("/me/reports")
  public ApiResponse<PageVO<ReportVO>> myReports(
      @RequestParam(required = false) String cursor,
      @RequestParam(defaultValue = "20") int size,
      HttpServletRequest r) {
    return ApiResponse.ok(use.reports(actor(r), false, cursor, size));
  }

  @GetMapping("/admin/reports")
  public ApiResponse<PageVO<ReportVO>> reports(
      @RequestParam(required = false) String cursor,
      @RequestParam(defaultValue = "20") int size,
      HttpServletRequest r) {
    return ApiResponse.ok(use.reports(actor(r), true, cursor, size));
  }

  @PostMapping("/admin/reports/{id}/decision")
  public ApiResponse<VersionVO> decideReport(
      @PathVariable long id, @Valid @RequestBody ReportDecisionRequest body, HttpServletRequest r) {
    return ApiResponse.ok(use.decideReport(actor(r), id, body, key(r)));
  }

  @PostMapping("/admin/notes/{id}/offline")
  public ApiResponse<EmptyVO> offline(
      @PathVariable long id, @Valid @RequestBody ModerationRequest body, HttpServletRequest r) {
    use.offlineNote(actor(r), id, body);
    return empty();
  }

  @PostMapping("/admin/comments/{id}/hide")
  public ApiResponse<EmptyVO> hideComment(
      @PathVariable long id, @Valid @RequestBody ModerationRequest body, HttpServletRequest r) {
    use.hideComment(actor(r), id, body);
    return empty();
  }

  @PostMapping("/admin/practices/{id}/offline")
  public ApiResponse<EmptyVO> offlineFeedback(
      @PathVariable long id, @Valid @RequestBody ModerationRequest body, HttpServletRequest r) {
    use.offlineFeedback(actor(r), id, body);
    return empty();
  }
}
