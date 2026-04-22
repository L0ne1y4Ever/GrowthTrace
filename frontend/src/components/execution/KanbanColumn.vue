<template>
  <div
    class="rounded-lg border p-4 space-y-3"
    :class="accentWrapperClass"
  >
    <div class="flex items-center justify-between">
      <div class="text-sm font-medium text-slate-700">{{ title }}</div>
      <span class="text-xs text-slate-400">{{ tasks.length }}</span>
    </div>

    <div v-if="tasks.length === 0" class="text-xs text-slate-400">（空）</div>

    <div
      v-for="t in tasks"
      :key="t.id"
      class="bg-white border border-slate-200 rounded-md p-3 space-y-2 hover:border-brand-300 transition-colors"
    >
      <div class="flex items-start justify-between gap-2">
        <button
          type="button"
          class="text-sm text-left font-medium text-slate-800 hover:text-brand-600 truncate flex-1 min-w-0"
          @click="openDetail(t)"
        >
          {{ t.title }}
        </button>
        <span class="text-xs px-1.5 py-0.5 rounded shrink-0" :class="priorityBadge(t.priority)">
          {{ t.priority }}
        </span>
      </div>

      <div class="flex items-center gap-2 flex-wrap text-xs text-slate-500">
        <span v-if="t.dueDate" :class="dueDateClass(t.dueDate)">
          due {{ t.dueDate }}
        </span>
        <span v-if="t.checkInCount > 0">打卡 {{ t.checkInCount }} 次</span>
        <span v-if="t.actualEffortMinutes > 0">投入 {{ t.actualEffortMinutes }} 分钟</span>
      </div>

      <div v-if="t.description" class="text-xs text-slate-600 line-clamp-2">
        {{ t.description }}
      </div>

      <div class="flex items-center gap-2 pt-1 border-t border-slate-100">
        <button
          v-if="t.status !== 'DONE'"
          type="button"
          :disabled="t.checkedInToday"
          class="text-xs px-2 py-1 rounded border text-brand-700 border-brand-200 hover:bg-brand-50 disabled:opacity-50 disabled:cursor-not-allowed"
          @click="checkIn(t)"
        >
          {{ t.checkedInToday ? '今日已打卡' : '打卡' }}
        </button>

        <select
          :value="t.status"
          class="text-xs px-2 py-1 border border-slate-200 rounded-md bg-white focus:outline-none focus:ring-2 focus:ring-brand-500"
          @change="(e) => changeStatus(t, (e.target as HTMLSelectElement).value as TaskStatus)"
        >
          <option value="TODO">TODO</option>
          <option value="IN_PROGRESS">IN_PROGRESS</option>
          <option value="DONE">DONE</option>
          <option value="ABANDONED">放弃</option>
        </select>

        <button
          type="button"
          class="text-xs text-slate-400 hover:underline ml-auto"
          @click="openDetail(t)"
        >
          详情
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { TaskPriority, TaskStatus, TaskView } from '@/types/execution'

const props = defineProps<{
  title: string
  accent: 'slate' | 'brand' | 'emerald'
  tasks: TaskView[]
  openDetail: (t: TaskView) => void
  checkIn: (t: TaskView) => void
  changeStatus: (t: TaskView, next: TaskStatus) => void
}>()

const accentWrapperClass = computed(() => {
  if (props.accent === 'brand') return 'bg-brand-50/40 border-brand-200'
  if (props.accent === 'emerald') return 'bg-emerald-50/40 border-emerald-200'
  return 'bg-slate-50/40 border-slate-200'
})

function priorityBadge(p: TaskPriority) {
  if (p === 'HIGH') return 'bg-red-50 text-red-700 border border-red-100'
  if (p === 'LOW') return 'bg-slate-100 text-slate-500 border border-slate-200'
  return 'bg-brand-50 text-brand-700 border border-brand-100'
}

function dueDateClass(iso: string) {
  try {
    const today = new Date()
    today.setHours(0, 0, 0, 0)
    const d = new Date(iso + 'T00:00:00')
    if (d.getTime() < today.getTime()) return 'text-red-600 font-medium'
    const diff = (d.getTime() - today.getTime()) / 86400000
    if (diff <= 2) return 'text-amber-600 font-medium'
    return 'text-slate-500'
  } catch {
    return 'text-slate-500'
  }
}
</script>
