package cn.shiguang.notes.schedule;

import cn.shiguang.notes.common.contract.MediaStore;
import cn.shiguang.notes.common.util.Data;
import cn.shiguang.notes.entity.*;
import cn.shiguang.notes.mapper.*;
import cn.shiguang.notes.repository.support.MapperRows;
import java.util.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Bounded orphan cleanup: mark under lock, unlink after commit, retry failures on the next pass.
 */
@Component
public class MediaMaintenance {
  private final MediaMapper mediaMapper;
  private final TransactionTemplate tx;
  private final MediaStore store;

  public MediaMaintenance(MediaMapper mediaMapper, TransactionTemplate tx, MediaStore store) {
    this.mediaMapper = mediaMapper;
    this.tx = tx;
    this.store = store;
  }

  @Scheduled(fixedDelay = 600000, initialDelay = 600000)
  public void cleanup() {
    var candidates =
        tx.execute(
            s -> {
              var rows = MapperRows.list(mediaMapper.mediaMaintenanceCleanup());
              for (var row : rows) mediaMapper.mediaMaintenanceCleanup2(Data.id(row, "id"));
              return rows;
            });
    if (candidates == null) return;
    for (var row : candidates)
      try {
        store.delete(Data.text(row, "storageKey"));
        if (!Data.text(row, "posterStorageKey").isEmpty())
          store.delete(Data.text(row, "posterStorageKey"));
        tx.execute(s -> mediaMapper.mediaMaintenanceCleanup3(Data.id(row, "id")));
      } catch (Exception e) {
        org.slf4j.LoggerFactory.getLogger(MediaMaintenance.class)
            .warn(
                "Orphan cleanup will retry media={} type={}",
                row.get("id"),
                e.getClass().getSimpleName());
      }
  }
}
