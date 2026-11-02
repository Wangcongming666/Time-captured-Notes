package cn.shiguang.notes.mapper;

import cn.shiguang.notes.entity.*;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.*;
import org.apache.ibatis.annotations.*;

/**
 * Typed CRUD plus bounded, parameterized business queries. SQL is maintained in
 * mapper/OutboxMapper.xml.
 */
@Mapper
public interface OutboxMapper extends BaseMapper<OutboxEntity> {
  long durableEventsAppend();

  int durableEventsAppend2(
      @Param("event") String event,
      @Param("consumer") String consumer,
      @Param("value2") String value2,
      @Param("encoded") String encoded);

  OutboxEntity inboxTransactionsConsume(@Param("id") long id);

  int inboxTransactionsConsume2(@Param("id") long id);

  int inboxTransactionsConsume3(@Param("id") long id);

  OutboxEntity inboxTransactionsFailed(@Param("id") long id);

  int inboxTransactionsFailed2(
      @Param("next") String next,
      @Param("attempts") int attempts,
      @Param("delay") int delay,
      @Param("value3") String value3,
      @Param("id") long id,
      @Param("token") String token);

  int outboxDispatcherDispatch();

  List<OutboxEntity> outboxDispatcherDispatch2();

  int outboxDispatcherDispatch3(@Param("token") String token, @Param("id") long id);

  int outboxDispatcherDispatch4(@Param("id") long id, @Param("token") String token);

  int outboxDispatcherDispatch5(
      @Param("value0") String value0, @Param("id") long id, @Param("token") String token);
}
