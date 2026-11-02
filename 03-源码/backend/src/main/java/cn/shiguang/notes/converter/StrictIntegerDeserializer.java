package cn.shiguang.notes.converter;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import java.io.IOException;

/** Keeps optimistic versions strict: decimals and overflow must never be rounded. */
public final class StrictIntegerDeserializer extends JsonDeserializer<Integer> {
  @Override
  public Integer deserialize(JsonParser parser, DeserializationContext context) throws IOException {
    if (parser.hasToken(JsonToken.VALUE_NUMBER_INT) || parser.hasToken(JsonToken.VALUE_STRING)) {
      try {
        return Integer.valueOf(parser.getText());
      } catch (NumberFormatException ignored) {
        /* Report a normal HTTP input error below. */
      }
    }
    return (Integer) context.handleUnexpectedToken(Integer.class, parser);
  }
}
