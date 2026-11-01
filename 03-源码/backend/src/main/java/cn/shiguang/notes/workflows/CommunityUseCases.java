package cn.shiguang.notes.workflows;

import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import cn.shiguang.notes.modules.identity.api.IdentityApi;
import cn.shiguang.notes.modules.content.api.ContentApi;
import cn.shiguang.notes.modules.collection.api.CollectionApi;
import cn.shiguang.notes.modules.social.api.SocialApi;
import cn.shiguang.notes.modules.governance.api.GovernanceApi;
import cn.shiguang.notes.modules.discovery.api.DiscoveryApi;
import cn.shiguang.notes.modules.notification.api.NotificationApi;
import cn.shiguang.notes.modules.statistics.api.StatisticsApi;
import cn.shiguang.notes.platform.application.Commands;
import cn.shiguang.notes.platform.readmodel.PublicReadModel;
import cn.shiguang.notes.shared.contract.*;
import cn.shiguang.notes.shared.error.BizException;

/** Explicit cross-context use cases; every business write runs in one MySQL transaction. */
@Service
public class CommunityUseCases {
    private final IdentityApi identity;private final ContentApi content;private final CollectionApi collections;
    private final SocialApi social;private final GovernanceApi governance;private final DiscoveryApi discovery;
    private final NotificationApi notifications;private final StatisticsApi statistics;private final EventPort events;
    private final Commands commands;private final PublicReadModel publicRead;
    public CommunityUseCases(IdentityApi identity,ContentApi content,CollectionApi collections,SocialApi social,GovernanceApi governance,DiscoveryApi discovery,NotificationApi notifications,StatisticsApi statistics,EventPort events,Commands commands,PublicReadModel publicRead){this.identity=identity;this.content=content;this.collections=collections;this.social=social;this.governance=governance;this.discovery=discovery;this.notifications=notifications;this.statistics=statistics;this.events=events;this.commands=commands;this.publicRead=publicRead;}
    private Map<String,Object> version(Map<String,Object> row){return Data.select(row,"id","version","status");}
    private void changed(Actor actor,String type,String aggregate,long id,Map<String,Object> payload){events.append(type,actor.id(),aggregate,id,0,payload,"statistics","cache");}
    private Map<String,Object> notePayload(Map<String,Object> note){return Data.map("noteId",note.get("id"),"authorId",note.get("authorId"));}

    @Transactional public Object saveNote(Actor actor,Long id,Map<String,Object> body,String key){return commands.run(actor,"note:save:"+id,key,body,()->{
        var row=content.save(actor,id,body);long target=Data.id(row,"id");governance.cancelReviews("NOTE",target);
        if("PENDING".equals(row.get("status")))governance.openReview("NOTE",target,Data.number(row,"revision"),actor.id());
        changed(actor,"NoteVisibilityChanged","NOTE",target,notePayload(row));return version(row);
    });}
    @Transactional public Object submit(Actor actor,long id,int v,String key){return commands.run(actor,"note:submit:"+id,key,Data.map("version",v),()->{
        var n=content.submit(actor,id,v);governance.openReview("NOTE",id,Data.number(n,"revision"),actor.id());changed(actor,"NoteSubmitted","NOTE",id,notePayload(n));return version(n);
    });}
    @Transactional public Object withdraw(Actor actor,long id,int v,String key){return commands.run(actor,"note:withdraw:"+id,key,Data.map("version",v),()->{var n=content.withdraw(actor,id,v);governance.cancelReviews("NOTE",id);changed(actor,"NoteVisibilityChanged","NOTE",id,notePayload(n));return version(n);});}
    @Transactional public void deleteNote(Actor actor,long id,int v){actor.requireLogin();var n=content.note(id,true);content.delete(actor,id,v);governance.cancelReviews("NOTE",id);changed(actor,"NoteVisibilityChanged","NOTE",id,notePayload(n));}

    @Transactional public void like(Actor actor,long id,boolean desired){actor.requireLogin();var n=desired?content.requirePublic(id,true):content.note(id,true);if(social.like(actor.id(),id,desired)){var p=notePayload(n);p.put("desired",desired);p.put("recipientId",n.get("authorId"));p.put("notificationType","LIKE");events.append("LikeChanged",actor.id(),"NOTE",id,0,p,desired?new String[]{"notification","statistics","cache"}:new String[]{"statistics","cache"});}}
    @Transactional public Object comment(Actor actor,long note,Map<String,Object> body,String key){return commands.run(actor,"comment:create:"+note,key,body,()->{var n=content.requirePublic(note,true);long id=social.createComment(actor,note,body);var p=notePayload(n);p.put("recipientId",n.get("authorId"));p.put("notificationType","COMMENT");events.append("CommentCreated",actor.id(),"NOTE",note,0,p,"notification","statistics","cache");return Map.of("id",Long.toString(id));});}
    @Transactional public void deleteComment(Actor actor,long id){actor.requireLogin();var c=social.comment(id,false);content.note(Data.id(c,"noteId"),true);social.deleteComment(actor,id);changed(actor,"CommentVisibilityChanged","NOTE",Data.id(c,"noteId"),Data.map("noteId",c.get("noteId")));}
    @Transactional public void follow(Actor actor,long id,boolean desired){actor.requireLogin();identity.profile(id,false);if(social.follow(actor.id(),id,desired)){events.append("FollowChanged",actor.id(),"USER",id,0,Data.map("recipientId",Long.toString(id),"notificationType","FOLLOW","desired",desired),desired?new String[]{"notification","statistics","cache"}:new String[]{"statistics","cache"});discovery.clear(actor.id());}}

