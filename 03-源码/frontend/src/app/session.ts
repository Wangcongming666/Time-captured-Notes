import { defineStore } from 'pinia'
import { ref } from 'vue'
import { api, ApiError, clearCsrf } from '../api/client'
import type { User } from '../api/types'
export const useSession = defineStore('session', () => {
  const user = ref<User | null>(null)
  const ready = ref(false)
  const toast = ref('')
  let timer: ReturnType<typeof setTimeout> | undefined
  function notify(message: string) { toast.value = message; clearTimeout(timer); timer = setTimeout(() => { toast.value = '' }, 4500) }
  async function bootstrap() { try { user.value = await api<User>('/me') } catch (error) { if (!(error instanceof ApiError && error.status === 401)) notify(error instanceof Error ? error.message : '连接失败') } finally { ready.value = true } }
  async function login(username: string, password: string) { user.value = await api<User>('/auth/login', { method: 'POST', body: { username, password } }); clearCsrf() }
  async function logout() { await api('/auth/logout', { method: 'POST' }); user.value = null; clearCsrf() }
  function has(role: string) { return user.value?.roles?.includes(role) ?? false }
  return { user, ready, toast, bootstrap, login, logout, notify, has }
})
