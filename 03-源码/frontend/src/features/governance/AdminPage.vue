<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted } from "vue";
import { useRoute } from "vue-router";
import { usePagedList } from "../../app/paging";
import { api, mediaUrl } from "../../api/client";
import type {
  Page,
  Review,
  ReviewDetail,
  Report,
  Topic,
  User,
} from "../../api/types";
import { useSession } from "../../app/session";
import RequireLogin from "../../components/RequireLogin.vue";
import ErrorBanner from "../../components/ErrorBanner.vue";
import DashboardPage from "../statistics/DashboardPage.vue";
const reviewPage = usePagedList<Review>(
  () => "/reviews?size=50&status=" + reviewStatus.value,
);
const reportPage = usePagedList<Report>(() => "/admin/reports?size=50");
const topicPage = usePagedList<Topic>(() => "/admin/topics?size=50");
const userPage = usePagedList<User>(() => "/admin/users?size=50");
const auditPage = usePagedList<Audit>(() => "/admin/audits?size=50");
const activePage = computed(
  () =>
    ({
      reviews: reviewPage,
      reports: reportPage,
      topics: topicPage,
      users: userPage,
      audits: auditPage,
    })[section.value],
);
interface Audit {
  id: string;
  actorId: string;
  action: string;
  targetType: string;
  targetId: string;
  reason: string;
  createdAt: string;
}
const session = useSession();
const route = useRoute();
const section = computed(() => String(route.params.section));
const reviews = reviewPage.items;
const reports = reportPage.items;
const topics = topicPage.items;
const users = userPage.items;
const audits = auditPage.items;
const preview = ref<ReviewDetail | null>(null);
const reviewReason = ref("");
const reason = reactive<Record<string, string>>({});
const roleEdits = reactive<Record<string, string[]>>({});
const error = ref("");
const busy = ref(false);
const reviewStatus = ref("PENDING");
const topicForm = reactive({ name: "", category: "", status: "ACTIVE" });
const editingTopic = ref<string | null>(null);
const tabs = computed(() =>
  [
    { key: "reviews", name: "内容审核", allowed: session.has("REVIEWER") },
    { key: "reports", name: "举报工单", allowed: session.has("REVIEWER") },
    { key: "users", name: "账号与角色", allowed: session.has("ADMIN") },
    { key: "topics", name: "话题配置", allowed: session.has("ADMIN") },
    {
      key: "audits",
      name: "操作审计",
      allowed: session.has("ADMIN") || session.has("REVIEWER"),
    },
    { key: "statistics", name: "平台统计", allowed: session.has("ADMIN") },
  ].filter((t) => t.allowed),
);
const allowed = computed(() => tabs.value.some((t) => t.key === section.value));
const title = computed(
  () => tabs.value.find((t) => t.key === section.value)?.name ?? "管理后台",
);
async function load(reset = true) {
  if (!session.user || !allowed.value || section.value === "statistics") return;
  busy.value = true;
  error.value = "";
  preview.value = null;
  try {
    await activePage.value?.load(reset);
    if (section.value === "users")
      for (const u of users.value) roleEdits[u.id] = [...(u.roles ?? ["USER"])];
  } catch (e) {
    error.value = e instanceof Error ? e.message : "加载失败";
  } finally {
    busy.value = false;
  }
}

