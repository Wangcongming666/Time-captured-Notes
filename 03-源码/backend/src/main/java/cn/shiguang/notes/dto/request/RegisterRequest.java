package cn.shiguang.notes.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.*;

/** HTTP input; unknown fields remain accepted for existing client compatibility. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RegisterRequest(
    @NotNull @Pattern(regexp = "[a-z0-9_]{4,32}") String username,
    @NotNull @Size(min = 8) String password,
    @NotBlank @CodePointLength(max = 40) String nickname) {

  @AssertTrue(message = "密码UTF-8编码不能超过72字节")
  @com.fasterxml.jackson.annotation.JsonIgnore
  public boolean isPasswordByteLengthValid() {
    return password == null
        || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length <= 72;
  }
}
