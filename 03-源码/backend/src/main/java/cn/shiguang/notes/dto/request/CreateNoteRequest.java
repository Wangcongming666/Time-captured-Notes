package cn.shiguang.notes.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.*;
import java.util.List;

/** HTTP input; unknown fields remain accepted for existing client compatibility. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CreateNoteRequest(
    @CodePointLength(max = 40) String title,
    @CodePointLength(max = 2000) String body,
    @NotNull @Size(max = 9) List<@NotNull String> mediaIds,
    @NotNull @Size(max = 5) List<@NotNull String> topicIds) {}