async function openReview(review: Review) {
  error.value = "";
  try {
    preview.value = await api<ReviewDetail>("/reviews/" + review.id);
    reviewReason.value = "";
  } catch (e) {
    error.value = e instanceof Error ? e.message : "预览失败";
  }
}
async function decide(decision: string) {
  if (!preview.value) return;
  busy.value = true;
  error.value = "";
  try {
    await api("/reviews/" + preview.value.review.id + "/decision", {
      method: "POST",
      body: {
        version: preview.value.review.version,
        decision,
        reason: reviewReason.value,
      },
    });
    session.notify(decision === "APPROVE" ? "审核已通过" : "已驳回并通知作者");
    await load();
  } catch (e) {
    error.value = e instanceof Error ? e.message : "处理失败";
  } finally {
    busy.value = false;
  }
}
async function decideReport(report: Report, status: string) {
  busy.value = true;
  try {
    await api("/admin/reports/" + report.id + "/decision", {
      method: "POST",
      body: {
        version: report.version,
        status,
        reason:
          reason[report.id] || (status === "PROCESSING" ? "开始受理调查" : ""),
      },
    });
    await load();
  } catch (e) {
    error.value = e instanceof Error ? e.message : "处理失败";
  } finally {
    busy.value = false;
  }
}
async function account(user: User, kind: "status" | "roles") {
  if (!reason[user.id]?.trim()) {
    error.value = "请先填写该账号的操作理由";
    return;
  }
  if (
    !confirm(
      "确认修改「" +
        user.nickname +
        "」的" +
        (kind === "status" ? "账号状态" : "权限角色") +
        "？现有会话将失效。",
    )
  )
    return;
  busy.value = true;
  try {
    await api("/admin/users/" + user.id + "/" + kind, {
      method: "PUT",
      body:
        kind === "status"
          ? {
              status: user.status === "ACTIVE" ? "BANNED" : "ACTIVE",
              reason: reason[user.id],
            }
          : { roles: roleEdits[user.id], reason: reason[user.id] },
    });
    if (user.id === session.user?.id) await session.bootstrap();
    await load();
  } catch (e) {
    error.value = e instanceof Error ? e.message : "更新失败";
  } finally {
    busy.value = false;
  }
}
function setRoles(event: Event, user: User) {
  roleEdits[user.id] = Array.from(
    (event.target as HTMLSelectElement).selectedOptions,
  ).map((o) => o.value);
}
function editTopic(topic: Topic) {
  editingTopic.value = topic.id;
  topicForm.name = topic.name;
  topicForm.category = topic.category;
  topicForm.status = topic.status;
}
async function saveTopic() {
  busy.value = true;
  try {
    await api(
      editingTopic.value
        ? "/admin/topics/" + editingTopic.value
        : "/admin/topics",
      { method: editingTopic.value ? "PUT" : "POST", body: { ...topicForm } },
    );
    editingTopic.value = null;
    topicForm.name = "";
    topicForm.category = "";
    topicForm.status = "ACTIVE";
    await load();
  } catch (e) {
    error.value = e instanceof Error ? e.message : "保存失败";
  } finally {
    busy.value = false;
  }
}
watch([section, reviewStatus], () => load());
onMounted(load);
</script>
<template>
  <RequireLogin
    ><section class="admin-layout">
      <header class="page-heading">
        <div>
          <h1>{{ title }}</h1>
          <p>每一次治理操作都有记录；管理员与审核员权限独立。</p>
        </div>
      </header>
      <nav class="tabs">
        <RouterLink
          v-for="tab in tabs"
          :key="tab.key"
          :to="'/admin/' + tab.key"
          :class="{ active: section === tab.key }"
          >{{ tab.name }}</RouterLink
        >
      </nav>
      <div v-if="!allowed" class="empty-state">
        <h2>没有访问此管理页面的权限</h2>
        <p>如需审核权限，请由管理员单独授予REVIEWER角色。</p>
      </div>
      <template v-else
        ><ErrorBanner :message="error" :retry="load" /><DashboardPage
          v-if="section === 'statistics'"
        /><template v-else-if="section === 'reviews'"
          ><label class="field narrow"
            ><span>任务状态</span
            ><select v-model="reviewStatus">
              <option value="PENDING">待审核</option>
              <option value="APPROVED">已通过</option>
              <option value="REJECTED">已驳回</option>
              <option value="CANCELLED">已取消</option>
            </select></label
          >
          <article v-for="review in reviews" :key="review.id" class="list-row">
            <div>
              <h3>
                {{ review.targetType === "NOTE" ? "笔记" : "公开实践反馈" }} #{{
                  review.targetId
                }}
              </h3>
              <p>
                内容版本 {{ review.targetRevision }} · 申请人
                {{ review.applicantId }}
                <span v-if="review.applicantId === session.user?.id"
                  >· 不允许自审</span
                >
              </p>
            </div>
            <div class="row-actions">
              <span class="status" :class="review.status">{{
                review.status
              }}</span
              ><button
                v-if="review.status === 'PENDING'"
                class="button small"
                :disabled="review.applicantId === session.user?.id || busy"
                @click="openReview(review)"
              >
                查看并审核
              </button>
            </div>
          </article>
          <p v-if="!reviews.length && !busy" class="empty-state">
            当前没有此状态的审核任务
          </p>
          <section v-if="preview" class="review-preview">
            <h2>{{ preview.note?.title ?? "公开实践反馈" }}</h2>
            <p class="note-body">
              {{ preview.note?.body ?? preview.practice?.resultText }}
            </p>
            <div class="review-images">
              <img
                v-for="id in preview.note?.mediaIds ??
                preview.practice?.mediaIds ??
                []"
                :key="id"
                :src="mediaUrl(id)"
                alt="审核内容图片"
              />
            </div>
            <label class="field"
              ><span>审核理由（驳回必填）</span
              ><textarea v-model="reviewReason" maxlength="500" rows="3" />
            </label>
            <div class="action-group section">
              <button
                class="button primary"
                :disabled="busy"
                @click="decide('APPROVE')"
              >
                审核通过</button
              ><button
                class="button danger"
                :disabled="busy || !reviewReason.trim()"
                @click="decide('REJECT')"
              >
                驳回并通知作者</button
              ><button class="text-button" @click="preview = null">
                关闭预览
              </button>
            </div>
          </section></template
        ><template v-else-if="section === 'reports'"
          ><article v-for="report in reports" :key="report.id" class="section">
            <div class="list-row">
              <div>
                <h3>{{ report.targetType }} #{{ report.targetId }}</h3>
                <p>{{ report.reason }}</p>
              </div>
              <span class="status" :class="report.status">{{
                report.status
              }}</span>
            </div>
            <label
              v-if="['PENDING', 'PROCESSING'].includes(report.status)"
              class="field"
              ><span>处理理由</span
              ><input
                v-model="reason[report.id]"
                maxlength="500"
                placeholder="记录核查情况与结论"
            /></label>
            <div class="action-group section">
              <button
                v-if="report.status === 'PENDING'"
                class="button"
                :disabled="busy"
                @click="decideReport(report, 'PROCESSING')"
              >
                受理调查</button
              ><template v-if="report.status === 'PROCESSING'"
                ><button
                  class="button primary"
                  :disabled="busy || !reason[report.id]?.trim()"
                  @click="decideReport(report, 'UPHELD')"
                >
                  举报成立并治理内容</button
                ><button
                  class="button"
                  :disabled="busy || !reason[report.id]?.trim()"
                  @click="decideReport(report, 'REJECTED')"
                >
                  驳回举报
                </button></template
              >
              <p v-if="report.resultReason" class="hint">
                {{ report.resultReason }}
              </p>
            </div>
          </article>
          <p v-if="!reports.length" class="empty-state">
            暂无举报工单
          </p></template
        ><template v-else-if="section === 'users'"
          ><article v-for="user in users" :key="user.id" class="section">
            <div class="list-row">
              <div>
                <h3>
                  {{ user.nickname }}
                  <span class="hint">@{{ user.username }}</span>
                </h3>
                <span class="status" :class="user.status">{{
                  user.status === "ACTIVE" ? "正常" : "已封禁"
                }}</span>
              </div>
              <button
                class="button small"
                :disabled="busy"
                @click="account(user, 'status')"
              >
                {{ user.status === "ACTIVE" ? "封禁" : "解封" }}
              </button>
            </div>
            <div class="inline-form">
              <label class="field"
                ><span>角色（可多选）</span
                ><select
                  multiple
                  :value="roleEdits[user.id]"
                  :aria-label="user.nickname + '的角色'"
                  @change="setRoles($event, user)"
                >
                  <option value="USER">普通用户</option>
                  <option value="REVIEWER">内容审核员</option>
                  <option value="ADMIN">系统管理员</option>
                </select></label
              ><label class="field spacer"
                ><span>操作理由</span
                ><input
                  v-model="reason[user.id]"
                  maxlength="500"
                  placeholder="必填，将保留在审计记录中" /></label
              ><button
                class="button small"
                :disabled="busy"
                @click="account(user, 'roles')"
              >
                保存角色
              </button>
            </div>
          </article></template
        ><template v-else-if="section === 'topics'"
          ><form class="inline-form section" @submit.prevent="saveTopic">
            <label class="field"
              ><span>话题名称</span
              ><input v-model="topicForm.name" maxlength="40" required /></label
            ><label class="field"
              ><span>分类</span
              ><input
                v-model="topicForm.category"
                maxlength="40"
                required /></label
            ><label class="field"
              ><span>状态</span
              ><select v-model="topicForm.status">
                <option value="ACTIVE">启用</option>
                <option value="DISABLED">停用</option>
              </select></label
            ><button class="button primary" :disabled="busy">
              {{ editingTopic ? "保存话题" : "新增话题" }}</button
            ><button
              v-if="editingTopic"
              type="button"
              class="text-button"
              @click="editingTopic = null"
            >
              取消编辑
            </button>
          </form>
          <article v-for="topic in topics" :key="topic.id" class="list-row">
            <div>
              <h3># {{ topic.name }}</h3>
              <p>
                {{ topic.category }} ·
                {{ topic.status === "ACTIVE" ? "启用" : "已停用" }}
              </p>
            </div>
            <button class="text-button" @click="editTopic(topic)">编辑</button>
          </article></template
        >
        <div v-else-if="section === 'audits'" class="table-scroll">
          <table class="data-table">
            <thead>
              <tr>
                <th>时间</th>
                <th>操作者</th>
                <th>操作</th>
                <th>目标</th>
                <th>理由</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in audits" :key="item.id">
                <td>{{ new Date(item.createdAt).toLocaleString("zh-CN") }}</td>
                <td>{{ item.actorId }}</td>
                <td>{{ item.action }}</td>
                <td>{{ item.targetType }} #{{ item.targetId }}</td>
                <td>{{ item.reason }}</td>
              </tr>
            </tbody>
          </table>
        </div>
        <div v-if="activePage?.hasMore.value" class="load-more">
          <button class="button" :disabled="busy" @click="load(false)">
            加载更多记录
          </button>
        </div></template
      >
    </section></RequireLogin
  >
</template>
