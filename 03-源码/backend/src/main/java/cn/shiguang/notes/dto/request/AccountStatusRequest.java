package cn.shiguang.notes.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.*;

/** HTTP input; unknown fields remain accepted for existing client compatibility. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AccountStatusRequest(
    @NotNull @Pattern(regexp = "ACTIVE|BANNED") String status, @NotBlank String reason) {}
