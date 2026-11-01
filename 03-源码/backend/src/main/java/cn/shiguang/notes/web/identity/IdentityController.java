package cn.shiguang.notes.web.identity;
import java.util.*;
import jakarta.servlet.http.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import cn.shiguang.notes.web.ApiResponse;
import cn.shiguang.notes.workflows.identity.IdentityUseCases;
import cn.shiguang.notes.platform.security.RequestSecurityFilter;
import cn.shiguang.notes.shared.contract.*;

/** Credential endpoints never serialize token fields into JSON. */
@RestController @RequestMapping("/api/v1")
public class IdentityController {
    private final IdentityUseCases use;private final RequestSecurityFilter filter;private final boolean secure;private final cn.shiguang.notes.platform.security.RateGuard limits;
    public IdentityController(IdentityUseCases use,RequestSecurityFilter filter,cn.shiguang.notes.platform.security.RateGuard limits,@Value("${app.secure-cookie}")boolean secure){this.use=use;this.filter=filter;this.secure=secure;this.limits=limits;}
    private void cookie(HttpServletResponse response,String name,String value,int seconds){response.addHeader("Set-Cookie",ResponseCookie.from(name,value).httpOnly(true).secure(secure).sameSite("Lax").path("/").maxAge(seconds).build().toString());}
    @GetMapping("/auth/csrf") public ApiResponse<?> csrf(HttpServletRequest req,HttpServletResponse res){String token=RequestSecurityFilter.cookie(req,"SG_SESSION");if(RequestSecurityFilter.actor(req).id()>0)return ApiResponse.ok(Map.of("token",use.csrf(token)));String anon=filter.anonymousToken();cookie(res,"SG_ANON",anon,600);return ApiResponse.ok(Map.of("token",filter.anonymousCsrf(anon)));}
    @PostMapping("/auth/register") @ResponseStatus(HttpStatus.CREATED) public ApiResponse<?> register(@RequestBody Map<String,Object> body){return ApiResponse.ok(use.register(body));}
    @PostMapping("/auth/login") public ApiResponse<?> login(@RequestBody Map<String,Object> body,HttpServletResponse res){limits.check("account:"+Data.text(body,"username").toLowerCase(Locale.ROOT),5);var data=use.login(Data.text(body,"username"),Data.text(body,"password"));String token=(String)data.remove("sessionToken");cookie(res,"SG_SESSION",token,86400);cookie(res,"SG_ANON","",0);return ApiResponse.ok(data);}
    @PostMapping("/auth/logout") public ApiResponse<?> logout(HttpServletRequest req,HttpServletResponse res){use.logout(RequestSecurityFilter.cookie(req,"SG_SESSION"));cookie(res,"SG_SESSION","",0);return ApiResponse.ok(Map.of());}
    @GetMapping("/me") public ApiResponse<?> me(HttpServletRequest req){return ApiResponse.ok(use.profile(RequestSecurityFilter.actor(req)));}
    @GetMapping("/users/{id}") public ApiResponse<?> user(@PathVariable long id){return ApiResponse.ok(use.publicProfile(id));}
    @PutMapping("/me/profile") public ApiResponse<?> profile(@RequestBody Map<String,Object> body,HttpServletRequest req){return ApiResponse.ok(use.update(RequestSecurityFilter.actor(req),body));}
    @PutMapping("/admin/users/{id}/status") public ApiResponse<?> status(@PathVariable long id,@RequestBody Map<String,Object> body,HttpServletRequest req){use.status(RequestSecurityFilter.actor(req),id,body);return ApiResponse.ok(Map.of());}
    @PutMapping("/admin/users/{id}/roles") public ApiResponse<?> roles(@PathVariable long id,@RequestBody Map<String,Object> body,HttpServletRequest req){use.roles(RequestSecurityFilter.actor(req),id,body);return ApiResponse.ok(Map.of());}
}
