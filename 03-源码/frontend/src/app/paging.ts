import { shallowRef, ref } from "vue";
import { api } from "../api/client";
import type { Page } from "../api/types";
export function usePagedList<T extends { id: string }>(
  path: () => string,
  loader: (path: string) => Promise<Page<T>> = (path) => api<Page<T>>(path),
) {
  const items = shallowRef<T[]>([]),
    hasMore = ref(false),
    busy = ref(false);
  let cursor: string | null = null,
    generation = 0;
  async function load(reset = false) {
    const request = ++generation;
    busy.value = true;
    try {
      const base = path(),
        next = reset ? null : cursor;
      const page = await loader(
        base +
          (next
            ? (base.includes("?") ? "&" : "?") +
              "cursor=" +
              encodeURIComponent(next)
            : ""),
      );
      if (request !== generation) return;
      items.value = [
        ...new Map(
          (reset ? page.items : [...items.value, ...page.items]).map((item) => [
            item.id,
            item,
          ]),
        ).values(),
      ];
      cursor = page.nextCursor;
      hasMore.value = page.hasMore;
    } finally {
      if (request === generation) busy.value = false;
    }
  }
  return { items, hasMore, busy, load };
}
