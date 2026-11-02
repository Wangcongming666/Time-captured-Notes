package cn.shiguang.notes.repository.impl;

import cn.shiguang.notes.common.util.Data;
import cn.shiguang.notes.entity.*;
import cn.shiguang.notes.entity.UserEntity;
import cn.shiguang.notes.mapper.*;
import cn.shiguang.notes.mapper.UserMapper;
import cn.shiguang.notes.repository.IdentityRepository;
import cn.shiguang.notes.repository.support.MapperRows;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import java.util.*;
import org.springframework.stereotype.Repository;

/** Prepared identity queries and MP-backed user insert/lookup. */
@Repository
public class MySqlIdentityRepository implements IdentityRepository {
  private final RoleMapper roleMapper;
  private final SessionMapper sessionMapper;
  private final UserMapper userMapper;
  private final UserRoleMapper userRoleMapper;

  public MySqlIdentityRepository(
      RoleMapper roleMapper,
      SessionMapper sessionMapper,
      UserMapper userMapper,
      UserRoleMapper userRoleMapper) {
    this.roleMapper = roleMapper;
    this.sessionMapper = sessionMapper;
    this.userMapper = userMapper;
    this.userRoleMapper = userRoleMapper;
  }

  public Map<String, Object> findByName(String name) {
    var u = userMapper.selectOne(new QueryWrapper<UserEntity>().eq("username", name));
    return u == null
        ? null
        : Data.map(
            "id",
            u.getId().toString(),
            "passwordHash",
            u.getPasswordHash(),
            "status",
            u.getStatus());
  }

  public Map<String, Object> user(long id, boolean lock) {
    return MapperRows.requiredEntity(userMapper.identityUser(id, lock));
  }

  public long create(String name, String hash, String nickname) {
    var u = new UserEntity();
    u.setUsername(name);
    u.setPasswordHash(hash);
    u.setNickname(nickname);
    u.setBio("");
    u.setStatus("ACTIVE");
    u.setAuthVersion(0);
    userMapper.insert(u);
    userRoleMapper.identityCreate(u.getId());
    return u.getId();
  }

  public List<String> roles(long id) {
    return MapperRows.list(roleMapper.identityRoles(id)).stream()
        .map(r -> r.get("code").toString())
        .toList();
  }

  public Map<String, Object> session(String hash) {
    return MapperRows.optional(sessionMapper.identitySession(hash));
  }

  public void addSession(String hash, long id, int v, String csrf) {
    sessionMapper.identityAddSession(hash, id, v, csrf);
  }

  public void revoke(String hash) {
    sessionMapper.identityRevoke(hash);
  }

  public void csrf(String hash, String csrf) {
    sessionMapper.identityCsrf(csrf, hash);
  }

  public void profile(long id, String name, String bio, Long avatar) {
    userMapper.identityProfile(name, bio, avatar, id);
  }

  public void changeStatus(long id, String status) {
    userMapper.identityChangeStatus(status, id);
  }

  public void changeRoles(long id, List<String> roles) {
    userRoleMapper.identityChangeRoles(id);
    for (String role : roles) userRoleMapper.identityChangeRoles2(id, role);
    userMapper.identityChangeRoles(id);
  }

  public long activeAdministrators() {
    return userMapper.identityActiveAdministrators();
  }

  public void lockAdministrators() {
    MapperRows.list(roleMapper.identityLockAdministrators());
  }
}
