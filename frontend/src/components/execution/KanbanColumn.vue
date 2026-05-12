<template>
  <div
    class="gt-card p-4 space-y-3"
    :class="accentWrapperClass"
  >
    <div class="flex items-center justify-between">
      <div class="text-sm font-semibold text-slate-800">{{ title }}</div>
      <span class="text-xs px-2 py-0.5 rounded-full bg-white/80 text-slate-500 border border-white">{{ tasks.length }}</span>
    </div>

    <div v-if="tasks.length === 0" class="text-xs text-slate-400">（空）</div>

    <div
      v-for="t in tasks"
      :key="t.id"
      class="rounded-2xl border border-slate-200/80 bg-white p-4 space-y-3 transition hover:-translate-y-0.5 hover:border-slate-300 hover:shadow-sm"
    >
      <div class="flex items-start justify-between gap-2">
        <button
          type="button"
          class="text-base text-left font-semibold text-slate-900 hover:text-brand-600 flex-1 min-w-0 leading-snug"
          @click="openDetail(t)"
        >
          {{ t.title }}
        </button>
        <span class="text-xs px-2 py-0.5 rounded-full shrink-0" :class="priorityBadge(t.priority)">
          {{ t.priority }}
        </span>
      </div>

      <div class="flex items-center gap-2 flex-wrap text-xs text-slate-500">
        <span v-if="t.dueDate" :class="dueDateClass(t.dueDate)">
          due {{ t.dueDate }}
        </span>
        <span class="px-2 py-0.5 rounded-full bg-slate-100">打卡 {{ t.checkInCount }} 次</span>
        <span class="px-2 py-0.5 rounded-full bg-slate-100">投入 {{ t.actualEffortMinutes }} 分钟</span>
      </div>

      <div class="space-y-1.5">
        <div class="flex items-center justify-between text-xs">
          <span class="text-slate-500">执行可信度</span>
          <span class="font-medium" :class="confidenceTextClass(t)">{{ confidenceLabel(t) }}</span>
        </div>
        <div class="h-1.5 rounded-full bg-slate-100 overflow-hidden">
          <div
            class="h-full rounded-full transition-all"
            :class="confidenceBarClass(t)"
            :style="{ width: `${executionConfidence(t)}%` }"
          />
        </div>
      </div>

      <div
        v-if="t.targetTitle || t.requirementName"
        class="rounded-xl bg-slate-50 border border-slate-100 px-3 py-2 text-xs text-slate-600 space-y-1"
      >
        <div v-if="t.targetTitle" class="truncate">目标：{{ t.targetTitle }}</div>
        <div v-if="t.requirementName" class="flex items-center gap-1 min-w-0">
          <span class="truncate">要求：{{ t.requirementName }}</span>
          <span
            v-if="t.requirementStatus"
            class="shrink-0 px-1.5 py-0.5 rounded bg-white border border-slate-200 text-slate-500"
          >
            {{ t.requirementStatus }}
          </span>
        </div>
      </div>

      <div class="grid grid-cols-3 gap-2 text-center text-[11px] text-slate-500">
        <div class="rounded-xl bg-slate-50 px-2 py-2">
          <div class="font-semibold text-slate-800">{{ structuredCount(t, '验收标准') }}</div>
          <div>验收</div>
        </div>
        <div class="rounded-xl bg-slate-50 px-2 py-2">
          <div class="font-semibold text-slate-800">{{ structuredCount(t, '建议打卡计划') }}</div>
          <div>打卡</div>
        </div>
        <div class="rounded-xl bg-slate-50 px-2 py-2">
          <div class="font-semibold text-slate-800">{{ structuredCount(t, '完成证据建议') }}</div>
          <div>证据</div>
        </div>
      </div>

      <div class="flex items-center gap-2 pt-2 border-t border-slate-100 flex-wrap">
        <button
          v-if="t.status !== 'DONE'"
          type="button"
          :disabled="t.checkedInToday"
          class="text-xs px-2.5 py-1 rounded-full border text-brand-700 border-brand-200 hover:bg-brand-50 disabled:opacity-50 disabled:cursor-not-allowed"
          @click="checkIn(t)"
        >
          {{ t.checkedInToday ? '今日已打卡' : '打卡' }}
        </button>

        <button
          type="button"
          class="text-xs px-2.5 py-1 rounded-full border border-slate-200 text-slate-700 bg-white hover:bg-slate-50"
          @click="openReader(t)"
        >
          查看正文
        </button>

        <button
          type="button"
          class="text-xs px-2.5 py-1 rounded-full border border-amber-200 text-amber-700 bg-amber-50 hover:bg-amber-100"
          @click="regenerateDraft(t)"
        >
          AI 优化
        </button>

        <select
          :value="t.status"
          class="text-xs px-2 py-1 border border-slate-200 rounded-full bg-white focus:outline-none focus:ring-2 focus:ring-brand-500"
          @change="(e) => changeStatus(t, (e.target as HTMLSelectElement).value as TaskStatus)"
        >
          <option value="TODO">TODO</option>
          <option value="IN_PROGRESS">IN_PROGRESS</option>
          <option value="DONE">DONE</option>
          <option value="ABANDONED">放弃</option>
        </select>

        <button
          type="button"
          class="text-xs text-slate-500 hover:text-brand-600 hover:underline ml-auto"
          @click="openDetail(t)"
        >
          编辑
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
  regenerateDraft: (t: TaskView) => void
  openReader: (t: TaskView) => void
}>()

