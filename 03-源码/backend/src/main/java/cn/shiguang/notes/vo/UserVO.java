package cn.shiguang.notes.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * Explicit public HTTP projection. Database entities and internal attributes cannot be serialized
 * here.
 */
public record UserVO(
    String id,
    String nickname,
    String bio,
    String avatarMediaId,
    @JsonInclude(JsonInclude.Include.NON_NULL) List<String> roles,
    @JsonInclude(JsonInclude.Include.NON_NULL) String status) {}
