package cn.shiguang.notes.mapper;

import cn.shiguang.notes.entity.*;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.*;
import org.apache.ibatis.annotations.*;

/**
 * Typed CRUD plus bounded, parameterized business queries. SQL is maintained in
 * mapper/ReportMapper.xml.
 */
@Mapper
public interface ReportMapper extends BaseMapper<ReportEntity> {
  List<Map<String, Object>> governanceReport(@Param("key") String key);

  ReportEntity governanceReport2(@Param("id") long id, @Param("lock") boolean lock);

  int governanceDecideReport(
      @Param("status") String status,
      @Param("actor") long actor,
      @Param("reason") String reason,
      @Param("closed") boolean closed,
      @Param("closed2") boolean closed2,
      @Param("id") long id,
      @Param("version") int version);

  List<Map<String, Object>> governanceReports(
      @Param("before") long before, @Param("size") int size);

  List<Map<String, Object>> governanceReports2(
      @Param("user") Long user, @Param("before") long before, @Param("size") int size);
}
