<template>
  <div class="space-y-3">
    <div class="flex items-center justify-between">
      <div class="text-sm font-medium text-slate-700">
        活动热力图
        <span class="text-xs text-slate-400 ml-2">
          （journal + task check-in + snapshot 综合）
        </span>
      </div>
      <div class="text-xs text-slate-400">
        近 {{ totalDays }} 天 · 共 {{ totalEvents }} 次
      </div>
    </div>

    <div class="overflow-x-auto">
      <!-- GitHub 风格：7 行（周一到周日） × N 列（周） -->
      <div
        class="inline-grid gap-[3px]"
        :style="{ gridTemplateColumns: `repeat(${weekCount}, 12px)` }"
      >
        <template v-for="(col, c) in grid" :key="`col-${c}`">
          <div
            v-for="(cell, r) in col"
            :key="`cell-${c}-${r}`"
            class="w-[12px] h-[12px] rounded-sm"
            :class="cell ? levelClass(cell.score) : 'bg-slate-100'"
            :title="cell ? `${cell.date} · ${cell.score} 次` : ''"
          />
        </template>
      </div>
    </div>

    <div class="flex items-center justify-end gap-1 text-[10px] text-slate-400">
      <span>少</span>
      <span class="w-2.5 h-2.5 rounded-sm bg-slate-100" />
      <span class="w-2.5 h-2.5 rounded-sm bg-brand-200" />
      <span class="w-2.5 h-2.5 rounded-sm bg-brand-400" />
      <span class="w-2.5 h-2.5 rounded-sm bg-brand-600" />
      <span class="w-2.5 h-2.5 rounded-sm bg-brand-800" />
      <span>多</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { HeatmapPoint } from '@/types/dashboard'

const props = defineProps<{
  points: HeatmapPoint[]
}>()

const totalDays = computed(() => props.points.length)
const totalEvents = computed(() => props.points.reduce((s, p) => s + (p.score || 0), 0))

/**
 * 把按日期升序的一维数据切成 [week-col][day-of-week] 形式。
 * 列 0 = 最早的那一周（可能首日不是周一，用 null 填充前置空格）。
 */
const grid = computed<(HeatmapPoint | null)[][]>(() => {
  if (props.points.length === 0) return []
  const sorted = [...props.points].sort((a, b) => a.date.localeCompare(b.date))
  const first = new Date(sorted[0].date + 'T00:00:00')
  const firstDow = (first.getDay() + 6) % 7 // 周一=0, 周日=6
  const cells: (HeatmapPoint | null)[] = Array(firstDow).fill(null)
  for (const p of sorted) cells.push(p)
  // 对齐到整周
  while (cells.length % 7 !== 0) cells.push(null)

  const cols: (HeatmapPoint | null)[][] = []
  for (let i = 0; i < cells.length; i += 7) {
    cols.push(cells.slice(i, i + 7))
  }
  return cols
})

const weekCount = computed(() => grid.value.length)

function levelClass(score: number) {
  if (score <= 0) return 'bg-slate-100'
  if (score === 1) return 'bg-brand-200'
  if (score === 2) return 'bg-brand-400'
  if (score <= 4) return 'bg-brand-600'
  return 'bg-brand-800'
}
</script>
