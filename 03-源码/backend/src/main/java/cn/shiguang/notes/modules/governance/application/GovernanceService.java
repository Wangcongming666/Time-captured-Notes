package cn.shiguang.notes.modules.governance.application;
import java.util.*;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import cn.shiguang.notes.modules.governance.api.GovernanceApi;
import cn.shiguang.notes.modules.governance.domain.GovernanceRepository;
import cn.shiguang.notes.shared.contract.*;
import cn.shiguang.notes.shared.error.BizException;
@Service
public class GovernanceService implements GovernanceApi {
    private final GovernanceRepository repo;private final ObjectMapper json;
    public GovernanceService(GovernanceRepository repo,ObjectMapper json){this.repo=repo;this.json=json;}
    public long openReview(String type,long id,int rev,long author){return repo.openReview(type,id,rev,author);}
    public Map<String,Object> review(long id,boolean lock){return repo.review(id,lock);}
    public void cancelReviews(String type,long id){repo.cancelReviews(type,id);}
    public void decide(Actor actor,long id,int version,String decision,String reason){actor.require("REVIEWER");var r=repo.review(id,true);if(actor.id()==Data.id(r,"applicantId"))throw BizException.forbidden();if(!Set.of("APPROVE","REJECT").contains(decision)||reason.length()>500||decision.equals("REJECT")&&reason.isBlank())throw BizException.invalid("驳回须填写原因");if(!"PENDING".equals(r.get("status"))||Data.number(r,"version")!=version)throw BizException.conflict("审核任务已变化");repo.decide(id,decision.equals("APPROVE")?"APPROVED":"REJECTED",actor.id(),reason,version);}
    public List<Map<String,Object>> reviews(String status,long before,int size){if(!Set.of("PENDING","APPROVED","REJECTED","CANCELLED").contains(status))throw BizException.invalid("无效审核状态");return repo.reviews(status,before,size);}
    public long report(Actor actor,String type,long target,String reason){if(!Set.of("NOTE","COMMENT","PRACTICE").contains(type)||reason.isBlank()||reason.length()>500)throw BizException.invalid("无效举报");return repo.report(actor.id(),type,target,reason,Tokens.hash(actor.id()+":"+type+":"+target));}
    public Map<String,Object> report(long id,boolean lock){return repo.report(id,lock);}
    public void decideReport(Actor actor,long id,int version,String status,String reason){actor.require("REVIEWER");var r=repo.report(id,true);String from=Data.text(r,"status");boolean allowed=from.equals("PENDING")&&status.equals("PROCESSING")||from.equals("PROCESSING")&&Set.of("UPHELD","REJECTED").contains(status);if(!allowed||Data.number(r,"version")!=version)throw BizException.conflict("举报状态或版本不允许此操作");if(reason.isBlank()||reason.length()>500)throw BizException.invalid("请填写处理理由");repo.decideReport(id,actor.id(),version,status,reason);}
    public List<Map<String,Object>> reports(Long user,long before,int size){return repo.reports(user,before,size);}
    public List<Map<String,Object>> topics(long before,int size){return repo.topics(before,size);}
    private void topic(Map<String,Object> b){String n=Data.text(b,"name"),c=Data.text(b,"category");if(n.isBlank()||n.length()>40||c.isBlank()||c.length()>40||!Set.of("ACTIVE","DISABLED").contains(Data.text(b,"status")))throw BizException.invalid("话题信息不完整");}
    public long createTopic(Map<String,Object> b){topic(b);return repo.createTopic(Data.text(b,"name"),Data.text(b,"category"),Data.text(b,"status"));}
    public Map<String,Object> updateTopic(long id,Map<String,Object> b){topic(b);return repo.updateTopic(id,Data.text(b,"name"),Data.text(b,"category"),Data.text(b,"status"));}
    public void audit(Actor actor,String action,String type,long id,Map<String,Object> before,Map<String,Object> after,String reason){try{repo.audit(Data.map("actor_id",actor.id(),"action",action,"target_type",type,"target_id",id,"before_json",json.writeValueAsString(before),"after_json",json.writeValueAsString(after),"reason",reason,"trace_id",Objects.toString(MDC.get("traceId"),"scheduled")));}catch(com.fasterxml.jackson.core.JsonProcessingException e){throw new IllegalArgumentException("Invalid audit",e);}}
    public List<Map<String,Object>> audits(long before,int size){return repo.audits(before,size);}
}
