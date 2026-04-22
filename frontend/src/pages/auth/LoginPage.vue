<template>
  <div class="bg-white border border-slate-200 rounded-lg shadow-sm p-8 space-y-5">
    <h1 class="text-xl font-semibold text-slate-800">登录</h1>

    <form class="space-y-4" @submit.prevent="onSubmit">
      <div>
        <label for="login-username" class="block text-sm text-slate-600 mb-1">用户名</label>
        <input
          id="login-username"
          v-model="form.username"
          type="text"
          autocomplete="username"
          required
          minlength="3"
          maxlength="64"
          class="w-full px-3 py-2 border border-slate-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-brand-500 focus:border-brand-500"
          placeholder="3–64 字符"
        />
      </div>

      <div>
        <label for="login-password" class="block text-sm text-slate-600 mb-1">密码</label>
        <input
          id="login-password"
          v-model="form.password"
          type="password"
          autocomplete="current-password"
          required
          minlength="6"
          maxlength="64"
          class="w-full px-3 py-2 border border-slate-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-brand-500 focus:border-brand-500"
          placeholder="至少 6 位"
        />
      </div>

      <div
        v-if="error"
        class="text-sm text-red-600 bg-red-50 border border-red-100 rounded px-3 py-2"
      >
        {{ error }}
      </div>

      <button
        type="submit"
        :disabled="submitting"
        class="w-full py-2 rounded-md bg-brand-600 text-white text-sm font-medium hover:bg-brand-700 disabled:opacity-60 disabled:cursor-not-allowed transition-colors"
      >
        {{ submitting ? '登录中…' : '登录' }}
      </button>
    </form>

    <div class="text-sm text-slate-500 text-center">
      还没有账号？
      <RouterLink to="/register" class="text-brand-600 hover:underline">去注册</RouterLink>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { login as apiLogin } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const form = reactive({ username: '', password: '' })
const submitting = ref(false)
const error = ref('')

async function onSubmit() {
  error.value = ''
  submitting.value = true
  try {
    const session = await apiLogin({
      username: form.username.trim(),
      password: form.password
    })
    auth.setSession(session.accessToken, session.user)
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/dashboard'
    router.push(redirect)
  } catch (e) {
    error.value = (e as Error).message || '登录失败'
  } finally {
    submitting.value = false
  }
}
</script>
