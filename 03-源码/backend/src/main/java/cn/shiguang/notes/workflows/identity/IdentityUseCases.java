package cn.shiguang.notes.workflows.identity;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import cn.shiguang.notes.modules.identity.api.IdentityApi;
import cn.shiguang.notes.shared.contract.Actor;

/** Identity transaction boundary used by HTTP and security adapters. */
@Service
public class IdentityUseCases {
    private final IdentityApi api;
    public IdentityUseCases(IdentityApi api){this.api=api;}
    @Transactional public Map<String,Object> register(Map<String,Object> input){return api.register(input);}
    @Transactional public Map<String,Object> login(String name,String pass){long id=api.verify(name,pass);String token=api.session(id);var result=new LinkedHashMap<>(api.profile(id,true));result.put("sessionToken",token);return result;}
    @Transactional public void logout(String token){api.revoke(token);}
    public Actor authenticate(String token){return api.authenticate(token);}
    @Transactional public String csrf(String token){return api.csrf(token);}
    public boolean checkCsrf(String token,String csrf){return api.checkCsrf(token,csrf);}
    public Map<String,Object> profile(Actor actor){actor.requireLogin();return api.profile(actor.id(),true);}
    public Map<String,Object> publicProfile(long id){return api.profile(id,false);}
    @Transactional public Map<String,Object> update(Actor actor,Map<String,Object> input){return api.updateProfile(actor,input);}
    @Transactional public void status(Actor actor,long target,Map<String,Object> input){api.status(actor,target,input);}
    @Transactional public void roles(Actor actor,long target,Map<String,Object> input){api.roles(actor,target,input);}
}
