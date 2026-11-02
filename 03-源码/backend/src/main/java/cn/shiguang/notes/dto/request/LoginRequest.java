package cn.shiguang.notes.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.*;

/** HTTP input; unknown fields remain accepted for existing client compatibility. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record LoginRequest(@NotNull String username, @NotNull String password) {}
