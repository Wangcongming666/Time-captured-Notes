package cn.shiguang.notes.controller;

import cn.shiguang.notes.dto.*;
import cn.shiguang.notes.dto.request.*;
import cn.shiguang.notes.service.application.DiscoveryApplicationService;
import cn.shiguang.notes.vo.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/** Discovery HTTP boundary. */
@RestController
@RequestMapping("/api/v1")
public class DiscoveryController extends ControllerSupport {

  private final DiscoveryApplicationService use;

  public DiscoveryController(DiscoveryApplicationService use) {
    this.use = use;
  }

  @GetMapping("/feed")
  public ApiResponse<PageVO<CardVO>> feed(
      @ModelAttribute FeedQueryRequest request, HttpServletRequest r) {
    return ApiResponse.ok(use.feed(actor(r), request));
  }

  @GetMapping("/search")
  public ApiResponse<PageVO<CardVO>> search(
      @ModelAttribute FeedQueryRequest request, HttpServletRequest r) {
    return ApiResponse.ok(use.search(actor(r), request));
  }

  @GetMapping("/me/preferences")
  public ApiResponse<PreferencesVO> preferences(HttpServletRequest r) {
    return ApiResponse.ok(use.preferences(actor(r)));
  }

  @PutMapping("/me/preferences")
  public ApiResponse<PreferencesVO> setPreferences(
      @Valid @RequestBody PreferencesRequest body, HttpServletRequest r) {
    use.preferences(actor(r), body);
    return preferences(r);
  }

  @DeleteMapping("/me/preferences")
  public ApiResponse<EmptyVO> resetPreferences(HttpServletRequest r) {
    use.resetPreferences(actor(r));
    return empty();
  }

  @PutMapping("/me/hidden-notes/{id}")
  public ApiResponse<EmptyVO> hide(@PathVariable long id, HttpServletRequest r) {
    use.hide(actor(r), id, true);
    return empty();
  }

  @DeleteMapping("/me/hidden-notes/{id}")
  public ApiResponse<EmptyVO> unhide(@PathVariable long id, HttpServletRequest r) {
    use.hide(actor(r), id, false);
    return empty();
  }

  @GetMapping("/topics")
  public ApiResponse<PageVO<TopicVO>> topics(
      @RequestParam(required = false) String cursor,
      @RequestParam(defaultValue = "50") int size,
      HttpServletRequest r) {
    return ApiResponse.ok(use.topics(actor(r), cursor, size));
  }
}
