package cn.shiguang.notes.mapper;

import cn.shiguang.notes.entity.*;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.*;
import org.apache.ibatis.annotations.*;

/**
 * Typed CRUD plus bounded, parameterized business queries. SQL is maintained in
 * mapper/CommandMapper.xml.
 */
@Mapper
public interface CommandMapper extends BaseMapper<CommandEntity> {
  List<Map<String, Object>> commandsRun(
      @Param("actorId") long actorId, @Param("scope") String scope, @Param("key") String key);

  int commandsRun2(
      @Param("actorId") long actorId,
      @Param("scope") String scope,
      @Param("key") String key,
      @Param("hash") String hash,
      @Param("value4") String value4);
}
