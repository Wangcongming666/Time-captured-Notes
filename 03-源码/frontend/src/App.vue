<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useSession } from './app/session'
import AppIcon from './components/AppIcon.vue'
const session = useSession(); const router = useRouter(); const route = useRoute(); const search = ref('')
const nav = [{ path: '/', name: '发现', icon: 'discover' }, { path: '/following', name: '关注', icon: 'follow' }, { path: '/editor', name: '发布笔记', icon: 'create' }, { path: '/collections', name: '收藏与实践', icon: 'collection' }, { path: '/notifications', name: '通知', icon: 'bell' }]
function submitSearch() { router.push({ path: '/search', query: { q: search.value } }) }
async function logout() { try { await session.logout(); router.push('/') } catch (e) { session.notify(e instanceof Error ? e.message : '退出失败') } }
onMounted(() => { void session.bootstrap() })
</script>
<template>
  <div class="app-layout">
    <aside class="sidebar">
      <RouterLink to="/" class="brand" aria-label="拾光笔记首页">拾光<span>笔记</span></RouterLink>
      <nav aria-label="主导航"><RouterLink v-for="item in nav" :key="item.path" :to="item.path" class="nav-link" :class="{ active: route.path === item.path }"><AppIcon :name="item.icon" /><span>{{ item.name }}</span></RouterLink></nav>
      <div class="sidebar-bottom"><RouterLink v-if="session.user" to="/dashboard" class="nav-link"><AppIcon name="chart" /><span>创作者中心</span></RouterLink><RouterLink v-if="session.has('REVIEWER')" to="/admin/reviews" class="nav-link"><AppIcon name="check" /><span>内容治理</span></RouterLink><RouterLink v-if="session.has('ADMIN')" to="/admin/users" class="nav-link"><AppIcon name="settings" /><span>系统管理</span></RouterLink><RouterLink v-if="session.user" to="/me" class="sidebar-profile"><span class="avatar">{{ session.user.nickname.slice(0, 1) }}</span><span>{{ session.user.nickname }}</span></RouterLink><RouterLink v-else to="/login" class="button primary wide">登录，开始分享</RouterLink><p class="sidebar-caption">记录生活，实践热爱</p></div>
    </aside>
    <div class="main-shell"><header class="topbar"><form class="search-field" role="search" @submit.prevent="submitSearch"><AppIcon name="search" :size="20" /><input v-model="search" aria-label="搜索笔记、话题或作者" placeholder="搜索笔记、话题或作者"><button type="submit">搜索</button></form><div class="top-actions"><RouterLink to="/creator" class="text-button">我的创作</RouterLink><button v-if="session.user" class="text-button" @click="logout">退出</button><RouterLink v-else class="text-button" to="/login">登录</RouterLink></div></header><main id="main-content" class="main-content"><RouterView v-if="session.ready" /><div v-else class="loading-state" role="status">正在连接拾光笔记…</div></main></div>
    <nav class="mobile-nav" aria-label="移动导航"><RouterLink v-for="item in nav" :key="item.path" :to="item.path" :class="{ active: route.path === item.path }"><AppIcon :name="item.icon" /><span>{{ item.name === '收藏与实践' ? '收藏' : item.name }}</span></RouterLink></nav>
    <div v-if="session.toast" class="toast" role="status">{{ session.toast }}</div>
  </div>
</template>
