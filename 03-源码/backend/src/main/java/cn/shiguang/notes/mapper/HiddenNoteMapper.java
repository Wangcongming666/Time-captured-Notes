package cn.shiguang.notes.mapper;

import cn.shiguang.notes.entity.*;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.*;
import org.apache.ibatis.annotations.*;

/**
 * Typed CRUD plus bounded, parameterized business queries. SQL is maintained in
 * mapper/HiddenNoteMapper.xml.
 */
@Mapper
public interface HiddenNoteMapper extends BaseMapper<HiddenNoteEntity> {
  int discoveryHide(@Param("user") long user, @Param("note") long note);

  int discoveryHide2(@Param("user") long user, @Param("note") long note);

  int discoveryReset(@Param("user") long user);
}