    @Transactional public Object createFolder(Actor actor,Map<String,Object> body,String key){return commands.run(actor,"folder:create",key,body,()->Map.of("id",Long.toString(collections.createFolder(actor,Data.text(body,"name")))));}
    @Transactional public Object renameFolder(Actor actor,long id,Map<String,Object> body){actor.requireLogin();return collections.renameFolder(actor,id,Data.text(body,"name"),Data.number(body,"version"));}
    @Transactional public void deleteFolder(Actor actor,long id){actor.requireLogin();collections.deleteFolder(actor,id);events.append("FavoriteChanged",actor.id(),"USER",actor.id(),0,Map.of(),"statistics","cache");}
    @Transactional public void favorite(Actor actor,long folder,long note,boolean desired){actor.requireLogin();var n=desired?content.requirePublic(note,true):content.note(note,true);if(collections.favorite(actor,folder,note,desired))changed(actor,"FavoriteChanged","NOTE",note,notePayload(n));}
    @Transactional public Object createPractice(Actor actor,Map<String,Object> body,String key){return commands.run(actor,"practice:create",key,body,()->{long note=Data.id(body,"noteId");content.requirePublic(note,true);long id=collections.createPractice(actor,note);changed(actor,"PracticeChanged","PRACTICE",id,Map.of());return version(collections.practice(id,false));});}
    @Transactional public Object transition(Actor actor,long id,Map<String,Object> body,String key){return commands.run(actor,"practice:transition:"+id,key,body,()->{var p=collections.transition(actor,id,Data.number(body,"version"),Data.text(body,"status"));changed(actor,"PracticeChanged","PRACTICE",id,Map.of());return version(p);});}
    @Transactional public Object result(Actor actor,long id,Map<String,Object> body){actor.requireLogin();var p=collections.result(actor,id,body);var media=Data.ids(body,"mediaIds");if(media.size()>9)throw BizException.invalid("最多9张图片");content.bind(actor,media,"PRACTICE","PRACTICE",id);governance.cancelReviews("PRACTICE",id);changed(actor,"FeedbackVisibilityChanged","PRACTICE",id,Map.of());return version(p);}
    @Transactional public Object publishFeedback(Actor actor,long id,int v,String key){return commands.run(actor,"practice:publish:"+id,key,Data.map("version",v),()->{var initial=collections.ownPractice(actor,id,false);var n=content.requirePublic(Data.id(initial,"noteId"),true);var p=collections.publishRequest(actor,id,v,Data.number(n,"revision"));governance.cancelReviews("PRACTICE",id);governance.openReview("PRACTICE",id,Data.number(p,"feedbackRevision"),actor.id());return version(p);});}
    @Transactional public Object unpublishFeedback(Actor actor,long id,int v,String key){return commands.run(actor,"practice:unpublish:"+id,key,Data.map("version",v),()->{var p=collections.unpublish(actor,id,v);governance.cancelReviews("PRACTICE",id);changed(actor,"FeedbackVisibilityChanged","PRACTICE",id,Map.of());return version(p);});}

