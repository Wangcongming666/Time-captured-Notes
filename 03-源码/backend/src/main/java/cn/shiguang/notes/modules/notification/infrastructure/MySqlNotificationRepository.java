package cn.shiguang.notes.modules.notification.infrastructure;
import java.util.*;
import org.springframework.stereotype.Repository;
import cn.shiguang.notes.modules.notification.domain.NotificationRepository;
import cn.shiguang.notes.platform.persistence.Database;
import cn.shiguang.notes.shared.error.BizException;
@Repository
public class MySqlNotificationRepository implements NotificationRepository {
    private final Database db;
    public MySqlNotificationRepository(Database db){this.db=db;}
    public void insert(long u,String event,String type,long actor,String targetType,long target){db.update("INSERT IGNORE INTO t_notification(recipient_id,event_id,type,actor_id,target_type,target_id) VALUES(?,?,?,?,?,?)",u,event,type,actor,targetType,target);}
    public List<Map<String,Object>> list(long u,String type,long before,int size){return type==null||type.isBlank()?db.query("SELECT id,type,actor_id,target_type,target_id,read_at,created_at FROM t_notification WHERE recipient_id=? AND id<? ORDER BY id DESC LIMIT ?",u,before,size):db.query("SELECT id,type,actor_id,target_type,target_id,read_at,created_at FROM t_notification WHERE recipient_id=? AND type=? AND id<? ORDER BY id DESC LIMIT ?",u,type,before,size);}
    public long unread(long u){return db.count("SELECT COUNT(*) FROM t_notification WHERE recipient_id=? AND read_at IS NULL",u);}
    public void read(long u,long id){if(db.count("SELECT COUNT(*) FROM t_notification WHERE id=? AND recipient_id=?",id,u)==0)throw BizException.missing();db.update("UPDATE t_notification SET read_at=COALESCE(read_at,UTC_TIMESTAMP(3)) WHERE id=? AND recipient_id=?",id,u);}
    public void readAll(long u,long through){db.update("UPDATE t_notification SET read_at=UTC_TIMESTAMP(3) WHERE recipient_id=? AND id<=? AND read_at IS NULL",u,through);}
}
