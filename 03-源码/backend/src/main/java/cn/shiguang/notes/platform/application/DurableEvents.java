package cn.shiguang.notes.platform.application;
import java.time.Instant;
import java.util.*;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import cn.shiguang.notes.platform.persistence.Database;
import cn.shiguang.notes.shared.contract.*;
import cn.shiguang.notes.shared.error.BizException;

/** Appends a consumer-specific recoverable event in the existing transaction. */
@Component
public class DurableEvents implements EventPort {
    private final Database db;private final ObjectMapper json;
    public DurableEvents(Database db,ObjectMapper json){this.db=db;this.json=json;}
    public void append(String type,long actor,String aggregate,long id,int revision,Map<String,Object> payload,String... consumers){
        if(!TransactionSynchronizationManager.isActualTransactionActive())throw new IllegalStateException("Outbox requires business transaction");
        if(db.count("SELECT COUNT(*) FROM t_outbox WHERE status NOT IN ('DONE','DEAD')")>100000)throw new BizException(503,"EVENT_BACKLOG","消息积压，请稍后重试");
        String event=UUID.randomUUID().toString();var value=Data.map("eventId",event,"eventType",type,"schemaVersion",1,"occurredAt",Instant.now().toString(),"actorId",Long.toString(actor),"aggregateType",aggregate,"aggregateId",Long.toString(id),"aggregateRevision",revision,"traceId",Objects.toString(MDC.get("traceId"),"scheduled"),"payload",payload);
        try{String encoded=json.writeValueAsString(value);for(String consumer:Set.of(consumers))db.update("INSERT INTO t_outbox(event_id,consumer,routing_key,payload,available_at) VALUES(?,?,?,?,UTC_TIMESTAMP(3))",event,consumer,type+"."+consumer,encoded);}
        catch(com.fasterxml.jackson.core.JsonProcessingException e){throw new IllegalArgumentException("Invalid event payload",e);}
    }
}
