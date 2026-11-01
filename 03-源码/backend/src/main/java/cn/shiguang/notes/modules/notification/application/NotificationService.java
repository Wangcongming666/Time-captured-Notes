package cn.shiguang.notes.modules.notification.application;
import java.util.*;
import org.springframework.stereotype.Service;
import cn.shiguang.notes.modules.notification.api.NotificationApi;
import cn.shiguang.notes.modules.notification.domain.NotificationRepository;
import cn.shiguang.notes.shared.contract.Data;
@Service
public class NotificationService implements NotificationApi {
    private final NotificationRepository repo;
    public NotificationService(NotificationRepository repo){this.repo=repo;}
    @SuppressWarnings("unchecked") public void receive(Map<String,Object> event){var p=(Map<String,Object>)event.get("payload");if(p==null||p.get("recipientId")==null)return;long recipient=Data.id(p,"recipientId"),actor=Data.id(event,"actorId");if(recipient==actor)return;repo.insert(recipient,Data.text(event,"eventId"),Data.text(p,"notificationType"),actor,Data.text(event,"aggregateType"),Data.id(event,"aggregateId"));}
    public List<Map<String,Object>> list(long u,String t,long b,int s){return repo.list(u,t,b,s);}
    public long unread(long u){return repo.unread(u);}
    public void read(long u,long id){repo.read(u,id);}
    public void readAll(long u,long id){repo.readAll(u,id);}
}
