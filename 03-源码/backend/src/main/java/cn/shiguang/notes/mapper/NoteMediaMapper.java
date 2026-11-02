package cn.shiguang.notes.mapper;

import cn.shiguang.notes.entity.*;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.*;
import org.apache.ibatis.annotations.*;

/**
 * Typed CRUD plus bounded, parameterized business queries. SQL is maintained in
 * mapper/NoteMediaMapper.xml.
 */
@Mapper
public interface NoteMediaMapper extends BaseMapper<NoteMediaEntity> {
  List<Map<String, Object>> contentMediaIds(@Param("id") long id);

  List<Map<String, Object>> contentMediaEntries(@Param("id") long id);

  long publicQueryMediaAllowed(@Param("id") long id);

  List<Map<String, Object>> bindingMedia(@Param("target") long target);

  int deleteBindings(@Param("target") long target);

  int insertBinding(
      @Param("target") long target,
      @Param("mediaId") long mediaId,
      @Param("position") int position);
}
