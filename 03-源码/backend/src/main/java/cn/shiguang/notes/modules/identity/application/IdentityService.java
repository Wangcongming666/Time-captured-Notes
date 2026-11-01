package cn.shiguang.notes.modules.identity.application;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.dao.DuplicateKeyException;
import cn.shiguang.notes.modules.identity.api.IdentityApi;
import cn.shiguang.notes.modules.identity.domain.*;
import cn.shiguang.notes.shared.contract.*;
import cn.shiguang.notes.shared.error.BizException;
/** Identity policy; transaction boundaries remain in workflows. */
@Service
public class IdentityService implements IdentityApi {
    private final IdentityRepository repo;private final BCryptPasswordEncoder passwords=new BCryptPasswordEncoder(12);
    private final String dummyHash=passwords.encode("non-existing-account-timing-guard");
    public IdentityService(IdentityRepository repo){this.repo=repo;}
    public Map<String,Object> register(Map<String,Object> input){
        String name=Data.text(input,"username"),pass=Data.text(input,"password"),nick=Data.text(input,"nickname");IdentityRules.validateRegistration(name,pass,nick);
        try{return Map.of("id",Long.toString(repo.create(name,passwords.encode(pass),nick)));}catch(DuplicateKeyException e){throw BizException.conflict("该账号已被注册");}
    }
    public long verify(String name,String password){
        var u=repo.findByName(name);boolean valid=passwords.matches(password,u==null?dummyHash:Data.text(u,"passwordHash"));
        if(u==null||!valid)throw new BizException(401,"INVALID_CREDENTIALS","账号或密码不正确");
        if(!"ACTIVE".equals(u.get("status")))throw BizException.forbidden();return Data.id(u,"id");
    }
    public String session(long id){var u=repo.user(id,false);String token=Tokens.random();repo.addSession(Tokens.hash(token),id,Data.number(u,"authVersion"),Tokens.hash(Tokens.random()));return token;}
    public Actor authenticate(String token){
        if(token==null)return Actor.guest();var s=repo.session(Tokens.hash(token));
        if(s==null||!"ACTIVE".equals(s.get("userStatus"))||!s.get("authVersion").toString().equals(s.get("currentAuthVersion").toString()))return Actor.guest();
        long id=Data.id(s,"userId");return new Actor(id,Set.copyOf(repo.roles(id)));
    }
    public void revoke(String token){if(token!=null)repo.revoke(Tokens.hash(token));}
    public String csrf(String token){String csrf=Tokens.random();repo.csrf(Tokens.hash(token),Tokens.hash(csrf));return csrf;}
    public boolean checkCsrf(String token,String csrf){var s=token==null?null:repo.session(Tokens.hash(token));return s!=null&&csrf!=null&&Tokens.same(Data.text(s,"csrfHash"),Tokens.hash(csrf));}
    public Map<String,Object> profile(long id,boolean privateView){var row=repo.user(id,false);var dto=Data.select(row,"id","nickname","bio","avatarMediaId");if(privateView){dto.put("roles",repo.roles(id));dto.put("status",row.get("status"));}return dto;}
    public Map<String,Object> updateProfile(Actor actor,Map<String,Object> input){actor.requireLogin();String name=Data.text(input,"nickname"),bio=Data.text(input,"bio");if(name.isBlank()||name.codePointCount(0,name.length())>40||bio.codePointCount(0,bio.length())>300)throw BizException.invalid("昵称或简介长度不符合要求");Long avatar=input.get("avatarMediaId")==null?null:Data.id(input,"avatarMediaId");repo.profile(actor.id(),name,bio,avatar);return profile(actor.id(),true);}
    public void status(Actor actor,long target,Map<String,Object> input){actor.require("ADMIN");String status=Data.text(input,"status");if(!Set.of("ACTIVE","BANNED").contains(status)||Data.text(input,"reason").isBlank())throw BizException.invalid("状态或理由无效");repo.lockAdministrators();var u=repo.user(target,true);if("BANNED".equals(status)&&"ACTIVE".equals(u.get("status"))&&repo.roles(target).contains("ADMIN")&&repo.activeAdministrators()<=1)throw BizException.conflict("不能封禁最后一名可用管理员");repo.changeStatus(target,status);}
    public void roles(Actor actor,long target,Map<String,Object> input){actor.require("ADMIN");Object raw=input.get("roles");if(!(raw instanceof List<?> values)||values.isEmpty()||values.stream().anyMatch(v->!Set.of("USER","REVIEWER","ADMIN").contains(v))||Data.text(input,"reason").isBlank())throw BizException.invalid("角色或理由无效");repo.lockAdministrators();var u=repo.user(target,true);var roles=values.stream().map(Object::toString).distinct().toList();if(repo.roles(target).contains("ADMIN")&&!roles.contains("ADMIN")&&"ACTIVE".equals(u.get("status"))&&repo.activeAdministrators()<=1)throw BizException.conflict("必须保留一名可用管理员");repo.changeRoles(target,roles);}
}
