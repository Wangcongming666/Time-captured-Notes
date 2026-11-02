package cn.shiguang.notes.dto.request;

import jakarta.validation.*;
import java.lang.annotation.*;

/** Counts Unicode code points, matching domain limits rather than UTF-16 units. */
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = CodePointLength.Validator.class)
public @interface CodePointLength {
  String message() default "文本长度超限";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};

  int max();

  class Validator implements ConstraintValidator<CodePointLength, String> {
    private int max;

    public void initialize(CodePointLength rule) {
      max = rule.max();
    }

    public boolean isValid(String value, ConstraintValidatorContext context) {
      return value == null || value.codePointCount(0, value.length()) <= max;
    }
  }
}
