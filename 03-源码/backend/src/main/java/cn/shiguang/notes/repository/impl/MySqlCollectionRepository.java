package cn.shiguang.notes.repository.impl;

import cn.shiguang.notes.common.exception.BizException;
import cn.shiguang.notes.common.util.Data;
import cn.shiguang.notes.entity.*;
import cn.shiguang.notes.mapper.*;
import cn.shiguang.notes.repository.CollectionRepository;
import cn.shiguang.notes.repository.support.EntityWrites;
import cn.shiguang.notes.repository.support.MapperRows;
import java.util.*;
import org.springframework.stereotype.Repository;

@Repository
public class MySqlCollectionRepository implements CollectionRepository {
  private final FavoriteMapper favoriteMapper;
  private final FolderMapper folderMapper;
  private final PracticeHistoryMapper practiceHistoryMapper;
  private final PracticeMapper practiceMapper;
  private final PracticeMediaMapper practiceMediaMapper;

  public MySqlCollectionRepository(
      FavoriteMapper favoriteMapper,
      FolderMapper folderMapper,
      PracticeHistoryMapper practiceHistoryMapper,
      PracticeMapper practiceMapper,
      PracticeMediaMapper practiceMediaMapper) {
    this.favoriteMapper = favoriteMapper;
    this.folderMapper = folderMapper;
    this.practiceHistoryMapper = practiceHistoryMapper;
    this.practiceMapper = practiceMapper;
    this.practiceMediaMapper = practiceMediaMapper;
  }

  public Map<String, Object> folder(long id, boolean lock) {
    return MapperRows.requiredEntity(folderMapper.collectionFolder(id, lock));
  }

  public List<Map<String, Object>> folders(long owner, long before, int size) {
    return MapperRows.list(folderMapper.collectionFolders(owner, before, size));
  }

  public long createFolder(long owner, String name) {
    return EntityWrites.insert(
        folderMapper, FolderEntity.class, Data.map("owner_id", owner, "name", name));
  }

  public void renameFolder(long id, String name, int version) {
    if (folderMapper.collectionRenameFolder(name, id, version) != 1)
      throw BizException.conflict("收藏夹已变化");
  }

  public void deleteFolder(long id) {
    favoriteMapper.collectionDeleteFolder(id);
    folderMapper.collectionDeleteFolder(id);
  }

  public boolean favorite(long user, long folder, long note, boolean desired) {
    return desired
        ? favoriteMapper.collectionFavorite(user, folder, note) > 0
        : favoriteMapper.collectionFavorite2(folder, note) > 0;
  }

  public boolean hasFavorite(long user, long note) {
    return favoriteMapper.collectionHasFavorite(user, note) > 0;
  }

  public List<Map<String, Object>> favorites(long folder, long before, int size) {
    return MapperRows.list(favoriteMapper.collectionFavorites(folder, before, size));
  }

  public long createPractice(long owner, long note) {
    long id =
        EntityWrites.insert(
            practiceMapper, PracticeEntity.class, Data.map("owner_id", owner, "note_id", note));
    EntityWrites.insert(
        practiceHistoryMapper,
        PracticeHistoryEntity.class,
        Data.map(
            "practice_id", id, "from_status", null, "to_status", "PLANNED", "actor_id", owner));
    return id;
  }

  public Map<String, Object> practice(long id, boolean lock) {
    var p = MapperRows.requiredEntity(practiceMapper.collectionPractice(id, lock));
    p.put(
        "mediaIds",
        MapperRows.list(practiceMediaMapper.collectionPractice(id)).stream()
            .map(r -> Data.text(r, "mediaId"))
            .toList());
    return p;
  }

  public List<Map<String, Object>> practices(long owner, long before, int size) {
    return MapperRows.entities(practiceMapper.collectionPractices(owner, before, size));
  }

  public void transition(long id, long actor, String from, String to) {
    practiceMapper.collectionTransition(to, to, to, id);
    EntityWrites.insert(
        practiceHistoryMapper,
        PracticeHistoryEntity.class,
        Data.map("practice_id", id, "from_status", from, "to_status", to, "actor_id", actor));
  }

  public void result(long id, String text, String helpfulness) {
    practiceMapper.collectionResult(text, helpfulness, id);
  }

  public void publicState(long id, String state, Integer source, boolean newRevision) {
    practiceMapper.collectionPublicState(state, source, newRevision ? 1 : 0, id);
  }

  public List<Map<String, Object>> history(long id, long before, int size) {
    return MapperRows.list(practiceHistoryMapper.collectionHistory(id, before, size));
  }
}
