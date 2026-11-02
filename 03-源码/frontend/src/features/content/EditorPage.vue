<script setup lang="ts">
import { ref, onMounted } from "vue";
import { useRoute, useRouter, onBeforeRouteLeave } from "vue-router";
import { api, mediaUrl, posterUrl, uploadMedia } from "../../api/client";
import type { Note, Topic, Page, Version } from "../../api/types";
import { useSession } from "../../app/session";
import RequireLogin from "../../components/RequireLogin.vue";
import ErrorBanner from "../../components/ErrorBanner.vue";
const route = useRoute();
const router = useRouter();
const session = useSession();
const title = ref("");
const body = ref("");
const mediaKind = ref<"IMAGE" | "VIDEO">("IMAGE");
const progress = ref(0);
const uploading = ref(false);
const limits = ref({ videoMaxBytes: 104857600, videoMaxSeconds: 300 });
function chooseKind(kind: "IMAGE" | "VIDEO") {
  if (kind === mediaKind.value || busy.value) return;
  if (
    mediaIds.value.length &&
    !confirm("切换发布类型将移除当前素材，是否继续？")
  )
    return;
  mediaIds.value = [];
  mediaKind.value = kind;
  dirty.value = true;
}
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
    limits.value = await api("/media/capabilities");
    topics.value = (await api<Page<Topic>>("/topics")).items;
    if (noteId.value) {
      const note = await api<Note>("/me/notes/" + noteId.value);
      title.value = note.title;
      body.value = note.body;
      mediaIds.value = note.mediaIds;
      mediaKind.value = note.mediaEntries?.[0]?.mediaType ?? "IMAGE";
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
  const video = mediaKind.value === "VIDEO";
  if (mediaIds.value.length + files.length > (video ? 1 : 9)) {
    error.value = video ? "每篇上传一段视频" : "最多选择9张图片";
    return;
  }
  busy.value = true;
  uploading.value = true;
  progress.value = 0;
  error.value = "";
  try {
    for (const file of files) {
      if (file.size > (video ? limits.value.videoMaxBytes : 10 * 1024 * 1024))
        throw new Error(video ? "视频超过大小限制" : "单张图片不能超过10MB");
      const form = new FormData();
      form.append("file", file);
      form.append("purpose", "NOTE");
      const result = await uploadMedia<{ id: string }>(
        video ? "/media/videos" : "/media",
        form,
        (value) => (progress.value = value),
      );
      mediaIds.value.push(result.id);
      dirty.value = true;
    }
    dirty.value = true;
  } catch (e) {
    error.value = e instanceof Error ? e.message : "上传失败";
  } finally {
    busy.value = false;
    uploading.value = false;
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
          <div class="publish-modes" role="group" aria-label="发布类型">
            <button
              type="button"
              :class="{ active: mediaKind === 'IMAGE' }"
              :aria-pressed="mediaKind === 'IMAGE'"
              :disabled="busy"
              @click="chooseKind('IMAGE')"
            >
              上传图文
            </button>
            <button
              type="button"
              :class="{ active: mediaKind === 'VIDEO' }"
              :aria-pressed="mediaKind === 'VIDEO'"
              :disabled="busy"
              @click="chooseKind('VIDEO')"
            >
              上传视频
            </button>
          </div>
          <p class="hint">
            {{
              mediaKind === "VIDEO"
                ? `MP4（H.264 / AAC） · 最长 ${Math.floor(limits.videoMaxSeconds / 60)} 分钟 · 最大 ${Math.floor(limits.videoMaxBytes / 1048576)} MB · 自动生成封面`
                : "1–9 张 JPG / PNG / WebP 图片，单张最大 10 MB"
            }}
          </p>
          <div
            v-if="mediaKind === 'VIDEO' && mediaIds[0]"
            class="video-preview"
          >
            <video
              :key="mediaIds[0]"
              :src="mediaUrl(mediaIds[0])"
              :poster="posterUrl(mediaIds[0])"
              controls
              playsinline
              preload="metadata"
            />
            <button
              class="button small"
              type="button"
              :disabled="busy"
              @click="
                mediaIds = [];
                dirty = true;
              "
            >
              移除视频
            </button>
          </div>
          <div :class="mediaKind === 'IMAGE' ? 'upload-grid' : 'video-upload'">
            <template v-if="mediaKind === 'IMAGE'">
              <div
                v-for="(id, index) in mediaIds"
                :key="id"
                class="upload-thumb"
              >
                <img :src="mediaUrl(id)" :alt="'笔记图片' + (index + 1)" />
                <button
                  type="button"
                  :aria-label="'移除第' + (index + 1) + '张图片'"
                  :disabled="busy"
                  @click="
                    mediaIds.splice(index, 1);
                    dirty = true;
                  "
                >
                  ×
                </button>
              </div>
            </template>
            <label
              v-if="mediaIds.length < (mediaKind === 'VIDEO' ? 1 : 9)"
              class="upload-tile"
              :class="{ 'video-tile': mediaKind === 'VIDEO' }"
            >
              <span class="upload-plus">＋</span
              ><strong>{{
                mediaKind === "VIDEO" ? "选择视频" : "添加图片"
              }}</strong>
              <small>{{
                mediaKind === "VIDEO"
                  ? "上传后可预览播放"
                  : "记录生活的每一个角度"
              }}</small>
              <input
                :key="mediaKind"
                type="file"
                :multiple="mediaKind === 'IMAGE'"
                :accept="
                  mediaKind === 'VIDEO'
                    ? 'video/mp4'
                    : 'image/jpeg,image/png,image/webp'
                "
                :disabled="busy"
                @change="upload"
              />
            </label>
          </div>
          <div v-if="uploading" class="upload-progress" role="status">
            <progress
              :value="progress"
              max="100"
              :aria-label="'上传进度 ' + progress + '%'"
            />
            <span>{{
              progress < 100
                ? `正在上传 ${progress}%`
                : "上传完成，正在校验并生成预览…"
            }}</span>
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
          ><span class="hint">素材和草稿默认仅本人可见</span>
        </footer>
      </form>
    </section></RequireLogin
  >
</template>
