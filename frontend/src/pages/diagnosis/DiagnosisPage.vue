<template>
  <div class="p-6 max-w-5xl mx-auto space-y-6">
    <!-- Header -->
    <header class="flex items-start justify-between gap-4">
      <div>
        <h1 class="text-2xl font-semibold text-slate-800">阶段诊断</h1>
        <p class="text-sm text-slate-500 mt-1">
          手动触发。系统先本地计算 7 指标（journal / task / skill / profile / target / activity），再调 AI 生成
          <span class="text-slate-700">阶段总结 · 重点问题 · 建议 · 纠偏方向</span>。
          <span class="text-slate-400">轻复盘并入同一条记录，不单独成模块。</span>
        </p>
      </div>
      <div class="flex flex-col items-end gap-2 shrink-0">
        <RouterLink to="/diagnosis/history" class="text-sm text-brand-600 hover:underline">
          查看历史 →
        </RouterLink>
        <div v-if="currentIdFromQuery" class="text-xs text-slate-400">
          正在查看诊断 #{{ currentIdFromQuery }}
        </div>
      </div>
    </header>

    <!-- 触发区 -->
    <section class="bg-white border border-slate-200 rounded-lg p-5 space-y-3">
      <div class="flex flex-wrap items-center gap-3">
        <label class="text-sm text-slate-600">回看窗口</label>
        <div class="flex items-center gap-1">
          <button
            v-for="opt in windowOptions"
            :key="opt"
            type="button"
            class="text-xs px-2.5 py-1 rounded-md border"
            :class="windowDays === opt
              ? 'border-brand-500 bg-brand-50 text-brand-700'
              : 'border-slate-200 text-slate-600 hover:bg-slate-50'"
            @click="windowDays = opt"
          >
            {{ opt }} 天
          </button>
        </div>

        <div class="flex-1" />

        <button
          type="button"
          :disabled="triggering"
          class="px-4 py-2 rounded-md bg-brand-600 text-white text-sm font-medium hover:bg-brand-700 disabled:opacity-60 disabled:cursor-not-allowed"
          @click="onTrigger"
        >
          {{ triggering ? 'AI 诊断中…（可能需要 1 分钟左右）' : '触发新一次诊断' }}
        </button>
      </div>

      <div v-if="triggerError" class="text-sm text-red-600 bg-red-50 border border-red-100 rounded px-3 py-2">
        {{ triggerError }}
      </div>

      <div v-if="triggering" class="text-xs text-slate-500">
        正在计算本地 7 指标并请求 AI 总结，模型响应较慢时可能需要 1 分钟左右…
      </div>
    </section>

    <!-- 状态：初始加载 -->
    <div v-if="loading" class="text-sm text-slate-500">加载最近一次诊断…</div>

    <!-- 空态 -->
    <div
      v-else-if="!diagnosis"
      class="bg-white border border-slate-200 border-dashed rounded-lg p-10 text-center"
    >
      <div class="text-sm text-slate-500 mb-2">还没有任何诊断记录</div>
      <div class="text-xs text-slate-400">
        选择回看窗口，点上方"触发新一次诊断"生成第一条。
      </div>
    </div>

    <!-- 诊断内容 -->
    <template v-else>
      <!-- 元信息 -->
      <section class="bg-white border border-slate-200 rounded-lg p-5 space-y-2">
        <div class="flex items-center gap-2 flex-wrap">
          <span class="text-xs px-2 py-0.5 rounded" :class="aiBadgeClass(diagnosis.aiStatus)">
            AI · {{ diagnosis.aiStatus }}
          </span>
          <span class="text-xs text-slate-400">#{{ diagnosis.id }}</span>
          <span class="text-xs text-slate-500">
            触发时间 {{ formatDateTime(diagnosis.triggerTime) }}
          </span>
          <span class="text-xs text-slate-400">
            · 窗口 {{ formatDate(diagnosis.windowStart) }} ~ {{ formatDate(diagnosis.windowEnd) }}
          </span>
          <span class="text-xs text-slate-400">· 画像 v{{ diagnosis.profileVersionAtTrigger }}</span>
        </div>
      </section>

      <!-- 指标区 -->
      <section class="bg-white border border-slate-200 rounded-lg p-5 space-y-4">
        <div class="flex items-center justify-between">
          <h2 class="text-sm font-medium text-slate-700">本地规则指标（7 类）</h2>
          <span class="text-xs text-slate-400">由 DiagnosisMetricsService 在 AI 前计算</span>
        </div>

        <div class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-5 gap-3">
          <MetricCard
            label="随记条数"
            :value="diagnosis.metrics.journal_count ?? 0"
            hint="window 内"
          />
          <MetricCard
            label="连续记录"
            :value="diagnosis.metrics.journal_streak ?? 0"
            hint="天"
          />
          <MetricCard
            label="任务完成率"
            :value="formatPercent(diagnosis.metrics.task_completion_rate)"
            hint="全量活动任务"
          />
          <MetricCard
            label="新增技能"
            :value="diagnosis.metrics.new_skills_count ?? 0"
            hint="window 内"
          />
          <MetricCard
            label="画像完整度"
            :value="`${diagnosis.metrics.profile_completeness ?? 0}/100`"
            hint="口径一致"
          />
        </div>

        <!-- target_requirement_progress -->
        <div class="border-t border-slate-100 pt-3">
          <div class="text-xs text-slate-500 mb-1.5">目标要求进度（ACTIVE targets）</div>
          <template v-if="requirementProgress">
            <div class="flex items-center gap-3">
              <div class="flex-1 h-2 bg-slate-100 rounded-full overflow-hidden">
                <div
                  class="h-2 bg-brand-500 rounded-full"
                  :style="{ width: `${Math.round((requirementProgress.met_ratio ?? 0) * 100)}%` }"
                />
              </div>
              <div class="text-xs text-slate-500 w-24 text-right">
                MET {{ requirementProgress.met }} / {{ requirementProgress.total }}
              </div>
            </div>
            <div class="flex flex-wrap gap-2 mt-2 text-xs">
              <span class="px-1.5 py-0.5 rounded bg-emerald-50 text-emerald-700 border border-emerald-100">
                MET {{ requirementProgress.met }}
              </span>
              <span class="px-1.5 py-0.5 rounded bg-brand-50 text-brand-700 border border-brand-100">
                IN_PROGRESS {{ requirementProgress.in_progress }}
              </span>
              <span class="px-1.5 py-0.5 rounded bg-slate-100 text-slate-600 border border-slate-200">
                TODO {{ requirementProgress.todo }}
              </span>
              <span class="text-slate-400">占比 {{ formatPercent(requirementProgress.met_ratio) }}</span>
            </div>
          </template>
          <div v-else class="text-xs text-slate-400">当前无 ACTIVE 目标</div>
        </div>

        <!-- activity_intensity -->
        <div class="border-t border-slate-100 pt-3">
          <div class="text-xs text-slate-500 mb-1.5">
            活动强度（按天，journal + task check-in + snapshot 综合 score）
          </div>
          <template v-if="activityPoints.length > 0">
            <div class="flex items-end gap-0.5 h-24 overflow-x-auto">
              <div
                v-for="p in activityPoints"
                :key="p.date"
                class="relative group shrink-0"
                :style="{ width: '14px' }"
              >
                <div
                  class="bg-brand-500/80 rounded-sm w-full"
                  :style="{ height: `${Math.min(100, p.score * 20)}%` }"
                />
                <div
                  class="absolute bottom-full mb-1 left-1/2 -translate-x-1/2 whitespace-nowrap text-xs bg-slate-800 text-white rounded px-1.5 py-0.5 opacity-0 group-hover:opacity-100 pointer-events-none"
                >
                  {{ p.date }} · {{ p.score }}
                </div>
              </div>
            </div>
            <div class="flex items-center justify-between text-xs text-slate-400 mt-1">
              <span>{{ activityPoints[0]?.date }}</span>
              <span>{{ activityPoints[activityPoints.length - 1]?.date }}</span>
            </div>
          </template>
          <div v-else class="text-xs text-slate-400">window 内无任何活动</div>
        </div>
      </section>

      <!-- AI 总结 -->
      <section
        class="bg-white border border-slate-200 rounded-lg p-5 space-y-4"
        :class="diagnosis.aiStatus === 'FAILED' ? 'opacity-90' : ''"
      >
        <div class="flex items-center justify-between">
          <h2 class="text-sm font-medium text-slate-700">AI 阶段总结</h2>
          <span v-if="diagnosis.aiStatus === 'FAILED'" class="text-xs text-red-600">
            本次 AI 调用失败，仅保留规则指标；可稍后重新触发
          </span>
        </div>

        <div v-if="diagnosis.stageSummary" class="text-sm text-slate-700 whitespace-pre-wrap leading-6">
          {{ diagnosis.stageSummary }}
        </div>
        <div v-else class="text-xs text-slate-400">
          （AI 未生成总结文本）
        </div>

        <!-- Key problems -->
        <div v-if="(diagnosis.keyProblems ?? []).length > 0" class="space-y-2">
          <div class="text-xs text-slate-500">重点问题</div>
          <div
            v-for="(p, i) in (diagnosis.keyProblems ?? [])"
            :key="`kp-${i}`"
            class="border border-slate-200 rounded-md p-3"
          >
            <div class="text-sm font-medium text-slate-800">
              {{ String(p.title ?? '—') }}
            </div>
            <div v-if="p.description" class="text-xs text-slate-500 mt-1 whitespace-pre-wrap">
              {{ String(p.description) }}
            </div>
          </div>
        </div>

        <!-- Suggestions -->
        <div v-if="(diagnosis.suggestions ?? []).length > 0" class="space-y-2">
          <div class="text-xs text-slate-500">建议</div>
          <div
            v-for="(s, i) in (diagnosis.suggestions ?? [])"
            :key="`sg-${i}`"
            class="border border-slate-200 rounded-md p-3"
          >
            <div class="flex items-center gap-2 flex-wrap">
              <span
                v-if="s.priority"
                class="text-xs px-1.5 py-0.5 rounded"
                :class="priorityBadgeClass(String(s.priority))"
              >
                {{ String(s.priority) }}
              </span>
              <span class="text-sm font-medium text-slate-800">{{ String(s.title ?? '—') }}</span>
            </div>
            <div v-if="s.detail" class="text-xs text-slate-600 mt-1 whitespace-pre-wrap">
              {{ String(s.detail) }}
            </div>
          </div>
        </div>

        <!-- Correction directions -->
        <div v-if="(diagnosis.correctionDirections ?? []).length > 0" class="space-y-2">
          <div class="text-xs text-slate-500">纠偏方向</div>
          <ul class="list-disc pl-5 space-y-1 text-sm text-slate-700">
            <li v-for="(c, i) in (diagnosis.correctionDirections ?? [])" :key="`cd-${i}`">
              <span class="font-medium">{{ String(c.direction ?? '—') }}</span>
              <span v-if="c.rationale" class="text-xs text-slate-500">
                —— {{ String(c.rationale) }}
              </span>
            </li>
          </ul>
        </div>
      </section>

      <!-- 轻复盘 -->
      <section class="bg-white border border-slate-200 rounded-lg p-5 space-y-4">
        <div class="flex items-center justify-between">
          <h2 class="text-sm font-medium text-slate-700">轻复盘</h2>
          <span class="text-xs text-slate-400">
            保存到 stage_assessment.review_notes，与诊断同一记录
          </span>
        </div>

        <ReviewListEditor
          label="本阶段亮点 (wins)"
          :items="reviewForm.wins"
          placeholder="一句话写一条亮点"
          :max="200"
          @add="reviewForm.wins.push('')"
          @remove="(i) => reviewForm.wins.splice(i, 1)"
        />
        <ReviewListEditor
          label="学到的东西 (learnings)"
          :items="reviewForm.learnings"
          placeholder="一句话写一条收获"
          :max="200"
          @add="reviewForm.learnings.push('')"
          @remove="(i) => reviewForm.learnings.splice(i, 1)"
        />
        <ReviewListEditor
          label="下阶段重点 (nextFocus)"
          :items="reviewForm.nextFocus"
          placeholder="一句话写一条重点"
          :max="200"
          @add="reviewForm.nextFocus.push('')"
          @remove="(i) => reviewForm.nextFocus.splice(i, 1)"
        />

        <div>
          <label class="block text-sm text-slate-600 mb-1">自由复盘（可选）</label>
          <textarea
            v-model="reviewForm.userFreeform"
            rows="3"
            maxlength="2000"
            class="w-full px-3 py-2 border border-slate-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-brand-500"
            placeholder="随便写点想说的"
          />
          <div class="text-xs text-slate-400 mt-1">
            {{ reviewForm.userFreeform.length }} / 2000
          </div>
        </div>

        <div v-if="reviewError" class="text-sm text-red-600">{{ reviewError }}</div>

        <div class="flex items-center gap-2 pt-2 border-t border-slate-100">
          <button
            type="button"
            :disabled="reviewSaving"
            class="px-3 py-1.5 rounded-md bg-brand-600 text-white text-sm hover:bg-brand-700 disabled:opacity-60"
            @click="onSaveReview"
          >
            {{ reviewSaving ? '保存中…' : '保存轻复盘' }}
          </button>
          <span v-if="reviewSavedAt" class="text-xs text-emerald-600">
            已保存 {{ formatDateTime(reviewSavedAt) }}
          </span>
        </div>
      </section>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import MetricCard from '@/components/diagnosis/MetricCard.vue'
