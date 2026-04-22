<template>
  <div class="bg-white border border-slate-200 rounded-lg shadow-sm p-8 space-y-5">
    <h1 class="text-xl font-semibold text-slate-800">注册</h1>

    <form class="space-y-4" @submit.prevent="onSubmit">
      <div>
        <label for="reg-username" class="block text-sm text-slate-600 mb-1">用户名 <span class="text-red-500">*</span></label>
        <input
          id="reg-username"
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
        <label for="reg-password" class="block text-sm text-slate-600 mb-1">密码 <span class="text-red-500">*</span></label>
        <input
          id="reg-password"
          v-model="form.password"
          type="password"
          autocomplete="new-password"
          required
          minlength="6"
          maxlength="64"
          class="w-full px-3 py-2 border border-slate-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-brand-500 focus:border-brand-500"
          placeholder="至少 6 位"
        />
      </div>

      <div>
        <label for="reg-nickname" class="block text-sm text-slate-600 mb-1">昵称</label>
        <input
          id="reg-nickname"
          v-model="form.nickname"
          type="text"
          maxlength="64"
          class="w-full px-3 py-2 border border-slate-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-brand-500 focus:border-brand-500"
          placeholder="可选，默认用用户名"
        />
      </div>

      <div>
        <label for="reg-email" class="block text-sm text-slate-600 mb-1">邮箱</label>
        <input
          id="reg-email"
          v-model="form.email"
          type="email"
          maxlength="128"
          class="w-full px-3 py-2 border border-slate-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-brand-500 focus:border-brand-500"
          placeholder="可选"
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
        {{ submitting ? '注册中…' : '注册并开始建档' }}
      </button>
    </form>

    <div class="text-sm text-slate-500 text-center">
      已有账号？
      <RouterLink to="/login" class="text-brand-600 hover:underline">去登录</RouterLink>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { login as apiLogin, register as apiRegister } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()

const form = reactive({
  username: '',
  password: '',
  nickname: '',
  email: ''
})
const submitting = ref(false)
const error = ref('')

async function onSubmit() {
  error.value = ''
  submitting.value = true
  try {
    const username = form.username.trim()
    const payload = {
      username,
      password: form.password,
      nickname: form.nickname.trim() || undefined,
      email: form.email.trim() || undefined
    }
    await apiRegister(payload)
    // 注册成功即自动登录，免一次跳转
    const session = await apiLogin({ username, password: form.password })
    auth.setSession(session.accessToken, session.user)
    router.push('/onboarding')
  } catch (e) {
    error.value = (e as Error).message || '注册失败'
  } finally {
    submitting.value = false
  }
}
</script>
