package cn.shiguang.notes.service;

import cn.shiguang.notes.dto.Actor;
import java.util.*;

/** Identity boundary; credentials never leave this API as raw database objects. */
public interface IdentityService {
  Map<String, Object> register(Map<String, Object> input);

  long verify(String username, String password);

  String session(long userId);

  Actor authenticate(String token);

  void revoke(String token);

  String csrf(String token);

  boolean checkCsrf(String token, String csrf);

  Map<String, Object> profile(long userId, boolean privateView);

  Map<String, Object> updateProfile(Actor actor, Map<String, Object> input);

  void status(Actor actor, long target, Map<String, Object> input);

  void roles(Actor actor, long target, Map<String, Object> input);
}
