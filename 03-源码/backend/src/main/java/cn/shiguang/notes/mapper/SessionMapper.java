package cn.shiguang.notes.mapper;

import cn.shiguang.notes.entity.*;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.*;
import org.apache.ibatis.annotations.*;

/**
 * Typed CRUD plus bounded, parameterized business queries. SQL is maintained in
 * mapper/SessionMapper.xml.
 */
@Mapper
public interface SessionMapper extends BaseMapper<SessionEntity> {
  List<Map<String, Object>> identitySession(@Param("hash") String hash);

  int identityAddSession(
      @Param("hash") String hash,
      @Param("id") long id,
      @Param("v") int v,
      @Param("csrf") String csrf);

  int identityRevoke(@Param("hash") String hash);

  int identityCsrf(@Param("csrf") String csrf, @Param("hash") String hash);
}
