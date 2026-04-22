<template>
  <div class="p-6 max-w-3xl mx-auto space-y-6">
    <!-- 顶部导航 -->
    <div class="flex items-center justify-between">
      <RouterLink to="/journal" class="text-sm text-brand-600 hover:underline">← 随记列表</RouterLink>
      <button
        v-if="canEdit"
        type="button"
        class="text-sm text-slate-600 hover:underline"
        @click="openEdit"
      >
        编辑原文
      </button>
    </div>

    <!-- 加载 / 错误 -->
    <div v-if="loading" class="text-sm text-slate-500">加载中…</div>
    <div v-else-if="loadError" class="text-sm text-red-600">{{ loadError }}</div>

    <template v-else-if="detail">
      <!-- 原文卡片 -->
      <section class="bg-white border border-slate-200 rounded-lg p-6 space-y-3">
        <div class="flex items-center gap-2 flex-wrap">
          <span class="text-xs text-slate-500">{{ formatDate(detail.journal.createdAt) }}</span>
          <span v-if="detail.journal.mood" class="text-xs px-1.5 py-0.5 rounded bg-slate-100 text-slate-600">{{ moodEmoji(detail.journal.mood) }}</span>
          <span class="text-xs text-slate-400">· {{ detail.journal.wordCount }} 字</span>
          <span v-if="detail.journal.status === 'ARCHIVED'" class="text-xs text-slate-400">· 已归档</span>
        </div>
        <div class="text-sm text-slate-800 whitespace-pre-wrap">{{ detail.journal.content }}</div>
        <div v-if="detail.journal.tags && detail.journal.tags.length" class="flex flex-wrap gap-1 pt-1">
          <span
            v-for="(t, i) in detail.journal.tags"
            :key="`t-${i}`"
            class="text-xs text-slate-500 bg-slate-100 px-1.5 py-0.5 rounded"
          >#{{ t }}</span>
        </div>
      </section>

      <!-- 抽取区 -->
      <section class="bg-white border border-slate-200 rounded-lg p-6 space-y-4">
        <div class="flex items-center justify-between gap-3">
          <h2 class="text-sm font-medium text-slate-700">AI 成长事件抽取</h2>
          <div class="flex items-center gap-2">
            <span v-if="extractionBadgeText" class="text-xs px-2 py-0.5 rounded" :class="extractionBadgeClass">
              {{ extractionBadgeText }}
            </span>
            <button
              v-if="canExtract"
              type="button"
              :disabled="extracting"
              class="px-3 py-1.5 rounded-md bg-brand-600 text-white text-sm hover:bg-brand-700 disabled:opacity-60"
              @click="onExtract"
            >
              {{ extracting ? 'AI 抽取中…（最长 30 秒）' : hasDraft ? '重新抽取' : 'AI 抽取草稿' }}
            </button>
          </div>
        </div>

        <div v-if="extractError" class="text-sm text-red-600">{{ extractError }}</div>

        <!-- 无草稿提示 -->
        <div v-if="!detail.extraction" class="text-sm text-slate-500">
          尚未触发 AI 抽取。点右上按钮生成草稿 —— 这是本随记允许的 1 次 AI 调用。
        </div>

        <!-- 草稿区：PENDING_CONFIRM -->
        <div v-else-if="detail.extraction.extractionStatus === 'PENDING_CONFIRM'" class="space-y-5">
          <div class="text-xs text-slate-500">
            草稿由 AI 产出，请确认后再写入画像与目标要求。你可以编辑或删除任何一条。
          </div>

          <!-- new skills -->
          <div class="space-y-2">
            <div class="flex items-center justify-between">
              <label class="text-sm text-slate-600">新技能</label>
              <button
                type="button"
                class="text-xs text-brand-600 hover:underline"
                @click="draftSkills.push({ name: '', level: 'BEGINNER', category: 'DOMAIN' })"
              >
                + 添加
              </button>
            </div>
            <div v-for="(s, i) in draftSkills" :key="`ns-${i}`" class="grid grid-cols-12 gap-2 items-center">
              <input
                v-model="s.name"
                type="text"
                maxlength="128"
                placeholder="技能名"
                class="col-span-5 px-3 py-1.5 border border-slate-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-brand-500"
              />
              <select
                v-model="s.level"
                class="col-span-3 px-2 py-1.5 border border-slate-300 rounded-md text-sm bg-white focus:outline-none focus:ring-2 focus:ring-brand-500"
              >
                <option value="BEGINNER">入门</option>
                <option value="INTERMEDIATE">进阶</option>
                <option value="ADVANCED">熟练</option>
              </select>
              <select
                v-model="s.category"
                class="col-span-3 px-2 py-1.5 border border-slate-300 rounded-md text-sm bg-white focus:outline-none focus:ring-2 focus:ring-brand-500"
              >
                <option value="LANGUAGE">LANGUAGE</option>
                <option value="FRAMEWORK">FRAMEWORK</option>
                <option value="TOOL">TOOL</option>
                <option value="DOMAIN">DOMAIN</option>
                <option value="SOFT">SOFT</option>
              </select>
              <button type="button" class="col-span-1 text-xs text-slate-400 hover:text-red-600" @click="draftSkills.splice(i, 1)">删除</button>
            </div>
            <div v-if="draftSkills.length === 0" class="text-xs text-slate-400">（AI 未识别到新技能）</div>
          </div>

          <!-- related requirements -->
          <div class="space-y-2">
            <label class="text-sm text-slate-600">相关目标要求的状态变更</label>
            <div v-for="(r, i) in draftReqs" :key="`rr-${i}`" class="border border-slate-200 rounded-md p-3 space-y-2">
              <div class="grid grid-cols-12 gap-2 items-center">
                <span class="col-span-3 text-xs text-slate-500">Requirement #{{ r.requirementId }}</span>
                <select
                  v-model="r.newStatus"
                  class="col-span-3 px-2 py-1.5 border border-slate-300 rounded-md text-sm bg-white focus:outline-none focus:ring-2 focus:ring-brand-500"
                >
                  <option value="">不更新</option>
                  <option value="TODO">TODO</option>
                  <option value="IN_PROGRESS">IN_PROGRESS</option>
                  <option value="MET">MET</option>
                </select>
                <button type="button" class="col-span-1 col-start-12 text-xs text-slate-400 hover:text-red-600" @click="draftReqs.splice(i, 1)">删除</button>
              </div>
              <textarea
                v-model="r.evidence"
                rows="2"
                maxlength="2000"
                placeholder="evidence / 证据（可选）"
                class="w-full px-3 py-1.5 border border-slate-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-brand-500"
              />
            </div>
            <div v-if="draftReqs.length === 0" class="text-xs text-slate-400">（AI 未识别到需要变更的要求）</div>
          </div>

          <!-- events (read-only display) -->
          <div v-if="draftEvents.length > 0" class="space-y-1">
            <label class="text-sm text-slate-600">识别到的事件</label>
            <div v-for="(ev, i) in draftEvents" :key="`ev-${i}`" class="text-sm border border-slate-200 rounded-md p-2">
              <div class="flex items-center gap-2">
                <span class="text-xs px-1.5 py-0.5 rounded bg-slate-100 text-slate-600">{{ ev.type || 'OTHER' }}</span>
                <span class="text-sm text-slate-800">{{ ev.title }}</span>
              </div>
              <div v-if="ev.outcome" class="text-xs text-slate-500 mt-1">{{ ev.outcome }}</div>
            </div>
          </div>

          <!-- blockers (editable chips) -->
          <div v-if="draftBlockers.length > 0 || true" class="space-y-1">
            <label class="text-sm text-slate-600">Blockers</label>
            <div class="flex flex-wrap gap-1.5">
              <span
                v-for="(b, i) in draftBlockers"
                :key="`b-${i}`"
                class="inline-flex items-center gap-1 px-2 py-0.5 rounded bg-amber-50 text-amber-700 text-xs border border-amber-100"
              >
                {{ b }}
                <button type="button" class="text-amber-400 hover:text-amber-700" @click="draftBlockers.splice(i, 1)">×</button>
              </span>
              <span v-if="draftBlockers.length === 0" class="text-xs text-slate-400">（无）</span>
            </div>
          </div>

          <!-- confirm actions -->
          <div class="flex items-center gap-3 pt-3 border-t border-slate-100">
            <button
              type="button"
              :disabled="confirming"
              class="px-3 py-1.5 rounded-md bg-brand-600 text-white text-sm hover:bg-brand-700 disabled:opacity-60"
              @click="onConfirm"
            >
              {{ confirming ? '入档中…' : '确认入档' }}
            </button>
            <span v-if="confirmError" class="text-sm text-red-600">{{ confirmError }}</span>
          </div>
        </div>

        <!-- 已确认：只读展示 confirmed_* -->
        <div v-else-if="detail.extraction.extractionStatus === 'CONFIRMED'" class="space-y-4">
          <div class="text-xs text-slate-500">
            已于 {{ formatDate(detail.extraction.confirmedAt) }} 入档；如需调整请写一条新的随记。
          </div>

          <div v-if="(detail.extraction.confirmedNewSkills ?? []).length > 0" class="space-y-1">
            <div class="text-xs text-slate-500">已写入的新技能</div>
            <div class="flex flex-wrap gap-1.5">
              <span
                v-for="(s, i) in (detail.extraction.confirmedNewSkills ?? [])"
                :key="`cs-${i}`"
                class="px-2 py-0.5 rounded bg-emerald-50 text-emerald-700 text-xs border border-emerald-100"
              >
                {{ (s.name as string) || '—' }}
                <span v-if="s.level" class="opacity-70">· {{ s.level }}</span>
              </span>
            </div>
          </div>

          <div v-if="(detail.extraction.confirmedRelatedRequirements ?? []).length > 0" class="space-y-1">
            <div class="text-xs text-slate-500">已应用的要求状态变更</div>
            <ul class="text-sm text-slate-700 list-disc pl-5 space-y-0.5">
              <li v-for="(r, i) in (detail.extraction.confirmedRelatedRequirements ?? [])" :key="`cr-${i}`">
                Requirement #{{ r.requirementId }} → {{ r.newStatus || '—' }}
              </li>
            </ul>
          </div>

          <div v-if="(detail.extraction.confirmedEvents ?? []).length > 0" class="space-y-1">
            <div class="text-xs text-slate-500">事件</div>
            <ul class="text-sm text-slate-700 list-disc pl-5 space-y-0.5">
              <li v-for="(ev, i) in (detail.extraction.confirmedEvents ?? [])" :key="`ce-${i}`">
                <span class="text-xs text-slate-500 mr-1">[{{ ev.type || 'OTHER' }}]</span>{{ ev.title }}
              </li>
            </ul>
          </div>
        </div>
      </section>
    </template>

    <!-- 编辑原文 modal -->
    <div
      v-if="editForm.open"
      class="fixed inset-0 bg-slate-900/40 flex items-center justify-center z-50 p-4"
      @click.self="closeEdit"
    >
      <div class="bg-white rounded-lg shadow-xl max-w-xl w-full p-6 space-y-4">
        <h3 class="text-base font-semibold text-slate-800">编辑随记</h3>
        <textarea
          v-model="editForm.content"
          rows="8"
          minlength="10"
          maxlength="10000"
          class="w-full px-3 py-2 border border-slate-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-brand-500"
        />
        <div class="grid grid-cols-2 gap-3">
          <select
            v-model="editForm.mood"
            class="px-3 py-2 border border-slate-300 rounded-md text-sm bg-white focus:outline-none focus:ring-2 focus:ring-brand-500"
          >
            <option value="">心情：—</option>
            <option value="GREAT">😊 很好</option>
            <option value="GOOD">🙂 不错</option>
            <option value="NORMAL">😐 一般</option>
            <option value="BAD">🙁 不佳</option>
            <option value="BLOCKED">😣 卡住了</option>
          </select>
          <input
            v-model="editForm.tagsInput"
            type="text"
            maxlength="200"
            class="px-3 py-2 border border-slate-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-brand-500"
            placeholder="标签（逗号分隔）"
          />
        </div>
        <div v-if="editForm.error" class="text-sm text-red-600">{{ editForm.error }}</div>
        <div class="flex justify-end gap-2 pt-2 border-t border-slate-100">
          <button type="button" class="px-3 py-1.5 rounded-md border border-slate-300 text-sm text-slate-600 hover:bg-slate-50" @click="closeEdit">取消</button>
          <button
            type="button"
            :disabled="editForm.submitting"
            class="px-3 py-1.5 rounded-md bg-brand-600 text-white text-sm hover:bg-brand-700 disabled:opacity-60"
            @click="submitEdit"
          >
            {{ editForm.submitting ? '保存中…' : '保存' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import {
  confirmExtraction,
  extractJournal,
  fetchJournal,
  updateJournal
} from '@/api/journal'
import type {
  ExtractionStatus,
  JournalDetailView,
  MoodCode,
  NewSkillConfirm,
  RequirementUpdateConfirm
} from '@/types/journal'

const route = useRoute()
const journalId = computed(() => Number(route.params.id))

const detail = ref<JournalDetailView | null>(null)
const loading = ref(true)
const loadError = ref('')

const extracting = ref(false)
const extractError = ref('')

const confirming = ref(false)
const confirmError = ref('')

const draftSkills = reactive<NewSkillConfirm[]>([])
const draftReqs = reactive<RequirementUpdateConfirm[]>([])
const draftEvents = reactive<Record<string, unknown>[]>([])
const draftBlockers = reactive<string[]>([])

const editForm = reactive({
  open: false,
  content: '',
  mood: '' as '' | MoodCode,
  tagsInput: '',
  submitting: false,
  error: ''
})

onMounted(load)
watch(journalId, (v, old) => { if (v !== old) void load() })

async function load() {
  loading.value = true
  loadError.value = ''
  try {
    detail.value = await fetchJournal(journalId.value)
    hydrateDraft()
  } catch (e) {
    loadError.value = (e as Error).message || '加载失败'
  } finally {
    loading.value = false
  }
}

function hydrateDraft() {
  draftSkills.splice(0, draftSkills.length)
  draftReqs.splice(0, draftReqs.length)
  draftEvents.splice(0, draftEvents.length)
  draftBlockers.splice(0, draftBlockers.length)

  const ex = detail.value?.extraction
  if (!ex || ex.extractionStatus !== 'PENDING_CONFIRM') return

  draftSkills.push(
    ...ex.draftNewSkills.map((raw) => ({
      name: String(raw.name ?? '').trim(),
      level: normalizeLevel(raw.level),
      category: normalizeCategory(raw.category),
      evidence: typeof raw.evidence === 'string' ? raw.evidence : ''
    }))
  )

  draftReqs.push(
    ...ex.draftRelatedRequirements
      .filter((raw) => raw && (raw.requirementId ?? raw.requirement_id) != null)
      .map((raw) => ({
        requirementId: Number(raw.requirementId ?? raw.requirement_id),
        newStatus: normalizeReqStatus(raw.newStatus ?? raw.new_status),
        evidence: typeof raw.evidence === 'string' ? raw.evidence : ''
      }))
  )

  draftEvents.push(...ex.draftEvents)
  draftBlockers.push(...(ex.draftBlockers || []))
}

async function onExtract() {
  extractError.value = ''
  extracting.value = true
  try {
    await extractJournal(journalId.value)
    await load()
  } catch (e) {
    extractError.value = (e as Error).message || 'AI 抽取失败'
  } finally {
    extracting.value = false
  }
}

async function onConfirm() {
  confirmError.value = ''
  confirming.value = true
  try {
    await confirmExtraction(journalId.value, {
      newSkills: draftSkills
        .filter((s) => s.name.trim() !== '')
        .map((s) => ({
          name: s.name.trim(),
          level: s.level,
          category: s.category || undefined,
          evidence: s.evidence?.trim() || undefined
        })),
      relatedRequirements: draftReqs
        .filter((r) => r.requirementId && (r.newStatus ?? '') !== '')
        .map((r) => ({
          requirementId: r.requirementId,
          newStatus: r.newStatus,
          evidence: r.evidence?.trim() || undefined
        })),
      events: draftEvents,
      blockers: draftBlockers
    })
    await load()
  } catch (e) {
    confirmError.value = (e as Error).message || '确认失败'
  } finally {
    confirming.value = false
  }
}

// ---- edit modal ----

function openEdit() {
  const j = detail.value?.journal
  if (!j) return
  editForm.open = true
  editForm.content = j.content
  editForm.mood = (j.mood as MoodCode | null) ?? ''
  editForm.tagsInput = (j.tags || []).join(', ')
  editForm.error = ''
}

function closeEdit() {
  editForm.open = false
}

async function submitEdit() {
  if (editForm.content.trim().length < 10) {
    editForm.error = '至少 10 字'
    return
  }
  editForm.submitting = true
  editForm.error = ''
  try {
    const tags = editForm.tagsInput.split(',').map((t) => t.trim()).filter((t) => t !== '')
    await updateJournal(journalId.value, {
      content: editForm.content.trim(),
      mood: editForm.mood || undefined,
      tags
    })
    closeEdit()
    await load()
  } catch (e) {
    editForm.error = (e as Error).message || '保存失败'
  } finally {
    editForm.submitting = false
  }
}

// ---- derived flags ----

const hasDraft = computed(() => detail.value?.extraction != null)
const canExtract = computed(() => {
  const s = detail.value?.extraction?.extractionStatus
  return s !== 'CONFIRMED'
})
const canEdit = computed(() => {
  const s = detail.value?.extraction?.extractionStatus
  return s !== 'CONFIRMED'
})

const extractionBadgeText = computed(() => {
  const s = detail.value?.extraction?.extractionStatus
  if (!s) return ''
  return { PENDING_CONFIRM: '草稿待确认', CONFIRMED: '已确认入档', DISCARDED: '已丢弃' }[s]
})

const extractionBadgeClass = computed(() => {
  const s = detail.value?.extraction?.extractionStatus
  if (!s) return ''
  return {
    PENDING_CONFIRM: 'bg-amber-50 text-amber-700 border border-amber-100',
    CONFIRMED: 'bg-emerald-50 text-emerald-700 border border-emerald-100',
    DISCARDED: 'bg-slate-100 text-slate-500 border border-slate-200'
  }[s]
})

function formatDate(iso: string | null | undefined) {
  if (!iso) return ''
  return iso.slice(0, 16).replace('T', ' ')
}

function moodEmoji(m: MoodCode) {
  return { GREAT: '😊', GOOD: '🙂', NORMAL: '😐', BAD: '🙁', BLOCKED: '😣' }[m] || '—'
}

function normalizeLevel(v: unknown): 'BEGINNER' | 'INTERMEDIATE' | 'ADVANCED' {
  const s = String(v ?? '').toUpperCase()
  if (s === 'INTERMEDIATE' || s === 'ADVANCED') return s
  return 'BEGINNER'
}

function normalizeCategory(v: unknown): '' | 'LANGUAGE' | 'FRAMEWORK' | 'TOOL' | 'DOMAIN' | 'SOFT' {
  const s = String(v ?? '').toUpperCase()
  if (s === 'LANGUAGE' || s === 'FRAMEWORK' || s === 'TOOL' || s === 'DOMAIN' || s === 'SOFT') return s
  return 'DOMAIN'
}

function normalizeReqStatus(v: unknown): '' | 'TODO' | 'IN_PROGRESS' | 'MET' {
  const s = String(v ?? '').toUpperCase()
  if (s === 'TODO' || s === 'IN_PROGRESS' || s === 'MET') return s
  return ''
}

type _ExtractionStatus = ExtractionStatus
</script>
