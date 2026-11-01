<script setup lang="ts">
import { ref, onMounted } from "vue";
import { api } from "../../api/client";
import type { Topic, Page } from "../../api/types";
import { useSession } from "../../app/session";
import RequireLogin from "../../components/RequireLogin.vue";
import ErrorBanner from "../../components/ErrorBanner.vue";
const session = useSession();
const topics = ref<Topic[]>([]);
const interests = ref<string[]>([]);
const reduced = ref<string[]>([]);
const error = ref("");
const busy = ref(false);
async function load() {
  if (!session.user) return;
  try {
    topics.value = (await api<Page<Topic>>("/topics")).items;
    const result = await api<{ interests: string[]; reducedTopics: string[] }>(
      "/me/preferences",
    );
    interests.value = result.interests;
    reduced.value = result.reducedTopics;
  } catch (e) {
    error.value = e instanceof Error ? e.message : "加载失败";
  }
}
function toggle(id: string, kind: "like" | "reduce") {
  const target = kind === "like" ? interests : reduced;
  const other = kind === "like" ? reduced : interests;
  target.value = target.value.includes(id)
    ? target.value.filter((x) => x !== id)
    : [...target.value, id];
  other.value = other.value.filter((x) => x !== id);
}
async function save() {
  busy.value = true;
  error.value = "";
  try {
    await api("/me/preferences", {
      method: "PUT",
      body: { interests: interests.value, reducedTopics: reduced.value },
    });
    session.notify("兴趣已更新，返回发现页刷新即可查看");
  } catch (e) {
    error.value = e instanceof Error ? e.message : "保存失败";
  } finally {
    busy.value = false;
  }
}
async function reset() {
  if (!confirm("恢复兴趣设置并清除不感兴趣的笔记记录？")) return;
  try {
    await api("/me/preferences", { method: "DELETE" });
    await load();
    session.notify("已恢复默认兴趣");
  } catch (e) {
    error.value = e instanceof Error ? e.message : "恢复失败";
  }
}
onMounted(load);
</script>
<template>
  <RequireLogin
    ><section class="narrow">
      <header class="page-heading">
        <div>
          <h1>你的兴趣，由你决定</h1>
          <p>告诉我们想多看什么，也可以让某个话题少出现一些。</p>
        </div>
      </header>
      <ErrorBanner :message="error" />
      <section class="section">
        <h2>我感兴趣的</h2>
        <div class="topic-picker">
          <button
            v-for="topic in topics"
            :key="topic.id"
            class="topic-chip"
            :class="{ selected: interests.includes(topic.id) }"
            :aria-pressed="interests.includes(topic.id)"
            @click="toggle(topic.id, 'like')"
          >
            # {{ topic.name }}
          </button>
        </div>
      </section>
      <section class="section">
        <h2>减少这些话题</h2>
        <div class="topic-picker">
          <button
            v-for="topic in topics"
            :key="topic.id"
            class="topic-chip"
            :class="{ selected: reduced.includes(topic.id) }"
            :aria-pressed="reduced.includes(topic.id)"
            @click="toggle(topic.id, 'reduce')"
          >
            # {{ topic.name }}
          </button>
        </div>
      </section>
      <div class="action-group">
        <button class="button primary" :disabled="busy" @click="save">
          保存设置</button
        ><button class="text-button" @click="reset">恢复默认</button>
      </div>
      <section class="section">
        <h2>为什么会推荐给我？</h2>
        <p class="hint">
          卡片会标注兴趣匹配、关注作者或热门来源。推荐使用明确规则，不读取你的私有实践记录；减少推荐会降低话题权重。你还可以在笔记详情选择“不感兴趣”，将单篇内容从推荐中隐藏。
        </p>
      </section>
    </section></RequireLogin
  >
</template>
