package cn.shiguang.notes.mapper;

import cn.shiguang.notes.entity.*;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.*;
import org.apache.ibatis.annotations.*;

/**
 * Typed CRUD plus bounded, parameterized business queries. SQL is maintained in
 * mapper/ReviewMapper.xml.
 */
@Mapper
public interface ReviewMapper extends BaseMapper<ReviewEntity> {
  ReviewEntity governanceReview(@Param("id") long id, @Param("lock") boolean lock);

  int governanceCancelReviews(@Param("type") String type, @Param("id") long id);

  int governanceDecide(
      @Param("status") String status,
      @Param("actor") long actor,
      @Param("reason") String reason,
      @Param("id") long id,
      @Param("version") int version);

  List<Map<String, Object>> governanceReviews(
      @Param("status") String status, @Param("before") long before, @Param("size") int size);

  List<Map<String, Object>> statisticsGovernance(
      @Param("days") int days,
      @Param("days2") int days2,
      @Param("days3") int days3,
      @Param("days4") int days4,
      @Param("days5") int days5,
      @Param("days6") int days6);

  long publicQueryMediaAllowed(
      @Param("actorId") long actorId, @Param("id") long id, @Param("id2") long id2);
}
