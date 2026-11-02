package cn.shiguang.notes.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.*;

/** HTTP input; unknown fields remain accepted for existing client compatibility. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TopicRequest(
    @NotBlank @Size(max = 40) String name,
    @NotBlank @Size(max = 40) String category,
    @NotNull @Pattern(regexp = "ACTIVE|DISABLED") String status) {}
