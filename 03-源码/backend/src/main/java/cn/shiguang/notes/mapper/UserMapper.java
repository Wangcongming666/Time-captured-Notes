package cn.shiguang.notes.mapper;

import cn.shiguang.notes.entity.*;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.*;
import org.apache.ibatis.annotations.*;

/**
 * Typed CRUD plus bounded, parameterized business queries. SQL is maintained in
 * mapper/UserMapper.xml.
 */
@Mapper
public interface UserMapper extends BaseMapper<UserEntity> {
  List<Map<String, Object>> demoBootstrapRun(@Param("value0") String value0);

  List<Map<String, Object>> demoContentRun();

  List<Map<String, Object>> demoContentRun2();

  UserEntity identityUser(@Param("id") long id, @Param("lock") boolean lock);

  int identityProfile(
      @Param("name") String name,
      @Param("bio") String bio,
      @Param("avatar") Long avatar,
      @Param("id") long id);

  int identityChangeStatus(@Param("status") String status, @Param("id") long id);

  int identityChangeRoles(@Param("id") long id);

  long identityActiveAdministrators();

  long publicQueryTargetVisible(@Param("id") long id);

  long publicQueryMediaAllowed(@Param("id") long id);

  List<Map<String, Object>> publicQueryUsers(@Param("before") long before, @Param("size") int size);

  List<Map<String, Object>> commandsRun(@Param("actorId") long actorId);
}
