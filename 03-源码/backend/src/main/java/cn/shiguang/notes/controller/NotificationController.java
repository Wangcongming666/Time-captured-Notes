package cn.shiguang.notes.controller;

import cn.shiguang.notes.dto.*;
import cn.shiguang.notes.dto.request.*;
import cn.shiguang.notes.service.application.NotificationApplicationService;
import cn.shiguang.notes.vo.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/** Notification HTTP boundary. */
@RestController
@RequestMapping("/api/v1")
public class NotificationController extends ControllerSupport {

  private final NotificationApplicationService use;

  public NotificationController(NotificationApplicationService use) {
    this.use = use;
  }

  @GetMapping("/notifications")
  public ApiResponse<PageVO<NotificationVO>> notifications(
      @RequestParam(required = false) String type,
      @RequestParam(required = false) String cursor,
      @RequestParam(defaultValue = "20") int size,
      HttpServletRequest r) {
    return ApiResponse.ok(use.notifications(actor(r), type, cursor, size));
  }

  @GetMapping("/notifications/unread")
  public ApiResponse<UnreadVO> unread(HttpServletRequest r) {
    return ApiResponse.ok(use.unread(actor(r)));
  }

  @PutMapping("/notifications/{id}/read")
  public ApiResponse<EmptyVO> read(@PathVariable long id, HttpServletRequest r) {
    use.readNotification(actor(r), id);
    return empty();
  }

  @PostMapping("/notifications/read-all")
  public ApiResponse<EmptyVO> readAll(
      @Valid @RequestBody ReadAllRequest body, HttpServletRequest r) {
    use.readAll(actor(r), body);
    return empty();
  }
}
