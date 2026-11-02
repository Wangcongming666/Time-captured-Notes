package cn.shiguang.notes.mapper;

import cn.shiguang.notes.entity.*;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.*;
import org.apache.ibatis.annotations.*;

/**
 * Typed CRUD plus bounded, parameterized business queries. SQL is maintained in
 * mapper/FavoriteMapper.xml.
 */
@Mapper
public interface FavoriteMapper extends BaseMapper<FavoriteEntity> {
  int collectionDeleteFolder(@Param("id") long id);

  int collectionFavorite(
      @Param("user") long user, @Param("folder") long folder, @Param("note") long note);

  int collectionFavorite2(@Param("folder") long folder, @Param("note") long note);

  long collectionHasFavorite(@Param("user") long user, @Param("note") long note);

  List<Map<String, Object>> collectionFavorites(
      @Param("folder") long folder, @Param("before") long before, @Param("size") int size);
}
