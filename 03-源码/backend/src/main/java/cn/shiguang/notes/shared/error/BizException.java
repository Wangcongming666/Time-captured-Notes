package cn.shiguang.notes.shared.error;

/** Application failure with a safe public code and HTTP status. */
public final class BizException extends RuntimeException {
    private final int status;
    private final String code;
    public BizException(int status, String code, String message) { super(message); this.status=status; this.code=code; }
    public int status() { return status; }
    public String code() { return code; }
    public static BizException invalid(String message) { return new BizException(400,"INVALID_INPUT",message); }
    public static BizException forbidden() { return new BizException(403,"FORBIDDEN","没有执行此操作的权限"); }
    public static BizException missing() { return new BizException(404,"NOT_FOUND","内容不存在或不可见"); }
    public static BizException conflict(String message) { return new BizException(409,"VERSION_CONFLICT",message); }
}
