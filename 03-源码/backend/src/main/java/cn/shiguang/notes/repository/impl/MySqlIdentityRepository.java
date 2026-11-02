package cn.shiguang.notes.repository.impl;

import cn.shiguang.notes.common.util.Data;
import cn.shiguang.notes.entity.UserEntity;
import cn.shiguang.notes.mapper.UserMapper;
import cn.shiguang.notes.repository.IdentityRepository;
import cn.shiguang.notes.repository.support.Database;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import java.util.*;
import org.springframework.stereotype.Repository;

/** Prepared identity queries and MP-backed user insert/lookup. */
@Repository
public class MySqlIdentityRepository implements IdentityRepository {
  private final Database db;
  private final UserMapper users;

  public MySqlIdentityRepository(Database db, UserMapper users) {
    this.db = db;
    this.users = users;
  }

  public Map<String, Object> findByName(String name) {
    var u = users.selectOne(new QueryWrapper<UserEntity>().eq("username", name));
    return u == null
        ? null
        : Data.map("id", u.id.toString(), "passwordHash", u.passwordHash, "status", u.status);
  }

  public Map<String, Object> user(long id, boolean lock) {
    return db.one("SELECT * FROM t_user WHERE id=?" + (lock ? " FOR UPDATE" : ""), id);
  }

  public long create(String name, String hash, String nickname) {
    var u = new UserEntity();
    u.username = name;
    u.passwordHash = hash;
    u.nickname = nickname;
    u.bio = "";
    u.status = "ACTIVE";
    u.authVersion = 0;
    users.insert(u);
    db.update("INSERT INTO t_user_role(user_id,role_id) VALUES(?,1)", u.id);
    return u.id;
  }

  public List<String> roles(long id) {
    return db
        .query(
            "SELECT r.code FROM t_role r JOIN t_user_role ur ON ur.role_id=r.id WHERE ur.user_id=?",
            id)
        .stream()
        .map(r -> r.get("code").toString())
        .toList();
  }

  public Map<String, Object> session(String hash) {
    return db.optional(
        "SELECT s.*,u.status AS user_status,u.auth_version AS current_auth_version FROM t_session s"
            + " JOIN t_user u ON u.id=s.user_id WHERE s.token_hash=? AND s.revoked_at IS NULL AND"
            + " s.expires_at>UTC_TIMESTAMP(3)",
        hash);
  }

  public void addSession(String hash, long id, int v, String csrf) {
    db.update(
        "INSERT INTO t_session(token_hash,user_id,auth_version,csrf_hash,expires_at)"
            + " VALUES(?,?,?,?,DATE_ADD(UTC_TIMESTAMP(3),INTERVAL 24 HOUR))",
        hash,
        id,
        v,
        csrf);
  }

  public void revoke(String hash) {
    db.update("UPDATE t_session SET revoked_at=UTC_TIMESTAMP(3) WHERE token_hash=?", hash);
  }

  public void csrf(String hash, String csrf) {
    db.update(
        "UPDATE t_session SET csrf_hash=? WHERE token_hash=? AND revoked_at IS NULL", csrf, hash);
  }

  public void profile(long id, String name, String bio, Long avatar) {
    db.update(
        "UPDATE t_user SET nickname=?,bio=?,avatar_media_id=? WHERE id=?", name, bio, avatar, id);
  }

  public void changeStatus(long id, String status) {
    db.update("UPDATE t_user SET status=?,auth_version=auth_version+1 WHERE id=?", status, id);
  }

  public void changeRoles(long id, List<String> roles) {
    db.update("DELETE FROM t_user_role WHERE user_id=?", id);
    for (String role : roles)
      db.update(
          "INSERT INTO t_user_role(user_id,role_id) SELECT ?,id FROM t_role WHERE code=?",
          id,
          role);
    db.update("UPDATE t_user SET auth_version=auth_version+1 WHERE id=?", id);
  }

  public long activeAdministrators() {
    return db.count(
        "SELECT COUNT(*) FROM t_user u JOIN t_user_role ur ON ur.user_id=u.id JOIN t_role r ON"
            + " r.id=ur.role_id WHERE r.code='ADMIN' AND u.status='ACTIVE'");
  }

  public void lockAdministrators() {
    db.query("SELECT id FROM t_role WHERE code='ADMIN' FOR UPDATE");
  }
}
