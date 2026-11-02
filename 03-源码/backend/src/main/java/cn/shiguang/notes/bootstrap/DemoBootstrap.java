package cn.shiguang.notes.bootstrap;

import cn.shiguang.notes.common.util.Data;
import cn.shiguang.notes.entity.*;
import cn.shiguang.notes.mapper.*;
import cn.shiguang.notes.repository.support.MapperRows;
import cn.shiguang.notes.service.IdentityService;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Explicitly opt-in synthetic development accounts; password comes only from configuration. */
@Component
@org.springframework.core.annotation.Order(10)
public class DemoBootstrap implements ApplicationRunner {
  private final boolean enabled;
  private final String password;
  private final IdentityService identity;
  private final TopicMapper topicMapper;
  private final UserMapper userMapper;
  private final UserRoleMapper userRoleMapper;

  public DemoBootstrap(
      @Value("${app.seed-demo:false}") boolean enabled,
      @Value("${app.demo-password:${DEMO_PASSWORD:}}") String password,
      IdentityService identity,
      TopicMapper topicMapper,
      UserMapper userMapper,
      UserRoleMapper userRoleMapper) {
    this.enabled = enabled;
    this.password = password;
    this.identity = identity;
    this.topicMapper = topicMapper;
    this.userMapper = userMapper;
    this.userRoleMapper = userRoleMapper;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    if (!enabled) return;
    if (password.length() < 8)
      throw new IllegalArgumentException("DEMO_PASSWORD is required when demo seeding is enabled");
    String[][] accounts = {
      {"demo_user", "拾光体验者", "USER"},
      {"demo_reviewer", "内容审核员", "REVIEWER"},
      {"demo_admin", "系统管理员", "ADMIN"}
    };
    for (String[] a : accounts) {
      var existing = MapperRows.optional(userMapper.demoBootstrapRun(a[0]));
      if (existing == null) {
        long id =
            Long.parseLong(
                identity
                    .register(Data.map("username", a[0], "password", password, "nickname", a[1]))
                    .get("id")
                    .toString());
        if (!a[2].equals("USER")) userRoleMapper.demoBootstrapRun(id, a[2]);
      }
    }
    for (String[] t :
        new String[][] {
          {"旅行", "旅行"},
          {"咖啡日常", "美食"},
          {"家居灵感", "家居"},
          {"摄影", "摄影"},
          {"阅读", "读书"},
          {"户外徒步", "户外"},
          {"日常穿搭", "穿搭"},
          {"周末食谱", "美食"}
        }) topicMapper.demoBootstrapRun(t[0], t[1]);
  }
}
