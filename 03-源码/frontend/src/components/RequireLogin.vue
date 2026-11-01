<script setup lang="ts">
import { useSession } from '../app/session'
import { useRoute } from 'vue-router'
const session = useSession(); const route = useRoute()
</script>
<template><slot v-if="session.user" /><section v-else class="empty-state"><h1>登录后，继续记录生活</h1><p>你的收藏、实践与创作，只属于你自己。</p><RouterLink class="button primary" :to="{ path: '/login', query: { next: route.fullPath } }">登录 / 注册</RouterLink></section></template>
