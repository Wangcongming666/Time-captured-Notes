package cn.shiguang.notes.vo;

import java.util.List;

/**
 * Explicit public HTTP projection. Database entities and internal attributes cannot be serialized
 * here.
 */
public record AdminUserVO(
    String id, String username, String nickname, String status, List<String> roles) {}
