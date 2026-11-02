package cn.shiguang.notes.mapper;

import cn.shiguang.notes.entity.*;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.*;
import org.apache.ibatis.annotations.*;

/**
 * Typed CRUD plus bounded, parameterized business queries. SQL is maintained in
 * mapper/FollowMapper.xml.
 */
@Mapper
public interface FollowMapper extends BaseMapper<FollowEntity> {
  int socialFollow(@Param("from") long from, @Param("to") long to);

  int socialFollow2(@Param("from") long from, @Param("to") long to);

  List<Map<String, Object>> socialFollows(
      @Param("user") long user,
      @Param("before") long before,
      @Param("size") int size,
      @Param("followers") boolean followers);

  List<Map<String, Object>> publicQueryProfileSummary(
      @Param("user") long user,
      @Param("user2") long user2,
      @Param("user3") long user3,
      @Param("user4") long user4,
      @Param("viewer") long viewer,
      @Param("user5") long user5);
}
