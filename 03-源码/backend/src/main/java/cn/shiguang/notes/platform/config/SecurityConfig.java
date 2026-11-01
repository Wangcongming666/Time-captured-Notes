package cn.shiguang.notes.platform.config;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.http.SessionCreationPolicy;
/** Authentication/CSRF are checked by RequestSecurityFilter against authoritative database state. */
@Configuration
public class SecurityConfig {
    @Bean org.springframework.security.core.userdetails.UserDetailsService noPasswordLogin(){return name->{throw new org.springframework.security.core.userdetails.UsernameNotFoundException("Use the application session API");};}
    @Bean SecurityFilterChain chain(HttpSecurity http)throws Exception {
        return http.csrf(c->c.disable()).sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a->a.anyRequest().permitAll()).formLogin(f->f.disable()).httpBasic(h->h.disable()).build();
    }
}
