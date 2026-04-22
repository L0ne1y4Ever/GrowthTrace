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
      <section class="bg-amber-50 border border-amber-100 rounded-lg p-5 space-y-3">
        <div class="flex items-center justify-between gap-3">
          <div>
            <h2 class="text-sm font-medium text-slate-800">本周建议先做这几步</h2>
            <p class="text-xs text-slate-500 mt-1">根据画像、目标、执行、随记和诊断状态，给你一个最短推进顺序。</p>
          </div>
          <span class="text-xs text-amber-700 bg-amber-100 px-2 py-1 rounded-full">Top {{ nextActions.length }}</span>
        </div>

        <div class="grid grid-cols-1 md:grid-cols-3 gap-3">
          <div
            v-for="(action, index) in nextActions"
            :key="action.title"
            class="bg-white border rounded-lg p-4 space-y-2"
            :class="action.level === 'high' ? 'border-red-100' : action.level === 'medium' ? 'border-amber-100' : 'border-slate-200'"
          >
            <div class="flex items-center justify-between gap-2">
              <span
                class="text-xs px-2 py-0.5 rounded-full"
                :class="action.level === 'high'
                  ? 'bg-red-50 text-red-700'
                  : action.level === 'medium'
                    ? 'bg-amber-50 text-amber-700'
                    : 'bg-slate-100 text-slate-600'"
              >
                Step {{ index + 1 }}
              </span>
              <span class="text-xs text-slate-400">{{ action.levelLabel }}</span>
            </div>
            <div class="text-sm font-medium text-slate-800">{{ action.title }}</div>
            <div class="text-xs text-slate-500 leading-5">{{ action.detail }}</div>
            <RouterLink :to="action.to" class="text-sm text-brand-600 hover:underline">
              {{ action.cta }} →
            </RouterLink>
          </div>
        </div>
      </section>

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
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import HeatmapGrid from '@/components/dashboard/HeatmapGrid.vue'
import GrowthCurveChart from '@/components/dashboard/GrowthCurveChart.vue'
import { fetchOverview } from '@/api/dashboard'
import type { DashboardOverviewView } from '@/types/dashboard'

const overview = ref<DashboardOverviewView | null>(null)
const loading = ref(true)
const loadError = ref('')
const nextActions = computed(() => buildNextActions(overview.value))

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

interface NextAction {
  title: string
  detail: string
  cta: string
  to: string
  level: 'high' | 'medium' | 'low'
  levelLabel: string
}

