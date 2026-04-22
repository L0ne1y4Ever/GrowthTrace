<template>
  <div class="p-6 max-w-6xl mx-auto space-y-6">
    <header class="flex items-start justify-between gap-4">
      <div>
        <h1 class="text-2xl font-semibold text-slate-800">成长总览</h1>
        <p class="text-sm text-slate-500 mt-1">
          不是 AI 首页 —— 读缓存列 + 组合 6 个模块的轻量视图；AI 洞察卡片只展示"最近一次"阶段诊断。
        </p>
      </div>
      <button
        type="button"
        :disabled="loading"
        class="text-sm text-brand-600 hover:underline disabled:opacity-50"
        @click="load"
      >
        {{ loading ? '加载中…' : '刷新' }}
      </button>
    </header>

    <div v-if="loadError" class="text-sm text-red-600 bg-red-50 border border-red-100 rounded px-3 py-2">
      {{ loadError }}
    </div>

    <div v-if="loading && !overview" class="text-sm text-slate-500">加载中…</div>

    <template v-else-if="overview">
      <!-- 热力图 -->
      <section class="bg-white border border-slate-200 rounded-lg p-5">
        <HeatmapGrid :points="overview.heatmap ?? []" />
      </section>

      <!-- 三卡：完整度 / 主目标 / 本周任务 -->
      <section class="grid grid-cols-1 md:grid-cols-3 gap-4">
        <!-- 画像完整度 -->
        <div class="bg-white border border-slate-200 rounded-lg p-5 space-y-3">
          <div class="flex items-center justify-between">
            <div class="text-sm font-medium text-slate-700">画像完整度</div>
            <RouterLink to="/profile" class="text-xs text-brand-600 hover:underline">去完善</RouterLink>
          </div>
          <template v-if="overview.profileCompleteness != null">
            <div class="text-3xl font-semibold text-brand-600">
              {{ overview.profileCompleteness }}<span class="text-sm text-slate-400">/100</span>
            </div>
            <div class="h-2 bg-slate-100 rounded-full overflow-hidden">
              <div
                class="h-2 bg-brand-500 rounded-full"
                :style="{ width: `${overview.profileCompleteness}%` }"
              />
            </div>
            <div class="text-xs text-slate-400">唯一口径 · 读 growth_profile.completeness 缓存列</div>
          </template>
          <template v-else>
            <div class="text-sm text-slate-500">尚未建档</div>
            <RouterLink to="/onboarding" class="text-sm text-brand-600 hover:underline">
              开始建档引导 →
            </RouterLink>
          </template>
        </div>

        <!-- 当前主目标 -->
        <div class="bg-white border border-slate-200 rounded-lg p-5 space-y-3">
          <div class="flex items-center justify-between">
            <div class="text-sm font-medium text-slate-700">当前主目标</div>
            <RouterLink to="/target" class="text-xs text-brand-600 hover:underline">管理</RouterLink>
          </div>
          <template v-if="overview.primaryTarget">
            <div class="flex items-center gap-2">
              <span class="text-xs px-1.5 py-0.5 rounded bg-slate-100 text-slate-600">
                {{ targetTypeLabel(overview.primaryTarget.targetType) }}
              </span>
              <div class="text-sm font-medium text-slate-800 truncate">
                {{ overview.primaryTarget.title }}
              </div>
            </div>
            <div class="flex items-center gap-2">
              <div class="flex-1 h-2 bg-slate-100 rounded-full overflow-hidden">
                <div
                  class="h-2 bg-brand-500 rounded-full"
                  :style="{ width: `${Math.round(overview.primaryTarget.metRatio * 100)}%` }"
                />
              </div>
              <div class="text-xs text-slate-500 w-24 text-right">
                MET {{ overview.primaryTarget.requirementMetCount }} / {{ overview.primaryTarget.requirementCount }}
              </div>
            </div>
            <div v-if="overview.primaryTarget.deadline" class="text-xs text-slate-400">
              deadline {{ overview.primaryTarget.deadline }}
            </div>
          </template>
          <template v-else>
            <div class="text-sm text-slate-500">暂无主目标</div>
            <RouterLink to="/target" class="text-sm text-brand-600 hover:underline">去设定 →</RouterLink>
          </template>
        </div>

        <!-- 本周任务 -->
        <div class="bg-white border border-slate-200 rounded-lg p-5 space-y-3">
          <div class="flex items-center justify-between">
            <div class="text-sm font-medium text-slate-700">本周任务</div>
            <RouterLink to="/execution" class="text-xs text-brand-600 hover:underline">看板</RouterLink>
          </div>
          <template v-if="overview.weeklyTask">
            <div class="text-3xl font-semibold text-brand-600">
              {{ overview.weeklyTask.doneThisWeek }}<span class="text-slate-400 text-sm"> / {{ overview.weeklyTask.dueThisWeek }}</span>
            </div>
            <div class="text-xs text-slate-400">
              本周到期完成率 {{ formatPercent(overview.weeklyTask.completionRate) }}
            </div>
            <div class="flex items-end gap-1 h-10 pt-1">
              <div
                v-for="p in overview.weeklyTask.checkInPerDay"
                :key="p.date"
                class="flex-1 bg-brand-500/70 rounded-sm"
                :style="{ height: `${Math.min(100, p.count * 25)}%` }"
                :title="`${p.date} · ${p.count} 次打卡`"
              />
            </div>
          </template>
          <template v-else>
            <div class="text-sm text-slate-500">—</div>
          </template>
        </div>
      </section>

      <!-- 成长曲线 + 最近随记 -->
      <section class="grid grid-cols-1 lg:grid-cols-2 gap-4">
        <div class="bg-white border border-slate-200 rounded-lg p-5">
          <GrowthCurveChart :points="overview.growthCurve ?? []" />
        </div>

        <div class="bg-white border border-slate-200 rounded-lg p-5 space-y-3">
          <div class="flex items-center justify-between">
            <div class="text-sm font-medium text-slate-700">最近随记</div>
            <RouterLink to="/journal" class="text-xs text-brand-600 hover:underline">全部</RouterLink>
          </div>
          <template v-if="(overview.recentJournals ?? []).length > 0">
            <RouterLink
              v-for="j in (overview.recentJournals ?? [])"
              :key="j.id"
              :to="`/journal/${j.id}`"
              class="block border border-slate-100 rounded-md p-2.5 hover:border-brand-200 transition-colors"
            >
              <div class="flex items-center gap-2 flex-wrap text-xs text-slate-500">
                <span>{{ formatDateTime(j.createdAt) }}</span>
                <span v-if="j.mood">· {{ moodEmoji(j.mood) }}</span>
                <span
                  v-if="j.extractionStatus"
                  class="text-xs px-1.5 py-0.5 rounded"
                  :class="extractionBadge(j.extractionStatus)"
                >
                  {{ extractionLabel(j.extractionStatus) }}
                </span>
                <span class="text-slate-400">· {{ j.wordCount ?? 0 }} 字</span>
              </div>
              <div class="text-sm text-slate-700 mt-1 line-clamp-2">
                {{ j.contentExcerpt || '（无内容）' }}
              </div>
            </RouterLink>
          </template>
          <div v-else class="text-xs text-slate-400 text-center py-6">
            还没有随记 —— <RouterLink to="/journal" class="text-brand-600 hover:underline">去写一条</RouterLink>
          </div>
        </div>
      </section>

      <!-- 最新一次诊断洞察 -->
      <section class="bg-white border border-slate-200 rounded-lg p-5 space-y-3">
        <div class="flex items-center justify-between">
          <div class="text-sm font-medium text-slate-700">最近一次阶段诊断</div>
          <RouterLink to="/diagnosis" class="text-xs text-brand-600 hover:underline">去诊断</RouterLink>
        </div>
        <template v-if="overview.latestDiagnosis">
          <div class="flex items-center gap-2 flex-wrap">
            <span
              class="text-xs px-1.5 py-0.5 rounded"
              :class="diagnosisBadge(overview.latestDiagnosis.aiStatus)"
            >
              {{ overview.latestDiagnosis.aiStatus }}
            </span>
            <span class="text-xs text-slate-500">
              {{ formatDateTime(overview.latestDiagnosis.triggerTime) }}
            </span>
            <RouterLink
              :to="{ path: '/diagnosis', query: { id: overview.latestDiagnosis.id } }"
              class="text-xs text-brand-600 hover:underline ml-auto"
            >
              查看详情
            </RouterLink>
          </div>

          <div v-if="overview.latestDiagnosis.stageSummaryExcerpt" class="text-sm text-slate-700 whitespace-pre-wrap">
            {{ overview.latestDiagnosis.stageSummaryExcerpt }}
          </div>
          <div
            v-else-if="overview.latestDiagnosis.aiStatus === 'FAILED'"
            class="text-sm text-red-600 bg-red-50 border border-red-100 rounded px-3 py-2"
          >
            最近一次诊断的 AI 总结失败，当前仅保留规则指标结果。可稍后重新触发诊断。
          </div>

          <div v-if="overview.latestDiagnosis.topSuggestions.length > 0" class="space-y-1">
            <div class="text-xs text-slate-500">建议</div>
            <ul class="list-disc pl-5 text-sm text-slate-700 space-y-0.5">
              <li v-for="(s, i) in overview.latestDiagnosis.topSuggestions" :key="`ds-${i}`">{{ s }}</li>
            </ul>
          </div>

          <div v-if="overview.latestDiagnosis.topCorrections.length > 0" class="space-y-1">
            <div class="text-xs text-slate-500">纠偏方向</div>
            <ul class="list-disc pl-5 text-sm text-slate-700 space-y-0.5">
              <li v-for="(c, i) in overview.latestDiagnosis.topCorrections" :key="`dc-${i}`">{{ c }}</li>
            </ul>
          </div>
        </template>
        <template v-else>
          <div class="text-xs text-slate-400">
            还没有诊断记录 —— <RouterLink to="/diagnosis" class="text-brand-600 hover:underline">触发第一次</RouterLink>
          </div>
        </template>
      </section>
    </template>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import HeatmapGrid from '@/components/dashboard/HeatmapGrid.vue'
