package cn.shiguang.notes.controller;

import cn.shiguang.notes.dto.*;
import cn.shiguang.notes.dto.request.*;
import cn.shiguang.notes.service.*;
import cn.shiguang.notes.vo.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/** Media HTTP boundary. */
@RestController
@RequestMapping("/api/v1")
public class MediaController extends ControllerSupport {
  private final cn.shiguang.notes.service.application.MediaApplicationService use;

  public MediaController(cn.shiguang.notes.service.application.MediaApplicationService use) {
    this.use = use;
  }

  @PostMapping("/media")
  public ApiResponse<ImageUploadVO> upload(
      @RequestParam MultipartFile file, @RequestParam String purpose, HttpServletRequest r)
      throws Exception {
    return ApiResponse.ok(use.upload(actor(r), file, purpose));
  }

  @GetMapping("/media/{id}/content")
  public ResponseEntity<Resource> image(@PathVariable long id, HttpServletRequest r)
      throws Exception {
    return use.stream(
        actor(r),
        id,
        false,
        r.getHeader("Range"),
        r.getHeader("If-Range"),
        r.getMethod().equals("HEAD"));
  }

  @GetMapping("/media/{id}/poster")
  public ResponseEntity<Resource> poster(@PathVariable long id, HttpServletRequest r)
      throws Exception {
    return use.stream(
        actor(r),
        id,
        true,
        r.getHeader("Range"),
        r.getHeader("If-Range"),
        r.getMethod().equals("HEAD"));
  }

  @GetMapping("/media/capabilities")
  public ApiResponse<MediaCapabilitiesVO> capabilities() {
    return ApiResponse.ok(use.capabilities());
  }

  @PostMapping("/media/videos")
  public ApiResponse<VideoUploadVO> video(@RequestParam MultipartFile file, HttpServletRequest r)
      throws Exception {
    return ApiResponse.ok(use.uploadVideo(actor(r), file));
  }
}
