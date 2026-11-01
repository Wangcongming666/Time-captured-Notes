import { test, expect } from "@playwright/test";
import { readFileSync } from "node:fs";
const password =
  process.env.DEMO_PASSWORD ??
  readFileSync(new URL("../../.env", import.meta.url), "utf8")
    .split(/\r?\n/)
    .find((line) => line.startsWith("DEMO_PASSWORD="))
    ?.slice(14);
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
  expect(errors).toEqual([]);
});
test("ordinary user creates a persisted private folder and cannot administer", async ({
  page,
}, info) => {
  if (!password)
    throw new Error("Set DEMO_PASSWORD for the running Compose instance");
  await page.goto("/login?next=/collections");
  await page.getByLabel("账号", { exact: true }).fill("demo_user");
  await page.getByLabel("密码", { exact: true }).fill(password);
  await page
    .locator(".login-form")
    .getByRole("button", { name: "登录", exact: true })
    .click();
  await expect(page).toHaveURL(/\/collections$/);
  const name = "浏览器验收" + Date.now().toString().slice(-8);
  await page.getByLabel("新建收藏夹", { exact: true }).fill(name);
  await page.getByRole("button", { name: "创建收藏夹", exact: true }).click();
  await expect(page.getByRole("button", { name, exact: true })).toBeVisible();
  await page.reload();
  await expect(page.getByRole("button", { name, exact: true })).toBeVisible();
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
