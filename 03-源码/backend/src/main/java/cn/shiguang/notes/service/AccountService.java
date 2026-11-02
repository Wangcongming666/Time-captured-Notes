package cn.shiguang.notes.service;

import cn.shiguang.notes.common.contract.EventPort;
import cn.shiguang.notes.common.util.Data;
import cn.shiguang.notes.dto.Actor;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Identity transaction boundary used by HTTP and security adapters. */
@Service
public class AccountService {
  private final IdentityService api;
  private final ContentService content;
  private final GovernanceService governance;
  private final EventPort events;
  private final cn.shiguang.notes.repository.query.PublicQueryRepository reads;
  private final cn.shiguang.notes.service.StatisticsService statistics;

  public AccountService(
      IdentityService api,
      ContentService content,
      GovernanceService governance,
      EventPort events,
      cn.shiguang.notes.repository.query.PublicQueryRepository reads,
      cn.shiguang.notes.service.StatisticsService statistics) {
    this.api = api;
    this.content = content;
    this.governance = governance;
    this.events = events;
    this.reads = reads;
    this.statistics = statistics;
  }

  @Transactional
  public void active(Actor actor) {
    statistics.active(actor.id());
  }

  @Transactional
  public Map<String, Object> register(Map<String, Object> input) {
    return api.register(input);
  }

  @Transactional
  public Map<String, Object> login(String name, String pass) {
    long id = api.verify(name, pass);
    String token = api.session(id);
    var result = new LinkedHashMap<>(api.profile(id, true));
    result.put("sessionToken", token);
    return result;
  }

  @Transactional
  public void logout(String token) {
    api.revoke(token);
  }

  public Actor authenticate(String token) {
    return api.authenticate(token);
  }

  @Transactional
  public String csrf(String token) {
    return api.csrf(token);
  }

  public boolean checkCsrf(String token, String csrf) {
    return api.checkCsrf(token, csrf);
  }

  public Map<String, Object> profile(Actor actor) {
    actor.requireLogin();
    return api.profile(actor.id(), true);
  }

  public Map<String, Object> publicProfile(long id) {
    return api.profile(id, false);
  }

  public Map<String, Object> summary(Actor viewer, long id) {
    api.profile(id, false);
    return reads.profileSummary(id, viewer.id());
  }

  @Transactional
  public Map<String, Object> update(Actor actor, Map<String, Object> input) {
    actor.requireLogin();
    var previous = api.profile(actor.id(), true);
    Long next = input.get("avatarMediaId") == null ? null : Data.id(input, "avatarMediaId");
    Long old = previous.get("avatarMediaId") == null ? null : Data.id(previous, "avatarMediaId");
    content.avatar(actor, next, old);
    return api.updateProfile(actor, input);
  }

  @Transactional
  public void status(Actor actor, long target, Map<String, Object> input) {
    actor.require("ADMIN");
    var before = api.profile(target, true);
    api.status(actor, target, input);
    governance.audit(
        actor,
        "USER_STATUS",
        "USER",
        target,
        Data.select(before, "status"),
        Data.select(input, "status"),
        Data.text(input, "reason"));
    events.append(
        "UserStatusChanged",
        actor.id(),
        "USER",
        target,
        0,
        Data.map("recipientId", Long.toString(target), "notificationType", "BAN"),
        "notification");
  }

  @Transactional
  public void roles(Actor actor, long target, Map<String, Object> input) {
    actor.require("ADMIN");
    var before = api.profile(target, true);
    api.roles(actor, target, input);
    governance.audit(
        actor,
        "USER_ROLES",
        "USER",
        target,
        Data.select(before, "roles"),
        Data.select(input, "roles"),
        Data.text(input, "reason"));
  }
}
