package cn.shiguang.notes.modules.${module}.domain;
import java.util.Map;
public interface ${className}Repository {
    Map<String,Object> findOwned(long ownerId,long id,boolean lock);
}
