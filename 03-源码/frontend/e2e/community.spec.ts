import { test, expect } from "@playwright/test";
import { readFileSync } from "node:fs";
import { mkdtempSync, unlinkSync, rmdirSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { execFileSync } from "node:child_process";
const password =
  process.env.DEMO_PASSWORD ??
  readFileSync(new URL("../../.env", import.meta.url), "utf8")
    .split(/\r?\n/)
    .find((line) => line.startsWith("DEMO_PASSWORD="))
    ?.slice(14);

// Both viewport projects share one real gateway IP. Start each in a fresh
// production rate-limit window instead of changing quotas or clearing Redis.
test.beforeAll(async () => {
  test.setTimeout(65000);
  await new Promise((resolve) =>
    setTimeout(resolve, 61000 - (Date.now() % 60000)),
  );
});
// A contract fixture tests page-51 UI reachability; other cases use the real database.
test("admin can reach record 51 using the next-page control", async ({
  page,
}) => {
  if (!password) throw new Error("DEMO_PASSWORD is required");
  await page.route("**/api/v1/admin/users?*", async (route) => {
    const next = new URL(route.request().url()).searchParams.has("cursor");
    const ids = next ? [51] : Array.from({ length: 50 }, (_, i) => i + 1);
    await route.fulfill({
      json: {
        code: "OK",
        message: "契约测试夹具",
        traceId: "test-fixture",
        data: {
          items: ids.map((id) => ({
            id: String(id),
            username: "fixture_" + id,
            nickname: "分页测试用户" + id,
            roles: ["USER"],
            status: "ACTIVE",
          })),
          nextCursor: next ? null : "fixture-page-2",
          hasMore: !next,
        },
      },
    });
  });
  await page.goto("/login?next=/admin/users");
  await page.getByLabel("账号", { exact: true }).fill("demo_admin");
  await page.getByLabel("密码", { exact: true }).fill(password);
  await page
    .locator(".login-form")
    .getByRole("button", { name: "登录", exact: true })
    .click();
  await expect(page).toHaveURL(/\/admin\/users$/);
  await expect(
    page.getByRole("heading", { name: /分页测试用户50/ }),
  ).toBeVisible();
  await expect(
    page.getByRole("heading", { name: /分页测试用户51/ }),
  ).toHaveCount(0);
  await page.getByRole("button", { name: "加载更多记录", exact: true }).click();
  await expect(
    page.getByRole("heading", { name: /分页测试用户51/ }),
  ).toBeVisible();
  await expect(
    page.getByRole("button", { name: "加载更多记录", exact: true }),
  ).toHaveCount(0);
});
test("visitor opens real published content with no horizontal overflow", async ({
  page,
}, info) => {
  const errors: string[] = [];
  page.on("pageerror", (e) => errors.push(e.message));
  await page.goto("/");
  await expect(page.locator(".note-card").first()).toBeVisible();
  await expect(page.locator(".note-card img").first()).toHaveJSProperty(
    "naturalWidth",
    800,
  );
  if (process.env.E2E_CAPTURE_SCREENSHOTS !== "0")
    await page.screenshot({
      path: `test-results/${info.project.name}-discover.png`,
      fullPage: true,
    });
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= innerWidth,
    ),
  ).toBeTruthy();
  await page.locator(".card-title").first().click();
  await expect(page.locator(".note-body")).toContainText("合成演示内容");
  if (process.env.E2E_CAPTURE_SCREENSHOTS !== "0")
    await page.screenshot({
      path: `test-results/${info.project.name}-detail.png`,
      fullPage: true,
    });
  expect(errors).toEqual([]);
});

