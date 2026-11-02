<script setup lang="ts">
import { ref, computed, watch, onMounted } from "vue";
import { useRoute, useRouter } from "vue-router";
import { api, queryString } from "../../api/client";
import type { Card, Page } from "../../api/types";
import { useSession } from "../../app/session";
import NoteCard from "../../components/NoteCard.vue";
import ErrorBanner from "../../components/ErrorBanner.vue";
import RequireLogin from "../../components/RequireLogin.vue";
const route = useRoute();
const router = useRouter();
const session = useSession();
const items = ref<Card[]>([]);
const loading = ref(false);
const error = ref("");
const cursor = ref<string | null>(null);
const more = ref(false);
const category = ref("全部");
const categories = [
  "全部",
  "旅行",
  "美食",
  "家居",
  "摄影",
  "读书",
  "户外",
  "穿搭",
];
const following = computed(() => route.path === "/following");
const searching = computed(() => route.path === "/search");
const mode = computed(() =>
  following.value ? "following" : session.user ? "recommend" : "discover",
);
let generation = 0;
async function load(reset = false) {
  if (loading.value && !reset) return;
  const current = ++generation;
  if (following.value && !session.user) return;
  loading.value = true;
  error.value = "";
  if (reset) {
    cursor.value = null;
    items.value = [];
  }
  try {
    const path = searching.value ? "/search" : "/feed";
    const page = await api<Page<Card>>(
      path +
        queryString({
          q: searching.value ? String(route.query.q ?? "") : undefined,
          mode: mode.value,
          category: category.value === "全部" ? undefined : category.value,
          cursor: cursor.value,
        }),
    );
    if (current !== generation) return;
    const seen = new Set(items.value.map((n) => n.id));
    items.value.push(...page.items.filter((n) => !seen.has(n.id)));
    cursor.value = page.nextCursor;
    more.value = page.hasMore;
  } catch (e) {
    if (current === generation)
      error.value = e instanceof Error ? e.message : "加载失败";
  } finally {
    if (current === generation) loading.value = false;
  }
}
function choose(value: string) {
  category.value = value;
  void load(true);
}
watch(
  () => route.fullPath,
  () => {
    category.value = "全部";
    void load(true);
  },
);
onMounted(() => {
  void load(true);
});
</script>
<template>
  <div v-if="searching" class="page-heading">
    <div>
      <h1>搜索「{{ route.query.q || "生活灵感" }}」</h1>
      <p>找到值得记录，也值得尝试的生活经验</p>
    </div>
  </div>
  <div v-else-if="following" class="page-heading">
    <div>
      <h1>关注的人，最近在分享</h1>
      <p>熟悉的生活，总有新的灵感。</p>
    </div>
  </div>
  <div v-else class="categories" aria-label="内容分类">
    <button
      v-for="item in categories"
      :key="item"
      class="category"
      :class="{ active: item === category }"
      :aria-pressed="item === category"
      @click="choose(item)"
    >
      {{ item === "全部" ? "推荐" : item }}</button
    ><RouterLink v-if="session.user" to="/preferences" class="category"
      >调整兴趣</RouterLink
    >
  </div>
  <ErrorBanner :message="error" :retry="() => load(true)" />
  <RequireLogin v-if="following && !session.user" />
  <div v-else-if="!loading && !error && !items.length" class="empty-state">
    <h1>{{ following ? "关注一些有趣的人吧" : "第一篇灵感，等你来分享" }}</h1>
    <p>
      {{
        following
          ? "关注作者后，他们发布的笔记会出现在这里。"
          : "这里还没有公开笔记。发布图文或视频，经审核通过后就能被大家发现。"
      }}
    </p>
    <RouterLink class="button primary" :to="following ? '/' : '/editor'">{{
      following ? "去发现" : "发布笔记"
    }}</RouterLink>
  </div>
  <section v-else class="waterfall" aria-label="生活笔记">
    <NoteCard v-for="note in items" :key="note.id" :note="note" />
  </section>
  <div v-if="loading" class="loading-state" role="status">
    正在寻找生活灵感…
  </div>
  <div v-if="more && !loading" class="load-more">
    <button class="button" @click="load()">查看更多</button>
  </div>
</template>