    @Transactional public Object decideReview(Actor actor,long id,Map<String,Object> body,String key){actor.require("REVIEWER");return commands.run(actor,"review:decision:"+id,key,body,()->{
        var before=governance.review(id,false);String type=Data.text(before,"targetType");long target=Data.id(before,"targetId");int revision=Data.number(before,"targetRevision");String decision=Data.text(body,"decision"),reason=Data.text(body,"reason");Map<String,Object> note;
        if(type.equals("NOTE")){note=content.note(target,true);}else{var p=collections.practice(target,false);note=content.requirePublic(Data.id(p,"noteId"),true);if(Data.number(p,"sourceRevision")!=Data.number(note,"revision"))throw BizException.conflict("源笔记版本已改变");}
        governance.decide(actor,id,Data.number(body,"version"),decision,reason);
        if(type.equals("NOTE"))content.moderate(target,revision,decision,reason);else collections.moderate(target,revision,decision);
        governance.audit(actor,"REVIEW_"+decision,type,target,Data.select(before,"status","targetRevision"),Data.map("decision",decision),reason);
        var payload=notePayload(note);payload.put("decision",decision);payload.put("recipientId",before.get("applicantId"));payload.put("notificationType","REVIEW");events.append("ReviewDecided",actor.id(),type,target,revision,payload,"notification","statistics","cache");return version(governance.review(id,false));
    });}
    @Transactional public void offlineNote(Actor actor,long id,Map<String,Object> body){actor.require("REVIEWER");String reason=reason(body);var n=content.note(id,true);content.offline(id,Data.number(body,"version"),reason);governance.audit(actor,"NOTE_OFFLINE","NOTE",id,Data.select(n,"status"),Map.of("status","OFFLINE"),reason);events.append("NoteVisibilityChanged",actor.id(),"NOTE",id,Data.number(n,"revision"),Data.map("noteId",Long.toString(id),"authorId",n.get("authorId"),"recipientId",n.get("authorId"),"notificationType","REVIEW"),"notification","statistics","cache");}
    @Transactional public void hideComment(Actor actor,long id,Map<String,Object> body){actor.require("REVIEWER");String reason=reason(body);var c=social.comment(id,false);content.note(Data.id(c,"noteId"),true);social.hideComment(id,Data.number(body,"version"));governance.audit(actor,"COMMENT_HIDE","COMMENT",id,Data.select(c,"status"),Map.of("status","HIDDEN"),reason);changed(actor,"CommentVisibilityChanged","NOTE",Data.id(c,"noteId"),Data.map("noteId",c.get("noteId")));}
    @Transactional public void offlineFeedback(Actor actor,long id,Map<String,Object> body){actor.require("REVIEWER");var p=collections.practice(id,true);collections.offline(id,Data.number(body,"version"));governance.audit(actor,"FEEDBACK_OFFLINE","PRACTICE",id,Data.select(p,"publicStatus"),Map.of("publicStatus","OFFLINE"),reason(body));changed(actor,"FeedbackVisibilityChanged","PRACTICE",id,Map.of());}
    private String reason(Map<String,Object> body){String r=Data.text(body,"reason");if(r.isBlank()||r.length()>500)throw BizException.invalid("请填写1–500字理由");return r;}
    @Transactional public Object report(Actor actor,Map<String,Object> body,String key){return commands.run(actor,"report:create",key,body,()->{String type=Data.text(body,"targetType");long target=Data.id(body,"targetId");if(!publicRead.targetVisible(type,target))throw BizException.missing();return Map.of("id",Long.toString(governance.report(actor,type,target,reason(body))));});}
    @Transactional public Object decideReport(Actor actor,long id,Map<String,Object> body,String key){actor.require("REVIEWER");return commands.run(actor,"report:decision:"+id,key,body,()->{
        var report=governance.report(id,false);String type=Data.text(report,"targetType"),status=Data.text(body,"status");long target=Data.id(report,"targetId");
        if(status.equals("UPHELD")&&publicRead.targetVisible(type,target)){if(type.equals("NOTE")){var n=content.note(target,true);content.offline(target,Data.number(n,"version"),reason(body));changed(actor,"NoteVisibilityChanged","NOTE",target,notePayload(n));}else if(type.equals("COMMENT")){var c=social.comment(target,false);content.note(Data.id(c,"noteId"),true);social.hideComment(target,Data.number(c,"version"));changed(actor,"CommentVisibilityChanged","NOTE",Data.id(c,"noteId"),Data.map("noteId",c.get("noteId")));}else{var p=collections.practice(target,true);collections.offline(target,Data.number(p,"version"));}}
        governance.decideReport(actor,id,Data.number(body,"version"),status,reason(body));governance.audit(actor,"REPORT_"+status,"REPORT",id,Data.select(report,"status"),Map.of("status",status),reason(body));
        if(!status.equals("PROCESSING"))events.append("ReportDecided",actor.id(),"REPORT",id,0,Data.map("recipientId",report.get("reporterId"),"notificationType","REPORT"),"notification");return version(governance.report(id,false));
    });}
    @Transactional public Object createTopic(Actor actor,Map<String,Object> body,String key){actor.require("ADMIN");return commands.run(actor,"topic:create",key,body,()->{long id=governance.createTopic(body);governance.audit(actor,"TOPIC_CREATE","TOPIC",id,Map.of(),body,"新增话题");return Map.of("id",Long.toString(id));});}
    @Transactional public Object updateTopic(Actor actor,long id,Map<String,Object> body){actor.require("ADMIN");var result=governance.updateTopic(id,body);governance.audit(actor,"TOPIC_UPDATE","TOPIC",id,Map.of(),body,"更新话题");return result;}
    @Transactional public void preferences(Actor actor,Map<String,Object> body){actor.requireLogin();discovery.preferences(actor.id(),body);}
    @Transactional public void hide(Actor actor,long id,boolean hidden){actor.requireLogin();if(hidden)content.requirePublic(id,false);discovery.hide(actor.id(),id,hidden);}
    @Transactional public void resetPreferences(Actor actor){actor.requireLogin();discovery.reset(actor.id());}
    @Transactional public void readNotification(Actor actor,long id){actor.requireLogin();notifications.read(actor.id(),id);}
    @Transactional public void readAll(Actor actor,long through){actor.requireLogin();notifications.readAll(actor.id(),through);}
    @Transactional public void active(Actor actor){statistics.active(actor.id());}
}
