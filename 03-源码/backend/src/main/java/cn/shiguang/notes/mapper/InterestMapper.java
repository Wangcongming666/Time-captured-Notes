package cn.shiguang.notes.mapper;

import cn.shiguang.notes.entity.*;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.*;
import org.apache.ibatis.annotations.*;

/**
 * Typed CRUD plus bounded, parameterized business queries. SQL is maintained in
 * mapper/InterestMapper.xml.
 */
@Mapper
public interface InterestMapper extends BaseMapper<InterestEntity> {
  List<Map<String, Object>> discoveryPreferences(@Param("user") long user);

  int discoveryPreferences2(@Param("user") long user);

  int discoveryInterest(
      @Param("user") long user, @Param("topic") long topic, @Param("mode") String mode);

  int discoveryReset(@Param("user") long user);
}
