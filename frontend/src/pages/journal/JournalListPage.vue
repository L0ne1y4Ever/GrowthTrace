<template>
  <div class="p-6 max-w-5xl mx-auto space-y-6">
    <header class="flex items-start justify-between gap-4">
      <div>
        <h1 class="text-2xl font-semibold text-slate-800">成长随记</h1>
        <p class="text-sm text-slate-500 mt-1">
          自由文本记录 → AI 抽取成长事件草稿 → 用户确认归档。
        </p>
      </div>
      <button
        type="button"
        class="px-3 py-1.5 rounded-md bg-brand-600 text-white text-sm hover:bg-brand-700"
        @click="openCreate"
      >
        + 写一条随记
      </button>
    </header>

    <!-- 过滤条 -->
    <section class="bg-white border border-slate-200 rounded-lg px-4 py-3 flex items-center gap-3">
      <span class="text-sm text-slate-600">状态：</span>
      <button
        v-for="opt in statusFilterOptions"
        :key="opt.value"
        type="button"
        class="text-sm px-2.5 py-1 rounded-md border"
        :class="filterStatus === opt.value
          ? 'border-brand-500 bg-brand-50 text-brand-700'
          : 'border-slate-200 text-slate-600 hover:bg-slate-50'"
        @click="onFilter(opt.value)"
      >
        {{ opt.label }}
      </button>
      <div class="flex-1" />
      <span v-if="!loading" class="text-xs text-slate-400">共 {{ total }} 条</span>
    </section>

    <!-- 列表 -->
    <div v-if="loading" class="text-sm text-slate-500">加载中…</div>
    <div v-else-if="loadError" class="text-sm text-red-600">{{ loadError }}</div>

    <div v-else-if="records.length === 0" class="bg-white border border-slate-200 border-dashed rounded-lg p-10 text-center text-slate-400 text-sm">
      还没有随记。点上方"写一条随记"开始记录。
    </div>

    <section v-else class="space-y-3">
      <RouterLink
        v-for="j in records"
        :key="j.id"
        :to="`/journal/${j.id}`"
        class="block bg-white border border-slate-200 rounded-lg p-4 hover:border-brand-300 transition-colors"
      >
        <div class="flex items-start justify-between gap-3">
          <div class="min-w-0 flex-1">
            <div class="flex items-center gap-2 flex-wrap">
              <span class="text-xs text-slate-500">{{ formatDate(j.createdAt) }}</span>
              <span v-if="j.mood" class="text-xs px-1.5 py-0.5 rounded bg-slate-100 text-slate-600">{{ moodLabel(j.mood) }}</span>
              <span
                v-if="j.extractionStatus"
                class="text-xs px-1.5 py-0.5 rounded"
                :class="extractionBadgeClass(j.extractionStatus)"
              >
                {{ extractionLabel(j.extractionStatus) }}
              </span>
              <span v-if="j.status === 'ARCHIVED'" class="text-xs text-slate-400">· 已归档</span>
              <span class="text-xs text-slate-400">· {{ j.wordCount }} 字</span>
            </div>
            <div class="text-sm text-slate-800 mt-1.5 line-clamp-3">{{ j.contentExcerpt }}</div>
            <div v-if="j.tags && j.tags.length" class="flex flex-wrap gap-1 mt-2">
              <span
                v-for="(t, i) in j.tags"
                :key="`t-${j.id}-${i}`"
                class="text-xs text-slate-500 bg-slate-100 px-1.5 py-0.5 rounded"
              >#{{ t }}</span>
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

    <!-- 创建弹窗 -->
    <div
      v-if="createForm.open"
      class="fixed inset-0 bg-slate-900/40 flex items-center justify-center z-50 p-4"
      @click.self="closeCreate"
    >
      <div class="bg-white rounded-lg shadow-xl max-w-xl w-full p-6 space-y-4">
        <h3 class="text-base font-semibold text-slate-800">写一条随记</h3>

        <div>
          <label class="block text-sm text-slate-600 mb-1">内容 <span class="text-red-500">*</span></label>
          <textarea
            v-model="createForm.content"
            rows="8"
            minlength="10"
            maxlength="10000"
            class="w-full px-3 py-2 border border-slate-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-brand-500"
            placeholder="写下今天做了什么、学到什么、卡在哪……"
          />
          <div class="text-xs text-slate-400 mt-1">{{ createForm.content.length }} / 10000</div>
        </div>

        <div class="grid grid-cols-2 gap-3">
          <div>
            <label class="block text-sm text-slate-600 mb-1">心情</label>
            <select
              v-model="createForm.mood"
              class="w-full px-3 py-2 border border-slate-300 rounded-md text-sm bg-white focus:outline-none focus:ring-2 focus:ring-brand-500"
            >
              <option value="">—</option>
              <option value="GREAT">😊 很好</option>
              <option value="GOOD">🙂 不错</option>
              <option value="NORMAL">😐 一般</option>
              <option value="BAD">🙁 不佳</option>
              <option value="BLOCKED">😣 卡住了</option>
            </select>
          </div>
          <div>
            <label class="block text-sm text-slate-600 mb-1">标签（逗号分隔）</label>
            <input
              v-model="createForm.tagsInput"
              type="text"
              maxlength="200"
              class="w-full px-3 py-2 border border-slate-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-brand-500"
              placeholder="algorithm, review"
            />
          </div>
        </div>

        <div v-if="createForm.error" class="text-sm text-red-600">{{ createForm.error }}</div>

        <div class="flex justify-end gap-2 pt-2 border-t border-slate-100">
          <button
            type="button"
            class="px-3 py-1.5 rounded-md border border-slate-300 text-sm text-slate-600 hover:bg-slate-50"
            @click="closeCreate"
          >
            取消
          </button>
          <button
            type="button"
            :disabled="createForm.submitting || createForm.content.trim().length < 10"
            class="px-3 py-1.5 rounded-md bg-brand-600 text-white text-sm hover:bg-brand-700 disabled:opacity-60"
            @click="submitCreate"
          >
            {{ createForm.submitting ? '提交中…' : '提交（后续在详情页触发 AI 抽取）' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { createJournal, listJournals } from '@/api/journal'
import type { ExtractionStatus, JournalSummary, MoodCode } from '@/types/journal'

const router = useRouter()

const PAGE_SIZE = 10
const records = ref<JournalSummary[]>([])
const total = ref(0)
const page = ref(1)
const loading = ref(true)
const loadError = ref('')
const filterStatus = ref<'POSTED' | 'ARCHIVED' | ''>('')

const statusFilterOptions = [
  { value: '' as const, label: '全部' },
  { value: 'POSTED' as const, label: 'POSTED' },
  { value: 'ARCHIVED' as const, label: 'ARCHIVED' }
]

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / PAGE_SIZE)))

