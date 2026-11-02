package cn.shiguang.notes.service.support;

import cn.shiguang.notes.common.exception.BizException;
import cn.shiguang.notes.common.util.Data;
import cn.shiguang.notes.common.util.Tokens;
import cn.shiguang.notes.dto.Actor;
import cn.shiguang.notes.entity.*;
import cn.shiguang.notes.mapper.*;
import cn.shiguang.notes.repository.support.MapperRows;
import com.fasterxml.jackson.databind.*;
import java.util.*;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Actor-serialized command deduplication, committed in the enclosing business transaction. */
@Component
public class Commands {
  private final CommandMapper commandMapper;
  private final UserMapper userMapper;
  private final ObjectMapper json;

  public Commands(CommandMapper commandMapper, UserMapper userMapper, ObjectMapper json) {
    this.commandMapper = commandMapper;
    this.userMapper = userMapper;
    this.json = json.copy().configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
  }

  public Object run(
      Actor actor, String scope, String key, Map<String, Object> body, Supplier<Object> operation) {
    actor.requireLogin();
    if (key == null || key.length() < 16 || key.length() > 80)
      throw BizException.invalid("缺少有效Idempotency-Key");
    if (!TransactionSynchronizationManager.isActualTransactionActive())
      throw new IllegalStateException("Command requires transaction");
    MapperRows.one(userMapper.commandsRun(actor.id()));
    String hash = Tokens.hash(encode(body));
    var old = MapperRows.optional(commandMapper.commandsRun(actor.id(), scope, key));
    if (old != null) {
      if (!Tokens.same(Data.text(old, "requestHash"), hash))
        throw BizException.conflict("幂等键已用于不同请求");
      return decode(Data.text(old, "responseJson"));
    }
    Object result = operation.get();
    commandMapper.commandsRun2(actor.id(), scope, key, hash, encode(result));
    return result;
  }

  private String encode(Object v) {
    try {
      return json.writeValueAsString(v);
    } catch (Exception e) {
      throw new IllegalArgumentException("Invalid command payload", e);
    }
  }

  private Object decode(String v) {
    try {
      return json.readValue(v, Object.class);
    } catch (Exception e) {
      throw new IllegalStateException("Invalid stored command", e);
    }
  }
}
