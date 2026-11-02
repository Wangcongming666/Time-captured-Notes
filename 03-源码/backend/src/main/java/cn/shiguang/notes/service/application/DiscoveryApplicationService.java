package cn.shiguang.notes.service.application;

import cn.shiguang.notes.converter.HttpBoundaryConverter;
import cn.shiguang.notes.dto.Actor;
import cn.shiguang.notes.dto.request.*;
import cn.shiguang.notes.service.*;
import cn.shiguang.notes.service.support.*;
import cn.shiguang.notes.vo.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Discovery use cases: authorization, transaction orchestration, and typed public projections. */
@Service
public class DiscoveryApplicationService {
  private final ContentService content;
  private final GovernanceService governance;
  private final DiscoveryService discovery;
  private final Pages pages;
  private final HttpBoundaryConverter views;

  public DiscoveryApplicationService(
      ContentService content,
      GovernanceService governance,
      DiscoveryService discovery,
      Pages pages,
      HttpBoundaryConverter views) {
    this.content = content;
    this.governance = governance;
    this.discovery = discovery;
    this.pages = pages;
    this.views = views;
  }

  @Transactional
  public void preferences(Actor actor, PreferencesRequest request) {
    var body = views.input(request);
    actor.requireLogin();
    discovery.preferences(actor.id(), body);
  }

  @Transactional
  public void hide(Actor actor, long id, boolean hidden) {
    actor.requireLogin();
    if (hidden) content.requirePublic(id, false);
    discovery.hide(actor.id(), id, hidden);
  }

  @Transactional
  public void resetPreferences(Actor actor) {
    actor.requireLogin();
    discovery.reset(actor.id());
  }

  public PageVO<CardVO> feed(Actor actor, FeedQueryRequest request) {
    var query = views.query(request);
    return views.page(discovery.feed(actor, query), CardVO.class);
  }

  public PreferencesVO preferences(Actor a) {
    a.requireLogin();
    return views.view(discovery.preferences(a.id()), PreferencesVO.class);
  }

  public PageVO<TopicVO> topics(Actor a, String cursor, int size) {
    return views.page(
        pages.fetch("topics", a.id(), cursor, size, (b, s) -> governance.topics(b, s)),
        TopicVO.class);
  }

  public PageVO<CardVO> search(Actor actor, FeedQueryRequest request) {
    var query = views.query(request);
    query.put("mode", "search");
    return views.page(discovery.feed(actor, query), CardVO.class);
  }
}
