package cn.shiguang.notes.web;
import java.util.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;
import cn.shiguang.notes.platform.security.RequestSecurityFilter;
import cn.shiguang.notes.shared.contract.*;
import cn.shiguang.notes.workflows.*;
import cn.shiguang.notes.workflows.content.MediaUseCases;

/** Thin transport adapter. Business decisions live in use cases and modules. */
@RestController @RequestMapping("/api/v1")
public class CommunityController {
    private final CommunityUseCases write;private final QueryUseCases query;private final MediaUseCases media;
    public CommunityController(CommunityUseCases write,QueryUseCases query,MediaUseCases media){this.write=write;this.query=query;this.media=media;}
    private Actor actor(HttpServletRequest r){return RequestSecurityFilter.actor(r);}
    private String key(HttpServletRequest r){return r.getHeader("Idempotency-Key");}
    private ApiResponse<?> ok(Object data){return ApiResponse.ok(data);}
    private ApiResponse<?> empty(){return ok(Map.of());}
    @PostMapping("/media") public ApiResponse<?> upload(@RequestParam MultipartFile file,@RequestParam String purpose,HttpServletRequest r)throws Exception{return ok(media.upload(actor(r),file,purpose));}
    @GetMapping("/media/{id}/content") public ResponseEntity<Resource> image(@PathVariable long id,HttpServletRequest r)throws Exception{var data=media.read(actor(r),id);return ResponseEntity.ok().contentType(MediaType.parseMediaType(Data.text(data,"mime"))).cacheControl(CacheControl.noStore()).header("X-Content-Type-Options","nosniff").body((Resource)data.get("resource"));}
    @PostMapping("/notes") public ApiResponse<?> createNote(@RequestBody Map<String,Object>b,HttpServletRequest r){return ok(write.saveNote(actor(r),null,b,key(r)));}
    @PutMapping("/notes/{id}") public ApiResponse<?> editNote(@PathVariable long id,@RequestBody Map<String,Object>b,HttpServletRequest r){return ok(write.saveNote(actor(r),id,b,key(r)));}
    @GetMapping("/notes/{id}") public ApiResponse<?> note(@PathVariable long id,HttpServletRequest r){String subject=Objects.toString(r.getAttribute("shiguang.visitor"));return ok(query.note(actor(r),id,actor(r).id()>0?"user:"+actor(r).id():subject));}
    @GetMapping("/me/notes/{id}") public ApiResponse<?> ownNote(@PathVariable long id,HttpServletRequest r){return ok(query.ownNote(actor(r),id));}
    @GetMapping("/me/notes") public ApiResponse<?> ownNotes(@RequestParam(required=false)String cursor,@RequestParam(defaultValue="20")int size,HttpServletRequest r){return ok(query.ownNotes(actor(r),cursor,size));}
    @PostMapping("/notes/{id}/submit") public ApiResponse<?> submit(@PathVariable long id,@RequestBody Map<String,Object>b,HttpServletRequest r){return ok(write.submit(actor(r),id,Data.number(b,"version"),key(r)));}
    @PostMapping("/notes/{id}/withdraw") public ApiResponse<?> withdraw(@PathVariable long id,@RequestBody Map<String,Object>b,HttpServletRequest r){return ok(write.withdraw(actor(r),id,Data.number(b,"version"),key(r)));}
    @DeleteMapping("/notes/{id}") public ApiResponse<?> deleteNote(@PathVariable long id,@RequestParam int version,HttpServletRequest r){write.deleteNote(actor(r),id,version);return empty();}
    @GetMapping("/feed") public ApiResponse<?> feed(@RequestParam Map<String,String>q,HttpServletRequest r){return ok(query.feed(actor(r),q));}
    @GetMapping("/search") public ApiResponse<?> search(@RequestParam Map<String,String>q,HttpServletRequest r){var copy=new HashMap<>(q);copy.put("mode","search");return ok(query.feed(actor(r),copy));}
    @GetMapping("/me/preferences") public ApiResponse<?> preferences(HttpServletRequest r){return ok(query.preferences(actor(r)));}
    @PutMapping("/me/preferences") public ApiResponse<?> setPreferences(@RequestBody Map<String,Object>b,HttpServletRequest r){write.preferences(actor(r),b);return preferences(r);}
    @DeleteMapping("/me/preferences") public ApiResponse<?> resetPreferences(HttpServletRequest r){write.resetPreferences(actor(r));return empty();}
    @PutMapping("/me/hidden-notes/{id}") public ApiResponse<?> hide(@PathVariable long id,HttpServletRequest r){write.hide(actor(r),id,true);return empty();}
    @DeleteMapping("/me/hidden-notes/{id}") public ApiResponse<?> unhide(@PathVariable long id,HttpServletRequest r){write.hide(actor(r),id,false);return empty();}
    @PutMapping("/notes/{id}/like") public ApiResponse<?> like(@PathVariable long id,HttpServletRequest r){write.like(actor(r),id,true);return empty();}
    @DeleteMapping("/notes/{id}/like") public ApiResponse<?> unlike(@PathVariable long id,HttpServletRequest r){write.like(actor(r),id,false);return empty();}
    @PostMapping("/notes/{id}/comments") @ResponseStatus(HttpStatus.CREATED) public ApiResponse<?> comment(@PathVariable long id,@RequestBody Map<String,Object>b,HttpServletRequest r){return ok(write.comment(actor(r),id,b,key(r)));}
    @GetMapping("/notes/{id}/comments") public ApiResponse<?> comments(@PathVariable long id,@RequestParam(required=false)String cursor,@RequestParam(defaultValue="20")int size,HttpServletRequest r){return ok(query.comments(actor(r),id,null,cursor,size));}
    @GetMapping("/comments/{id}/replies") public ApiResponse<?> replies(@PathVariable long id,@RequestParam(required=false)String cursor,@RequestParam(defaultValue="20")int size,HttpServletRequest r){return ok(query.replies(actor(r),id,cursor,size));}
    @DeleteMapping("/comments/{id}") public ApiResponse<?> deleteComment(@PathVariable long id,HttpServletRequest r){write.deleteComment(actor(r),id);return empty();}
    @PutMapping("/users/{id}/follow") public ApiResponse<?> follow(@PathVariable long id,HttpServletRequest r){write.follow(actor(r),id,true);return empty();}
    @DeleteMapping("/users/{id}/follow") public ApiResponse<?> unfollow(@PathVariable long id,HttpServletRequest r){write.follow(actor(r),id,false);return empty();}
    @GetMapping("/users/{id}/following") public ApiResponse<?> following(@PathVariable long id,@RequestParam(required=false)String cursor,@RequestParam(defaultValue="20")int size,HttpServletRequest r){return ok(query.follows(actor(r),id,false,cursor,size));}
    @GetMapping("/users/{id}/followers") public ApiResponse<?> followers(@PathVariable long id,@RequestParam(required=false)String cursor,@RequestParam(defaultValue="20")int size,HttpServletRequest r){return ok(query.follows(actor(r),id,true,cursor,size));}
    @PostMapping("/folders") @ResponseStatus(HttpStatus.CREATED) public ApiResponse<?> createFolder(@RequestBody Map<String,Object>b,HttpServletRequest r){return ok(write.createFolder(actor(r),b,key(r)));}
    @GetMapping("/folders") public ApiResponse<?> folders(@RequestParam(required=false)String cursor,@RequestParam(defaultValue="20")int size,HttpServletRequest r){return ok(query.folders(actor(r),cursor,size));}
    @PutMapping("/folders/{id}") public ApiResponse<?> renameFolder(@PathVariable long id,@RequestBody Map<String,Object>b,HttpServletRequest r){return ok(write.renameFolder(actor(r),id,b));}
    @DeleteMapping("/folders/{id}") public ApiResponse<?> deleteFolder(@PathVariable long id,HttpServletRequest r){write.deleteFolder(actor(r),id);return empty();}
    @GetMapping("/folders/{id}/notes") public ApiResponse<?> favorites(@PathVariable long id,@RequestParam(required=false)String cursor,@RequestParam(defaultValue="20")int size,HttpServletRequest r){return ok(query.favorites(actor(r),id,cursor,size));}
    @PutMapping("/folders/{folder}/notes/{note}") public ApiResponse<?> favorite(@PathVariable long folder,@PathVariable long note,HttpServletRequest r){write.favorite(actor(r),folder,note,true);return empty();}
    @DeleteMapping("/folders/{folder}/notes/{note}") public ApiResponse<?> unfavorite(@PathVariable long folder,@PathVariable long note,HttpServletRequest r){write.favorite(actor(r),folder,note,false);return empty();}
    @PostMapping("/practices") public ApiResponse<?> createPractice(@RequestBody Map<String,Object>b,HttpServletRequest r){return ok(write.createPractice(actor(r),b,key(r)));}
    @GetMapping("/practices") public ApiResponse<?> practices(@RequestParam(required=false)String cursor,@RequestParam(defaultValue="20")int size,HttpServletRequest r){return ok(query.practices(actor(r),cursor,size));}
    @GetMapping("/practices/{id}") public ApiResponse<?> practice(@PathVariable long id,HttpServletRequest r){return ok(query.practice(actor(r),id));}
    @PostMapping("/practices/{id}/transition") public ApiResponse<?> transition(@PathVariable long id,@RequestBody Map<String,Object>b,HttpServletRequest r){return ok(write.transition(actor(r),id,b,key(r)));}
    @GetMapping("/practices/{id}/history") public ApiResponse<?> history(@PathVariable long id,@RequestParam(required=false)String cursor,@RequestParam(defaultValue="20")int size,HttpServletRequest r){return ok(query.history(actor(r),id,cursor,size));}
    @PutMapping("/practices/{id}/result") public ApiResponse<?> result(@PathVariable long id,@RequestBody Map<String,Object>b,HttpServletRequest r){return ok(write.result(actor(r),id,b));}
    @PostMapping("/practices/{id}/publish-request") public ApiResponse<?> publish(@PathVariable long id,@RequestBody Map<String,Object>b,HttpServletRequest r){return ok(write.publishFeedback(actor(r),id,Data.number(b,"version"),key(r)));}
    @PostMapping("/practices/{id}/unpublish") public ApiResponse<?> unpublish(@PathVariable long id,@RequestBody Map<String,Object>b,HttpServletRequest r){return ok(write.unpublishFeedback(actor(r),id,Data.number(b,"version"),key(r)));}
    @GetMapping("/notes/{id}/feedback") public ApiResponse<?> feedback(@PathVariable long id,@RequestParam(required=false)String cursor,@RequestParam(defaultValue="20")int size,HttpServletRequest r){return ok(query.feedback(actor(r),id,cursor,size));}
    @GetMapping("/reviews") public ApiResponse<?> reviews(@RequestParam(defaultValue="PENDING")String status,@RequestParam(required=false)String cursor,@RequestParam(defaultValue="20")int size,HttpServletRequest r){return ok(query.reviews(actor(r),status,cursor,size));}
    @GetMapping("/reviews/{id}") public ApiResponse<?> review(@PathVariable long id,HttpServletRequest r){return ok(query.review(actor(r),id));}
    @PostMapping("/reviews/{id}/decision") public ApiResponse<?> decide(@PathVariable long id,@RequestBody Map<String,Object>b,HttpServletRequest r){return ok(write.decideReview(actor(r),id,b,key(r)));}
    @PostMapping("/reports") @ResponseStatus(HttpStatus.CREATED) public ApiResponse<?> report(@RequestBody Map<String,Object>b,HttpServletRequest r){return ok(write.report(actor(r),b,key(r)));}
    @GetMapping("/me/reports") public ApiResponse<?> myReports(@RequestParam(required=false)String cursor,@RequestParam(defaultValue="20")int size,HttpServletRequest r){return ok(query.reports(actor(r),false,cursor,size));}
    @GetMapping("/admin/reports") public ApiResponse<?> reports(@RequestParam(required=false)String cursor,@RequestParam(defaultValue="20")int size,HttpServletRequest r){return ok(query.reports(actor(r),true,cursor,size));}
    @PostMapping("/admin/reports/{id}/decision") public ApiResponse<?> decideReport(@PathVariable long id,@RequestBody Map<String,Object>b,HttpServletRequest r){return ok(write.decideReport(actor(r),id,b,key(r)));}
    @PostMapping("/admin/notes/{id}/offline") public ApiResponse<?> offline(@PathVariable long id,@RequestBody Map<String,Object>b,HttpServletRequest r){write.offlineNote(actor(r),id,b);return empty();}
    @PostMapping("/admin/comments/{id}/hide") public ApiResponse<?> hideComment(@PathVariable long id,@RequestBody Map<String,Object>b,HttpServletRequest r){write.hideComment(actor(r),id,b);return empty();}
    @PostMapping("/admin/practices/{id}/offline") public ApiResponse<?> offlineFeedback(@PathVariable long id,@RequestBody Map<String,Object>b,HttpServletRequest r){write.offlineFeedback(actor(r),id,b);return empty();}
    @GetMapping("/topics") public ApiResponse<?> topics(@RequestParam(required=false)String cursor,@RequestParam(defaultValue="50")int size,HttpServletRequest r){return ok(query.topics(actor(r),cursor,size));}
    @PostMapping("/admin/topics") @ResponseStatus(HttpStatus.CREATED) public ApiResponse<?> createTopic(@RequestBody Map<String,Object>b,HttpServletRequest r){return ok(write.createTopic(actor(r),b,key(r)));}
    @PutMapping("/admin/topics/{id}") public ApiResponse<?> updateTopic(@PathVariable long id,@RequestBody Map<String,Object>b,HttpServletRequest r){return ok(write.updateTopic(actor(r),id,b));}
    @GetMapping("/admin/audits") public ApiResponse<?> audits(@RequestParam(required=false)String cursor,@RequestParam(defaultValue="20")int size,HttpServletRequest r){return ok(query.audits(actor(r),cursor,size));}
    @GetMapping("/admin/users") public ApiResponse<?> users(@RequestParam(required=false)String cursor,@RequestParam(defaultValue="20")int size,HttpServletRequest r){return ok(query.users(actor(r),cursor,size));}
    @GetMapping("/admin/topics") public ApiResponse<?> adminTopics(@RequestParam(required=false)String cursor,@RequestParam(defaultValue="50")int size,HttpServletRequest r){return ok(query.adminTopics(actor(r),cursor,size));}
    @GetMapping("/notifications") public ApiResponse<?> notifications(@RequestParam(required=false)String type,@RequestParam(required=false)String cursor,@RequestParam(defaultValue="20")int size,HttpServletRequest r){return ok(query.notifications(actor(r),type,cursor,size));}
    @GetMapping("/notifications/unread") public ApiResponse<?> unread(HttpServletRequest r){return ok(query.unread(actor(r)));}
    @PutMapping("/notifications/{id}/read") public ApiResponse<?> read(@PathVariable long id,HttpServletRequest r){write.readNotification(actor(r),id);return empty();}
    @PostMapping("/notifications/read-all") public ApiResponse<?> readAll(@RequestBody Map<String,Object>b,HttpServletRequest r){write.readAll(actor(r),Data.id(b,"throughId"));return empty();}
    @GetMapping("/statistics/me") public ApiResponse<?> authorStats(@RequestParam(defaultValue="7")int days,HttpServletRequest r){return ok(query.dashboard(actor(r),"AUTHOR",days));}
    @GetMapping("/admin/statistics/platform") public ApiResponse<?> platformStats(@RequestParam(defaultValue="7")int days,HttpServletRequest r){return ok(query.dashboard(actor(r),"PLATFORM",days));}
    @GetMapping("/admin/statistics/governance") public ApiResponse<?> governanceStats(@RequestParam(defaultValue="7")int days,HttpServletRequest r){return ok(query.dashboard(actor(r),"GOVERNANCE",days));}
    @GetMapping("/statistics/me/ranking") public ApiResponse<?> authorRank(@RequestParam(required=false)String cursor,@RequestParam(defaultValue="20")int size,HttpServletRequest r){return ok(query.ranking(actor(r),false,cursor,size));}
    @GetMapping("/admin/statistics/ranking") public ApiResponse<?> platformRank(@RequestParam(required=false)String cursor,@RequestParam(defaultValue="20")int size,HttpServletRequest r){return ok(query.ranking(actor(r),true,cursor,size));}
}
