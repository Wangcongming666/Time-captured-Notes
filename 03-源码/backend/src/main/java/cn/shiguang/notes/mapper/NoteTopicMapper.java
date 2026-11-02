package cn.shiguang.notes.mapper;

import cn.shiguang.notes.entity.*;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.*;
import org.apache.ibatis.annotations.*;

/**
 * Typed CRUD plus bounded, parameterized business queries. SQL is maintained in
 * mapper/NoteTopicMapper.xml.
 */
@Mapper
public interface NoteTopicMapper extends BaseMapper<NoteTopicEntity> {
  List<Map<String, Object>> contentTopicIds(@Param("id") long id);

  int contentTopics(@Param("id") long id);

  int contentTopics2(@Param("id") long id, @Param("t") long t);
}
