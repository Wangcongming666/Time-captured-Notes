package cn.shiguang.notes.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.*;

/** HTTP input; unknown fields remain accepted for existing client compatibility. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ReportRequest(
    @NotNull @Pattern(regexp = "NOTE|COMMENT|PRACTICE") String targetType,
    @NotNull String targetId,
    @NotBlank @Size(max = 500) String reason) {}
