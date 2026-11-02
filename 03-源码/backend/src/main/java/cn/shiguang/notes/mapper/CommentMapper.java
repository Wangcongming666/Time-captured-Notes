package cn.shiguang.notes.mapper;

import cn.shiguang.notes.entity.*;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.*;
import org.apache.ibatis.annotations.*;

/**
 * Typed CRUD plus bounded, parameterized business queries. SQL is maintained in
 * mapper/CommentMapper.xml.
 */
@Mapper
public interface CommentMapper extends BaseMapper<CommentEntity> {
  CommentEntity socialComment(@Param("id") long id, @Param("lock") boolean lock);

  int socialState(
      @Param("state") String state,
      @Param("state2") String state2,
      @Param("id") long id,
      @Param("version") int version);

  List<Map<String, Object>> socialComments(
      @Param("note") long note, @Param("before") long before, @Param("size") int size);

  List<Map<String, Object>> socialComments2(
      @Param("note") long note,
      @Param("root") Long root,
      @Param("before") long before,
      @Param("size") int size);

  long publicQueryCommentVisible(@Param("id") long id);
}
