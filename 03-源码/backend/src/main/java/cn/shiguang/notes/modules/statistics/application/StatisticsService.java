package cn.shiguang.notes.modules.statistics.application;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import cn.shiguang.notes.modules.statistics.api.StatisticsApi;
import cn.shiguang.notes.modules.statistics.domain.StatisticsRepository;
import cn.shiguang.notes.shared.contract.*;
import cn.shiguang.notes.shared.error.BizException;
@Service
public class StatisticsService implements StatisticsApi {
    private final StatisticsRepository repo;private final EventPort events;
    public StatisticsService(StatisticsRepository repo,EventPort events){this.repo=repo;this.events=events;}
    @SuppressWarnings("unchecked") public void receive(Map<String,Object> event){var p=(Map<String,Object>)event.get("payload");String type=Data.text(event,"eventType"),day=Instant.parse(Data.text(event,"occurredAt")).atZone(ZoneId.of("Asia/Shanghai")).toLocalDate().toString();long note=p.get("noteId")==null?0:Data.id(p,"noteId");
        if(type.equals("NoteViewed"))repo.incrementView(note,Data.id(p,"authorId"),day);
        String metric=switch(type){case "LikeChanged" -> Boolean.TRUE.equals(p.get("desired"))?"LIKE_ADD":"LIKE_REMOVE";case "CommentCreated" -> "COMMENT_ADD";case "FollowChanged" -> Boolean.TRUE.equals(p.get("desired"))?"FOLLOW_ADD":"FOLLOW_REMOVE";case "ReviewDecided" -> "APPROVE".equals(p.get("decision"))&&"NOTE".equals(event.get("aggregateType"))?"NOTE_PUBLISHED":"";default -> "";};
        if(!metric.isEmpty()){repo.metric(day,"PLATFORM",0,metric,1,true);if(p.get("authorId")!=null)repo.metric(day,"AUTHOR",Data.id(p,"authorId"),metric,1,true);}
        if(note>0)repo.recalculate(note);
    }
    public void viewed(long viewer,String subject,long note,long author){if(repo.view(Tokens.hash(subject),note))events.append("NoteViewed",viewer,"NOTE",note,0,Data.map("noteId",Long.toString(note),"authorId",Long.toString(author)),"statistics");}
    public void active(long user){if(user>0)repo.active(user);}
    public Map<String,Object> dashboard(Actor actor,String scope,int days){if(days!=7&&days!=30)throw BizException.invalid("统计范围只能为7或30日");List<Map<String,Object>> points;if(scope.equals("GOVERNANCE")){if(!actor.has("REVIEWER")&&!actor.has("ADMIN"))throw BizException.forbidden();points=repo.governance(days);}else{if(scope.equals("PLATFORM"))actor.require("ADMIN");else actor.requireLogin();points=repo.points(scope,scope.equals("AUTHOR")?actor.id():0,days);}return Data.map("points",points,"calculatedAt",Instant.now().toString(),"timezone","Asia/Shanghai");}
    public List<Map<String,Object>> ranking(Actor actor,boolean platform,long before,int size){if(platform)actor.require("ADMIN");else actor.requireLogin();return repo.ranking(platform?null:actor.id(),before,size);}
    public void reconcile(){long after=0;while(true){var rows=repo.notes(after,100);if(rows.isEmpty())return;for(var row:rows){after=Data.id(row,"id");repo.recalculate(after);}}}
}
