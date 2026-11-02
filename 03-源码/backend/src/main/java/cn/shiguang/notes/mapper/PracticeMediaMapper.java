package cn.shiguang.notes.mapper;

import cn.shiguang.notes.entity.*;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.*;
import org.apache.ibatis.annotations.*;

/**
 * Typed CRUD plus bounded, parameterized business queries. SQL is maintained in
 * mapper/PracticeMediaMapper.xml.
 */
@Mapper
public interface PracticeMediaMapper extends BaseMapper<PracticeMediaEntity> {
  List<Map<String, Object>> collectionPractice(@Param("id") long id);

  long publicQueryMediaAllowed(@Param("id") long id);

  List<Map<String, Object>> bindingMedia(@Param("target") long target);

  int deleteBindings(@Param("target") long target);

  int insertBinding(
      @Param("target") long target,
      @Param("mediaId") long mediaId,
      @Param("position") int position);
}
