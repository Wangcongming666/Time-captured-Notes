package cn.shiguang.notes.platform.application;
import java.time.Duration;
import org.slf4j.*;
import org.springframework.data.redis.core.*;
import org.springframework.stereotype.Component;
import cn.shiguang.notes.shared.contract.CachePort;
/** Cache failures never grant permission or lose business facts. */
@Component
public class RedisCache implements CachePort {
    private static final Logger LOG=LoggerFactory.getLogger(RedisCache.class);
    private final StringRedisTemplate redis;
    public RedisCache(StringRedisTemplate redis){this.redis=redis;}
    public String get(String key){try{return redis.opsForValue().get(key);}catch(RuntimeException e){LOG.debug("Cache unavailable");return null;}}
    public boolean put(String key,String value,int ttl){try{redis.opsForValue().set(key,value,Duration.ofSeconds(ttl));return true;}catch(RuntimeException e){return false;}}
    public void removePrefix(String prefix){try(var cursor=redis.scan(ScanOptions.scanOptions().match(prefix+"*").count(100).build())){while(cursor.hasNext())redis.delete(cursor.next());}catch(RuntimeException e){LOG.debug("Cache invalidation unavailable");}}
}
