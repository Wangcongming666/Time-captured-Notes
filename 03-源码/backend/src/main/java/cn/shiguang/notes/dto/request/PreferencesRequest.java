package cn.shiguang.notes.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.*;
import java.util.List;

/** HTTP input; unknown fields remain accepted for existing client compatibility. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PreferencesRequest(
    @NotNull @Size(max = 30) List<@NotNull String> interests,
    @NotNull @Size(max = 30) List<@NotNull String> reducedTopics) {}