import ReviewListEditor from '@/components/diagnosis/ReviewListEditor.vue'
import {
  fetchDiagnosis,
  listDiagnosisHistory,
  triggerDiagnosis,
  updateDiagnosisReview
} from '@/api/diagnosis'
import { explainAiError } from '@/utils/aiError'
import type { ActivityPoint, AiStatus, DiagnosisView, RequirementProgress } from '@/types/diagnosis'

const route = useRoute()
const router = useRouter()

const windowOptions = [7, 14, 30, 60, 90] as const
const windowDays = ref<number>(30)

const diagnosis = ref<DiagnosisView | null>(null)
const loading = ref(true)
const triggering = ref(false)
const triggerError = ref('')

const reviewForm = reactive({
  wins: [] as string[],
  learnings: [] as string[],
  nextFocus: [] as string[],
  userFreeform: ''
})
const reviewSaving = ref(false)
const reviewError = ref('')
const reviewSavedAt = ref('')

const currentIdFromQuery = computed(() => {
  const v = route.query.id
  return typeof v === 'string' && /^\d+$/.test(v) ? Number(v) : null
})

onMounted(load)

watch(currentIdFromQuery, (v, old) => {
  if (v !== old) void load()
})

async function load() {
  loading.value = true
  try {
    const targetId = currentIdFromQuery.value
    if (targetId != null) {
      diagnosis.value = await fetchDiagnosis(targetId)
    } else {
      const history = await listDiagnosisHistory({ page: 1, size: 1 })
      if (history.records.length === 0) {
        diagnosis.value = null
      } else {
        diagnosis.value = await fetchDiagnosis(history.records[0].id)
      }
    }
    hydrateReviewForm()
  } catch (e) {
    diagnosis.value = null
    triggerError.value = (e as Error).message || '加载失败'
  } finally {
    loading.value = false
  }
}

