package cn.shiguang.notes.repository.query;

import cn.shiguang.notes.common.util.Data;
import cn.shiguang.notes.dto.Actor;
import cn.shiguang.notes.entity.*;
import cn.shiguang.notes.mapper.*;
import cn.shiguang.notes.repository.support.MapperRows;
import java.util.*;
import org.springframework.stereotype.Repository;

/** Read-only cross-context projections; no cached authorization. */
@Repository
public class PublicQueryRepository {
  private final CommentMapper commentMapper;
  private final FollowMapper followMapper;
  private final LikeMapper likeMapper;
  private final NoteMapper noteMapper;
  private final NoteMediaMapper noteMediaMapper;
  private final PracticeMapper practiceMapper;
  private final PracticeMediaMapper practiceMediaMapper;
  private final ReviewMapper reviewMapper;
  private final UserMapper userMapper;

  public PublicQueryRepository(
      CommentMapper commentMapper,
      FollowMapper followMapper,
      LikeMapper likeMapper,
      NoteMapper noteMapper,
      NoteMediaMapper noteMediaMapper,
      PracticeMapper practiceMapper,
      PracticeMediaMapper practiceMediaMapper,
      ReviewMapper reviewMapper,
      UserMapper userMapper) {
    this.commentMapper = commentMapper;
    this.followMapper = followMapper;
    this.likeMapper = likeMapper;
    this.noteMapper = noteMapper;
    this.noteMediaMapper = noteMediaMapper;
    this.practiceMapper = practiceMapper;
    this.practiceMediaMapper = practiceMediaMapper;
    this.reviewMapper = reviewMapper;
    this.userMapper = userMapper;
  }

  public Map<String, Object> profileSummary(long user, long viewer) {
    var row =
        MapperRows.one(
            followMapper.publicQueryProfileSummary(user, user, user, user, viewer, user));
    Object following = row.get("following");
    row.put(
        "following",
        Boolean.TRUE.equals(following)
            || following instanceof Number number && number.intValue() != 0);
    return row;
  }

  public boolean noteVisible(long id) {
    return noteMapper.publicQueryNoteVisible(id) == 1;
  }

  public boolean practiceVisible(long id) {
    return practiceMapper.publicQueryPracticeVisible(id) == 1;
  }

  public boolean commentVisible(long id) {
    return commentMapper.publicQueryCommentVisible(id) == 1;
  }

  public boolean targetVisible(String type, long id) {
    return switch (type) {
      case "NOTE" -> noteVisible(id);
      case "COMMENT" -> commentVisible(id);
      case "PRACTICE" -> practiceVisible(id);
      case "USER" -> userMapper.publicQueryTargetVisible(id) == 1;
      default -> false;
    };
  }

  public boolean mediaAllowed(Actor actor, Map<String, Object> media) {
    long id = Data.id(media, "id");
    if ("DELETED".equals(media.get("status"))) return false;
    if (actor.id() > 0 && Data.id(media, "ownerId") == actor.id()) return true;
    if (noteMediaMapper.publicQueryMediaAllowed(id) > 0) return true;
    if (practiceMediaMapper.publicQueryMediaAllowed(id) > 0) return true;
    if (userMapper.publicQueryMediaAllowed(id) > 0) return true;
    return actor.has("REVIEWER") && reviewMapper.publicQueryMediaAllowed(actor.id(), id, id) > 0;
  }

  public Map<String, Object> interaction(long user, long note) {
    var r =
        MapperRows.one(
            likeMapper.publicQueryInteraction(
                user, note, user, note, user, note, note, note, note));
    for (String field : List.of("liked", "favorite", "following"))
      r.put(field, ((Number) r.get(field)).longValue() > 0);
    return r;
  }

  public List<Map<String, Object>> feedback(long note, long before, int size) {
    return MapperRows.list(practiceMapper.publicQueryFeedback(note, before, size));
  }

  public List<Map<String, Object>> users(long before, int size) {
    var rows = MapperRows.list(userMapper.publicQueryUsers(before, size));
    for (var row : rows) {
      String codes = Data.text(row, "roleCodes");
      row.remove("roleCodes");
      row.put("roles", codes.isBlank() ? List.of() : List.of(codes.split(",")));
    }
    return rows;
  }
}
