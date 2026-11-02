package cn.shiguang.notes.mapper;

import cn.shiguang.notes.entity.*;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.*;
import org.apache.ibatis.annotations.*;

/**
 * Typed CRUD plus bounded, parameterized business queries. SQL is maintained in
 * mapper/LikeMapper.xml.
 */
@Mapper
public interface LikeMapper extends BaseMapper<LikeEntity> {
  int socialLike(@Param("user") long user, @Param("note") long note);

  int socialLike2(@Param("user") long user, @Param("note") long note);

  List<Map<String, Object>> publicQueryInteraction(
      @Param("user") long user,
      @Param("note") long note,
      @Param("user2") long user2,
      @Param("note2") long note2,
      @Param("user3") long user3,
      @Param("note3") long note3,
      @Param("note4") long note4,
      @Param("note5") long note5,
      @Param("note6") long note6);
}
