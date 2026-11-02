package cn.shiguang.notes.mapper;

import cn.shiguang.notes.entity.*;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.*;
import org.apache.ibatis.annotations.*;

/**
 * Typed CRUD plus bounded, parameterized business queries. SQL is maintained in
 * mapper/TopicMapper.xml.
 */
@Mapper
public interface TopicMapper extends BaseMapper<TopicEntity> {
  int demoBootstrapRun(@Param("value0") String value0, @Param("value1") String value1);

  List<Map<String, Object>> demoContentRun();

  long contentActiveTopics(@Param("id") long id);

  long discoveryInterest(@Param("topic") long topic);

  List<Map<String, Object>> governanceTopics(@Param("before") long before, @Param("size") int size);

  List<Map<String, Object>> governanceAllTopics(
      @Param("before") long before, @Param("size") int size);

  int governanceUpdateTopic(
      @Param("name") String name,
      @Param("category") String category,
      @Param("status") String status,
      @Param("id") long id);

  List<Map<String, Object>> governanceUpdateTopic2(@Param("id") long id);

  List<Map<String, Object>> statisticsHotTopics(@Param("days") int days);
}
