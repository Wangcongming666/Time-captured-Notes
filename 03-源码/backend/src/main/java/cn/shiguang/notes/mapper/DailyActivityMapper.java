package cn.shiguang.notes.mapper;

import cn.shiguang.notes.entity.*;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.*;
import org.apache.ibatis.annotations.*;

/**
 * Typed CRUD plus bounded, parameterized business queries. SQL is maintained in
 * mapper/DailyActivityMapper.xml.
 */
@Mapper
public interface DailyActivityMapper extends BaseMapper<DailyActivityEntity> {
  int statisticsActive(@Param("user") long user);

  List<Map<String, Object>> statisticsPoints(@Param("value0") int value0);
}
