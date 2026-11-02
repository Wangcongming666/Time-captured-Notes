package cn.shiguang.notes.schedule;

import cn.shiguang.notes.common.contract.MediaStore;
import cn.shiguang.notes.common.util.Data;
import cn.shiguang.notes.repository.support.Database;
import java.util.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Bounded orphan cleanup: mark under lock, unlink after commit, retry failures on the next pass.
 */
@Component
public class MediaMaintenance {
  private final Database db;
  private final TransactionTemplate tx;
  private final MediaStore store;

  public MediaMaintenance(Database db, TransactionTemplate tx, MediaStore store) {
    this.db = db;
    this.tx = tx;
    this.store = store;
  }

  @Scheduled(fixedDelay = 600000, initialDelay = 600000)
  public void cleanup() {
    var candidates =
        tx.execute(
            s -> {
              var rows =
                  db.query(
                      "SELECT m.id,m.storage_key,m.poster_storage_key FROM t_media m WHERE m.status"
                          + " IN ('STAGED','DELETED') AND"
                          + " m.created_at<DATE_SUB(UTC_TIMESTAMP(),INTERVAL 24 HOUR) AND NOT"
                          + " EXISTS(SELECT 1 FROM t_note_media n WHERE n.media_id=m.id) AND NOT"
                          + " EXISTS(SELECT 1 FROM t_practice_media p WHERE p.media_id=m.id) AND"
                          + " NOT EXISTS(SELECT 1 FROM t_user u WHERE u.avatar_media_id=m.id) ORDER"
                          + " BY m.id LIMIT 100 FOR UPDATE SKIP LOCKED");
              for (var row : rows)
                db.update("UPDATE t_media SET status='DELETED' WHERE id=?", Data.id(row, "id"));
              return rows;
            });
    if (candidates == null) return;
    for (var row : candidates)
      try {
        store.delete(Data.text(row, "storageKey"));
        if (!Data.text(row, "posterStorageKey").isEmpty())
          store.delete(Data.text(row, "posterStorageKey"));
        tx.execute(
            s ->
                db.update(
                    "DELETE FROM t_media WHERE id=? AND status='DELETED'", Data.id(row, "id")));
      } catch (Exception e) {
        org.slf4j.LoggerFactory.getLogger(MediaMaintenance.class)
            .warn(
                "Orphan cleanup will retry media={} type={}",
                row.get("id"),
                e.getClass().getSimpleName());
      }
  }
}
