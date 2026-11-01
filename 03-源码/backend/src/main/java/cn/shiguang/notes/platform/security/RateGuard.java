package cn.shiguang.notes.platform.security;

import cn.shiguang.notes.shared.contract.Tokens;
import cn.shiguang.notes.shared.error.BizException;
import java.time.Instant;
import java.util.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

/** Shared atomic minute windows; bounded, stricter per-process fallback during Redis outages. */
@Component
public class RateGuard {
  private final StringRedisTemplate redis;
  private final Map<String, Integer> local = new HashMap<>();
  private long minute = -1;
  private static final DefaultRedisScript<Long> SCRIPT =
      new DefaultRedisScript<>(
          "local n=redis.call('INCR',KEYS[1]); if n==1 then redis.call('EXPIRE',KEYS[1],61) end;"
              + " return n",
          Long.class);

  public RateGuard(StringRedisTemplate redis) {
    this.redis = redis;
  }

  public void check(String subject, int quota) {
    long window = Instant.now().getEpochSecond() / 60;
    Long count;
    try {
      count = redis.execute(SCRIPT, List.of("sg:limit:" + Tokens.hash(subject) + ":" + window));
      if (count == null) throw new IllegalStateException("Redis unavailable");
    } catch (Exception unavailable) {
      synchronized (local) {
        if (minute != window) {
          local.clear();
          minute = window;
        }
        if (!local.containsKey(subject) && local.size() >= 10000) throw limited();
        count = (long) local.merge(subject, 1, Integer::sum);
        quota = Math.max(1, quota / 2);
      }
    }
    if (count > quota) throw limited();
  }

  private BizException limited() {
    return new BizException(429, "RATE_LIMITED", "请求过于频繁，请稍后重试");
  }
}
