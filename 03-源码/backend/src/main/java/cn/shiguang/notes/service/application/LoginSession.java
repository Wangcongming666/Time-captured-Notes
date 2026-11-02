package cn.shiguang.notes.service.application;

import cn.shiguang.notes.vo.UserVO;
import com.fasterxml.jackson.annotation.JsonIgnore;

/** Internal login result. The credential is consumed only by the cookie adapter. */
public record LoginSession(UserVO user, @JsonIgnore String token) {}
