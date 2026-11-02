package cn.shiguang.notes.mapper;

import cn.shiguang.notes.entity.*;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.*;
import org.apache.ibatis.annotations.*;

/**
 * Typed CRUD plus bounded, parameterized business queries. SQL is maintained in
 * mapper/MediaMapper.xml.
 */
@Mapper
public interface MediaMapper extends BaseMapper<MediaEntity> {
  MediaEntity contentMedia(@Param("id") long id, @Param("lock") boolean lock);

  int contentAvatar(@Param("previous") Long previous);

  int contentAvatar2(@Param("next") Long next);

  int contentBind(@Param("id") long id);

  int contentBind2(@Param("value0") Long value0);

  List<Map<String, Object>> mediaMaintenanceCleanup();

  int mediaMaintenanceCleanup2(@Param("id") long id);

  int mediaMaintenanceCleanup3(@Param("id") long id);
}
