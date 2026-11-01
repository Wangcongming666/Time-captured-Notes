package cn.shiguang.notes.modules.content.api;

import cn.shiguang.notes.shared.contract.Actor;
import java.util.*;

/** Content-owned persistence and lifecycle boundary. */
public interface ContentApi {
  Map<String, Object> note(long id, boolean lock);

  Map<String, Object> requirePublic(long id, boolean lock);

  Map<String, Object> detail(long id, Actor actor, boolean ownerView);

  Map<String, Object> preview(long id);

  List<Map<String, Object>> owned(long actor, long before, int size);

  Map<String, Object> save(Actor actor, Long id, Map<String, Object> body);

  Map<String, Object> submit(Actor actor, long id, int version);

  Map<String, Object> withdraw(Actor actor, long id, int version);

  void delete(Actor actor, long id, int version);

  void moderate(long id, int revision, String decision, String reason);

  void offline(long id, int version, String reason);

  Map<String, Object> addMedia(
      Actor actor,
      String key,
      String purpose,
      String mime,
      long size,
      int width,
      int height,
      String sha);

  Map<String, Object> media(long id);

  void bind(Actor actor, List<Long> media, String purpose, String ownerType, long ownerId);

  void avatar(Actor actor, Long next, Long previous);
}
