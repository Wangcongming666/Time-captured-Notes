package cn.shiguang.notes.platform.config;
import java.util.*;
import org.springframework.amqp.core.*;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.*;

/** Durable independent queues; no single-node high availability claim. */
@Configuration
public class MessagingConfig {
    @Bean Declarables declarations(){var d=new ArrayList<Declarable>();var ex=new TopicExchange("sg.events",true,false);d.add(ex);for(String c:List.of("notification","statistics","cache")){var q=new Queue("sg."+c+".q",true);d.add(q);d.add(BindingBuilder.bind(q).to(ex).with("*."+c));}d.add(new Queue("sg.dlq",true));return new Declarables(d);}
}
