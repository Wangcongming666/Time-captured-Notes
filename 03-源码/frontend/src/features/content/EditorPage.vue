<script setup lang="ts">
import { ref, onMounted } from "vue";
import { useRoute, useRouter, onBeforeRouteLeave } from "vue-router";
import { api, mediaUrl } from "../../api/client";
import type { Note, Topic, Page, Version } from "../../api/types";
import { useSession } from "../../app/session";
import RequireLogin from "../../components/RequireLogin.vue";
import ErrorBanner from "../../components/ErrorBanner.vue";
const route = useRoute();
const router = useRouter();
const session = useSession();
const title = ref("");
const body = ref("");
const mediaIds = ref<string[]>([]);
const topicIds = ref<string[]>([]);
const topics = ref<Topic[]>([]);
const noteId = ref<string | null>(
  route.params.id ? String(route.params.id) : null,
);
const version = ref(0);
const status = ref("DRAFT");
const busy = ref(false);
const error = ref("");
const dirty = ref(false);
async function load() {
  if (!session.user) return;
  try {
    topics.value = (await api<Page<Topic>>("/topics")).items;
    if (noteId.value) {
      const note = await api<Note>("/me/notes/" + noteId.value);
      title.value = note.title;
      body.value = note.body;
      mediaIds.value = note.mediaIds;
      topicIds.value = note.topicIds;
      version.value = note.version ?? 0;
      status.value = note.status ?? "DRAFT";
    }
  } catch (e) {
    error.value = e instanceof Error ? e.message : "加载失败";
  }
}
async function upload(event: Event) {
  const files = Array.from((event.target as HTMLInputElement).files ?? []);
  if (mediaIds.value.length + files.length > 9) {
    error.value = "最多选择9张图片";
    return;
  }
  busy.value = true;
  error.value = "";
  try {
    for (const file of files) {
      if (file.size > 10 * 1024 * 1024) throw new Error("单张图片不能超过10MB");
      const form = new FormData();
      form.append("file", file);
      form.append("purpose", "NOTE");
      const result = await api<{ id: string }>("/media", {
        method: "POST",
        body: form,
      });
      mediaIds.value.push(result.id);
    }
    dirty.value = true;
  } catch (e) {
    error.value = e instanceof Error ? e.message : "上传失败";
  } finally {
    busy.value = false;
    (event.target as HTMLInputElement).value = "";
  }
}
function toggleTopic(id: string) {
  if (topicIds.value.includes(id))
    topicIds.value = topicIds.value.filter((t) => t !== id);
  else if (topicIds.value.length < 5) topicIds.value.push(id);
  else session.notify("最多选择5个话题");
  dirty.value = true;
}
async function save(publish: boolean) {
  busy.value = true;
  error.value = "";
  try {
    const payload = {
      title: title.value,
      body: body.value,
      mediaIds: mediaIds.value,
      topicIds: topicIds.value,
      ...(noteId.value ? { version: version.value } : {}),
    };
    const saved = await api<Version>(
      noteId.value ? "/notes/" + noteId.value : "/notes",
      { method: noteId.value ? "PUT" : "POST", body: payload },
    );
    noteId.value = saved.id;
    version.value = saved.version;
    status.value = saved.status;
    dirty.value = false;
    if (publish && saved.status === "DRAFT") {
      const submitted = await api<Version>("/notes/" + saved.id + "/submit", {
        method: "POST",
        body: { version: saved.version },
      });
      status.value = submitted.status;
      version.value = submitted.version;
    }
    session.notify(
      status.value === "PENDING" ? "已提交，审核通过后公开展示" : "草稿已保存",
    );
    if (publish || status.value === "PENDING") await router.push("/creator");
  } catch (e) {
    error.value = e instanceof Error ? e.message : "保存失败，请刷新后检查版本";
  } finally {
    busy.value = false;
  }
}
onBeforeRouteLeave(
  () => !dirty.value || confirm("有尚未保存的修改，确定离开？"),
);
onMounted(load);
</script>
<template>
  <RequireLogin
    ><section class="editor">
      <header class="page-heading">
        <div>
          <h1>{{ noteId ? "继续你的记录" : "分享一个生活瞬间" }}</h1>
          <p>记录真实经验，让灵感有迹可循。</p>
        </div>
      </header>
      <ErrorBanner :message="error" />
      <p v-if="status === 'PUBLISHED'" class="hint">
        已发布笔记修改后将重新审核，期间旧内容暂不公开。
      </p>
      <form class="form-stack" @submit.prevent="save(true)">
        <div>
          <h2 class="small-heading">
            图文图片 <span class="muted">{{ mediaIds.length }}/9</span>
          </h2>
          <div class="upload-grid">
            <div v-for="(id, index) in mediaIds" :key="id" class="upload-thumb">
              <img :src="mediaUrl(id)" :alt="'笔记图片' + (index + 1)" /><button
                type="button"
                :aria-label="'移除第' + (index + 1) + '张图片'"
                @click="
                  mediaIds.splice(index, 1);
                  dirty = true;
                "
              >
                ×
              </button>
            </div>
            <label v-if="mediaIds.length < 9" class="upload-tile"
              ><span>＋ 添加图片</span><small>JPG / PNG / WebP</small
              ><input
                type="file"
                multiple
                accept="image/jpeg,image/png,image/webp"
                :disabled="busy"
                @change="upload"
            /></label>
          </div>
        </div>
        <label class="field"
          ><span>标题</span
          ><input
            v-model="title"
            placeholder="给你的记录起个标题"
            @input="dirty = true"
          /><small class="hint">{{ [...title].length }}/40 字</small></label
        ><label class="field"
          ><span>正文</span
          ><textarea
            v-model="body"
            rows="10"
            placeholder="分享你的感受、经验或实用建议…"
            @input="dirty = true"
          /><small class="hint"
            >{{ [...body].length }}/2000 字 · 使用纯文本记录</small
          ></label
        >
        <div class="field">
          <span>选择话题 · {{ topicIds.length }}/5</span>
          <div class="topic-picker">
            <button
              v-for="topic in topics"
              :key="topic.id"
              type="button"
              class="topic-chip"
              :class="{ selected: topicIds.includes(topic.id) }"
              :aria-pressed="topicIds.includes(topic.id)"
              @click="toggleTopic(topic.id)"
            >
              # {{ topic.name }}
            </button>
          </div>
        </div>
        <footer class="sticky-actions">
          <button
            class="button"
            type="button"
            :disabled="busy || status === 'PENDING'"
            @click="save(false)"
          >
            保存草稿</button
          ><button
            class="button primary"
            :disabled="busy || status === 'PENDING'"
          >
            {{ busy ? "处理中…" : "提交审核" }}</button
          ><span class="hint">图片和草稿默认仅本人可见</span>
        </footer>
      </form>
    </section></RequireLogin
  >
</template>