const accentWrapperClass = computed(() => {
  if (props.accent === 'brand') return 'bg-sky-50/60 border-sky-100'
  if (props.accent === 'emerald') return 'bg-emerald-50/60 border-emerald-100'
  return 'bg-slate-50/70 border-slate-200'
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

function taskSummary(t: TaskView) {
  return splitTaskDescription(t.description).summary
}

function structuredSections(t: TaskView) {
  return splitTaskDescription(t.description).sections
}

function structuredCount(t: TaskView, title: string) {
  return structuredSections(t).find((section) => section.title === title)?.items.length ?? 0
}

function splitTaskDescription(description: string | null | undefined) {
  const text = (description ?? '').trim()
  if (!text) return { summary: '', sections: [] as { title: string; items: string[] }[] }
  const markers = ['验收标准：', '建议打卡计划：', '完成证据建议：', '完成证据（']
  const firstMarker = markers
    .map((marker) => text.indexOf(marker))
    .filter((idx) => idx >= 0)
    .sort((a, b) => a - b)[0]
  const summary = firstMarker == null ? text : text.slice(0, firstMarker).trim()
  const sections = [
    readSection(text, '验收标准：', ['建议打卡计划：', '完成证据建议：', '完成证据（']),
    readSection(text, '建议打卡计划：', ['完成证据建议：', '完成证据（']),
    readSection(text, '完成证据建议：', ['完成证据（'])
  ].filter((section): section is { title: string; items: string[] } => section != null)
  return { summary, sections }
}

function readSection(text: string, title: string, endMarkers: string[]) {
  const start = text.indexOf(title)
  if (start < 0) return null
  const bodyStart = start + title.length
  const end = endMarkers
    .map((marker) => text.indexOf(marker, bodyStart))
    .filter((idx) => idx >= 0)
    .sort((a, b) => a - b)[0] ?? text.length
  const items = text
    .slice(bodyStart, end)
    .split('\n')
    .map((line) => line.replace(/^\s*(\d+[.)、]|[-*•])\s*/, '').trim())
    .filter(Boolean)
  return { title: title.replace('：', ''), items }
}

function executionConfidence(t: TaskView) {
  let score = 15
  if (t.description?.includes('验收标准：')) score += 25
  if (t.description?.includes('建议打卡计划：')) score += 20
  if (t.checkInCount > 0) score += 20
  if (t.actualEffortMinutes > 0) score += 10
  if (t.status === 'DONE' && t.description?.includes('完成证据（')) score += 10
  return Math.min(100, score)
}

function confidenceLabel(t: TaskView) {
  const score = executionConfidence(t)
  if (score >= 80) return '证据充分'
  if (score >= 55) return '路径清晰'
  return '待补指引'
}

function confidenceTextClass(t: TaskView) {
  const score = executionConfidence(t)
  if (score >= 80) return 'text-emerald-700'
  if (score >= 55) return 'text-sky-700'
  return 'text-amber-700'
}

function confidenceBarClass(t: TaskView) {
  const score = executionConfidence(t)
  if (score >= 80) return 'bg-emerald-500'
  if (score >= 55) return 'bg-sky-500'
  return 'bg-amber-500'
}
</script>
