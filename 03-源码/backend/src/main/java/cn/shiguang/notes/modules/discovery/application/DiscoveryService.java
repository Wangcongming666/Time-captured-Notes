package cn.shiguang.notes.modules.discovery.application;

import cn.shiguang.notes.modules.discovery.api.DiscoveryApi;
import cn.shiguang.notes.modules.discovery.domain.*;
import cn.shiguang.notes.shared.contract.*;
import cn.shiguang.notes.shared.error.BizException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class DiscoveryService implements DiscoveryApi {
  private final DiscoveryRepository repo;
  private final CachePort cache;
  private final ObjectMapper json;
  private final String key;

  public DiscoveryService(
      DiscoveryRepository repo,
      CachePort cache,
      ObjectMapper json,
      @Value("${app.cursor-key}") String key) {
    this.repo = repo;
    this.cache = cache;
    this.json = json;
    this.key = key;
  }

  private boolean flag(Object v) {
    return Boolean.TRUE.equals(v) || "1".equals(Objects.toString(v));
  }

  @SuppressWarnings("unchecked")
  public Map<String, Object> feed(Actor actor, Map<String, String> query) {
    String mode = query.getOrDefault("mode", "discover");
    if (!Set.of("discover", "following", "recommend", "search").contains(mode))
      throw BizException.invalid("无效信息流类型");
    if (mode.equals("following") || mode.equals("recommend")) actor.requireLogin();
    int size;
    try {
      size = Integer.parseInt(query.getOrDefault("size", "20"));
    } catch (NumberFormatException e) {
      throw BizException.invalid("分页数量无效");
    }
    if (size < 1 || size > 50) throw BizException.invalid("分页数量须为1–50");
    var identity = new TreeMap<>(query);
    identity.remove("cursor");
    identity.remove("size");
    String queryHash = Tokens.hash(identity.toString());
    String cursor = query.get("cursor"), snapshot = null;
    int offset = 0;
    List<Map<String, Object>> rows;
    if (cursor != null && !cursor.isBlank()) {
      String[] p = cursor.split("\\.");
      if (p.length != 4
          || !Tokens.same(
              p[3],
              Tokens.sign(
                  p[0] + "." + p[1] + "." + p[2] + ":" + actor.id() + ":" + queryHash, key)))
        throw BizException.invalid("分页游标无效");
      snapshot = p[0];
      try {
        offset = Integer.parseInt(p[1]);
      } catch (NumberFormatException e) {
        throw BizException.invalid("分页游标无效");
      }
      String saved = cache.get("sg:rec:" + actor.id() + ":" + snapshot);
      if (saved == null) throw new BizException(409, "FEED_REFRESH_REQUIRED", "信息流已更新，请刷新");
      try {
        rows = json.readValue(saved, List.class);
      } catch (Exception e) {
        throw BizException.conflict("请刷新信息流");
      }
    } else {
      Long topic = query.get("topicId") == null ? null : Long.valueOf(query.get("topicId")),
          author = query.get("authorId") == null ? null : Long.valueOf(query.get("authorId"));
      rows =
          repo.candidates(actor.id(), mode, query.get("category"), query.get("q"), topic, author);
      double max =
          rows.stream()
              .mapToDouble(
                  r ->
                      Math.log1p(
                          ((Number) r.get("likeCount")).doubleValue()
                              + 2 * ((Number) r.get("favoriteCount")).doubleValue()
                              + ((Number) r.get("commentCount")).doubleValue()))
              .max()
              .orElse(1);
      final double denominator = Math.max(1, max);
      for (var r : rows) {
        double heat =
            Math.log1p(
                    ((Number) r.get("likeCount")).doubleValue()
                        + 2 * ((Number) r.get("favoriteCount")).doubleValue()
                        + ((Number) r.get("commentCount")).doubleValue())
                / denominator;
        double fresh =
            Math.max(
                0,
                1
                    - Duration.between(Instant.parse(Data.text(r, "publishedAt")), Instant.now())
                            .toHours()
                        / 168.0);
        r.put(
            "score",
            RecommendationPolicy.score(
                flag(r.get("interest")),
                flag(r.get("following")),
                heat,
                fresh,
                flag(r.get("reduced"))));
        r.put(
            "reason",
            mode.equals("recommend")
                ? RecommendationPolicy.reason(flag(r.get("interest")), flag(r.get("following")))
                : "LATEST");
      }
      if (mode.equals("recommend") || query.getOrDefault("sort", "").equals("hot"))
        rows.sort(
            Comparator.<Map<String, Object>>comparingDouble(
                    r -> ((Number) r.get("score")).doubleValue())
                .reversed()
                .thenComparing(
                    r -> Long.parseLong(r.get("id").toString()), Comparator.reverseOrder()));
      snapshot = UUID.randomUUID().toString();
      try {
        if (!cache.put("sg:rec:" + actor.id() + ":" + snapshot, json.writeValueAsString(rows), 120))
          snapshot = null;
      } catch (Exception e) {
        snapshot = null;
      }
    }
    if (offset < 0 || offset > rows.size()) throw BizException.invalid("分页游标无效");
    var visible = repo.visible(actor.id(), rows.stream().map(r -> Data.text(r, "id")).toList());
    var items = new ArrayList<Map<String, Object>>();
    int end = offset;
    while (end < rows.size() && items.size() < size) {
      var r = rows.get(end++);
      if (visible.contains(Data.text(r, "id")))
        items.add(
            Data.select(
                r,
                "id",
                "authorId",
                "title",
                "coverMediaId",
                "coverMediaType",
                "durationMs",
                "authorAvatarMediaId",
                "authorName",
                "reason",
                "score",
                "likeCount",
                "favoriteCount"));
    }
    String next = null;
    if (snapshot != null && end < rows.size()) {
      String raw = snapshot + "." + end + "." + queryHash;
      next = raw + "." + Tokens.sign(raw + ":" + actor.id() + ":" + queryHash, key);
    }
    return Data.map("items", items, "nextCursor", next, "hasMore", next != null);
  }

  public Map<String, Object> preferences(long user) {
    return repo.preferences(user);
  }

  public void preferences(long user, Map<String, Object> body) {
    var likes = Data.ids(body, "interests");
    var reduced = Data.ids(body, "reducedTopics");
    if (likes.stream().anyMatch(reduced::contains)) throw BizException.invalid("兴趣与减少推荐不能包含同一话题");
    repo.preferences(user, likes, reduced);
    clear(user);
  }

  public void hide(long user, long note, boolean hidden) {
    repo.hide(user, note, hidden);
    clear(user);
  }

  public void reset(long user) {
    repo.reset(user);
    clear(user);
  }

  public void clear(long user) {
    cache.removePrefix("sg:rec:" + user + ":");
  }
}
