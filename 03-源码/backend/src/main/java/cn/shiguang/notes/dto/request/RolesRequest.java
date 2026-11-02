package cn.shiguang.notes.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.*;
import java.util.List;

/** HTTP input; unknown fields remain accepted for existing client compatibility. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RolesRequest(
    @NotEmpty List<@NotNull @Pattern(regexp = "USER|REVIEWER|ADMIN") String> roles,
    @NotBlank String reason) {}
