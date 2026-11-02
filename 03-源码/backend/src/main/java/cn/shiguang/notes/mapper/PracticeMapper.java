package cn.shiguang.notes.mapper;

import cn.shiguang.notes.entity.*;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.*;
import org.apache.ibatis.annotations.*;

/**
 * Typed CRUD plus bounded, parameterized business queries. SQL is maintained in
 * mapper/PracticeMapper.xml.
 */
@Mapper
public interface PracticeMapper extends BaseMapper<PracticeEntity> {
  PracticeEntity collectionPractice(@Param("id") long id, @Param("lock") boolean lock);

  List<PracticeEntity> collectionPractices(
      @Param("owner") long owner, @Param("before") long before, @Param("size") int size);

  int collectionTransition(
      @Param("to") String to,
      @Param("to2") String to2,
      @Param("to3") String to3,
      @Param("id") long id);

  int collectionResult(
      @Param("text") String text, @Param("helpfulness") String helpfulness, @Param("id") long id);

  int collectionPublicState(
      @Param("state") String state,
      @Param("source") Integer source,
      @Param("value2") int value2,
      @Param("id") long id);

  long publicQueryPracticeVisible(@Param("id") long id);

  List<Map<String, Object>> publicQueryFeedback(
      @Param("note") long note, @Param("before") long before, @Param("size") int size);
}
