package cn.shiguang.notes.controller;

import cn.shiguang.notes.dto.*;
import cn.shiguang.notes.dto.request.*;
import cn.shiguang.notes.service.application.AdministrationApplicationService;
import cn.shiguang.notes.vo.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

/** Administration HTTP boundary. */
@RestController
@RequestMapping("/api/v1")
public class AdministrationController extends ControllerSupport {

  private final AdministrationApplicationService use;

  public AdministrationController(AdministrationApplicationService use) {
    this.use = use;
  }

  @PostMapping("/admin/topics")
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<IdVO> createTopic(
      @Valid @RequestBody TopicRequest body, HttpServletRequest r) {
    return ApiResponse.ok(use.createTopic(actor(r), body, key(r)));
  }

  @PutMapping("/admin/topics/{id}")
  public ApiResponse<TopicVO> updateTopic(
      @PathVariable long id, @Valid @RequestBody TopicRequest body, HttpServletRequest r) {
    return ApiResponse.ok(use.updateTopic(actor(r), id, body));
  }

  @GetMapping("/admin/audits")
  public ApiResponse<PageVO<AuditVO>> audits(
      @RequestParam(required = false) String cursor,
      @RequestParam(defaultValue = "20") int size,
      HttpServletRequest r) {
    return ApiResponse.ok(use.audits(actor(r), cursor, size));
  }

  @GetMapping("/admin/users")
  public ApiResponse<PageVO<AdminUserVO>> users(
      @RequestParam(required = false) String cursor,
      @RequestParam(defaultValue = "20") int size,
      HttpServletRequest r) {
    return ApiResponse.ok(use.users(actor(r), cursor, size));
  }

  @GetMapping("/admin/topics")
  public ApiResponse<PageVO<TopicVO>> adminTopics(
      @RequestParam(required = false) String cursor,
      @RequestParam(defaultValue = "50") int size,
      HttpServletRequest r) {
    return ApiResponse.ok(use.adminTopics(actor(r), cursor, size));
  }
}
