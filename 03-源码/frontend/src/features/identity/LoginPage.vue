<script setup lang="ts">
import { computed, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useSession } from "../../app/session";
import { api } from "../../api/client";
import ErrorBanner from "../../components/ErrorBanner.vue";
const route = useRoute();
const router = useRouter();
const session = useSession();
const registering = computed(() => route.path === "/register");
const username = ref("");
const password = ref("");
const nickname = ref("");
const busy = ref(false);
const error = ref("");
async function submit() {
  busy.value = true;
  error.value = "";
  try {
    if (registering.value)
      await api("/auth/register", {
        method: "POST",
        body: {
          username: username.value,
          password: password.value,
          nickname: nickname.value,
        },
      });
    await session.login(username.value, password.value);
    const next =
      typeof route.query.next === "string" &&
      route.query.next.startsWith("/") &&
      !route.query.next.startsWith("//")
        ? route.query.next
        : "/";
    await router.replace(next);
  } catch (e) {
    error.value = e instanceof Error ? e.message : "登录失败";
  } finally {
    busy.value = false;
  }
}
</script>
<template>
  <section class="login-layout">
    <div class="login-intro">
      <span class="brand">拾光笔记</span>
      <p>发现生活的小美好，<br />把喜欢，变成一次尝试。</p>
      <p class="hint">图文分享 · 兴趣推荐 · 收藏实践</p>
    </div>
    <form class="login-form form-stack" @submit.prevent="submit">
      <h1>{{ registering ? "创建你的账号" : "欢迎回到拾光" }}</h1>
      <ErrorBanner :message="error" /><label class="field"
        ><span>账号</span
        ><input
          v-model="username"
          name="username"
          autocomplete="username"
          placeholder="4–32位小写字母、数字或下划线"
          pattern="[a-z0-9_]{4,32}"
          required /></label
      ><label v-if="registering" class="field"
        ><span>昵称</span
        ><input
          v-model="nickname"
          name="nickname"
          maxlength="40"
          placeholder="大家怎么称呼你"
          required /></label
      ><label class="field"
        ><span>密码</span
        ><input
          v-model="password"
          type="password"
          name="password"
          :autocomplete="registering ? 'new-password' : 'current-password'"
          minlength="8"
          placeholder="至少8位，不超过72个UTF-8字节"
          required /></label
      ><button class="button primary wide" :disabled="busy">
        {{ busy ? "正在处理…" : registering ? "注册并登录" : "登录" }}
      </button>
      <p class="hint">
        {{ registering ? "已经有账号？" : "第一次来？" }}
        <RouterLink
          :to="{
            path: registering ? '/login' : '/register',
            query: route.query,
          }"
          class="text-button"
          >{{ registering ? "直接登录" : "创建账号" }}</RouterLink
        >
      </p>
      <p class="hint">
        本地演示账号由启动配置生成。请查看运行指南获取账号和本地密码，不使用固定公开密码。
      </p>
    </form>
  </section>
</template>
