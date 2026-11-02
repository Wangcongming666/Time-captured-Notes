package cn.shiguang.notes.mapper;

import cn.shiguang.notes.entity.*;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.*;
import org.apache.ibatis.annotations.*;

/**
 * Typed CRUD plus bounded, parameterized business queries. SQL is maintained in
 * mapper/PracticeHistoryMapper.xml.
 */
@Mapper
public interface PracticeHistoryMapper extends BaseMapper<PracticeHistoryEntity> {
  List<Map<String, Object>> collectionHistory(
      @Param("id") long id, @Param("before") long before, @Param("size") int size);
}
