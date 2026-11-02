package cn.shiguang.notes.mapper;

import cn.shiguang.notes.entity.*;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.*;
import org.apache.ibatis.annotations.*;

/**
 * Typed CRUD plus bounded, parameterized business queries. SQL is maintained in
 * mapper/FolderMapper.xml.
 */
@Mapper
public interface FolderMapper extends BaseMapper<FolderEntity> {
  FolderEntity collectionFolder(@Param("id") long id, @Param("lock") boolean lock);

  List<Map<String, Object>> collectionFolders(
      @Param("owner") long owner, @Param("before") long before, @Param("size") int size);

  int collectionRenameFolder(
      @Param("name") String name, @Param("id") long id, @Param("version") int version);

  int collectionDeleteFolder(@Param("id") long id);
}
