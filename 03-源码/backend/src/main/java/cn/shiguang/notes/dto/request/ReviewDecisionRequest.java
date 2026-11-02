package cn.shiguang.notes.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.*;

/** HTTP input; unknown fields remain accepted for existing client compatibility. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ReviewDecisionRequest(
    @NotNull
        @com.fasterxml.jackson.databind.annotation.JsonDeserialize(
            using = cn.shiguang.notes.converter.StrictIntegerDeserializer.class)
        Integer version,
    @NotNull @Pattern(regexp = "APPROVE|REJECT") String decision,
    @Size(max = 500) String reason) {

  @AssertTrue(message = "驳回须填写原因")
  @com.fasterxml.jackson.annotation.JsonIgnore
  public boolean isRejectionReasonValid() {
    return !"REJECT".equals(decision) || reason != null && !reason.isBlank();
  }
}
