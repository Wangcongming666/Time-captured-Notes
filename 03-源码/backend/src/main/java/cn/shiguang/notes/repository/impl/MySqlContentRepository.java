package cn.shiguang.notes.repository.impl;

import cn.shiguang.notes.common.exception.BizException;
import cn.shiguang.notes.common.util.Data;
import cn.shiguang.notes.entity.*;
import cn.shiguang.notes.mapper.*;
import cn.shiguang.notes.repository.ContentRepository;
import cn.shiguang.notes.repository.support.EntityWrites;
import cn.shiguang.notes.repository.support.MapperRows;
import java.util.*;
import org.springframework.stereotype.Repository;

/** All media rows are locked in numeric order before binding or releasing. */
@Repository
public class MySqlContentRepository implements ContentRepository {
  private final MediaMapper mediaMapper;
  private final NoteMapper noteMapper;
  private final NoteMediaMapper noteMediaMapper;
  private final PracticeMediaMapper practiceMediaMapper;
  private final NoteTopicMapper noteTopicMapper;
  private final TopicMapper topicMapper;

  public MySqlContentRepository(
      MediaMapper mediaMapper,
      NoteMapper noteMapper,
      NoteMediaMapper noteMediaMapper,
      PracticeMediaMapper practiceMediaMapper,
      NoteTopicMapper noteTopicMapper,
      TopicMapper topicMapper) {
    this.mediaMapper = mediaMapper;
    this.noteMapper = noteMapper;
    this.noteMediaMapper = noteMediaMapper;
    this.practiceMediaMapper = practiceMediaMapper;
    this.noteTopicMapper = noteTopicMapper;
    this.topicMapper = topicMapper;
  }

  public Map<String, Object> note(long id, boolean lock) {
    return MapperRows.requiredEntity(noteMapper.contentNote(id, lock));
  }

  public List<Long> mediaIds(long id) {
    return MapperRows.list(noteMediaMapper.contentMediaIds(id)).stream()
        .map(m -> Data.id(m, "mediaId"))
        .toList();
  }

  public List<Map<String, Object>> mediaEntries(long id) {
    return MapperRows.list(noteMediaMapper.contentMediaEntries(id));
  }

  public List<Long> topicIds(long id) {
    return MapperRows.list(noteTopicMapper.contentTopicIds(id)).stream()
        .map(m -> Data.id(m, "topicId"))
        .toList();
  }

  public List<Map<String, Object>> owned(long author, long before, int size) {
    return MapperRows.entities(noteMapper.contentOwned(author, before, size));
  }

  public long create(long author, String title, String body) {
    return EntityWrites.insert(
        noteMapper, NoteEntity.class, Data.map("author_id", author, "title", title, "body", body));
  }

  public void edit(long id, String title, String body, String status, int version) {
    int n = noteMapper.contentEdit(title, body, status, id, version);
    if (n != 1) throw BizException.conflict("笔记已被修改，请刷新");
  }

  public void state(long id, String status, String reason, boolean newSubmission) {
    noteMapper.contentState(status, reason, newSubmission ? 1 : 0, status, id);
  }

  public void topics(long id, List<Long> topics) {
    noteTopicMapper.contentTopics(id);
    for (long t : topics) noteTopicMapper.contentTopics2(id, t);
  }

  public boolean activeTopics(List<Long> ids) {
    for (long id : ids) if (topicMapper.contentActiveTopics(id) != 1) return false;
    return true;
  }

  public Map<String, Object> media(long id, boolean lock) {
    return MapperRows.requiredEntity(mediaMapper.contentMedia(id, lock));
  }

  public long addMedia(Map<String, Object> values) {
    return EntityWrites.insert(mediaMapper, MediaEntity.class, values);
  }

  public void avatar(long owner, Long next, Long previous) {
    var ids = new TreeSet<Long>();
    if (next != null) ids.add(next);
    if (previous != null) ids.add(previous);
    for (long id : ids) {
      var m = media(id, true);
      if (Data.id(m, "ownerId") != owner || !"AVATAR".equals(m.get("purpose")))
        throw BizException.forbidden();
      if (Objects.equals(next, id)
          && !Objects.equals(previous, id)
          && !"STAGED".equals(m.get("status"))) throw BizException.conflict("图片已被使用");
    }
    if (previous != null && !Objects.equals(next, previous)) mediaMapper.contentAvatar(previous);
    if (next != null) mediaMapper.contentAvatar2(next);
  }

  public void bind(long actor, List<Long> ids, String purpose, String type, long target) {
    boolean note = type.equals("NOTE");
    var old =
        MapperRows.list(
                note
                    ? noteMediaMapper.bindingMedia(target)
                    : practiceMediaMapper.bindingMedia(target))
            .stream()
            .map(r -> Data.id(r, "mediaId"))
            .toList();
    var all = new TreeSet<Long>(ids);
    all.addAll(old);
    var types = new ArrayList<String>();
    for (long id : all) {
      var m = media(id, true);
      if (ids.contains(id)) types.add(Data.text(m, "mediaType"));
      if (Data.id(m, "ownerId") != actor || !purpose.equals(m.get("purpose")))
        throw BizException.forbidden();
      if (!old.contains(id) && !"STAGED".equals(m.get("status")))
        throw BizException.conflict("图片已被其他内容使用");
    }
    if (note) cn.shiguang.notes.validation.MediaRules.validateNote(types);
    else if (types.contains("VIDEO")) throw BizException.invalid("实践反馈仅支持图片");
    if (note) noteMediaMapper.deleteBindings(target);
    else practiceMediaMapper.deleteBindings(target);
    for (long id : old) if (!ids.contains(id)) mediaMapper.contentBind(id);
    for (int i = 0; i < ids.size(); i++) {
      if (note) noteMediaMapper.insertBinding(target, ids.get(i), i + 1);
      else practiceMediaMapper.insertBinding(target, ids.get(i), i + 1);
      mediaMapper.contentBind2(ids.get(i));
    }
  }
}
