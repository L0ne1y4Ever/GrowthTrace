<template>
  <div class="p-6 max-w-4xl mx-auto space-y-6">
    <header class="flex items-start justify-between gap-4">
      <div>
        <h1 class="text-2xl font-semibold text-slate-800">诊断历史</h1>
        <p class="text-sm text-slate-500 mt-1">
          按创建时间倒序。点击进入详情可查看指标、AI 总结、轻复盘。
        </p>
      </div>
      <RouterLink to="/diagnosis" class="text-sm text-brand-600 hover:underline">
        ← 触发新诊断
      </RouterLink>
    </header>

    <div v-if="loading" class="text-sm text-slate-500">加载中…</div>
    <div v-else-if="loadError" class="text-sm text-red-600">{{ loadError }}</div>

    <div
      v-else-if="records.length === 0"
      class="bg-white border border-slate-200 border-dashed rounded-lg p-10 text-center text-slate-400 text-sm"
    >
      还没有诊断记录。
    </div>

    <section v-else class="space-y-3">
      <RouterLink
        v-for="r in records"
        :key="r.id"
        :to="{ path: '/diagnosis', query: { id: r.id } }"
        class="block bg-white border border-slate-200 rounded-lg p-4 hover:border-brand-300 transition-colors"
      >
        <div class="flex items-start justify-between gap-3">
          <div class="min-w-0 flex-1">
            <div class="flex items-center gap-2 flex-wrap">
              <span class="text-xs px-1.5 py-0.5 rounded" :class="aiBadgeClass(r.aiStatus)">
                {{ r.aiStatus }}
              </span>
              <span class="text-xs text-slate-400">#{{ r.id }}</span>
              <span class="text-xs text-slate-500">
                {{ formatDateTime(r.triggerTime) }}
              </span>
              <span class="text-xs text-slate-400">
                · 窗口 {{ formatDate(r.windowStart) }} ~ {{ formatDate(r.windowEnd) }}
              </span>
              <span class="text-xs text-slate-400">· v{{ r.profileVersionAtTrigger }}</span>
            </div>
            <div class="text-sm text-slate-800 mt-1.5 line-clamp-3">
              {{ historySummaryText(r) }}
            </div>
            <div class="flex items-center gap-3 mt-2 text-xs text-slate-500">
              <span>重点问题 {{ r.keyProblemCount }} 条</span>
              <span>建议 {{ r.suggestionCount }} 条</span>
            </div>
          </div>
          <div class="text-xs text-brand-600 shrink-0 pt-0.5">查看 →</div>
        </div>
      </RouterLink>

      <!-- 分页 -->
      <div v-if="totalPages > 1" class="flex items-center justify-center gap-2 pt-2">
        <button
          type="button"
          :disabled="page <= 1"
          class="px-3 py-1 text-sm border border-slate-200 rounded text-slate-600 hover:bg-slate-50 disabled:opacity-50"
          @click="gotoPage(page - 1)"
        >
          上一页
        </button>
        <span class="text-sm text-slate-500">{{ page }} / {{ totalPages }}</span>
        <button
          type="button"
          :disabled="page >= totalPages"
          class="px-3 py-1 text-sm border border-slate-200 rounded text-slate-600 hover:bg-slate-50 disabled:opacity-50"
          @click="gotoPage(page + 1)"
        >
          下一页
        </button>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { listDiagnosisHistory } from '@/api/diagnosis'
import type { AiStatus, DiagnosisSummary } from '@/types/diagnosis'

const PAGE_SIZE = 10
const records = ref<DiagnosisSummary[]>([])
const total = ref(0)
const page = ref(1)
const loading = ref(true)
const loadError = ref('')

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / PAGE_SIZE)))

onMounted(load)

async function load() {
  loading.value = true
  loadError.value = ''
  try {
    const res = await listDiagnosisHistory({ page: page.value, size: PAGE_SIZE })
    records.value = res.records
    total.value = Number(res.total)
  } catch (e) {
    loadError.value = (e as Error).message || '加载失败'
  } finally {
    loading.value = false
  }
}

async function gotoPage(p: number) {
  if (p < 1 || p > totalPages.value) return
  page.value = p
  await load()
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

function historySummaryText(item: DiagnosisSummary) {
  if (item.stageSummaryExcerpt) return item.stageSummaryExcerpt
  if (item.aiStatus === 'FAILED') return '（本次 AI 失败，仅保留规则指标与复盘内容）'
  return '（无 AI 摘要）'
}
</script>