import GrowthCurveChart from '@/components/dashboard/GrowthCurveChart.vue'
import { fetchOverview } from '@/api/dashboard'
import type { DashboardOverviewView } from '@/types/dashboard'

const overview = ref<DashboardOverviewView | null>(null)
const loading = ref(true)
const loadError = ref('')

onMounted(load)

async function load() {
  loading.value = true
  loadError.value = ''
  try {
    overview.value = await fetchOverview()
  } catch (e) {
    loadError.value = (e as Error).message || '加载失败'
  } finally {
    loading.value = false
  }
}

function formatPercent(v: number | undefined) {
  const n = typeof v === 'number' ? v : 0
  return `${Math.round(n * 100)}%`
}

function formatDateTime(iso: string | undefined) {
  if (!iso) return ''
  return iso.slice(0, 16).replace('T', ' ')
}

function targetTypeLabel(v: string) {
  return { JOB_SEEKING: '求职', POSTGRAD: '考研', SKILL_GROWTH: '技能成长' }[v] ?? v
}

function moodEmoji(m: string | null | undefined) {
  return { GREAT: '😊', GOOD: '🙂', NORMAL: '😐', BAD: '🙁', BLOCKED: '😣' }[m ?? 'NORMAL'] ?? '—'
}

function extractionBadge(s: string) {
  return {
    PENDING_CONFIRM: 'bg-amber-50 text-amber-700 border border-amber-100',
    CONFIRMED: 'bg-emerald-50 text-emerald-700 border border-emerald-100',
    DISCARDED: 'bg-slate-100 text-slate-500 border border-slate-200'
  }[s] ?? 'bg-slate-100 text-slate-500 border border-slate-200'
}

function extractionLabel(s: string) {
  return { PENDING_CONFIRM: '草稿待确认', CONFIRMED: '已确认', DISCARDED: '已丢弃' }[s] ?? s
}

function diagnosisBadge(status: string) {
  return {
    SUCCESS: 'bg-emerald-50 text-emerald-700 border border-emerald-100',
    FALLBACK: 'bg-amber-50 text-amber-700 border border-amber-100',
    FAILED: 'bg-red-50 text-red-700 border border-red-100'
  }[status] ?? 'bg-slate-100 text-slate-500 border border-slate-200'
}
</script>