async function onTrigger() {
  triggerError.value = ''
  triggering.value = true
  try {
    const created = await triggerDiagnosis({ windowDays: windowDays.value })
    diagnosis.value = created
    hydrateReviewForm()
    // 触发后始终显示新条目，把 URL 里残留的 id 清掉
    if (currentIdFromQuery.value != null) {
      router.replace({ path: '/diagnosis' })
    }
  } catch (e) {
    triggerError.value = explainAiError(e, '触发失败，请稍后重试')
  } finally {
    triggering.value = false
  }
}

function hydrateReviewForm() {
  reviewError.value = ''
  reviewSavedAt.value = ''
  const n = diagnosis.value?.reviewNotes
  reviewForm.wins = [...(n?.wins ?? [])]
  reviewForm.learnings = [...(n?.learnings ?? [])]
  reviewForm.nextFocus = [...(n?.nextFocus ?? [])]
  reviewForm.userFreeform = n?.userFreeform ?? ''
}

async function onSaveReview() {
  if (!diagnosis.value) return
  reviewSaving.value = true
  reviewError.value = ''
  try {
    const updated = await updateDiagnosisReview(diagnosis.value.id, {
      wins: reviewForm.wins.map((s) => s.trim()).filter((s) => s !== ''),
      learnings: reviewForm.learnings.map((s) => s.trim()).filter((s) => s !== ''),
      nextFocus: reviewForm.nextFocus.map((s) => s.trim()).filter((s) => s !== ''),
      userFreeform: reviewForm.userFreeform.trim()
    })
    diagnosis.value = updated
    reviewSavedAt.value = new Date().toISOString()
  } catch (e) {
    reviewError.value = (e as Error).message || '保存失败'
  } finally {
    reviewSaving.value = false
  }
}