const createForm = reactive({
  open: false,
  content: '',
  mood: '' as '' | MoodCode,
  tagsInput: '',
  submitting: false,
  error: ''
})

onMounted(load)

async function load() {
  loading.value = true
  loadError.value = ''
  try {
    const res = await listJournals({
      page: page.value,
      size: PAGE_SIZE,
      status: filterStatus.value === '' ? undefined : filterStatus.value
    })
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

function onFilter(v: 'POSTED' | 'ARCHIVED' | '') {
  if (filterStatus.value === v) return
  filterStatus.value = v
  page.value = 1
  void load()
}

function openCreate() {
  createForm.open = true
  createForm.content = ''
  createForm.mood = ''
  createForm.tagsInput = ''
  createForm.error = ''
}

function closeCreate() {
  createForm.open = false
}

async function submitCreate() {
  if (createForm.content.trim().length < 10) {
    createForm.error = '至少 10 字'
    return
  }
  createForm.submitting = true
  createForm.error = ''
  try {
    const tags = createForm.tagsInput
      .split(',')
      .map((t) => t.trim())
      .filter((t) => t !== '')
    const created = await createJournal({
      content: createForm.content.trim(),
      mood: createForm.mood || undefined,
      tags
    })
    closeCreate()
    router.push(`/journal/${created.id}`)
  } catch (e) {
    createForm.error = (e as Error).message || '提交失败'
  } finally {
    createForm.submitting = false
  }
}

function formatDate(iso: string | undefined) {
  if (!iso) return ''
  // 后端以 "yyyy-MM-dd HH:mm:ss" 或 ISO-8601 返回；取前 16 位作为显示。
  return iso.slice(0, 16).replace('T', ' ')
}

function moodLabel(m: MoodCode | null | undefined) {
  return { GREAT: '😊', GOOD: '🙂', NORMAL: '😐', BAD: '🙁', BLOCKED: '😣' }[m ?? 'NORMAL'] || '—'
}

function extractionLabel(s: ExtractionStatus) {
  return { PENDING_CONFIRM: '草稿待确认', CONFIRMED: '已确认', DISCARDED: '已丢弃' }[s]
}

function extractionBadgeClass(s: ExtractionStatus) {
  return {
    PENDING_CONFIRM: 'bg-amber-50 text-amber-700 border border-amber-100',
    CONFIRMED: 'bg-emerald-50 text-emerald-700 border border-emerald-100',
    DISCARDED: 'bg-slate-100 text-slate-500 border border-slate-200'
  }[s]
}
</script>
