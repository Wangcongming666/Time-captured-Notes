package cn.shiguang.notes.mapper;

import cn.shiguang.notes.entity.*;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.*;
import org.apache.ibatis.annotations.*;

/**
 * Typed CRUD plus bounded, parameterized business queries. SQL is maintained in
 * mapper/NoteMetricMapper.xml.
 */
@Mapper
public interface NoteMetricMapper extends BaseMapper<NoteMetricEntity> {
  int statisticsRecalculate(
      @Param("note") long note,
      @Param("note2") long note2,
      @Param("note3") long note3,
      @Param("note4") long note4,
      @Param("note5") long note5);

  int statisticsReconcileAll();
}
