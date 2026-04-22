<template>
  <div class="p-6 max-w-6xl mx-auto space-y-6">
    <header class="flex items-start justify-between gap-4">
      <div>
        <h1 class="text-2xl font-semibold text-slate-800">成长执行</h1>
        <p class="text-sm text-slate-500 mt-1">
          任务是 V1 的执行核心（不保留 growth_plan）。打卡 = 追加日期 + 计数 +1；首次打卡自动从 TODO 切到 IN_PROGRESS。
        </p>
      </div>
      <button
        type="button"
        class="px-3 py-1.5 rounded-md bg-brand-600 text-white text-sm hover:bg-brand-700"
        @click="openCreate"
      >
        + 新建任务
      </button>
    </header>

    <!-- 本周进度 -->
    <section class="bg-white border border-slate-200 rounded-lg p-5 space-y-3">
      <div class="flex items-center justify-between">
        <div>
          <div class="text-sm font-medium text-slate-700">本周进度</div>
          <div class="text-xs text-slate-400">
            <template v-if="weekly">{{ weekly.weekStart }} ~ {{ weekly.weekEnd }}</template>
            <template v-else>加载中…</template>
          </div>
        </div>
        <div v-if="weekly" class="text-right">
          <div class="text-xl font-semibold text-brand-600">
            {{ weekly.doneThisWeek }} / {{ weekly.dueThisWeek }}
          </div>
          <div class="text-xs text-slate-400">本周到期完成率 {{ formatPercent(weekly.completionRate) }}</div>
        </div>
      </div>

      <div v-if="weekly" class="flex items-end gap-2 h-16">
        <div
          v-for="p in weekly.checkInPerDay"
          :key="p.date"
          class="flex-1 flex flex-col items-center justify-end gap-1"
        >
          <div
            class="w-full bg-brand-500/80 rounded-sm"
            :style="{ height: `${Math.min(100, p.count * 25)}%` }"
            :title="`${p.date} · ${p.count} 次打卡`"
          />
          <div class="text-[10px] text-slate-400">{{ formatWeekday(p.date) }}</div>
        </div>
      </div>
    </section>

    <!-- 过滤条 -->
    <section class="bg-white border border-slate-200 rounded-lg px-4 py-3 flex items-center gap-3 flex-wrap">
      <span class="text-sm text-slate-600">可见状态：</span>
      <label class="inline-flex items-center gap-1 text-sm text-slate-600">
        <input type="checkbox" v-model="showAbandoned" />
        显示已放弃
      </label>
      <div class="flex-1" />
      <span v-if="!loading" class="text-xs text-slate-400">共 {{ totalVisible }} 条</span>
    </section>

    <div v-if="loading" class="text-sm text-slate-500">加载中…</div>
    <div v-else-if="loadError" class="text-sm text-red-600">{{ loadError }}</div>

    <!-- Kanban 看板 -->
    <section v-else class="grid grid-cols-1 md:grid-cols-3 gap-4">
      <KanbanColumn
        title="待办 (TODO)"
        accent="slate"
        :tasks="byStatus('TODO')"
        :open-detail="openDetail"
        :check-in="onCheckIn"
        :change-status="onChangeStatus"
      />
      <KanbanColumn
        title="进行中 (IN_PROGRESS)"
        accent="brand"
        :tasks="byStatus('IN_PROGRESS')"
        :open-detail="openDetail"
        :check-in="onCheckIn"
        :change-status="onChangeStatus"
      />
      <KanbanColumn
        title="已完成 (DONE)"
        accent="emerald"
        :tasks="byStatus('DONE')"
        :open-detail="openDetail"
        :check-in="onCheckIn"
        :change-status="onChangeStatus"
      />
    </section>

    <section v-if="!loading && showAbandoned" class="space-y-3">
      <div class="text-sm text-slate-500">已放弃</div>
      <div
        v-for="t in byStatus('ABANDONED')"
        :key="t.id"
        class="bg-white border border-slate-200 border-dashed rounded-md p-3 flex items-center justify-between"
      >
        <div class="min-w-0">
          <div class="text-sm text-slate-500 line-through truncate">{{ t.title }}</div>
        </div>
        <div class="flex gap-2 text-xs">
          <button type="button" class="text-brand-600 hover:underline" @click="onChangeStatus(t, 'TODO')">
            恢复
          </button>
          <button type="button" class="text-slate-400 hover:text-red-600" @click="onDelete(t)">
            删除
          </button>
        </div>
      </div>
      <div v-if="byStatus('ABANDONED').length === 0" class="text-xs text-slate-400">（无）</div>
    </section>

    <!-- 创建/编辑 modal -->
    <div
      v-if="formState.open"
      class="fixed inset-0 bg-slate-900/40 flex items-center justify-center z-50 p-4"
      @click.self="closeForm"
    >
      <div class="bg-white rounded-lg shadow-xl max-w-lg w-full p-6 space-y-4 max-h-[90vh] overflow-y-auto">
        <h3 class="text-base font-semibold text-slate-800">
          {{ formState.id ? '编辑任务' : '新建任务' }}
        </h3>

        <div>
          <label class="block text-sm text-slate-600 mb-1">标题 <span class="text-red-500">*</span></label>
          <input
            v-model="formState.title"
            type="text"
            maxlength="255"
            class="w-full px-3 py-2 border border-slate-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-brand-500"
          />
        </div>

        <div>
          <label class="block text-sm text-slate-600 mb-1">描述</label>
          <textarea
            v-model="formState.description"
            rows="3"
            maxlength="2000"
            class="w-full px-3 py-2 border border-slate-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-brand-500"
          />
        </div>

        <div class="grid grid-cols-3 gap-3">
          <div>
            <label class="block text-sm text-slate-600 mb-1">优先级</label>
            <select
              v-model="formState.priority"
              class="w-full px-3 py-2 border border-slate-300 rounded-md text-sm bg-white focus:outline-none focus:ring-2 focus:ring-brand-500"
            >
              <option value="HIGH">HIGH</option>
              <option value="MEDIUM">MEDIUM</option>
              <option value="LOW">LOW</option>
            </select>
          </div>
          <div>
            <label class="block text-sm text-slate-600 mb-1">截止日期</label>
            <input
              v-model="formState.dueDate"
              type="date"
              class="w-full px-3 py-2 border border-slate-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-brand-500"
            />
          </div>
          <div>
            <label class="block text-sm text-slate-600 mb-1">计划分钟</label>
            <input
              v-model.number="formState.plannedEffortMinutes"
              type="number"
              min="0"
              max="100000"
              class="w-full px-3 py-2 border border-slate-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-brand-500"
            />
          </div>
        </div>

        <div class="grid grid-cols-2 gap-3">
          <div>
            <label class="block text-sm text-slate-600 mb-1">关联目标</label>
            <select
              v-model.number="formState.targetId"
              class="w-full px-3 py-2 border border-slate-300 rounded-md text-sm bg-white focus:outline-none focus:ring-2 focus:ring-brand-500"
            >
              <option :value="null">—</option>
              <option v-for="t in targetOptions" :key="t.id" :value="t.id">
                [{{ t.targetType }}] {{ t.title }}
              </option>
            </select>
          </div>
          <div>
            <label class="block text-sm text-slate-600 mb-1">关联要求</label>
            <select
              v-model.number="formState.requirementId"
              :disabled="!formState.targetId"
              class="w-full px-3 py-2 border border-slate-300 rounded-md text-sm bg-white focus:outline-none focus:ring-2 focus:ring-brand-500 disabled:bg-slate-50"
            >
              <option :value="null">—</option>
              <option v-for="r in requirementOptionsForForm" :key="r.id" :value="r.id">
                [{{ r.reqType }}] {{ r.reqName }}
              </option>
            </select>
          </div>
        </div>

        <div v-if="formState.error" class="text-sm text-red-600">{{ formState.error }}</div>

        <div class="flex justify-between pt-2 border-t border-slate-100">
          <div>
            <button
              v-if="formState.id"
              type="button"
              class="text-sm text-slate-400 hover:text-red-600"
              @click="onAbandon"
            >
              放弃此任务
            </button>
          </div>
          <div class="flex gap-2">
            <button
              type="button"
              class="px-3 py-1.5 rounded-md border border-slate-300 text-sm text-slate-600 hover:bg-slate-50"
              @click="closeForm"
            >
              取消
            </button>
            <button
              type="button"
              :disabled="formState.submitting"
              class="px-3 py-1.5 rounded-md bg-brand-600 text-white text-sm hover:bg-brand-700 disabled:opacity-60"
              @click="submitForm"
            >
              {{ formState.submitting ? '保存中…' : '保存' }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import KanbanColumn from '@/components/execution/KanbanColumn.vue'
import {
  checkInTask,
  createTask,
  deleteTask,
  fetchWeeklyProgress,
  listTasks,
  updateTask,
  updateTaskStatus
} from '@/api/execution'
import { fetchTargetDetail, listTargets } from '@/api/target'
import type {
  TaskPriority,
  TaskStatus,
  TaskView,
  WeeklyProgressView
} from '@/types/execution'
import type { RequirementView, TargetView } from '@/types/target'

const tasks = ref<TaskView[]>([])
const weekly = ref<WeeklyProgressView | null>(null)
const loading = ref(true)
const loadError = ref('')
const showAbandoned = ref(false)

const targetOptions = ref<TargetView[]>([])
const requirementCache = ref<Record<number, RequirementView[]>>({})

const totalVisible = computed(() =>
  tasks.value.filter((t) => showAbandoned.value || t.status !== 'ABANDONED').length
)

const formState = reactive({
  open: false,
  id: null as number | null,
  title: '',
  description: '',
  priority: 'MEDIUM' as TaskPriority,
  dueDate: '',
  plannedEffortMinutes: null as number | null,
  targetId: null as number | null,
  requirementId: null as number | null,
  submitting: false,
  error: ''
})

const requirementOptionsForForm = computed(() => {
  const tid = formState.targetId
  if (!tid) return []
  return requirementCache.value[tid] ?? []
})

watch(() => formState.targetId, async (newId) => {
  formState.requirementId = null
  if (newId != null && !requirementCache.value[newId]) {
    try {
      const detail = await fetchTargetDetail(newId)
      requirementCache.value[newId] = detail.requirements
    } catch {
      requirementCache.value[newId] = []
    }
  }
})

onMounted(load)

async function load() {
  loading.value = true
  loadError.value = ''
  try {
    const [taskList, activeTargets, weeklyData] = await Promise.all([
      listTasks({}),
      listTargets(),
      fetchWeeklyProgress()
    ])
    tasks.value = taskList
    targetOptions.value = activeTargets
    weekly.value = weeklyData
    // 若开了"显示已放弃"，追加拉一份
    if (showAbandoned.value) {
      const abandoned = await listTasks({ status: 'ABANDONED' })
      tasks.value = [...tasks.value, ...abandoned]
    }
  } catch (e) {
    loadError.value = (e as Error).message || '加载失败'
  } finally {
    loading.value = false
  }
}

watch(showAbandoned, async (v) => {
  if (v) {
    try {
      const abandoned = await listTasks({ status: 'ABANDONED' })
      // 合并去重
      const existing = new Set(tasks.value.map((t) => t.id))
      for (const t of abandoned) {
        if (!existing.has(t.id)) tasks.value.push(t)
      }
    } catch (e) {
      loadError.value = (e as Error).message || '加载失败'
    }
  } else {
    tasks.value = tasks.value.filter((t) => t.status !== 'ABANDONED')
  }
})

function byStatus(s: TaskStatus): TaskView[] {
  return tasks.value.filter((t) => t.status === s)
}

// ------ create / edit ------

function openCreate() {
  formState.open = true
  formState.id = null
  formState.title = ''
  formState.description = ''
  formState.priority = 'MEDIUM'
  formState.dueDate = ''
  formState.plannedEffortMinutes = null
  formState.targetId = null
  formState.requirementId = null
  formState.error = ''
}

function openDetail(t: TaskView) {
  formState.open = true
  formState.id = t.id
  formState.title = t.title
  formState.description = t.description ?? ''
  formState.priority = t.priority
  formState.dueDate = t.dueDate ?? ''
  formState.plannedEffortMinutes = t.plannedEffortMinutes ?? null
  formState.targetId = t.targetId ?? null
  formState.requirementId = t.requirementId ?? null
  formState.error = ''
}

function closeForm() {
  formState.open = false
}

async function submitForm() {
  if (!formState.title.trim()) {
    formState.error = '标题不能为空'
    return
  }
  formState.submitting = true
  formState.error = ''
  try {
    const payload = {
      title: formState.title.trim(),
      description: formState.description.trim() || undefined,
      priority: formState.priority,
      dueDate: formState.dueDate || undefined,
      plannedEffortMinutes: formState.plannedEffortMinutes ?? undefined,
      targetId: formState.targetId ?? undefined,
      requirementId: formState.requirementId ?? undefined
    }
    if (formState.id) {
      const updated = await updateTask(formState.id, payload)
      replaceTask(updated)
    } else {
      const created = await createTask(payload)
      tasks.value.push(created)
    }
    closeForm()
  } catch (e) {
    formState.error = (e as Error).message || '保存失败'
  } finally {
    formState.submitting = false
  }
}

async function onAbandon() {
  if (!formState.id) return
  if (!window.confirm('将此任务标记为已放弃？（可从"显示已放弃"列表恢复）')) return
  try {
    const updated = await updateTaskStatus(formState.id, { status: 'ABANDONED' })
    replaceTask(updated)
    closeForm()
  } catch (e) {
    formState.error = (e as Error).message || '放弃失败'
  }
}

// ------ inline actions ------

async function onCheckIn(t: TaskView) {
  try {
    const updated = await checkInTask(t.id)
    replaceTask(updated)
    weekly.value = await fetchWeeklyProgress()
  } catch (e) {
    loadError.value = (e as Error).message || '打卡失败'
  }
}

async function onChangeStatus(t: TaskView, next: TaskStatus) {
  if (t.status === next) return
  try {
    const updated = await updateTaskStatus(t.id, { status: next })
    replaceTask(updated)
    weekly.value = await fetchWeeklyProgress()
  } catch (e) {
    loadError.value = (e as Error).message || '状态切换失败'
  }
}

async function onDelete(t: TaskView) {
  if (!window.confirm('彻底删除此任务？')) return
  try {
    await deleteTask(t.id)
    tasks.value = tasks.value.filter((x) => x.id !== t.id)
  } catch (e) {
    loadError.value = (e as Error).message || '删除失败'
  }
}

// ------ helpers ------

function replaceTask(updated: TaskView) {
  const idx = tasks.value.findIndex((x) => x.id === updated.id)
  if (idx >= 0) tasks.value.splice(idx, 1, updated)
  else tasks.value.push(updated)
}

function formatPercent(v: number | undefined) {
  const n = typeof v === 'number' ? v : 0
  return `${Math.round(n * 100)}%`
}

function formatWeekday(iso: string) {
  try {
    const d = new Date(iso + 'T00:00:00')
    return ['一', '二', '三', '四', '五', '六', '日'][((d.getDay() + 6) % 7)]
  } catch {
    return ''
  }
}
</script>
