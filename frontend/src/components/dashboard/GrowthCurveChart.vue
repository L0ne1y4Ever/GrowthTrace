<template>
  <div class="space-y-2">
    <div class="flex items-center justify-between">
      <div class="text-sm font-medium text-slate-700">成长曲线</div>
      <span class="text-xs text-slate-400">基于 growth_snapshot · 共 {{ points.length }} 个节点</span>
    </div>

    <div v-if="points.length === 0" class="text-xs text-slate-400 py-8 text-center">
      还没有快照。触发一次阶段诊断后会自动生成快照。
    </div>

    <svg
      v-else
      :viewBox="`0 0 ${chartWidth} ${chartHeight}`"
      class="w-full h-32"
      preserveAspectRatio="none"
    >
      <!-- baseline -->
      <line
        x1="0"
        y1="64"
        :x2="chartWidth"
        y2="64"
        stroke="#e2e8f0"
        stroke-dasharray="3,3"
      />
      <polyline
        v-if="linePath.length >= 2"
        :points="linePath.map(p => `${p.x},${p.y}`).join(' ')"
        fill="none"
        stroke="#3a6df0"
        stroke-width="1.5"
      />
      <circle
        v-for="(p, i) in linePath"
        :key="`c-${i}`"
        :cx="p.x"
        :cy="p.y"
        r="2.5"
        fill="#3a6df0"
      >
        <title>
          {{ formatDate(p.source.date) }} · 完整度 {{ p.source.completeness ?? '—' }}
        </title>
      </circle>
    </svg>

    <div v-if="points.length > 0" class="flex items-center justify-between text-xs text-slate-400">
      <span>{{ formatDate(points[0].date) }}</span>
      <span>{{ formatDate(points[points.length - 1].date) }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { GrowthCurvePoint } from '@/types/dashboard'

const props = defineProps<{
  points: GrowthCurvePoint[]
}>()

const chartWidth = 400
const chartHeight = 80

const linePath = computed(() => {
  const pts = props.points
  if (pts.length === 0) return []
  const values = pts.map((p) => p.completeness ?? 0)
  const maxV = Math.max(100, ...values) // 固定 0-100 区间
  const minV = 0
  const stepX = pts.length === 1 ? 0 : chartWidth / (pts.length - 1)
  return pts.map((p, i) => {
    const v = p.completeness ?? 0
    const y = chartHeight - ((v - minV) / (maxV - minV)) * (chartHeight - 8) - 4
    const x = pts.length === 1 ? chartWidth / 2 : i * stepX
    return { x, y, source: p }
  })
})

function formatDate(iso: string | undefined) {
  if (!iso) return ''
  return iso.slice(0, 10)
}
</script>
