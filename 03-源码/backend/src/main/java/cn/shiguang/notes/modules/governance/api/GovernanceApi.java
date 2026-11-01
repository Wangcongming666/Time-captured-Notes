package cn.shiguang.notes.modules.governance.api;
import java.util.*;
import cn.shiguang.notes.shared.contract.Actor;
public interface GovernanceApi {
    long openReview(String type,long target,int revision,long applicant);
    Map<String,Object> review(long id,boolean lock);
    void cancelReviews(String type,long target);
    void decide(Actor actor,long id,int version,String decision,String reason);
    List<Map<String,Object>> reviews(String status,long before,int size);
    long report(Actor actor,String type,long target,String reason);
    Map<String,Object> report(long id,boolean lock);
    void decideReport(Actor actor,long id,int version,String status,String reason);
    List<Map<String,Object>> reports(Long user,long before,int size);
    List<Map<String,Object>> topics(long before,int size);
    List<Map<String,Object>> allTopics(long before,int size);
    long createTopic(Map<String,Object> input);
    Map<String,Object> updateTopic(long id,Map<String,Object> input);
    void audit(Actor actor,String action,String type,long id,Map<String,Object> before,Map<String,Object> after,String reason);
    List<Map<String,Object>> audits(long before,int size);
}
