package cn.shiguang.notes.modules.content.infrastructure;
import java.util.*;
import org.springframework.stereotype.Repository;
import cn.shiguang.notes.modules.content.domain.ContentRepository;
import cn.shiguang.notes.platform.persistence.Database;
import cn.shiguang.notes.shared.contract.Data;
import cn.shiguang.notes.shared.error.BizException;

/** All media rows are locked in numeric order before binding or releasing. */
@Repository
public class MySqlContentRepository implements ContentRepository {
    private final Database db;
    public MySqlContentRepository(Database db){this.db=db;}
    public Map<String,Object> note(long id,boolean lock){return db.one("SELECT * FROM t_note WHERE id=?"+(lock?" FOR UPDATE":""),id);}
    public List<Long> mediaIds(long id){return db.query("SELECT media_id FROM t_note_media WHERE note_id=? ORDER BY position",id).stream().map(m->Data.id(m,"mediaId")).toList();}
    public List<Long> topicIds(long id){return db.query("SELECT topic_id FROM t_note_topic WHERE note_id=? ORDER BY id",id).stream().map(m->Data.id(m,"topicId")).toList();}
    public List<Map<String,Object>> owned(long author,long before,int size){return db.query("SELECT * FROM t_note WHERE author_id=? AND id<? ORDER BY id DESC LIMIT ?",author,before,size);}
    public long create(long author,String title,String body){return db.insert("t_note",Data.map("author_id",author,"title",title,"body",body));}
    public void edit(long id,String title,String body,String status,int version){int n=db.update("UPDATE t_note SET title=?,body=?,status=?,revision=revision+1,version=version+1,reject_reason=NULL WHERE id=? AND version=?",title,body,status,id,version);if(n!=1)throw BizException.conflict("笔记已被修改，请刷新");}
    public void state(long id,String status,String reason,boolean newSubmission){db.update("UPDATE t_note SET status=?,reject_reason=?,version=version+1,revision=revision+?,published_at=IF(?='PUBLISHED',UTC_TIMESTAMP(3),published_at) WHERE id=?",status,reason,newSubmission?1:0,status,id);}
    public void topics(long id,List<Long> topics){db.update("DELETE FROM t_note_topic WHERE note_id=?",id);for(long t:topics)db.update("INSERT INTO t_note_topic(note_id,topic_id) VALUES(?,?)",id,t);}
    public boolean activeTopics(List<Long> ids){for(long id:ids)if(db.count("SELECT COUNT(*) FROM t_topic WHERE id=? AND status='ACTIVE'",id)!=1)return false;return true;}
    public Map<String,Object> media(long id,boolean lock){return db.one("SELECT * FROM t_media WHERE id=?"+(lock?" FOR UPDATE":""),id);}
    public long addMedia(Map<String,Object> values){return db.insert("t_media",values);}
    public void bind(long actor,List<Long> ids,String purpose,String type,long target){
        boolean note=type.equals("NOTE");String table=note?"t_note_media":"t_practice_media",column=note?"note_id":"practice_id";
        var old=db.query("SELECT media_id FROM "+table+" WHERE "+column+"=?",target).stream().map(r->Data.id(r,"mediaId")).toList();
        var all=new TreeSet<Long>(ids);all.addAll(old);
        for(long id:all){var m=media(id,true);if(Data.id(m,"ownerId")!=actor||!purpose.equals(m.get("purpose")))throw BizException.forbidden();if(!old.contains(id)&&!"STAGED".equals(m.get("status")))throw BizException.conflict("图片已被其他内容使用");}
        db.update("DELETE FROM "+table+" WHERE "+column+"=?",target);
        for(long id:old)if(!ids.contains(id))db.update("UPDATE t_media SET status='STAGED' WHERE id=?",id);
        for(int i=0;i<ids.size();i++){db.update("INSERT INTO "+table+"("+column+",media_id,position) VALUES(?,?,?)",target,ids.get(i),i+1);db.update("UPDATE t_media SET status='BOUND' WHERE id=?",ids.get(i));}
    }
}
