<template>
  <div class="min-h-screen flex bg-slate-50">
    <aside class="w-56 shrink-0 border-r border-slate-200 bg-white flex flex-col">
      <div class="px-4 py-5 border-b border-slate-200">
        <div class="text-lg font-semibold text-slate-800">GrowthTrace</div>
        <div class="text-xs text-slate-500 mt-0.5">成长跟踪 · 阶段诊断</div>
      </div>

      <nav class="flex-1 overflow-y-auto py-2 px-2 space-y-0.5">
        <RouterLink
          v-for="item in navItems"
          :key="item.to"
          :to="item.to"
          class="block px-3 py-2 rounded-md text-sm text-slate-600 hover:bg-slate-100 transition-colors"
          active-class="bg-brand-50 text-brand-700 font-medium"
        >
          {{ item.label }}
        </RouterLink>
      </nav>

      <div class="border-t border-slate-200 p-3 flex items-center justify-between text-xs">
        <div class="truncate">
          <span class="text-slate-500">当前：</span>
          <span class="text-slate-700">{{ auth.user?.nickname || auth.user?.username || '未登录' }}</span>
        </div>
        <button class="text-brand-600 hover:underline" @click="logout">退出</button>
      </div>
    </aside>

    <main class="flex-1 overflow-auto">
      <RouterView />
    </main>
  </div>
</template>

<script setup lang="ts">
import { RouterView, RouterLink, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()

const navItems: { to: string; label: string }[] = [
  { to: '/dashboard',         label: '成长总览' },
  { to: '/profile',           label: '成长档案' },
  { to: '/target',            label: '目标设定' },
  { to: '/journal',           label: '成长随记' },
  { to: '/execution',         label: '成长执行' },
  { to: '/diagnosis',         label: '阶段诊断' },
  { to: '/diagnosis/history', label: '诊断历史' }
]

function logout() {
  auth.clear()
  router.push({ name: 'Login' })
}
</script>
