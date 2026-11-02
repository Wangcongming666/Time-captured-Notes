package cn.shiguang.notes.mapper;

import cn.shiguang.notes.entity.*;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.*;
import org.apache.ibatis.annotations.*;

/**
 * Typed CRUD plus bounded, parameterized business queries. SQL is maintained in
 * mapper/UserRoleMapper.xml.
 */
@Mapper
public interface UserRoleMapper extends BaseMapper<UserRoleEntity> {
  int demoBootstrapRun(@Param("id") long id, @Param("value1") String value1);

  int identityCreate(@Param("value0") Long value0);

  int identityChangeRoles(@Param("id") long id);

  int identityChangeRoles2(@Param("id") long id, @Param("role") String role);
}
