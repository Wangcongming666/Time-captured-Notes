package cn.shiguang.notes.controller;

import cn.shiguang.notes.dto.*;
import cn.shiguang.notes.dto.request.*;
import cn.shiguang.notes.service.application.ContentApplicationService;
import cn.shiguang.notes.vo.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.web.bind.annotation.*;

/** Content HTTP boundary. */
@RestController
@RequestMapping("/api/v1")
public class ContentController extends ControllerSupport {

  private final ContentApplicationService use;

  public ContentController(ContentApplicationService use) {
    this.use = use;
  }

  @PostMapping("/notes")
  public ApiResponse<VersionVO> createNote(
      @Valid @RequestBody CreateNoteRequest body, HttpServletRequest r) {
    return ApiResponse.ok(use.createNote(actor(r), body, key(r)));
  }

  @PutMapping("/notes/{id}")
  public ApiResponse<VersionVO> editNote(
      @PathVariable long id, @Valid @RequestBody EditNoteRequest body, HttpServletRequest r) {
    return ApiResponse.ok(use.editNote(actor(r), id, body, key(r)));
  }

  @GetMapping("/notes/{id}")
  public ApiResponse<NoteVO> note(@PathVariable long id, HttpServletRequest r) {
    String subject = Objects.toString(r.getAttribute("shiguang.visitor"));
    return ApiResponse.ok(
        use.note(actor(r), id, actor(r).id() > 0 ? "user:" + actor(r).id() : subject));
  }

  @GetMapping("/me/notes/{id}")
  public ApiResponse<OwnedNoteVO> ownNote(@PathVariable long id, HttpServletRequest r) {
    return ApiResponse.ok(use.ownNote(actor(r), id));
  }

  @GetMapping("/me/notes")
  public ApiResponse<PageVO<OwnedNoteVO>> ownNotes(
      @RequestParam(required = false) String cursor,
      @RequestParam(defaultValue = "20") int size,
      HttpServletRequest r) {
    return ApiResponse.ok(use.ownNotes(actor(r), cursor, size));
  }

  @PostMapping("/notes/{id}/submit")
  public ApiResponse<VersionVO> submit(
      @PathVariable long id, @Valid @RequestBody VersionRequest body, HttpServletRequest r) {
    return ApiResponse.ok(use.submit(actor(r), id, body, key(r)));
  }

  @PostMapping("/notes/{id}/withdraw")
  public ApiResponse<VersionVO> withdraw(
      @PathVariable long id, @Valid @RequestBody VersionRequest body, HttpServletRequest r) {
    return ApiResponse.ok(use.withdraw(actor(r), id, body, key(r)));
  }

  @DeleteMapping("/notes/{id}")
  public ApiResponse<EmptyVO> deleteNote(
      @PathVariable long id, @RequestParam int version, HttpServletRequest r) {
    use.deleteNote(actor(r), id, version);
    return empty();
  }
}
