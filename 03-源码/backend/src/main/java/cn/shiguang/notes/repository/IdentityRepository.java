package cn.shiguang.notes.repository;

import java.util.*;

/** Identity persistence port. */
public interface IdentityRepository {
  Map<String, Object> findByName(String name);

  Map<String, Object> user(long id, boolean lock);

  long create(String name, String hash, String nickname);

  List<String> roles(long userId);

  Map<String, Object> session(String hash);

  void addSession(String hash, long userId, int authVersion, String csrfHash);

  void revoke(String hash);

  void csrf(String hash, String csrfHash);

  void profile(long id, String nickname, String bio, Long avatar);

  void changeStatus(long id, String status);

  void changeRoles(long id, List<String> roles);

  long activeAdministrators();

  void lockAdministrators();
}
