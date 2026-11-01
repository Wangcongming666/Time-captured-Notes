package cn.shiguang.notes.platform.readmodel;
import java.util.*;
import org.springframework.stereotype.Repository;
import cn.shiguang.notes.platform.persistence.Database;
import cn.shiguang.notes.shared.contract.*;
/** Read-only cross-context projections; no cached authorization. */
@Repository
public class PublicReadModel {
    private final Database db;
    public PublicReadModel(Database db){this.db=db;}
    public boolean noteVisible(long id){return db.count("SELECT COUNT(*) FROM t_note WHERE id=? AND status='PUBLISHED'",id)==1;}
    public boolean practiceVisible(long id){return db.count("SELECT COUNT(*) FROM t_practice p JOIN t_note n ON n.id=p.note_id WHERE p.id=? AND p.public_status='PUBLISHED' AND n.status='PUBLISHED' AND p.source_revision=n.revision",id)==1;}
    public boolean commentVisible(long id){return db.count("SELECT COUNT(*) FROM t_comment c JOIN t_note n ON n.id=c.note_id WHERE c.id=? AND c.status='VISIBLE' AND n.status='PUBLISHED'",id)==1;}
    public boolean targetVisible(String type,long id){return switch(type){case "NOTE"->noteVisible(id);case "COMMENT"->commentVisible(id);case "PRACTICE"->practiceVisible(id);case "USER"->db.count("SELECT COUNT(*) FROM t_user WHERE id=? AND status='ACTIVE'",id)==1;default->false;};}
    public boolean mediaAllowed(Actor actor,Map<String,Object> media){long id=Data.id(media,"id");if("DELETED".equals(media.get("status")))return false;if(actor.id()>0&&Data.id(media,"ownerId")==actor.id())return true;
        if(db.count("SELECT COUNT(*) FROM t_note_media nm JOIN t_note n ON n.id=nm.note_id WHERE nm.media_id=? AND n.status='PUBLISHED'",id)>0)return true;
        if(db.count("SELECT COUNT(*) FROM t_practice_media pm JOIN t_practice p ON p.id=pm.practice_id JOIN t_note n ON n.id=p.note_id WHERE pm.media_id=? AND p.public_status='PUBLISHED' AND n.status='PUBLISHED' AND p.source_revision=n.revision",id)>0)return true;
        if(db.count("SELECT COUNT(*) FROM t_user WHERE avatar_media_id=? AND status='ACTIVE'",id)>0)return true;
        return actor.has("REVIEWER")&&db.count("SELECT COUNT(*) FROM t_review r WHERE r.status='PENDING' AND r.applicant_id<>? AND ((r.target_type='NOTE' AND EXISTS(SELECT 1 FROM t_note n JOIN t_note_media nm ON nm.note_id=n.id WHERE n.id=r.target_id AND n.revision=r.target_revision AND nm.media_id=?)) OR (r.target_type='PRACTICE' AND EXISTS(SELECT 1 FROM t_practice p JOIN t_practice_media pm ON pm.practice_id=p.id WHERE p.id=r.target_id AND p.feedback_revision=r.target_revision AND pm.media_id=?)))",actor.id(),id,id)>0;
    }
    public Map<String,Object> interaction(long user,long note){return db.one("SELECT (SELECT COUNT(*)>0 FROM t_like WHERE user_id=? AND note_id=?) AS liked,(SELECT COUNT(*)>0 FROM t_favorite WHERE user_id=? AND note_id=?) AS favorite,COALESCE((SELECT like_count FROM t_note_metric WHERE note_id=?),0) AS like_count,COALESCE((SELECT favorite_count FROM t_note_metric WHERE note_id=?),0) AS favorite_count,COALESCE((SELECT comment_count FROM t_note_metric WHERE note_id=?),0) AS comment_count",user,note,user,note,note,note,note);}
    public List<Map<String,Object>> feedback(long note,long before,int size){return db.query("SELECT p.id,p.note_id,p.owner_id AS author_id,p.result_text,p.helpfulness FROM t_practice p JOIN t_note n ON n.id=p.note_id WHERE p.note_id=? AND p.id<? AND p.public_status='PUBLISHED' AND n.status='PUBLISHED' AND p.source_revision=n.revision ORDER BY p.id DESC LIMIT ?",note,before,size);}
    public List<Map<String,Object>> users(long before,int size){return db.query("SELECT id,username,nickname,status FROM t_user WHERE id<? ORDER BY id DESC LIMIT ?",before,size);}
}
