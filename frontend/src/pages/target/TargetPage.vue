<template>
  <div class="p-6 max-w-5xl mx-auto space-y-6">
    <header class="flex items-start justify-between gap-4">
      <div>
        <h1 class="text-2xl font-semibold text-slate-800">目标设定</h1>
        <p class="text-sm text-slate-500 mt-1">
          三类目标：求职 / 考研 / 技能成长。每个目标需要 ≥1 条要求；主目标同一时间至多一个。
        </p>
      </div>
      <button
        type="button"
        class="px-3 py-1.5 rounded-md bg-brand-600 text-white text-sm hover:bg-brand-700"
        @click="openCreate"
      >
        + 新建目标
      </button>
    </header>

    <!-- 状态过滤 -->
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
    </section>

    <div v-if="loading" class="text-sm text-slate-500">加载中…</div>
    <div v-else-if="loadError" class="text-sm text-red-600">{{ loadError }}</div>

    <div
      v-else-if="targets.length === 0"
      class="bg-white border border-slate-200 border-dashed rounded-lg p-10 text-center text-slate-400 text-sm"
    >
      还没有目标。点右上"新建目标"基于模板或手动创建。
    </div>

    <section v-else class="space-y-3">
      <div
        v-for="t in targets"
        :key="t.id"
        class="bg-white border rounded-lg p-4"
        :class="t.isPrimary ? 'border-brand-400 shadow-sm' : 'border-slate-200'"
      >
        <div class="flex items-start justify-between gap-3">
          <div class="min-w-0 flex-1">
            <div class="flex items-center gap-2 flex-wrap">
              <span v-if="t.isPrimary" class="text-xs px-1.5 py-0.5 rounded bg-brand-50 text-brand-700 border border-brand-200">主目标</span>
              <span class="text-xs px-1.5 py-0.5 rounded bg-slate-100 text-slate-600">{{ targetTypeLabel(t.targetType) }}</span>
              <span
                class="text-xs px-1.5 py-0.5 rounded"
                :class="statusBadgeClass(t.status)"
              >
                {{ targetStatusLabel(t.status) }}
              </span>
              <span v-if="t.deadline" class="text-xs text-slate-400">deadline {{ t.deadline }}</span>
            </div>
            <div class="text-base font-medium text-slate-800 mt-1.5 truncate">{{ t.title }}</div>
            <div v-if="t.description" class="text-sm text-slate-500 mt-1 line-clamp-2">{{ t.description }}</div>

            <div class="flex items-center gap-3 mt-2">
              <div class="flex-1 h-1.5 bg-slate-100 rounded-full overflow-hidden">
                <div
                  class="h-1.5 bg-brand-500 rounded-full"
                  :style="{ width: `${progressPercent(t)}%` }"
                />
              </div>
              <span class="text-xs text-slate-500">{{ t.requirementMetCount }} / {{ t.requirementCount }} 已达成</span>
            </div>
          </div>

          <div class="flex flex-col gap-1 shrink-0 text-sm">
            <button
              v-if="t.status === 'ACTIVE' && !t.isPrimary"
              type="button"
              class="text-brand-600 hover:underline"
              @click="onSetPrimary(t.id)"
            >
              设为主目标
            </button>
            <button
              type="button"
              class="text-slate-600 hover:underline"
              @click="toggleExpand(t.id)"
            >
              {{ expandedId === t.id ? '收起' : '展开' }}
            </button>
            <button
              type="button"
              class="text-slate-400 hover:text-red-600"
              @click="onDeleteTarget(t.id)"
            >
              删除
            </button>
          </div>
        </div>

        <!-- 展开区：requirements -->
        <div v-if="expandedId === t.id" class="mt-4 pt-4 border-t border-slate-100 space-y-3">
          <div v-if="expandedLoading" class="text-sm text-slate-500">加载要求…</div>
          <template v-else-if="expandedDetail">
            <!-- 目标级操作 -->
            <div class="flex items-center gap-2 flex-wrap">
              <select
                :value="expandedDetail.target.status"
                class="text-sm px-2 py-1 border border-slate-300 rounded-md bg-white focus:outline-none focus:ring-2 focus:ring-brand-500"
                @change="(e) => onChangeTargetStatus(t.id, (e.target as HTMLSelectElement).value)"
              >
                <option value="ACTIVE">ACTIVE</option>
                <option value="ACHIEVED">ACHIEVED</option>
                <option value="ABANDONED">ABANDONED</option>
              </select>
              <button
                type="button"
                class="text-sm text-brand-600 hover:underline"
                @click="openEditTarget(t.id)"
              >
                编辑目标
              </button>
              <button
                type="button"
                class="text-sm text-brand-600 hover:underline"
                @click="openAddRequirement(t.id)"
              >
                + 添加要求
              </button>
            </div>

            <!-- requirements list -->
            <div
              v-for="r in expandedDetail.requirements"
              :key="r.id"
              class="border border-slate-200 rounded-md p-3"
            >
              <div class="flex items-start justify-between gap-3">
                <div class="min-w-0 flex-1">
                  <div class="flex items-center gap-2 flex-wrap">
                    <span class="text-xs px-1.5 py-0.5 rounded bg-slate-100 text-slate-600">{{ r.reqType }}</span>
                    <span class="text-sm font-medium text-slate-800">{{ r.reqName }}</span>
                    <span v-if="r.dueDate" class="text-xs text-slate-400">due {{ r.dueDate }}</span>
                  </div>
                  <div v-if="r.description" class="text-xs text-slate-500 mt-1">{{ r.description }}</div>
                  <div class="flex items-center gap-2 mt-2">
                    <div class="flex-1 h-1 bg-slate-100 rounded-full overflow-hidden">
                      <div class="h-1 bg-brand-500 rounded-full" :style="{ width: `${r.progress}%` }" />
                    </div>
                    <span class="text-xs text-slate-500 w-16 text-right">{{ r.progress }}%</span>
                  </div>
                </div>
                <div class="flex flex-col items-end gap-1 shrink-0">
                  <select
                    :value="r.status"
                    class="text-xs px-2 py-1 border border-slate-300 rounded-md bg-white focus:outline-none focus:ring-2 focus:ring-brand-500"
                    @change="(e) => onChangeReqStatus(t.id, r.id, (e.target as HTMLSelectElement).value)"
                  >
                    <option value="TODO">TODO</option>
                    <option value="IN_PROGRESS">IN_PROGRESS</option>
                    <option value="MET">MET</option>
                  </select>
                  <div class="flex gap-2">
                    <button type="button" class="text-xs text-brand-600 hover:underline" @click="openEditRequirement(t.id, r)">编辑</button>
                    <button type="button" class="text-xs text-slate-400 hover:text-red-600" @click="onDeleteRequirement(t.id, r.id)">删除</button>
                  </div>
                </div>
              </div>
            </div>
            <div v-if="expandedDetail.requirements.length === 0" class="text-sm text-slate-400">（暂无要求）</div>
          </template>
        </div>
      </div>
    </section>

    <!-- 新建目标 modal -->
    <div
      v-if="createForm.open"
      class="fixed inset-0 bg-slate-900/40 flex items-center justify-center z-50 p-4"
      @click.self="closeCreate"
    >
      <div class="bg-white rounded-lg shadow-xl max-w-2xl w-full p-6 space-y-4 max-h-[90vh] overflow-y-auto">
        <h3 class="text-base font-semibold text-slate-800">新建目标</h3>

        <!-- 模板选择 -->
        <div>
          <label class="block text-sm text-slate-600 mb-1">基于模板（可选）</label>
          <div class="grid grid-cols-1 gap-2 max-h-60 overflow-y-auto">
            <button
              v-for="tpl in templates"
              :key="tpl.templateKey"
              type="button"
              class="text-left border rounded-md p-3 hover:bg-slate-50"
              :class="createForm.templateKey === tpl.templateKey ? 'border-brand-400 bg-brand-50' : 'border-slate-200'"
              @click="pickTemplate(tpl)"
            >
              <div class="flex items-center gap-2">
                <span class="text-xs px-1.5 py-0.5 rounded bg-slate-100 text-slate-600">{{ targetTypeLabel(tpl.targetType) }}</span>
                <span class="text-sm font-medium text-slate-800">{{ tpl.title }}</span>
              </div>
              <div v-if="tpl.description" class="text-xs text-slate-500 mt-1">{{ tpl.description }}</div>
              <div class="text-xs text-slate-400 mt-1">含 {{ tpl.defaultRequirements.length }} 条预置要求</div>
            </button>
            <button
              type="button"
              class="text-left border rounded-md p-3 hover:bg-slate-50"
              :class="createForm.templateKey === '' ? 'border-brand-400 bg-brand-50' : 'border-slate-200'"
              @click="pickTemplate(null)"
            >
              <span class="text-sm font-medium text-slate-800">纯手动（不选模板）</span>
              <div class="text-xs text-slate-500 mt-1">从零开始，自己写要求</div>
            </button>
          </div>
        </div>

        <!-- 目标字段 -->
        <div class="grid grid-cols-3 gap-3">
          <div>
            <label class="block text-sm text-slate-600 mb-1">类型</label>
            <select
              v-model="createForm.targetType"
              class="w-full px-3 py-2 border border-slate-300 rounded-md text-sm bg-white focus:outline-none focus:ring-2 focus:ring-brand-500"
            >
              <option value="JOB_SEEKING">求职</option>
              <option value="POSTGRAD">考研</option>
              <option value="SKILL_GROWTH">技能成长</option>
            </select>
          </div>
          <div class="col-span-2">
            <label class="block text-sm text-slate-600 mb-1">标题 <span class="text-red-500">*</span></label>
            <input
              v-model="createForm.title"
              type="text"
              maxlength="255"
              class="w-full px-3 py-2 border border-slate-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-brand-500"
            />
          </div>
        </div>

        <div>
          <label class="block text-sm text-slate-600 mb-1">描述</label>
          <textarea
            v-model="createForm.description"
            rows="2"
            maxlength="2000"
            class="w-full px-3 py-2 border border-slate-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-brand-500"
          />
        </div>

        <div class="grid grid-cols-2 gap-3">
          <div>
            <label class="block text-sm text-slate-600 mb-1">截止日期</label>
            <input
              v-model="createForm.deadline"
              type="date"
              class="w-full px-3 py-2 border border-slate-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-brand-500"
            />
          </div>
          <div class="flex items-end pb-1">
            <label class="inline-flex items-center gap-2 text-sm text-slate-600">
              <input type="checkbox" v-model="createForm.isPrimary" />
              设为主目标（会清零其他目标的 primary）
            </label>
          </div>
        </div>

        <!-- requirements 编辑区 -->
        <div class="space-y-2 pt-2 border-t border-slate-100">
          <div class="flex items-center justify-between">
            <label class="text-sm text-slate-600">要求（至少 1 条）</label>
            <button
              type="button"
              class="text-xs text-brand-600 hover:underline"
              @click="createForm.requirements.push({ reqName: '', reqType: 'SKILL', description: '' })"
            >
              + 添加
            </button>
          </div>
          <div
            v-for="(r, i) in createForm.requirements"
            :key="`cr-${i}`"
            class="grid grid-cols-12 gap-2 items-start"
          >
            <select
              v-model="r.reqType"
              class="col-span-3 px-2 py-1.5 border border-slate-300 rounded-md text-sm bg-white focus:outline-none focus:ring-2 focus:ring-brand-500"
            >
              <option value="SKILL">SKILL</option>
              <option value="KNOWLEDGE">KNOWLEDGE</option>
              <option value="EXPERIENCE">EXPERIENCE</option>
              <option value="OTHER">OTHER</option>
            </select>
            <input
              v-model="r.reqName"
              type="text"
              maxlength="255"
              placeholder="要求名称"
              class="col-span-8 px-3 py-1.5 border border-slate-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-brand-500"
            />
            <button type="button" class="col-span-1 text-xs text-slate-400 hover:text-red-600" @click="createForm.requirements.splice(i, 1)">删除</button>
            <textarea
              v-model="r.description"
              rows="1"
              maxlength="2000"
              placeholder="描述（可选）"
              class="col-span-12 px-3 py-1.5 border border-slate-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-brand-500"
            />
          </div>
          <div v-if="createForm.requirements.length === 0" class="text-xs text-slate-400">（暂无，至少加一条再提交）</div>
        </div>

        <div v-if="createForm.error" class="text-sm text-red-600">{{ createForm.error }}</div>

        <div class="flex justify-end gap-2 pt-2 border-t border-slate-100">
          <button type="button" class="px-3 py-1.5 rounded-md border border-slate-300 text-sm text-slate-600 hover:bg-slate-50" @click="closeCreate">取消</button>
          <button
            type="button"
            :disabled="createForm.submitting"
            class="px-3 py-1.5 rounded-md bg-brand-600 text-white text-sm hover:bg-brand-700 disabled:opacity-60"
            @click="submitCreate"
          >
            {{ createForm.submitting ? '创建中…' : '创建' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 编辑目标 modal -->
    <div
      v-if="editTargetForm.open"
      class="fixed inset-0 bg-slate-900/40 flex items-center justify-center z-50 p-4"
      @click.self="closeEditTarget"
    >
      <div class="bg-white rounded-lg shadow-xl max-w-lg w-full p-6 space-y-4">
        <h3 class="text-base font-semibold text-slate-800">编辑目标</h3>
        <div>
          <label class="block text-sm text-slate-600 mb-1">标题</label>
          <input
            v-model="editTargetForm.title"
            type="text"
            maxlength="255"
            class="w-full px-3 py-2 border border-slate-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-brand-500"
          />
        </div>
        <div>
          <label class="block text-sm text-slate-600 mb-1">描述</label>
          <textarea
            v-model="editTargetForm.description"
            rows="3"
            maxlength="2000"
            class="w-full px-3 py-2 border border-slate-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-brand-500"
          />
        </div>
        <div>
          <label class="block text-sm text-slate-600 mb-1">截止日期</label>
          <input
            v-model="editTargetForm.deadline"
            type="date"
            class="w-full px-3 py-2 border border-slate-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-brand-500"
          />
        </div>
        <div v-if="editTargetForm.error" class="text-sm text-red-600">{{ editTargetForm.error }}</div>
        <div class="flex justify-end gap-2 pt-2 border-t border-slate-100">
          <button type="button" class="px-3 py-1.5 rounded-md border border-slate-300 text-sm text-slate-600 hover:bg-slate-50" @click="closeEditTarget">取消</button>
          <button
            type="button"
            :disabled="editTargetForm.submitting"
            class="px-3 py-1.5 rounded-md bg-brand-600 text-white text-sm hover:bg-brand-700 disabled:opacity-60"
            @click="submitEditTarget"
          >
            {{ editTargetForm.submitting ? '保存中…' : '保存' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 编辑 / 新增要求 modal -->
    <div
      v-if="reqForm.open"
      class="fixed inset-0 bg-slate-900/40 flex items-center justify-center z-50 p-4"
      @click.self="closeRequirement"
    >
      <div class="bg-white rounded-lg shadow-xl max-w-lg w-full p-6 space-y-4">
        <h3 class="text-base font-semibold text-slate-800">{{ reqForm.reqId ? '编辑要求' : '新增要求' }}</h3>

        <div class="grid grid-cols-3 gap-3">
          <div>
            <label class="block text-sm text-slate-600 mb-1">类型</label>
            <select
              v-model="reqForm.reqType"
              class="w-full px-3 py-2 border border-slate-300 rounded-md text-sm bg-white focus:outline-none focus:ring-2 focus:ring-brand-500"
            >
              <option value="SKILL">SKILL</option>
              <option value="KNOWLEDGE">KNOWLEDGE</option>
              <option value="EXPERIENCE">EXPERIENCE</option>
              <option value="OTHER">OTHER</option>
            </select>
          </div>
          <div class="col-span-2">
            <label class="block text-sm text-slate-600 mb-1">名称</label>
            <input
              v-model="reqForm.reqName"
              type="text"
              maxlength="255"
              class="w-full px-3 py-2 border border-slate-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-brand-500"
            />
          </div>
        </div>

        <div>
          <label class="block text-sm text-slate-600 mb-1">描述</label>
          <textarea
            v-model="reqForm.description"
            rows="3"
            maxlength="2000"
            class="w-full px-3 py-2 border border-slate-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-brand-500"
          />
        </div>

        <div class="grid grid-cols-3 gap-3">
          <div>
            <label class="block text-sm text-slate-600 mb-1">状态</label>
            <select
              v-model="reqForm.status"
              class="w-full px-3 py-2 border border-slate-300 rounded-md text-sm bg-white focus:outline-none focus:ring-2 focus:ring-brand-500"
            >
              <option value="TODO">TODO</option>
              <option value="IN_PROGRESS">IN_PROGRESS</option>
              <option value="MET">MET</option>
            </select>
          </div>
          <div>
            <label class="block text-sm text-slate-600 mb-1">进度 %</label>
            <input
              v-model.number="reqForm.progress"
              type="number"
              min="0"
              max="100"
              class="w-full px-3 py-2 border border-slate-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-brand-500"
            />
          </div>
          <div>
            <label class="block text-sm text-slate-600 mb-1">截止日期</label>
            <input
              v-model="reqForm.dueDate"
              type="date"
              class="w-full px-3 py-2 border border-slate-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-brand-500"
            />
          </div>
        </div>

        <div v-if="reqForm.error" class="text-sm text-red-600">{{ reqForm.error }}</div>

        <div class="flex justify-end gap-2 pt-2 border-t border-slate-100">
          <button type="button" class="px-3 py-1.5 rounded-md border border-slate-300 text-sm text-slate-600 hover:bg-slate-50" @click="closeRequirement">取消</button>
          <button
            type="button"
            :disabled="reqForm.submitting"
            class="px-3 py-1.5 rounded-md bg-brand-600 text-white text-sm hover:bg-brand-700 disabled:opacity-60"
            @click="submitRequirement"
          >
            {{ reqForm.submitting ? '保存中…' : '保存' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import {
  addRequirement,
  createTarget,
  deleteRequirement,
  deleteTarget,
  fetchTargetDetail,
  fetchTemplates,
  listTargets,
  setPrimaryTarget,
  updateRequirement,
  updateRequirementStatus,
  updateTarget,
  updateTargetStatus
} from '@/api/target'
import type {
  RequirementPayload,
  RequirementStatus,
  RequirementType,
  RequirementView,
  TargetDetailView,
  TargetStatus,
  TargetTemplateVO,
  TargetType,
  TargetView
} from '@/types/target'

const targets = ref<TargetView[]>([])
const templates = ref<TargetTemplateVO[]>([])
const loading = ref(true)
const loadError = ref('')
const filterStatus = ref<'' | TargetStatus>('')

const expandedId = ref<number | null>(null)
const expandedDetail = ref<TargetDetailView | null>(null)
const expandedLoading = ref(false)

const statusFilterOptions = [
  { value: '' as const, label: '全部' },
  { value: 'ACTIVE' as const, label: 'ACTIVE' },
  { value: 'ACHIEVED' as const, label: 'ACHIEVED' },
  { value: 'ABANDONED' as const, label: 'ABANDONED' }
]

const createForm = reactive({
  open: false,
  templateKey: '' as string,
  targetType: 'JOB_SEEKING' as TargetType,
  title: '',
  description: '',
  deadline: '',
  isPrimary: false,
  requirements: [] as RequirementPayload[],
  submitting: false,
  error: ''
})

const editTargetForm = reactive({
  open: false,
  id: 0,
  title: '',
  description: '',
  deadline: '',
  submitting: false,
  error: ''
})

const reqForm = reactive({
  open: false,
  targetId: 0,
  reqId: null as number | null,
  reqType: 'SKILL' as RequirementType,
  reqName: '',
  description: '',
  status: 'TODO' as RequirementStatus,
  progress: 0,
  dueDate: '',
  submitting: false,
  error: ''
})

onMounted(async () => {
  await Promise.all([loadTargets(), loadTemplates()])
})

async function loadTargets() {
  loading.value = true
  loadError.value = ''
  try {
    targets.value = await listTargets({ status: filterStatus.value === '' ? undefined : filterStatus.value })
  } catch (e) {
    loadError.value = (e as Error).message || '加载失败'
  } finally {
    loading.value = false
  }
}

async function loadTemplates() {
  try {
    templates.value = await fetchTemplates()
  } catch {
    templates.value = []
  }
}

function onFilter(v: '' | TargetStatus) {
  if (filterStatus.value === v) return
  filterStatus.value = v
  void loadTargets()
}

async function toggleExpand(id: number) {
  if (expandedId.value === id) {
    expandedId.value = null
    expandedDetail.value = null
    return
  }
  expandedId.value = id
  expandedDetail.value = null
  expandedLoading.value = true
  try {
    expandedDetail.value = await fetchTargetDetail(id)
  } catch (e) {
    loadError.value = (e as Error).message || '加载详情失败'
    expandedId.value = null
  } finally {
    expandedLoading.value = false
  }
}

async function refreshExpanded() {
  if (expandedId.value == null) return
  expandedDetail.value = await fetchTargetDetail(expandedId.value)
}

// ------ target-level actions ------

async function onSetPrimary(id: number) {
  try {
    await setPrimaryTarget(id)
    await loadTargets()
  } catch (e) {
    loadError.value = (e as Error).message || '设置失败'
  }
}

async function onDeleteTarget(id: number) {
  if (!window.confirm('删除目标会连同其要求一起删掉，确认？')) return
  try {
    await deleteTarget(id)
    if (expandedId.value === id) {
      expandedId.value = null
      expandedDetail.value = null
    }
    await loadTargets()
  } catch (e) {
    loadError.value = (e as Error).message || '删除失败'
  }
}

async function onChangeTargetStatus(id: number, status: string) {
  if (!['ACTIVE', 'ACHIEVED', 'ABANDONED'].includes(status)) return
  try {
    await updateTargetStatus(id, { status: status as TargetStatus })
    await loadTargets()
    await refreshExpanded()
  } catch (e) {
    loadError.value = (e as Error).message || '状态更新失败'
  }
}

// ------ create modal ------

function openCreate() {
  createForm.open = true
  createForm.templateKey = ''
  createForm.targetType = 'JOB_SEEKING'
  createForm.title = ''
  createForm.description = ''
  createForm.deadline = ''
  createForm.isPrimary = false
  createForm.requirements = []
  createForm.error = ''
}

function closeCreate() {
  createForm.open = false
}

function pickTemplate(tpl: TargetTemplateVO | null) {
  if (!tpl) {
    createForm.templateKey = ''
    return
  }
  createForm.templateKey = tpl.templateKey
  createForm.targetType = tpl.targetType
  createForm.title = tpl.title
  createForm.description = tpl.description ?? ''
  createForm.requirements = tpl.defaultRequirements.map((r) => ({
    reqName: r.reqName,
    reqType: r.reqType,
    description: r.description ?? ''
  }))
}

async function submitCreate() {
  if (!createForm.title.trim()) {
    createForm.error = '标题不能为空'
    return
  }
  if (createForm.requirements.length === 0) {
    createForm.error = '至少需要 1 条要求'
    return
  }
  createForm.submitting = true
  createForm.error = ''
  try {
    await createTarget({
      targetType: createForm.targetType,
      title: createForm.title.trim(),
      description: createForm.description.trim() || undefined,
      templateKey: createForm.templateKey || undefined,
      deadline: createForm.deadline || undefined,
      isPrimary: createForm.isPrimary,
      requirements: createForm.requirements
        .filter((r) => r.reqName.trim() !== '')
        .map((r) => ({
          reqName: r.reqName.trim(),
          reqType: r.reqType,
          description: r.description?.trim() || undefined
        }))
    })
    closeCreate()
    await loadTargets()
  } catch (e) {
    createForm.error = (e as Error).message || '创建失败'
  } finally {
    createForm.submitting = false
  }
}

// ------ edit target modal ------

function openEditTarget(id: number) {
  const d = expandedDetail.value
  if (!d || d.target.id !== id) return
  editTargetForm.open = true
  editTargetForm.id = id
  editTargetForm.title = d.target.title
  editTargetForm.description = d.target.description ?? ''
  editTargetForm.deadline = d.target.deadline ?? ''
  editTargetForm.error = ''
}

function closeEditTarget() {
  editTargetForm.open = false
}

async function submitEditTarget() {
  if (!editTargetForm.title.trim()) {
    editTargetForm.error = '标题不能为空'
    return
  }
  editTargetForm.submitting = true
  editTargetForm.error = ''
  try {
    await updateTarget(editTargetForm.id, {
      title: editTargetForm.title.trim(),
      description: editTargetForm.description.trim() || undefined,
      deadline: editTargetForm.deadline || undefined
    })
    closeEditTarget()
    await loadTargets()
    await refreshExpanded()
  } catch (e) {
    editTargetForm.error = (e as Error).message || '保存失败'
  } finally {
    editTargetForm.submitting = false
  }
}

// ------ requirement modal ------

function openAddRequirement(targetId: number) {
  reqForm.open = true
  reqForm.targetId = targetId
  reqForm.reqId = null
  reqForm.reqType = 'SKILL'
  reqForm.reqName = ''
  reqForm.description = ''
  reqForm.status = 'TODO'
  reqForm.progress = 0
  reqForm.dueDate = ''
  reqForm.error = ''
}

function openEditRequirement(targetId: number, r: RequirementView) {
  reqForm.open = true
  reqForm.targetId = targetId
  reqForm.reqId = r.id
  reqForm.reqType = r.reqType
  reqForm.reqName = r.reqName
  reqForm.description = r.description ?? ''
  reqForm.status = r.status
  reqForm.progress = r.progress
  reqForm.dueDate = r.dueDate ?? ''
  reqForm.error = ''
}

function closeRequirement() {
  reqForm.open = false
}

async function submitRequirement() {
  if (!reqForm.reqName.trim()) {
    reqForm.error = '名称不能为空'
    return
  }
  reqForm.submitting = true
  reqForm.error = ''
  try {
    const payload: RequirementPayload = {
      reqName: reqForm.reqName.trim(),
      reqType: reqForm.reqType,
      description: reqForm.description.trim() || undefined,
      status: reqForm.status,
      progress: clamp(reqForm.progress, 0, 100),
      dueDate: reqForm.dueDate || undefined
    }
    if (reqForm.reqId == null) {
      await addRequirement(reqForm.targetId, payload)
    } else {
      await updateRequirement(reqForm.targetId, reqForm.reqId, payload)
    }
    closeRequirement()
    await refreshExpanded()
    await loadTargets()
  } catch (e) {
    reqForm.error = (e as Error).message || '保存失败'
  } finally {
    reqForm.submitting = false
  }
}

async function onChangeReqStatus(targetId: number, reqId: number, status: string) {
  if (!['TODO', 'IN_PROGRESS', 'MET'].includes(status)) return
  try {
    await updateRequirementStatus(targetId, reqId, { status: status as RequirementStatus })
    await refreshExpanded()
    await loadTargets()
  } catch (e) {
    loadError.value = (e as Error).message || '状态更新失败'
  }
}

async function onDeleteRequirement(targetId: number, reqId: number) {
  if (!window.confirm('删除这条要求？')) return
  try {
    await deleteRequirement(targetId, reqId)
    await refreshExpanded()
    await loadTargets()
  } catch (e) {
    loadError.value = (e as Error).message || '删除失败'
  }
}

// ------ helpers ------

function progressPercent(t: TargetView): number {
  if (t.requirementCount === 0) return 0
  return Math.round((t.requirementMetCount / t.requirementCount) * 100)
}

function targetTypeLabel(v: TargetType) {
  return { JOB_SEEKING: '求职', POSTGRAD: '考研', SKILL_GROWTH: '技能成长' }[v]
}
function targetStatusLabel(v: TargetStatus) {
  return { ACTIVE: '进行中', ACHIEVED: '已达成', ABANDONED: '已放弃' }[v]
}
function statusBadgeClass(v: TargetStatus) {
  return {
    ACTIVE: 'bg-emerald-50 text-emerald-700 border border-emerald-100',
    ACHIEVED: 'bg-brand-50 text-brand-700 border border-brand-200',
    ABANDONED: 'bg-slate-100 text-slate-500 border border-slate-200'
  }[v]
}
function clamp(n: number | undefined, lo: number, hi: number) {
  const v = typeof n === 'number' ? n : 0
  return Math.min(hi, Math.max(lo, v))
}
</script>
