package cn.shiguang.notes.platform.application;
import java.util.*;
import java.util.function.BiFunction;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import cn.shiguang.notes.shared.contract.*;
import cn.shiguang.notes.shared.error.BizException;
/** HMAC-signed keyset cursors bound to actor, route and filters. */
@Component
public class Pages {
    private final String key;
    public Pages(@Value("${app.cursor-key}")String key){this.key=key;}
    public Map<String,Object> fetch(String scope,long actor,String cursor,int size,BiFunction<Long,Integer,List<Map<String,Object>>> query){return fetch(scope,actor,cursor,size,"id",query);}
    public Map<String,Object> fetch(String scope,long actor,String cursor,int size,String idField,BiFunction<Long,Integer,List<Map<String,Object>>> query){
        if(size<1||size>50)throw BizException.invalid("分页数量须为1–50");long before=Long.MAX_VALUE;
        if(cursor!=null&&!cursor.isBlank()){String[] p=cursor.split("\\.");if(p.length!=2||!Tokens.same(p[1],Tokens.sign(p[0]+":"+scope+":"+actor,key)))throw BizException.invalid("分页游标无效");try{before=Long.parseLong(new String(Base64.getUrlDecoder().decode(p[0]),StandardCharsets.UTF_8));}catch(Exception e){throw BizException.invalid("分页游标无效");}}
        var rows=new ArrayList<>(query.apply(before,size+1));boolean more=rows.size()>size;if(more)rows.removeLast();String next=null;
        if(more&&!rows.isEmpty()){String p=Base64.getUrlEncoder().withoutPadding().encodeToString(Data.text(rows.getLast(),idField).getBytes(StandardCharsets.UTF_8));next=p+"."+Tokens.sign(p+":"+scope+":"+actor,key);}
        return Data.map("items",rows,"nextCursor",next,"hasMore",more);
    }
}
