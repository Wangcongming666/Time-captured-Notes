package cn.shiguang.notes.modules.social.application;
import java.util.*;
import org.springframework.stereotype.Service;
import cn.shiguang.notes.modules.social.api.SocialApi;
import cn.shiguang.notes.modules.social.domain.SocialRepository;
import cn.shiguang.notes.shared.contract.*;
import cn.shiguang.notes.shared.error.BizException;
@Service
public class SocialService implements SocialApi {
    private final SocialRepository repo;
    public SocialService(SocialRepository repo){this.repo=repo;}
    public boolean like(long u,long n,boolean d){return repo.like(u,n,d);}
    public boolean follow(long from,long to,boolean desired){if(from==to)throw BizException.invalid("不能关注自己");return repo.follow(from,to,desired);}
    public Map<String,Object> comment(long id,boolean lock){return repo.comment(id,lock);}
    public long createComment(Actor actor,long note,Map<String,Object> input){String body=Data.text(input,"body");if(body.isBlank()||body.codePointCount(0,body.length())>500)throw BizException.invalid("评论长度须为1–500字");Long reply=input.get("replyToId")==null?null:Data.id(input,"replyToId"),root=null;if(reply!=null){var parent=repo.comment(reply,true);if(Data.id(parent,"noteId")!=note||!"VISIBLE".equals(parent.get("status")))throw BizException.missing();root=parent.get("rootId")==null?reply:Data.id(parent,"rootId");}return repo.createComment(actor.id(),note,root,reply,body);}
    public void deleteComment(Actor actor,long id){var c=repo.comment(id,true);if(Data.id(c,"authorId")!=actor.id())throw BizException.missing();if(!"DELETED".equals(c.get("status")))repo.state(id,"DELETED",Data.number(c,"version"));}
    public void hideComment(long id,int version){var c=repo.comment(id,true);if(Data.number(c,"version")!=version)throw BizException.conflict("评论版本变化");repo.state(id,"HIDDEN",version);}
    public List<Map<String,Object>> comments(long note,Long root,long before,int size){return repo.comments(note,root,before,size);}
    public List<Map<String,Object>> follows(long user,boolean followers,long before,int size){return repo.follows(user,followers,before,size);}
}
