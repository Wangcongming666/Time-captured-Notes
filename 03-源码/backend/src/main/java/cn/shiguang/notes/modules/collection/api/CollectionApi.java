package cn.shiguang.notes.modules.collection.api;

import cn.shiguang.notes.shared.contract.Actor;
import java.util.*;

public interface CollectionApi {
  Map<String, Object> folder(Actor actor, long id, boolean lock);

  List<Map<String, Object>> folders(Actor actor, long before, int size);

  long createFolder(Actor actor, String name);

  Map<String, Object> renameFolder(Actor actor, long id, String name, int version);

  void deleteFolder(Actor actor, long id);

  boolean favorite(Actor actor, long folder, long note, boolean desired);

  boolean hasFavorite(long user, long note);

  List<Map<String, Object>> favorites(Actor actor, long folder, long before, int size);

  long createPractice(Actor actor, long note);

  Map<String, Object> practice(long id, boolean lock);

  Map<String, Object> ownPractice(Actor actor, long id, boolean lock);

  List<Map<String, Object>> practices(Actor actor, long before, int size);

  Map<String, Object> transition(Actor actor, long id, int version, String to);

  Map<String, Object> result(Actor actor, long id, Map<String, Object> input);

  Map<String, Object> publishRequest(Actor actor, long id, int version, int sourceRevision);

  Map<String, Object> unpublish(Actor actor, long id, int version);

  void moderate(long id, int revision, String decision);

  void offline(long id, int version);

  List<Map<String, Object>> history(Actor actor, long id, long before, int size);
}
