package cn.shiguang.notes.service.support;

import cn.shiguang.notes.common.exception.BizException;
import cn.shiguang.notes.common.util.Data;
import cn.shiguang.notes.common.util.Tokens;
import cn.shiguang.notes.dto.Actor;
import cn.shiguang.notes.repository.support.Database;
import com.fasterxml.jackson.databind.*;
import java.util.*;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Actor-serialized command deduplication, committed in the enclosing business transaction. */
@Component
public class Commands {
  private final Database db;
  private final ObjectMapper json;

  public Commands(Database db, ObjectMapper json) {
    this.db = db;
    this.json = json.copy().configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
  }

  public Object run(
      Actor actor, String scope, String key, Map<String, Object> body, Supplier<Object> operation) {
    actor.requireLogin();
    if (key == null || key.length() < 16 || key.length() > 80)
      throw BizException.invalid("缺少有效Idempotency-Key");
    if (!TransactionSynchronizationManager.isActualTransactionActive())
      throw new IllegalStateException("Command requires transaction");
    db.one("SELECT id FROM t_user WHERE id=? FOR UPDATE", actor.id());
    String hash = Tokens.hash(encode(body));
    var old =
        db.optional(
            "SELECT request_hash,response_json FROM t_command WHERE actor_id=? AND scope=? AND"
                + " request_key=?",
            actor.id(),
            scope,
            key);
    if (old != null) {
      if (!Tokens.same(Data.text(old, "requestHash"), hash))
        throw BizException.conflict("幂等键已用于不同请求");
      return decode(Data.text(old, "responseJson"));
    }
    Object result = operation.get();
    db.update(
        "INSERT INTO t_command(actor_id,scope,request_key,request_hash,response_json,expires_at)"
            + " VALUES(?,?,?,?,?,DATE_ADD(UTC_TIMESTAMP(3),INTERVAL 7 DAY))",
        actor.id(),
        scope,
        key,
        hash,
        encode(result));
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
