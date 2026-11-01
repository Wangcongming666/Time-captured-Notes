interface Envelope<T> {
  code: string;
  message: string;
  data: T;
  traceId: string;
}
interface Options {
  method?: string;
  body?: unknown;
  key?: string;
}
export class ApiError extends Error {
  constructor(
    public status: number,
    message: string,
    public code = "",
    public traceId = "",
  ) {
    super(message);
  }
}
let csrf: string | null = null;
let csrfPromise: Promise<string> | null = null;
export function clearCsrf() {
  csrf = null;
  csrfPromise = null;
}
async function csrfToken(): Promise<string> {
  if (csrf) return csrf;
  if (!csrfPromise)
    csrfPromise = fetch("/api/v1/auth/csrf", { credentials: "same-origin" })
      .then(async (response) => {
        if (!response.ok)
          throw new ApiError(response.status, "无法获取安全令牌，请稍后重试");
        const result = (await response.json()) as Envelope<{ token: string }>;
        csrf = result.data.token;
        return csrf;
      })
      .finally(() => {
        csrfPromise = null;
      });
  return csrfPromise;
}
export async function api<T>(
  path: string,
  options: Options = {},
  retry = true,
): Promise<T> {
  const method = options.method ?? "GET";
  const headers: Record<string, string> = {};
  if (!["GET", "HEAD"].includes(method)) {
    headers["X-CSRF-TOKEN"] = await csrfToken();
    headers["Idempotency-Key"] = options.key ?? crypto.randomUUID();
  }
  const isForm = options.body instanceof FormData;
  if (options.body !== undefined && !isForm)
    headers["Content-Type"] = "application/json";
  let response: Response;
  try {
    response = await fetch("/api/v1" + path, {
      method,
      credentials: "same-origin",
      headers,
      body:
        options.body === undefined
          ? undefined
          : isForm
            ? (options.body as FormData)
            : JSON.stringify(options.body),
    });
  } catch {
    throw new ApiError(0, "暂时无法连接服务，请检查网络后重试");
  }
  let result: Envelope<T>;
  try {
    result = (await response.json()) as Envelope<T>;
  } catch {
    throw new ApiError(response.status, "服务返回异常，请稍后重试");
  }
  if (!response.ok) {
    if (retry && result.code === "CSRF_INVALID") {
      clearCsrf();
      return api(path, { ...options, key: headers["Idempotency-Key"] }, false);
    }
    throw new ApiError(
      response.status,
      result.message || "操作失败",
      result.code,
      result.traceId,
    );
  }
  return result.data;
}
export const mediaUrl = (id: string) =>
  "/api/v1/media/" + encodeURIComponent(id) + "/content";
export function queryString(
  values: Record<string, string | number | undefined | null>,
): string {
  const params = new URLSearchParams();
  for (const [key, value] of Object.entries(values))
    if (value !== undefined && value !== null && value !== "")
      params.set(key, String(value));
  return params.size ? "?" + params.toString() : "";
}
