package cn.shiguang.notes.repository.impl;

import cn.shiguang.notes.common.exception.BizException;
import cn.shiguang.notes.common.util.Data;
import cn.shiguang.notes.entity.*;
import cn.shiguang.notes.mapper.*;
import cn.shiguang.notes.repository.SocialRepository;
import cn.shiguang.notes.repository.support.EntityWrites;
import cn.shiguang.notes.repository.support.MapperRows;
import java.util.*;
import org.springframework.stereotype.Repository;

@Repository
public class MySqlSocialRepository implements SocialRepository {
  private final CommentMapper commentMapper;
  private final FollowMapper followMapper;
  private final LikeMapper likeMapper;

  public MySqlSocialRepository(
      CommentMapper commentMapper, FollowMapper followMapper, LikeMapper likeMapper) {
    this.commentMapper = commentMapper;
    this.followMapper = followMapper;
    this.likeMapper = likeMapper;
  }

  public boolean like(long user, long note, boolean desired) {
    return desired ? likeMapper.socialLike(user, note) > 0 : likeMapper.socialLike2(user, note) > 0;
  }

  public boolean follow(long from, long to, boolean desired) {
    return desired
        ? followMapper.socialFollow(from, to) > 0
        : followMapper.socialFollow2(from, to) > 0;
  }

  public Map<String, Object> comment(long id, boolean lock) {
    return MapperRows.requiredEntity(commentMapper.socialComment(id, lock));
  }

  public long createComment(long author, long note, Long root, Long reply, String body) {
    return EntityWrites.insert(
        commentMapper,
        CommentEntity.class,
        Data.map(
            "author_id",
            author,
            "note_id",
            note,
            "root_id",
            root,
            "reply_to_id",
            reply,
            "body",
            body));
  }

  public void state(long id, String state, int version) {
    if (commentMapper.socialState(state, state, id, version) != 1)
      throw BizException.conflict("评论已变化");
  }

  public List<Map<String, Object>> comments(long note, Long root, long before, int size) {
    if (root == null) return MapperRows.list(commentMapper.socialComments(note, before, size));
    return MapperRows.list(commentMapper.socialComments2(note, root, before, size));
  }

  public List<Map<String, Object>> follows(long user, boolean followers, long before, int size) {
    return MapperRows.list(followMapper.socialFollows(user, before, size, followers));
  }
}
