package cn.shiguang.notes.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.*;
import java.util.List;

/** HTTP input; unknown fields remain accepted for existing client compatibility. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PracticeResultRequest(
    @NotNull
        @com.fasterxml.jackson.databind.annotation.JsonDeserialize(
            using = cn.shiguang.notes.converter.StrictIntegerDeserializer.class)
        Integer version,
    @NotBlank @CodePointLength(max = 2000) String resultText,
    @NotNull @Pattern(regexp = "HELPFUL|NEUTRAL|NOT_APPLICABLE") String helpfulness,
    @NotNull @Size(max = 9) List<@NotNull String> mediaIds) {}
