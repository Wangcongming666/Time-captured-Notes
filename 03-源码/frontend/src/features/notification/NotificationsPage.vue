<script setup lang="ts">
import { ref, onMounted, onUnmounted } from "vue";
import { api, queryString } from "../../api/client";
import type { Notice, Report, Page } from "../../api/types";
import { useSession } from "../../app/session";
import RequireLogin from "../../components/RequireLogin.vue";
import ErrorBanner from "../../components/ErrorBanner.vue";
const session = useSession();
const items = ref<Notice[]>([]);
const reports = ref<Report[]>([]);
const type = ref("");
const error = ref("");
const unread = ref(0);
const cursor = ref<string | null>(null);
const more = ref(false);
let timer: ReturnType<typeof setInterval> | undefined;
const labels: Record<string, string> = {
  LIKE: "有人赞了你的笔记",
  COMMENT: "你的笔记收到新评论",
  FOLLOW: "你有一位新关注者",
  REVIEW: "内容审核有了新结果",
  REPORT: "你的举报有了处理结果",
  BAN: "账号状态有更新",
};
async function load(reset = true) {
  if (!session.user) return;
  try {
    if (reset) cursor.value = null;
    const page = await api<Page<Notice>>(
      "/notifications" +
        queryString({ type: type.value, cursor: cursor.value }),
    );
    items.value = reset ? page.items : [...items.value, ...page.items];
    cursor.value = page.nextCursor;
    more.value = page.hasMore;
    unread.value = (
      await api<{ count: number }>("/notifications/unread")
    ).count;
    reports.value = (await api<Page<Report>>("/me/reports")).items;
    error.value = "";
  } catch (e) {
    error.value = e instanceof Error ? e.message : "加载失败";
  }
}
async function read(item: Notice) {
  if (item.readAt) return;
  try {
    await api("/notifications/" + item.id + "/read", { method: "PUT" });
    item.readAt = new Date().toISOString();
    unread.value = (
      await api<{ count: number }>("/notifications/unread")
    ).count;
  } catch (e) {
    error.value = e instanceof Error ? e.message : "更新失败";
  }
}
async function readAll() {
  if (!items.value.length) return;
  try {
    await api("/notifications/read-all", {
      method: "POST",
      body: { throughId: items.value[0].id },
    });
    await load();
  } catch (e) {
    error.value = e instanceof Error ? e.message : "更新失败";
  }
}
onMounted(() => {
  void load();
  timer = setInterval(() => {
    if (!document.hidden) void load();
  }, 30000);
});
onUnmounted(() => clearInterval(timer));
</script>
<template>
  <RequireLogin
    ><header class="page-heading">
      <div>
        <h1>与你有关的新消息</h1>
        <p>{{ unread }} 条未读 · 互动与审核结果异步送达</p>
      </div>
      <button class="button" @click="readAll">本页及以前全部已读</button>
    </header>
    <nav class="tabs">
      <button
        v-for="tab in [
          { value: '', label: '全部' },
          { value: 'LIKE', label: '赞' },
          { value: 'COMMENT', label: '评论' },
          { value: 'FOLLOW', label: '关注' },
          { value: 'REVIEW', label: '审核' },
          { value: 'REPORT', label: '举报' },
        ]"
        :key="tab.value"
        :class="{ active: type === tab.value }"
        @click="
          type = tab.value;
          load();
        "
      >
        {{ tab.label }}
      </button>
    </nav>
    <ErrorBanner :message="error" :retry="load" />
    <article
      v-for="item in items"
      :key="item.id"
      class="list-row notice"
      :class="{ unread: !item.readAt }"
    >
      <div class="notice-main">
        <span v-if="!item.readAt" class="notification-dot" aria-label="未读" />
        <div>
          <h3>{{ labels[item.type] ?? "系统通知" }}</h3>
          <p>
            {{ new Date(item.createdAt).toLocaleString("zh-CN")
            }}<span v-if="!item.targetAvailable">
              · 相关内容当前不可公开访问</span
            >
          </p>
        </div>
      </div>
      <div class="row-actions">
        <RouterLink
          v-if="item.targetAvailable && item.targetType === 'NOTE'"
          :to="'/notes/' + item.targetId"
          class="text-button"
          @click="read(item)"
          >查看笔记</RouterLink
        ><RouterLink
          v-if="item.type === 'REVIEW'"
          to="/creator"
          class="text-button"
          @click="read(item)"
          >查看审核状态</RouterLink
        ><button v-if="!item.readAt" class="text-button" @click="read(item)">
          标为已读
        </button>
      </div>
    </article>
    <div v-if="!items.length" class="empty-state">
      <h2>暂时没有新消息</h2>
      <p>收到互动或治理结果后，会在这里通知你。</p>
    </div>
    <div v-if="more" class="load-more">
      <button class="button" @click="load(false)">加载更多</button>
    </div>
    <section v-if="reports.length" class="section">
      <h2>我的举报进度</h2>
      <article v-for="report in reports" :key="report.id" class="list-row">
        <div>
          <h3>{{ report.targetType }} #{{ report.targetId }}</h3>
          <p>{{ report.resultReason || "等待审核员处理" }}</p>
        </div>
        <span class="status" :class="report.status">{{
          {
            PENDING: "待处理",
            PROCESSING: "处理中",
            UPHELD: "举报成立",
            REJECTED: "已驳回",
          }[report.status]
        }}</span>
      </article>
    </section></RequireLogin
  >
</template>