test("author previews video, saves a draft and persists a private folder", async ({
  page,
}, info) => {
  test.setTimeout(60000);
  if (!password) throw new Error("DEMO_PASSWORD is required");
  await page.goto("/login?next=/editor");
  await page.getByLabel("账号", { exact: true }).fill("demo_user");
  await page.getByLabel("密码", { exact: true }).fill(password);
  await page
    .locator(".login-form")
    .getByRole("button", { name: "登录", exact: true })
    .click();
  await expect(
    page.getByRole("button", { name: "上传图文", exact: true }),
  ).toHaveAttribute("aria-pressed", "true");
  await page.getByRole("button", { name: "上传视频", exact: true }).click();
  await expect(page.locator('.editor input[type="file"]')).toHaveAttribute(
    "accept",
    "video/mp4",
  );
  const dir = mkdtempSync(join(tmpdir(), "shiguang-e2e-"));
  const file = join(dir, "sample.mp4");
  try {
    execFileSync("ffmpeg", [
      "-v",
      "error",
      "-nostdin",
      "-y",
      "-f",
      "lavfi",
      "-i",
      "color=c=teal:s=240x320:d=2",
      "-c:v",
      "libx264",
      "-pix_fmt",
      "yuv420p",
      file,
    ]);
    await page.locator('.editor input[type="file"]').setInputFiles(file);
    const video = page.locator(".video-preview video");
    await expect(video).toBeVisible({ timeout: 45000 });
    await expect
      .poll(() =>
        video.evaluate((element: HTMLVideoElement) => element.readyState),
      )
      .toBeGreaterThanOrEqual(1);
    await video.evaluate((element: HTMLVideoElement) => {
      element.muted = true;
      return element.play();
    });
    await expect
      .poll(() =>
        video.evaluate((element: HTMLVideoElement) => element.currentTime),
      )
      .toBeGreaterThan(0);
    await page
      .getByPlaceholder("给你的记录起个标题")
      .fill("浏览器视频草稿验收");
    if (process.env.E2E_CAPTURE_SCREENSHOTS !== "0")
      await page.screenshot({
        path: `test-results/${info.project.name}-video-editor.png`,
        fullPage: true,
      });
    const saved = page.waitForResponse(
      (response) =>
        response.url().endsWith("/api/v1/notes") &&
        response.request().method() === "POST",
    );
    await page.getByRole("button", { name: "保存草稿", exact: true }).click();
    const result = (await (await saved).json()).data;
    const csrf = (await (await page.request.get("/api/v1/auth/csrf")).json())
      .data.token;
    const deleted = await page.request.delete(
      `/api/v1/notes/${result.id}?version=${result.version}`,
      { headers: { "X-CSRF-TOKEN": csrf } },
    );
    expect(deleted.status()).toBe(200);
    const summaryResponse = page.waitForResponse((response) =>
      /\/api\/v1\/users\/[^/]+\/summary$/.test(response.url()),
    );
    await page.goto("/me");
    await expect(page.locator(".profile-stats")).toContainText("粉丝");
    const summary = (await (await summaryResponse).json()).data;
    expect(typeof summary.following).toBe("boolean");
    expect(summary.noteCount).toBeGreaterThanOrEqual(0);
    if (process.env.E2E_CAPTURE_SCREENSHOTS !== "0")
      await page.screenshot({
        path: `test-results/${info.project.name}-profile.png`,
        fullPage: true,
      });
  } finally {
    unlinkSync(file);
    rmdirSync(dir);
  }
  await page
    .getByRole("link", { name: "收藏 · 仅自己可见", exact: true })
    .click();
  const name = "浏览器验收" + Date.now().toString().slice(-8);
  await page.getByLabel("新建收藏夹", { exact: true }).fill(name);
  await page.getByRole("button", { name: "创建收藏夹", exact: true }).click();
  await expect(page.getByRole("button", { name, exact: true })).toBeVisible();
  await page.reload();
  await expect(page.getByRole("button", { name, exact: true })).toBeVisible();
  if (process.env.E2E_CAPTURE_SCREENSHOTS !== "0")
    await page.screenshot({
      path: `test-results/${info.project.name}-collections.png`,
      fullPage: true,
    });
  const response = await page.request.get("/api/v1/admin/users");
  expect(response.status()).toBe(403);
  await page.getByRole("button", { name, exact: true }).click();
  page.once("dialog", (d) => d.accept());
  await page.getByRole("button", { name: "删除收藏夹", exact: true }).click();
  await expect(page.getByRole("button", { name, exact: true })).toHaveCount(0);
});
