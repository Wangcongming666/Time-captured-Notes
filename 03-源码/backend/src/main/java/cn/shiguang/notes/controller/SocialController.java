package cn.shiguang.notes.controller;

import cn.shiguang.notes.dto.*;
import cn.shiguang.notes.dto.request.*;
import cn.shiguang.notes.service.application.SocialApplicationService;
import cn.shiguang.notes.vo.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

/** Social HTTP boundary. */
@RestController
@RequestMapping("/api/v1")
public class SocialController extends ControllerSupport {

  private final SocialApplicationService use;

  public SocialController(SocialApplicationService use) {
    this.use = use;
  }

  @PutMapping("/notes/{id}/like")
  public ApiResponse<EmptyVO> like(@PathVariable long id, HttpServletRequest r) {
    use.like(actor(r), id, true);
    return empty();
  }

  @DeleteMapping("/notes/{id}/like")
  public ApiResponse<EmptyVO> unlike(@PathVariable long id, HttpServletRequest r) {
    use.like(actor(r), id, false);
    return empty();
  }

  @PostMapping("/notes/{id}/comments")
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<IdVO> comment(
      @PathVariable long id, @Valid @RequestBody CommentRequest body, HttpServletRequest r) {
    return ApiResponse.ok(use.comment(actor(r), id, body, key(r)));
  }

  @GetMapping("/notes/{id}/comments")
  public ApiResponse<PageVO<CommentVO>> comments(
      @PathVariable long id,
      @RequestParam(required = false) String cursor,
      @RequestParam(defaultValue = "20") int size,
      HttpServletRequest r) {
    return ApiResponse.ok(use.comments(actor(r), id, null, cursor, size));
  }

  @GetMapping("/comments/{id}/replies")
  public ApiResponse<PageVO<CommentVO>> replies(
      @PathVariable long id,
      @RequestParam(required = false) String cursor,
      @RequestParam(defaultValue = "20") int size,
      HttpServletRequest r) {
    return ApiResponse.ok(use.replies(actor(r), id, cursor, size));
  }

  @DeleteMapping("/comments/{id}")
  public ApiResponse<EmptyVO> deleteComment(@PathVariable long id, HttpServletRequest r) {
    use.deleteComment(actor(r), id);
    return empty();
  }

  @PutMapping("/users/{id}/follow")
  public ApiResponse<EmptyVO> follow(@PathVariable long id, HttpServletRequest r) {
    use.follow(actor(r), id, true);
    return empty();
  }

  @DeleteMapping("/users/{id}/follow")
  public ApiResponse<EmptyVO> unfollow(@PathVariable long id, HttpServletRequest r) {
    use.follow(actor(r), id, false);
    return empty();
  }

  @GetMapping("/users/{id}/following")
  public ApiResponse<PageVO<FollowVO>> following(
      @PathVariable long id,
      @RequestParam(required = false) String cursor,
      @RequestParam(defaultValue = "20") int size,
      HttpServletRequest r) {
    return ApiResponse.ok(use.follows(actor(r), id, false, cursor, size));
  }

  @GetMapping("/users/{id}/followers")
  public ApiResponse<PageVO<FollowVO>> followers(
      @PathVariable long id,
      @RequestParam(required = false) String cursor,
      @RequestParam(defaultValue = "20") int size,
      HttpServletRequest r) {
    return ApiResponse.ok(use.follows(actor(r), id, true, cursor, size));
  }
}
