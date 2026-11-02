package cn.shiguang.notes.service.application;

import cn.shiguang.notes.common.exception.BizException;
import cn.shiguang.notes.converter.HttpBoundaryConverter;
import cn.shiguang.notes.dto.Actor;
import cn.shiguang.notes.dto.request.*;
import cn.shiguang.notes.repository.query.PublicQueryRepository;
import cn.shiguang.notes.service.*;
import cn.shiguang.notes.service.support.*;
import cn.shiguang.notes.vo.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Administration use cases: authorization, transaction orchestration, and typed public projections.
 */
@Service
public class AdministrationApplicationService {
  private final GovernanceService governance;
  private final Commands commands;
  private final Pages pages;
  private final PublicQueryRepository read;
  private final HttpBoundaryConverter views;

  public AdministrationApplicationService(
      GovernanceService governance,
      Commands commands,
      Pages pages,
      PublicQueryRepository read,
      HttpBoundaryConverter views) {
    this.governance = governance;
    this.commands = commands;
    this.pages = pages;
    this.read = read;
    this.views = views;
  }

  @Transactional
  public IdVO createTopic(Actor actor, TopicRequest request, String key) {
    var body = views.input(request);
    actor.require("ADMIN");
    return views.view(
        commands.run(
            actor,
            "topic:create",
            key,
            body,
            () -> {
              long id = governance.createTopic(body);
              governance.audit(actor, "TOPIC_CREATE", "TOPIC", id, Map.of(), body, "新增话题");
              return Map.of("id", Long.toString(id));
            }),
        IdVO.class);
  }

  @Transactional
  public TopicVO updateTopic(Actor actor, long id, TopicRequest request) {
    var body = views.input(request);
    actor.require("ADMIN");
    var result = governance.updateTopic(id, body);
    governance.audit(actor, "TOPIC_UPDATE", "TOPIC", id, Map.of(), body, "更新话题");
    return views.view(result, TopicVO.class);
  }

  public PageVO<TopicVO> adminTopics(Actor a, String cursor, int size) {
    a.require("ADMIN");
    return views.page(
        pages.fetch("admin-topics", a.id(), cursor, size, (b, s) -> governance.allTopics(b, s)),
        TopicVO.class);
  }

  public PageVO<AuditVO> audits(Actor a, String cursor, int size) {
    a.requireLogin();
    if (!a.has("REVIEWER") && !a.has("ADMIN")) throw BizException.forbidden();
    return views.page(
        pages.fetch("audits", a.id(), cursor, size, (b, s) -> governance.audits(b, s)),
        AuditVO.class);
  }

  public PageVO<AdminUserVO> users(Actor a, String cursor, int size) {
    a.require("ADMIN");
    return views.page(
        pages.fetch("users", a.id(), cursor, size, (b, s) -> read.users(b, s)), AdminUserVO.class);
  }
}
