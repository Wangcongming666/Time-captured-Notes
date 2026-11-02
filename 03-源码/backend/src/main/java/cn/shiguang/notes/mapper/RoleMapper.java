package cn.shiguang.notes.mapper;

import cn.shiguang.notes.entity.*;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.*;
import org.apache.ibatis.annotations.*;

/**
 * Typed CRUD plus bounded, parameterized business queries. SQL is maintained in
 * mapper/RoleMapper.xml.
 */
@Mapper
public interface RoleMapper extends BaseMapper<RoleEntity> {
  List<Map<String, Object>> identityRoles(@Param("id") long id);

  List<Map<String, Object>> identityLockAdministrators();
}