function buildNextActions(data: DashboardOverviewView | null): NextAction[] {
  if (!data) return []

  const actions: NextAction[] = []
  const profileCompleteness = data.profileCompleteness ?? null
  const pendingJournal = (data.recentJournals ?? []).find((item) => item.extractionStatus === 'PENDING_CONFIRM')
  const weeklyTask = data.weeklyTask
  const latestDiagnosis = data.latestDiagnosis
  const primaryTarget = data.primaryTarget

  if (profileCompleteness == null) {
    actions.push({
      title: '先完成第一次建档',
      detail: '没有画像就很难让目标、诊断和 AI 抽取形成闭环。先把基础画像立起来。',
      cta: '开始建档',
      to: '/onboarding',
      level: 'high',
      levelLabel: '基础缺口'
    })
  } else if (profileCompleteness < 60) {
    actions.push({
      title: '先补到 60 分以上的画像完整度',
      detail: `当前完整度 ${profileCompleteness}/100，建议先补技能、经历和当前状态，再做后续诊断。`,
      cta: '继续完善画像',
      to: '/profile',
      level: 'high',
      levelLabel: '基础缺口'
    })
  }

  if (!primaryTarget) {
    actions.push({
      title: '设定一个主目标',
      detail: '没有主目标时，任务、随记和诊断都缺少统一参照，先把当前阶段最重要的目标定下来。',
      cta: '去设定目标',
      to: '/target',
      level: 'high',
      levelLabel: '主线缺失'
    })
  } else if (primaryTarget.requirementCount === 0) {
    actions.push({
      title: '给主目标补充可验证要求',
      detail: `当前目标“${primaryTarget.title}”还没有 requirement，后续很难衡量是否真的在推进。`,
      cta: '补目标要求',
      to: '/target',
      level: 'medium',
      levelLabel: '目标不够具体'
    })
  }

  if (weeklyTask) {
    if (weeklyTask.dueThisWeek > 0 && weeklyTask.completionRate < 0.6) {
      actions.push({
        title: '优先清理本周到期任务',
        detail: `本周到期 ${weeklyTask.dueThisWeek} 条，只完成了 ${weeklyTask.doneThisWeek} 条，建议先把到期项推进到 DONE。`,
        cta: '去任务看板',
        to: '/execution',
        level: 'high',
        levelLabel: '执行掉队'
      })
    }
  } else if (primaryTarget) {
    actions.push({
      title: '把主目标拆成第一批执行任务',
      detail: '现在有目标但执行面还是空的，先补 2 到 3 条本周能完成的任务，闭环才会跑起来。',
      cta: '去创建任务',
      to: '/execution',
      level: 'medium',
      levelLabel: '执行未启动'
    })
  }

  if (pendingJournal) {
    actions.push({
      title: '确认最近一条随记抽取草稿',
      detail: '你已经有待确认的 AI 草稿，确认后技能、事件和阻塞才能真正进入后续诊断链路。',
      cta: '去确认草稿',
      to: `/journal/${pendingJournal.id}`,
      level: 'medium',
      levelLabel: '待确认'
    })
  } else if ((data.recentJournals ?? []).length === 0) {
    actions.push({
      title: '补一条本周随记',
      detail: '没有随记时，阶段诊断会缺少近期行为样本。写一条最近的学习、任务或阻塞记录即可。',
      cta: '去写随记',
      to: '/journal',
      level: 'medium',
      levelLabel: '样本不足'
    })
  }

  if (!latestDiagnosis) {
    actions.push({
      title: '触发第一次阶段诊断',
      detail: '你已经有基础数据后，可以生成一次阶段总结，看看当前最主要的问题和纠偏方向。',
      cta: '去触发诊断',
      to: '/diagnosis',
      level: 'medium',
      levelLabel: '缺少复盘'
    })
  } else if (latestDiagnosis.aiStatus === 'FAILED') {
    actions.push({
      title: '重新触发一次诊断',
      detail: '最近一次诊断只保留了规则指标，AI 总结没有成功，建议修正配置或稍后重试。',
      cta: '重新诊断',
      to: '/diagnosis',
      level: 'high',
      levelLabel: 'AI 失败'
    })
  } else if (latestDiagnosis.aiStatus === 'FALLBACK') {
    actions.push({
      title: '复查最近一次诊断结果',
      detail: '最近一次诊断使用了降级结果，建议人工看一遍建议和纠偏方向，再决定本周任务。',
      cta: '查看诊断',
      to: '/diagnosis',
      level: 'low',
      levelLabel: '建议复查'
    })
  }

  if (actions.length === 0) {
    actions.push(
      {
        title: '继续保持本周执行节奏',
        detail: '主线数据已经比较完整，当前更重要的是持续打卡、按周写随记，而不是频繁改配置。',
        cta: '去任务看板',
        to: '/execution',
        level: 'low',
        levelLabel: '状态稳定'
      },
      {
        title: '写一条带复盘信息的随记',
        detail: '把最近一周最有代表性的进展、问题和判断写下来，会让下次诊断更有价值。',
        cta: '去写随记',
        to: '/journal',
        level: 'low',
        levelLabel: '持续沉淀'
      },
      {
        title: '回看最近一次诊断的建议',
        detail: '把建议和纠偏方向对照到本周任务，检查是否真的转成了行动项。',
        cta: '查看诊断',
        to: '/diagnosis',
        level: 'low',
        levelLabel: '闭环复查'
      }
    )
  }

  return actions.slice(0, 3)
}
</script>
