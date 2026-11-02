package cn.shiguang.notes.mapper;

import cn.shiguang.notes.entity.*;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.*;
import org.apache.ibatis.annotations.*;

/**
 * Typed CRUD plus bounded, parameterized business queries. SQL is maintained in
 * mapper/AuditMapper.xml.
 */
@Mapper
public interface AuditMapper extends BaseMapper<AuditEntity> {
  List<Map<String, Object>> governanceAudits(@Param("before") long before, @Param("size") int size);
}
