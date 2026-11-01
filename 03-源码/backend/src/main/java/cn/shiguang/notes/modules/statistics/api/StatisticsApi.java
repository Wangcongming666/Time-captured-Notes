package cn.shiguang.notes.modules.statistics.api;
import java.util.*;
import cn.shiguang.notes.shared.contract.Actor;
public interface StatisticsApi {
    void receive(Map<String,Object> event);
    void viewed(long viewer,String subject,long note,long author);
    void active(long user);
    Map<String,Object> dashboard(Actor actor,String scope,int days);
    List<Map<String,Object>> ranking(Actor actor,boolean platform,long before,int size);
    void reconcile();
}
