import { describe, it, expect } from "vitest";
import { queryString, mediaUrl, ApiError } from "./client";
describe("safe request construction", () => {
  it("encodes Chinese keywords and excludes absent filters", () => {
    expect(queryString({ q: "咖啡 & 茶", size: 20, cursor: null })).toBe(
      "?q=%E5%92%96%E5%95%A1+%26+%E8%8C%B6&size=20",
    );
  });
  it("encodes media identifiers rather than accepts arbitrary URLs", () => {
    expect(mediaUrl("../private")).toBe("/api/v1/media/..%2Fprivate/content");
  });
  it("keeps safe business failure information for user recovery", () => {
    const error = new ApiError(409, "请刷新", "VERSION_CONFLICT", "trace");
    expect(error.status).toBe(409);
    expect(error.message).toBe("请刷新");
  });
});
