<script setup lang="ts">
import { ref, onMounted } from "vue";
import { useRoute } from "vue-router";
import { api, mediaUrl } from "../../api/client";
import type { Practice, History, Page } from "../../api/types";
import { useSession } from "../../app/session";
import RequireLogin from "../../components/RequireLogin.vue";
import ErrorBanner from "../../components/ErrorBanner.vue";
const route = useRoute();
const session = useSession();
const practice = ref<Practice | null>(null);
const history = ref<History[]>([]);
const result = ref("");
const helpfulness = ref("HELPFUL");
const images = ref<string[]>([]);
const busy = ref(false);
const error = ref("");
const dirty = ref(false);
const labels: Record<string, string> = {
  PLANNED: "想尝试",
  DOING: "进行中",
  DONE: "已完成",
  ABANDONED: "已放弃",
  PRIVATE: "仅自己可见",
  PENDING: "公开申请审核中",
  PUBLISHED: "已审核公开",
  REJECTED: "公开申请被驳回",
  OFFLINE: "公开反馈已下架",
};
async function load() {
  if (!session.user) return;
  try {
    practice.value = await api<Practice>("/practices/" + route.params.id);
    result.value = practice.value.resultText ?? "";
    helpfulness.value = practice.value.helpfulness ?? "HELPFUL";
    images.value = practice.value.mediaIds;
    history.value = (
      await api<Page<History>>("/practices/" + route.params.id + "/history")
    ).items;
    dirty.value = false;
  } catch (e) {
    error.value = e instanceof Error ? e.message : "加载失败";
  }
}
async function transition(status: string) {
  if (!practice.value) return;
  busy.value = true;
  error.value = "";
  try {
    await api("/practices/" + practice.value.id + "/transition", {
      method: "POST",
      body: { version: practice.value.version, status },
    });
    await load();
  } catch (e) {
    error.value = e instanceof Error ? e.message : "操作失败";
  } finally {
    busy.value = false;
  }
}
async function save() {
  if (!practice.value) return;
  busy.value = true;
  error.value = "";
  try {
    await api("/practices/" + practice.value.id + "/result", {
      method: "PUT",
      body: {
        version: practice.value.version,
        resultText: result.value,
        helpfulness: helpfulness.value,
        mediaIds: images.value,
      },
    });
    await load();
    session.notify("私人实践结果已保存");
  } catch (e) {
    error.value = e instanceof Error ? e.message : "保存失败";
  } finally {
    busy.value = false;
  }
}
async function visibility(publish: boolean) {
  if (!practice.value) return;
  if (dirty.value) {
    error.value = "请先保存修改后的结果，再申请公开";
    return;
  }
  busy.value = true;
  error.value = "";
  try {
    await api(
      "/practices/" +
        practice.value.id +
        (publish ? "/publish-request" : "/unpublish"),
      { method: "POST", body: { version: practice.value.version } },
    );
    await load();
    session.notify(publish ? "公开申请已提交审核" : "已撤回为仅自己可见");
  } catch (e) {
    error.value = e instanceof Error ? e.message : "操作失败";
  } finally {
    busy.value = false;
  }
}
async function upload(event: Event) {
  const files = Array.from((event.target as HTMLInputElement).files ?? []);
  if (images.value.length + files.length > 9) {
    error.value = "最多9张图片";
    return;
  }
  busy.value = true;
  try {
    for (const file of files) {
      const form = new FormData();
      form.append("file", file);
      form.append("purpose", "PRACTICE");
      images.value.push(
        (await api<{ id: string }>("/media", { method: "POST", body: form }))
          .id,
      );
    }
    dirty.value = true;
  } catch (e) {
    error.value = e instanceof Error ? e.message : "上传失败";
  } finally {
    busy.value = false;
  }
}
onMounted(load);
</script>
<template>
  <RequireLogin
    ><section class="practice-detail">
      <header class="page-heading">
        <div>
          <h1>一次属于你的实践</h1>
          <p>从“想尝试”，到亲身体验过。</p>
        </div>
        <RouterLink to="/collections" class="text-button">返回清单</RouterLink>
      </header>
      <ErrorBanner :message="error" :retry="load" /><template v-if="practice"
        ><div class="action-group">
          <span class="status" :class="practice.status">{{
            labels[practice.status]
          }}</span
          ><span class="status">{{ labels[practice.publicStatus] }}</span
          ><RouterLink
            v-if="practice.sourceAvailable"
            :to="'/notes/' + practice.noteId"
            class="text-button"
            >查看原笔记 →</RouterLink
          ><span v-else class="hint">原笔记已不可见，私人实践记录仍保留</span>
        </div>
        <section class="section">
          <h2>实践进度</h2>
          <div class="timeline">
            <div v-for="item in [...history].reverse()" :key="item.id">
              <time>{{ new Date(item.createdAt).toLocaleString("zh-CN") }}</time
              ><span>{{ labels[item.toStatus] }}</span>
            </div>
          </div>
          <div class="action-group">
            <button
              v-if="practice.status === 'PLANNED'"
              class="button primary"
              :disabled="busy"
              @click="transition('DOING')"
            >
              开始尝试</button
            ><button
              v-if="practice.status === 'DOING'"
              class="button primary"
              :disabled="busy"
              @click="transition('DONE')"
            >
              我已完成</button
            ><button
              v-if="['PLANNED', 'DOING'].includes(practice.status)"
              class="button"
              :disabled="busy"
              @click="transition('ABANDONED')"
            >
              放弃本次计划
            </button>
          </div>
        </section>
        <form
          v-if="practice.status === 'DONE'"
          class="form-stack section"
          @submit.prevent="save"
        >
          <h2>亲身尝试后的感受</h2>
          <label class="field"
            ><span>实践结果</span
            ><textarea
              v-model="result"
              rows="8"
              placeholder="哪些地方有帮助？实际尝试有什么不同？"
              required
              @input="dirty = true"
            /></label
          ><label class="field"
            ><span>对我的帮助程度</span
            ><select v-model="helpfulness" @change="dirty = true">
              <option value="HELPFUL">有帮助</option>
              <option value="NEUTRAL">一般</option>
              <option value="NOT_APPLICABLE">不适用</option>
            </select></label
          >
          <div class="upload-grid">
            <div v-for="(id, index) in images" :key="id" class="upload-thumb">
              <img :src="mediaUrl(id)" alt="私人实践图片" /><button
                type="button"
                aria-label="移除图片"
                @click="
                  images.splice(index, 1);
                  dirty = true;
                "
              >
                ×
              </button>
            </div>
            <label v-if="images.length < 9" class="upload-tile"
              ><span>＋ 实践图片</span
              ><input
                type="file"
                multiple
                accept="image/jpeg,image/png,image/webp"
                :disabled="busy"
                @change="upload"
            /></label>
          </div>
          <div>
            <button class="button primary" :disabled="busy">
              保存私人结果
            </button>
          </div>
          <p class="hint">
            修改结果会撤回原有公开状态，保存后须重新申请审核。结果默认仅本人可见。
          </p>
        </form>
        <section v-if="practice.status === 'DONE'" class="section">
          <h2>是否愿意分享这次体验？</h2>
          <p class="hint">
            公开申请通过后，反馈会出现在原笔记下；原笔记下架或修改后，反馈停止公开。你可以随时撤回。
          </p>
          <button
            v-if="
              ['PRIVATE', 'REJECTED', 'OFFLINE'].includes(practice.publicStatus)
            "
            class="button"
            :disabled="
              busy || !practice.resultText || !practice.sourceAvailable
            "
            @click="visibility(true)"
          >
            申请公开反馈</button
          ><button
            v-else
            class="button"
            :disabled="busy"
            @click="visibility(false)"
          >
            撤回公开
          </button>
        </section></template
      >
    </section></RequireLogin
  >
</template>
