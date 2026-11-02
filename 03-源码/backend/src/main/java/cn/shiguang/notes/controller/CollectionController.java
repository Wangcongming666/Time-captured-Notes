package cn.shiguang.notes.controller;

import cn.shiguang.notes.dto.*;
import cn.shiguang.notes.dto.request.*;
import cn.shiguang.notes.service.application.CollectionApplicationService;
import cn.shiguang.notes.vo.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

/** Collection HTTP boundary. */
@RestController
@RequestMapping("/api/v1")
public class CollectionController extends ControllerSupport {

  private final CollectionApplicationService use;

  public CollectionController(CollectionApplicationService use) {
    this.use = use;
  }

  @PostMapping("/folders")
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<IdVO> createFolder(
      @Valid @RequestBody CreateFolderRequest body, HttpServletRequest r) {
    return ApiResponse.ok(use.createFolder(actor(r), body, key(r)));
  }

  @GetMapping("/folders")
  public ApiResponse<PageVO<FolderVO>> folders(
      @RequestParam(required = false) String cursor,
      @RequestParam(defaultValue = "20") int size,
      HttpServletRequest r) {
    return ApiResponse.ok(use.folders(actor(r), cursor, size));
  }

  @PutMapping("/folders/{id}")
  public ApiResponse<FolderVO> renameFolder(
      @PathVariable long id, @Valid @RequestBody RenameFolderRequest body, HttpServletRequest r) {
    return ApiResponse.ok(use.renameFolder(actor(r), id, body));
  }

  @DeleteMapping("/folders/{id}")
  public ApiResponse<EmptyVO> deleteFolder(@PathVariable long id, HttpServletRequest r) {
    use.deleteFolder(actor(r), id);
    return empty();
  }

  @GetMapping("/folders/{id}/notes")
  public ApiResponse<PageVO<FavoriteVO>> favorites(
      @PathVariable long id,
      @RequestParam(required = false) String cursor,
      @RequestParam(defaultValue = "20") int size,
      HttpServletRequest r) {
    return ApiResponse.ok(use.favorites(actor(r), id, cursor, size));
  }

  @PutMapping("/folders/{folder}/notes/{note}")
  public ApiResponse<EmptyVO> favorite(
      @PathVariable long folder, @PathVariable long note, HttpServletRequest r) {
    use.favorite(actor(r), folder, note, true);
    return empty();
  }

  @DeleteMapping("/folders/{folder}/notes/{note}")
  public ApiResponse<EmptyVO> unfavorite(
      @PathVariable long folder, @PathVariable long note, HttpServletRequest r) {
    use.favorite(actor(r), folder, note, false);
    return empty();
  }
}
