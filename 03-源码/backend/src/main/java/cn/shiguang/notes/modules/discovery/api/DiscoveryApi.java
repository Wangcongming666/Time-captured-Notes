package cn.shiguang.notes.modules.discovery.api;
import java.util.*;
import cn.shiguang.notes.shared.contract.Actor;
public interface DiscoveryApi {
    Map<String,Object> feed(Actor actor,Map<String,String> query);
    Map<String,Object> preferences(long user);
    void preferences(long user,Map<String,Object> body);
    void hide(long user,long note,boolean hidden);
    void reset(long user);
    void clear(long user);
}