const requirementProgress = computed<RequirementProgress | null>(() => {
  const v = diagnosis.value?.metrics.target_requirement_progress
  if (!v || typeof v !== 'object') return null
  if (!('total' in v)) return null
  return v as RequirementProgress
})

const activityPoints = computed<ActivityPoint[]>(() => {
  const raw = diagnosis.value?.metrics.activity_intensity
  if (!Array.isArray(raw)) return []
  return raw as ActivityPoint[]
})

function formatPercent(v: unknown): string {
  const n = typeof v === 'number' ? v : Number(v ?? 0)
  if (Number.isNaN(n)) return '—'
  return `${Math.round(n * 100)}%`
}

function formatDateTime(iso: string | undefined) {
  if (!iso) return ''
  return iso.slice(0, 16).replace('T', ' ')
}

function formatDate(iso: string | undefined) {
  if (!iso) return ''
  return iso.slice(0, 10)
}

function aiBadgeClass(status: AiStatus) {
  return {
    SUCCESS: 'bg-emerald-50 text-emerald-700 border border-emerald-100',
    FALLBACK: 'bg-amber-50 text-amber-700 border border-amber-100',
    FAILED: 'bg-red-50 text-red-700 border border-red-100'
  }[status]
}

function priorityBadgeClass(p: string) {
  const u = p.toUpperCase()
  if (u === 'HIGH') return 'bg-red-50 text-red-700 border border-red-100'
  if (u === 'LOW') return 'bg-slate-100 text-slate-600 border border-slate-200'
  return 'bg-brand-50 text-brand-700 border border-brand-100'
}
</script>
