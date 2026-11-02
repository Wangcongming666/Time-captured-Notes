package cn.shiguang.notes.common.exception;

import java.io.IOException;

/** Safe boundary between storage availability errors and invalid image content. */
public class MediaStoreException extends IOException {
  public MediaStoreException(String operation, Exception cause) {
    super("Media store " + operation + " failed", cause);
  }
}
