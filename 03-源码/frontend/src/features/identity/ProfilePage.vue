<script setup lang="ts">
import { ref, computed, onMounted, watch } from "vue";
import { useRoute } from "vue-router";
import { useSession } from "../../app/session";
import { usePagedList } from "../../app/paging";
import { api, mediaUrl } from "../../api/client";
import type { User, Page, Card } from "../../api/types";
import NoteCard from "../../components/NoteCard.vue";
import ErrorBanner from "../../components/ErrorBanner.vue";
import RequireLogin from "../../components/RequireLogin.vue";
const notePage = usePagedList<Card>(
  () => "/search?authorId=" + profile.value?.id,
);
const followingPage = usePagedList<User>(
  () => "/users/" + profile.value?.id + "/following",
);
const followerPage = usePagedList<User>(
  () => "/users/" + profile.value?.id + "/followers",
);
async function more(load: () => Promise<void>) {
  try {
    await load();
  } catch (e) {
    error.value = e instanceof Error ? e.message : "加载失败";
  }
}
const route = useRoute();
const session = useSession();
const own = computed(() => route.path === "/me");
const profile = ref<User | null>(null);
const cards = notePage.items;
const error = ref("");
const summary = ref({
  followingCount: 0,
  followerCount: 0,
  noteCount: 0,
  appreciationCount: 0,
  following: false,
});
async function toggleFollow() {
  if (!session.user) {
    location.assign("/login?next=" + encodeURIComponent(route.fullPath));
    return;
  }
  if (!profile.value || busy.value) return;
  busy.value = true;
  try {
    await api("/users/" + profile.value.id + "/follow", {
      method: summary.value.following ? "DELETE" : "PUT",
    });
    summary.value = await api("/users/" + profile.value.id + "/summary");
    await followerPage.load(true);
  } catch (e) {
    error.value = e instanceof Error ? e.message : "关注失败";
  } finally {
    busy.value = false;
  }
}
const busy = ref(false);
const editing = ref(false);
const bio = ref("");
const nickname = ref("");
const following = followingPage.items;
const followers = followerPage.items;
async function load() {
  if (own.value && !session.user) return;
  error.value = "";
  try {
    profile.value = await api<User>(
      own.value ? "/me" : "/users/" + route.params.id,
    );
    summary.value = await api("/users/" + profile.value.id + "/summary");
    nickname.value = profile.value.nickname;
    bio.value = profile.value.bio;
    await Promise.all([
      notePage.load(true),
      followingPage.load(true),
      followerPage.load(true),
    ]);
  } catch (e) {
    error.value = e instanceof Error ? e.message : "加载失败";
  }
}
async function save() {
  busy.value = true;
  try {
    profile.value = await api<User>("/me/profile", {
      method: "PUT",
      body: {
        nickname: nickname.value,
        bio: bio.value,
        avatarMediaId: profile.value?.avatarMediaId ?? null,
      },
    });
    session.user = profile.value;
    editing.value = false;
    session.notify("资料已更新");
  } catch (e) {
    error.value = e instanceof Error ? e.message : "保存失败";
  } finally {
    busy.value = false;
  }
}
async function avatar(event: Event) {
  const file = (event.target as HTMLInputElement).files?.[0];
  if (!file || !profile.value) return;
  const body = new FormData();
  body.append("file", file);
  body.append("purpose", "AVATAR");
  try {
    const uploaded = await api<{ id: string }>("/media", {
      method: "POST",
      body,
    });
    profile.value.avatarMediaId = uploaded.id;
    await save();
  } catch (e) {
    error.value = e instanceof Error ? e.message : "上传失败";
  }
}
watch(() => route.fullPath, load);
onMounted(load);
</script>
<template>
  <RequireLogin v-if="own && !session.user" />
  <div v-else>
    <ErrorBanner :message="error" :retry="load" /><template v-if="profile"
      ><header class="profile-header">
        <img
          v-if="profile.avatarMediaId"
          class="avatar large"
          :src="mediaUrl(profile.avatarMediaId)"
          alt="头像"
        /><span v-else class="avatar large">{{
          profile.nickname.slice(0, 1)
        }}</span>
        <div>
          <h1>{{ profile.nickname }}</h1>
          <p class="profile-id">拾光号：{{ profile.id }}</p>
          <p class="profile-bio">
            {{ profile.bio || "还没有简介，生活本身就是最好的介绍。" }}
          </p>
          <div class="profile-stats">
            <span
              ><strong>{{ summary.followingCount }}</strong> 关注</span
            >
            <span
              ><strong>{{ summary.followerCount }}</strong> 粉丝</span
            >
            <span
              ><strong>{{ summary.appreciationCount }}</strong> 获赞与收藏</span
            >
          </div>
        </div>
        <button v-if="own" class="button" @click="editing = !editing">
          编辑资料
        </button>
        <button
          v-if="!own && session.user?.id !== profile.id"
          class="button follow-button"
          :disabled="busy"
          @click="toggleFollow"
        >
          {{ summary.following ? "已关注" : "关注" }}
        </button>
      </header>
      <form
        v-if="editing && own"
        class="form-stack narrow section"
        @submit.prevent="save"
      >
        <label class="field"
          ><span>昵称</span
          ><input v-model="nickname" maxlength="40" required /></label
        ><label class="field"
          ><span>简介</span><textarea v-model="bio" maxlength="300" /></label
        ><label class="field"
          ><span>更新头像</span
          ><input
            type="file"
            accept="image/jpeg,image/png,image/webp"
            @change="avatar" /></label
        ><button class="button primary" :disabled="busy">保存资料</button>
      </form>
      <nav class="tabs profile-tabs">
        <span class="active">笔记 {{ summary.noteCount }}</span
        ><RouterLink v-if="own" to="/creator">我的草稿与发布状态</RouterLink
        ><RouterLink v-if="own" to="/collections">收藏 · 仅自己可见</RouterLink
        ><RouterLink v-if="own" to="/preferences">兴趣设置</RouterLink>
      </nav>
      <div v-if="cards.length" class="waterfall">
        <NoteCard v-for="note in cards" :key="note.id" :note="note" />
      </div>
      <div v-else class="empty-state">
        <h2>还没有公开笔记</h2>
        <p>每段值得分享的生活，都可以从这里开始。</p>
      </div>
      <button
        v-if="notePage.hasMore.value"
        class="button"
        :disabled="notePage.busy.value"
        @click="more(notePage.load)"
      >
        更多公开笔记
      </button>
      <details class="section">
        <summary>关注与粉丝</summary>
        <h3 class="small-heading">正在关注</h3>
        <div class="action-group">
          <RouterLink
            v-for="person in following"
            :key="person.id"
            :to="'/users/' + person.id"
            class="button small"
            >{{ person.nickname }}</RouterLink
          >
        </div>
        <button
          v-if="followingPage.hasMore.value"
          class="text-button"
          :disabled="followingPage.busy.value"
          @click="more(followingPage.load)"
        >
          更多关注
        </button>
        <h3 class="small-heading">粉丝</h3>
        <div class="action-group">
          <RouterLink
            v-for="person in followers"
            :key="person.id"
            :to="'/users/' + person.id"
            class="button small"
            >{{ person.nickname }}</RouterLink
          >
        </div>
        <button
          v-if="followerPage.hasMore.value"
          class="text-button"
          :disabled="followerPage.busy.value"
          @click="more(followerPage.load)"
        >
          更多粉丝
        </button>
      </details></template
    >
  </div>
</template>
