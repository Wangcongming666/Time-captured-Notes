package cn.shiguang.notes.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.*;

/** HTTP input; unknown fields remain accepted for existing client compatibility. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ReportDecisionRequest(
    @NotNull
        @com.fasterxml.jackson.databind.annotation.JsonDeserialize(
            using = cn.shiguang.notes.converter.StrictIntegerDeserializer.class)
        Integer version,
    String status,
    @NotBlank @Size(max = 500) String reason) {}
