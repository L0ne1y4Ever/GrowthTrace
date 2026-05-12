<template>
  <div class="h-screen flex overflow-hidden bg-transparent">
    <aside class="sticky top-0 h-screen w-64 shrink-0 border-r border-slate-200/80 bg-white/85 backdrop-blur-xl flex flex-col">
      <div class="px-5 py-6 border-b border-slate-200/80">
        <div class="text-xs uppercase tracking-[0.35em] text-slate-400">GrowthTrace</div>
        <div class="text-xl font-semibold mt-1 text-slate-950">成长跟踪</div>
        <div class="text-xs text-slate-500 mt-2">目标 · 执行 · 随记 · 诊断</div>
      </div>

      <nav class="flex-1 overflow-y-auto py-3 px-3 space-y-1">
        <RouterLink
          v-for="item in navItems"
          :key="item.to"
          :to="item.to"
          class="block px-3 py-2.5 rounded-2xl text-sm text-slate-600 hover:bg-slate-100 hover:text-slate-950 transition-colors"
          active-class="bg-slate-950 text-white font-medium shadow-sm"
        >
          {{ item.label }}
        </RouterLink>
      </nav>

      <div class="border-t border-slate-200/80 p-3 flex items-center justify-between text-xs">
        <div class="truncate">
          <span class="text-slate-400">当前：</span>
          <span class="text-slate-700">{{ auth.user?.nickname || auth.user?.username || '未登录' }}</span>
        </div>
        <button class="text-slate-500 hover:text-slate-950" @click="logout">退出</button>
      </div>
    </aside>

    <main class="app-main h-screen flex-1 overflow-y-auto">
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
