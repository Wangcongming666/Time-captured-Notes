package cn.shiguang.notes.mapper;

import cn.shiguang.notes.entity.*;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.*;
import org.apache.ibatis.annotations.*;

/**
 * Typed CRUD plus bounded, parameterized business queries. SQL is maintained in
 * mapper/NotificationMapper.xml.
 */
@Mapper
public interface NotificationMapper extends BaseMapper<NotificationEntity> {
  int notificationInsert(
      @Param("u") long u,
      @Param("event") String event,
      @Param("type") String type,
      @Param("actor") long actor,
      @Param("targetType") String targetType,
      @Param("target") long target);

  List<Map<String, Object>> notificationList(
      @Param("u") long u, @Param("before") long before, @Param("size") int size);

  List<Map<String, Object>> notificationList2(
      @Param("u") long u,
      @Param("type") String type,
      @Param("before") long before,
      @Param("size") int size);

  long notificationUnread(@Param("u") long u);

  long notificationRead(@Param("id") long id, @Param("u") long u);

  int notificationRead2(@Param("id") long id, @Param("u") long u);

  int notificationReadAll(@Param("u") long u, @Param("through") long through);
}
