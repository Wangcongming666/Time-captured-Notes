package cn.shiguang.notes.mapper;

import cn.shiguang.notes.entity.*;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.*;
import org.apache.ibatis.annotations.*;

/**
 * Typed CRUD plus bounded, parameterized business queries. SQL is maintained in
 * mapper/ViewWindowMapper.xml.
 */
@Mapper
public interface ViewWindowMapper extends BaseMapper<ViewWindowEntity> {
  int statisticsView(@Param("subject") String subject, @Param("note") long note);
}
