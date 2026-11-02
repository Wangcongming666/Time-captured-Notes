package cn.shiguang.notes.mapper;

import cn.shiguang.notes.entity.*;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.*;
import org.apache.ibatis.annotations.*;

/**
 * Typed CRUD plus bounded, parameterized business queries. SQL is maintained in
 * mapper/DailyMetricMapper.xml.
 */
@Mapper
public interface DailyMetricMapper extends BaseMapper<DailyMetricEntity> {
  int statisticsMetric(
      @Param("day") String day,
      @Param("scope") String scope,
      @Param("subject") long subject,
      @Param("metric") String metric,
      @Param("value") double value,
      @Param("increment") boolean increment);

  List<Map<String, Object>> statisticsPoints(
      @Param("scope") String scope, @Param("subject") long subject, @Param("value2") int value2);
}
