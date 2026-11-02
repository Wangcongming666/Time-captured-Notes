package cn.shiguang.notes.mapper;

import cn.shiguang.notes.entity.*;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.*;
import org.apache.ibatis.annotations.*;

/**
 * Typed CRUD plus bounded, parameterized business queries. SQL is maintained in
 * mapper/NoteMapper.xml.
 */
@Mapper
public interface NoteMapper extends BaseMapper<NoteEntity> {
  long demoContentRun();

  NoteEntity contentNote(@Param("id") long id, @Param("lock") boolean lock);

  List<NoteEntity> contentOwned(
      @Param("author") long author, @Param("before") long before, @Param("size") int size);

  int contentEdit(
      @Param("title") String title,
      @Param("body") String body,
      @Param("status") String status,
      @Param("id") long id,
      @Param("version") int version);

  int contentState(
      @Param("status") String status,
      @Param("reason") String reason,
      @Param("value2") int value2,
      @Param("status2") String status2,
      @Param("id") long id);

  List<Map<String, Object>> statisticsNotes(@Param("after") long after, @Param("size") int size);

  long publicQueryNoteVisible(@Param("id") long id);

  List<Map<String, Object>> discoveryCandidates(
      @Param("user") long user,
      @Param("mode") String mode,
      @Param("category") String category,
      @Param("pattern") String pattern,
      @Param("topic") Long topic,
      @Param("author") Long author);

  List<Map<String, Object>> discoveryVisible(
      @Param("user") long user, @Param("ids") List<String> ids);

  List<Map<String, Object>> statisticsRanking(
      @Param("author") Long author, @Param("offset") long offset, @Param("size") int size);
}
