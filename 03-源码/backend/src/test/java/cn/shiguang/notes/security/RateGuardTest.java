package cn.shiguang.notes.security;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import cn.shiguang.notes.platform.security.RateGuard;
import cn.shiguang.notes.shared.error.BizException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class RateGuardTest {
    @Test void redisFailureUsesStricterBoundedLocalQuota() {
        var redis=mock(StringRedisTemplate.class);
        when(redis.execute(any(org.springframework.data.redis.core.script.RedisScript.class),anyList(),any(Object[].class))).thenThrow(new org.springframework.data.redis.RedisConnectionFailureException("offline"));
        var guard=new RateGuard(redis);
        guard.check("login:test",5);guard.check("login:test",5);
        assertEquals(429,assertThrows(BizException.class,()->guard.check("login:test",5)).status());
        assertDoesNotThrow(()->guard.check("login:other",5));
    }
}
