import { it, expect } from "vitest";
import { usePagedList } from "./paging";
it("retains record 51 with independent cursors and deduplicates page overlap", async () => {
  const paths: string[] = [];
  const load = async (path: string) => {
    paths.push(path);
    return path.includes("cursor=")
      ? {
          items: [{ id: "50" }, { id: "51" }],
          nextCursor: null,
          hasMore: false,
        }
      : {
          items: Array.from({ length: 50 }, (_, i) => ({ id: String(i + 1) })),
          nextCursor: "signed cursor",
          hasMore: true,
        };
  };
  const list = usePagedList<{ id: string }>(() => "/folders?size=50", load);
  await list.load(true);
  expect(list.items.value).toHaveLength(50);
  expect(list.hasMore.value).toBe(true);
  await list.load();
  expect(paths[1]).toContain("cursor=signed%20cursor");
  expect(list.items.value).toHaveLength(51);
  expect(list.items.value[50]?.id).toBe("51");
  expect(list.hasMore.value).toBe(false);
  await list.load(true);
  expect(list.items.value).toHaveLength(50);
});
