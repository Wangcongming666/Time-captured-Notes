package cn.shiguang.notes.platform.messaging;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.slf4j.*;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import cn.shiguang.notes.platform.application.InboxTransactions;

/** Manual ACK only after a durable outcome; stale tokens cannot bypass database retry delays. */
@Component
public class IdempotentConsumer {
    private static final Logger LOG=LoggerFactory.getLogger(IdempotentConsumer.class);
    private final InboxTransactions inbox;private final ObjectMapper json;private final RabbitTemplate rabbit;
    public IdempotentConsumer(InboxTransactions inbox,ObjectMapper json,RabbitTemplate rabbit){this.inbox=inbox;this.json=json;this.rabbit=rabbit;}
    @RabbitListener(queues={"sg.notification.q","sg.statistics.q","sg.cache.q"},concurrency="1-2")
    @SuppressWarnings("unchecked") public void receive(Message message,Channel channel)throws IOException {
        long tag=message.getMessageProperties().getDeliveryTag();String consumer=message.getMessageProperties().getConsumerQueue().split("\\.")[1];
        String token=Objects.toString(message.getMessageProperties().getHeader("leaseToken"),"");long id;
        try{id=Long.parseLong(Objects.toString(message.getMessageProperties().getHeader("outboxId"),""));}catch(NumberFormatException e){channel.basicReject(tag,false);LOG.warn("Rejected message without durable outbox identity");return;}
        Map<String,Object> event;
        try{event=json.readValue(message.getBody(),Map.class);String result=inbox.consume(id,token,consumer,event);if(result.equals("DEAD")){deadLetter(id,message,channel,tag);return;}channel.basicAck(tag,false);}
        catch(Exception error){
            try{String result=inbox.failed(id,token,error);if(result.equals("DEAD")){deadLetter(id,message,channel,tag);return;}channel.basicAck(tag,false);LOG.warn("Consumer retry scheduled id={} type={}",id,error.getClass().getSimpleName());}
            catch(Exception scheduleFailure){LOG.error("Cannot persist consumer outcome id={} type={}",id,scheduleFailure.getClass().getSimpleName());channel.basicNack(tag,false,true);}
        }
    }
    private void deadLetter(long id,Message original,Channel channel,long tag)throws IOException {
        try{var props=new MessageProperties();props.setContentType("application/json");props.setDeliveryMode(MessageDeliveryMode.PERSISTENT);var diagnostic=new Message(json.writeValueAsBytes(Map.of("outboxId",Long.toString(id),"reason","CONSUMER_RETRIES_EXHAUSTED")),props);var correlation=new CorrelationData(UUID.randomUUID().toString());rabbit.send("","sg.dlq",diagnostic,correlation);var confirm=correlation.getFuture().get(3,TimeUnit.SECONDS);if(!confirm.isAck()||correlation.getReturned()!=null)throw new IOException("Dead-letter delivery not confirmed");channel.basicAck(tag,false);}
        catch(Exception error){if(error instanceof InterruptedException)Thread.currentThread().interrupt();try{Thread.sleep(1000);}catch(InterruptedException interrupted){Thread.currentThread().interrupt();}channel.basicNack(tag,false,true);}
    }
}
