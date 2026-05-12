<template>
  <div class="gt-page-wide">
    <header class="gt-header">
        <div class="flex items-start justify-between gap-4">
      <div>
        <div class="gt-eyebrow">Execution Loop</div>
        <h1 class="gt-title">成长执行</h1>
        <p class="gt-subtitle">
          AI 草案负责拆出验收标准、打卡计划和证据建议；任务完成需要打卡或证据，阶段诊断再基于这些执行痕迹给出纠偏。
        </p>
      </div>
      <button
        type="button"
        class="gt-button-primary"
        @click="openCreate"
      >
        + 新建任务
      </button>
        </div>
        <div class="grid grid-cols-2 md:grid-cols-4 gap-3 mt-6">
          <div class="rounded-2xl bg-slate-50 border border-slate-100 p-4">
            <div class="text-xs text-slate-500">全部可见任务</div>
            <div class="text-2xl font-semibold mt-1 text-slate-950">{{ totalVisible }}</div>
          </div>
          <div class="rounded-2xl bg-slate-50 border border-slate-100 p-4">
            <div class="text-xs text-slate-500">进行中</div>
            <div class="text-2xl font-semibold mt-1 text-slate-950">{{ byStatus('IN_PROGRESS').length }}</div>
          </div>
          <div class="rounded-2xl bg-slate-50 border border-slate-100 p-4">
            <div class="text-xs text-slate-500">当前完成</div>
            <div class="text-2xl font-semibold mt-1 text-slate-950">{{ doneVisibleCount }} / {{ totalVisible }}</div>
          </div>
          <div class="rounded-2xl bg-slate-50 border border-slate-100 p-4">
            <div class="text-xs text-slate-500">执行可信度</div>
            <div class="text-2xl font-semibold mt-1 text-slate-950">{{ averageConfidence }}%</div>
          </div>
        </div>
    </header>

    <!-- 本周进度 -->
    <section class="gt-card p-5 space-y-4">
      <div class="flex items-center justify-between">
        <div>
          <div class="text-sm font-semibold text-slate-800">本周到期节奏</div>
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

      <div v-if="weekly" class="h-2 bg-slate-100 rounded-full overflow-hidden">
        <div
          class="h-full bg-gradient-to-r from-sky-500 to-emerald-400 rounded-full transition-all"
          :style="{ width: `${Math.min(100, weekly.completionRate * 100)}%` }"
        />
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
    <section class="gt-card px-4 py-3 flex items-center gap-3 flex-wrap">
      <span class="text-sm text-slate-600">可见状态：</span>
      <label class="inline-flex items-center gap-1 text-sm text-slate-600">
        <input type="checkbox" v-model="showAbandoned" />
        显示已放弃
      </label>
      <div class="flex-1" />
      <div v-if="taskFilterLabel" class="flex items-center gap-2 text-xs text-brand-700 bg-brand-50 border border-brand-100 rounded-md px-2 py-1">
        <span>{{ taskFilterLabel }}</span>
        <button type="button" class="hover:underline" @click="clearTaskFilter">清除</button>
      </div>
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
        :regenerate-draft="regenerateDraftForTask"
        :open-reader="openTaskReader"
      />
      <KanbanColumn
        title="进行中 (IN_PROGRESS)"
        accent="brand"
        :tasks="byStatus('IN_PROGRESS')"
        :open-detail="openDetail"
        :check-in="onCheckIn"
        :change-status="onChangeStatus"
        :regenerate-draft="regenerateDraftForTask"
        :open-reader="openTaskReader"
      />
      <KanbanColumn
        title="已完成 (DONE)"
        accent="emerald"
        :tasks="byStatus('DONE')"
        :open-detail="openDetail"
        :check-in="onCheckIn"
        :change-status="onChangeStatus"
        :regenerate-draft="regenerateDraftForTask"
        :open-reader="openTaskReader"
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
      <div class="bg-white rounded-[1.75rem] shadow-2xl max-w-6xl w-full p-5 md:p-6 space-y-5 max-h-[92vh] overflow-y-auto">
        <div class="flex items-start justify-between gap-4 border-b border-slate-100 pb-4">
          <div>
            <div class="text-xs uppercase tracking-[0.28em] text-brand-600">Guided Task</div>
            <h3 class="text-xl font-semibold text-slate-900 mt-1">
              {{ formState.id ? '任务工作台' : '新建 AI 指引任务' }}
            </h3>
            <p class="text-sm text-slate-500 mt-1">左侧编辑，右侧直接预览完整任务，不需要在描述框里滚动找内容。</p>
          </div>
          <button
            type="button"
            :disabled="draftState.loading || (!formState.title.trim() && !formState.description.trim() && !formState.requirementId)"
            class="shrink-0 px-4 py-2 rounded-full bg-amber-400 text-slate-950 text-sm font-medium hover:bg-amber-300 disabled:opacity-50"
            @click="generateDraftForForm(formState.requirementId ? 'REQUIREMENT' : 'MANUAL')"
          >
            {{ draftState.loading ? 'AI 正在规划…' : (formState.id ? 'AI 重新生成草案' : 'AI 生成草案') }}
          </button>
        </div>

        <div v-if="draftState.loading" class="rounded-2xl border border-sky-100 bg-sky-50/70 p-4 space-y-3">
          <div class="flex items-center justify-between text-sm">
            <span class="font-medium text-sky-900">{{ aiProgressLabel }}</span>
            <span class="text-sky-700">{{ aiProgress }}%</span>
          </div>
          <div class="h-2 rounded-full bg-white overflow-hidden">
            <div
              class="h-full rounded-full bg-gradient-to-r from-sky-500 via-brand-500 to-emerald-400 transition-all duration-500"
              :style="{ width: `${aiProgress}%` }"
            />
          </div>
          <div class="grid grid-cols-3 gap-2 text-[11px] text-sky-700">
            <span :class="aiProgress >= 25 ? 'font-semibold' : ''">理解目标</span>
            <span :class="aiProgress >= 55 ? 'font-semibold' : ''">拆验收标准</span>
            <span :class="aiProgress >= 82 ? 'font-semibold' : ''">生成执行计划</span>
          </div>
        </div>

        <div class="grid grid-cols-1 lg:grid-cols-[minmax(0,1fr)_minmax(360px,0.95fr)] gap-5">
        <div class="space-y-4">
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
            rows="12"
            maxlength="2000"
            class="w-full px-3 py-2 border border-slate-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-brand-500"
          />
          <div class="mt-2 rounded-md border border-brand-100 bg-brand-50/60 p-3 space-y-2">
            <div class="flex items-center justify-between gap-2">
              <div>
                <div class="text-sm font-medium text-brand-800">AI 执行草案</div>
                <div class="text-xs text-brand-700/80">根据目标/要求生成验收标准、打卡计划和完成证据建议。</div>
              </div>
              <button
                type="button"
                :disabled="draftState.loading || (!formState.title.trim() && !formState.description.trim() && !formState.requirementId)"
                class="shrink-0 px-2.5 py-1 rounded-md bg-white border border-brand-200 text-xs text-brand-700 hover:bg-brand-50 disabled:opacity-50"
                @click="generateDraftForForm(formState.requirementId ? 'REQUIREMENT' : 'MANUAL')"
              >
                {{ draftState.loading ? '生成中…' : '应用 AI 草案' }}
              </button>
            </div>
            <div v-if="draftState.error" class="text-xs text-amber-700">{{ draftState.error }}</div>
            <div v-if="draftState.aiStatus === 'FALLBACK'" class="text-xs text-amber-700">
              AI 暂不可用，已使用本地规则生成保守草案。
            </div>
            <div
              v-if="draftState.acceptanceCriteria.length || draftState.checkInPlan.length || draftState.evidenceSuggestions.length"
              class="grid grid-cols-1 md:grid-cols-3 gap-2 text-xs"
            >
              <div v-if="draftState.acceptanceCriteria.length" class="bg-white/80 border border-brand-100 rounded p-2">
                <div class="font-medium text-slate-700 mb-1">验收标准</div>
                <ol class="list-decimal list-inside text-slate-600 space-y-0.5">
                  <li v-for="item in draftState.acceptanceCriteria" :key="item">{{ item }}</li>
                </ol>
              </div>
              <div v-if="draftState.checkInPlan.length" class="bg-white/80 border border-brand-100 rounded p-2">
                <div class="font-medium text-slate-700 mb-1">打卡计划</div>
                <ol class="list-decimal list-inside text-slate-600 space-y-0.5">
                  <li v-for="item in draftState.checkInPlan" :key="item">{{ item }}</li>
                </ol>
              </div>
              <div v-if="draftState.evidenceSuggestions.length" class="bg-white/80 border border-brand-100 rounded p-2">
                <div class="font-medium text-slate-700 mb-1">证据建议</div>
                <ol class="list-decimal list-inside text-slate-600 space-y-0.5">
                  <li v-for="item in draftState.evidenceSuggestions" :key="item">{{ item }}</li>
                </ol>
              </div>
            </div>
          </div>
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
              min="1900-01-01"
              max="2099-12-31"
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
        </div>

        <aside class="rounded-[1.5rem] border border-slate-200 bg-gradient-to-b from-slate-50 to-white p-4 space-y-4 self-start lg:sticky lg:top-4">
          <div class="flex items-center justify-between">
            <div>
              <div class="text-xs uppercase tracking-[0.24em] text-slate-400">Preview</div>
              <div class="text-sm font-semibold text-slate-800 mt-1">完整任务卡</div>
            </div>
            <span class="text-xs px-2 py-1 rounded-full" :class="priorityBadge(formState.priority)">
              {{ formState.priority }}
            </span>
          </div>

          <div>
            <div class="text-xl font-semibold text-slate-900 leading-snug">
              {{ formState.title || '尚未填写任务标题' }}
            </div>
            <div class="flex gap-2 flex-wrap mt-3 text-xs text-slate-500">
              <span v-if="formState.dueDate" class="px-2 py-1 rounded-full bg-white border border-slate-200">截止 {{ formState.dueDate }}</span>
              <span v-if="formState.plannedEffortMinutes" class="px-2 py-1 rounded-full bg-white border border-slate-200">计划 {{ formState.plannedEffortMinutes }} 分钟</span>
              <span v-if="selectedTargetLabel" class="px-2 py-1 rounded-full bg-white border border-slate-200">{{ selectedTargetLabel }}</span>
              <span v-if="selectedRequirementLabel" class="px-2 py-1 rounded-full bg-white border border-slate-200">{{ selectedRequirementLabel }}</span>
            </div>
          </div>

          <div class="space-y-3">
            <div v-if="previewSummary" class="rounded-2xl bg-white border border-slate-200 p-3">
              <div class="text-xs font-medium text-slate-500 mb-1">任务说明</div>
              <div class="text-sm text-slate-700 whitespace-pre-wrap leading-6">{{ previewSummary }}</div>
            </div>
            <div
              v-for="section in previewSections"
              :key="section.title"
              class="rounded-2xl bg-white border border-brand-100 p-3"
            >
              <div class="text-xs font-semibold text-brand-800 mb-2">{{ section.title }}</div>
              <ol class="space-y-1 text-sm text-slate-700">
                <li v-for="item in section.items" :key="item" class="flex gap-2 leading-6">
                  <span class="text-brand-500">•</span>
                  <span>{{ item }}</span>
                </li>
              </ol>
            </div>
            <div v-if="!previewSummary && previewSections.length === 0" class="text-sm text-slate-400 border border-dashed border-slate-200 rounded-2xl p-6 text-center">
              点击 AI 生成草案后，这里会展示完整任务说明、验收标准和打卡计划。
            </div>
          </div>
        </aside>
        </div>

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

    <!-- 任务正文阅读 modal -->
    <div
      v-if="readerState.open && readerState.task"
      class="fixed inset-0 bg-slate-950/35 flex items-center justify-center z-50 p-4"
      @click.self="closeTaskReader"
    >
      <div class="bg-white rounded-[1.75rem] shadow-2xl max-w-3xl w-full max-h-[86vh] overflow-y-auto p-6 space-y-5">
        <div class="flex items-start justify-between gap-4 border-b border-slate-100 pb-4">
          <div>
            <div class="gt-eyebrow">Task Brief</div>
            <h3 class="text-2xl font-semibold text-slate-950 mt-1">{{ readerState.task.title }}</h3>
            <div class="flex flex-wrap gap-2 mt-3 text-xs text-slate-500">
              <span class="px-2 py-1 rounded-full bg-slate-100">{{ readerState.task.status }}</span>
              <span class="px-2 py-1 rounded-full bg-slate-100">{{ readerState.task.priority }}</span>
              <span v-if="readerState.task.dueDate" class="px-2 py-1 rounded-full bg-slate-100">截止 {{ readerState.task.dueDate }}</span>
              <span v-if="readerState.task.targetTitle" class="px-2 py-1 rounded-full bg-slate-100">目标：{{ readerState.task.targetTitle }}</span>
              <span v-if="readerState.task.requirementName" class="px-2 py-1 rounded-full bg-slate-100">要求：{{ readerState.task.requirementName }}</span>
            </div>
          </div>
          <button type="button" class="gt-button-soft" @click="closeTaskReader">关闭</button>
        </div>

        <div v-if="readerSummary" class="rounded-2xl border border-slate-200 bg-slate-50/80 p-4">
          <div class="text-xs font-medium text-slate-500 mb-2">任务说明</div>
          <div class="text-sm leading-7 text-slate-700 whitespace-pre-wrap">{{ readerSummary }}</div>
        </div>

        <div class="grid grid-cols-1 md:grid-cols-3 gap-3">
          <div
            v-for="section in readerSections"
            :key="section.title"
            class="rounded-2xl border border-slate-200 bg-white p-4"
          >
            <div class="text-sm font-semibold text-slate-900 mb-3">{{ section.title }}</div>
            <ol class="space-y-2 text-sm text-slate-700">
              <li v-for="item in section.items" :key="item" class="flex gap-2 leading-6">
                <span class="mt-2 size-1.5 rounded-full bg-slate-400 shrink-0" />
                <span>{{ item }}</span>
              </li>
            </ol>
          </div>
        </div>

        <div v-if="!readerSummary && readerSections.length === 0" class="text-sm text-slate-400 text-center py-8">
          这个任务还没有正文。可以点击“AI 优化”生成更完整的执行草案。
        </div>
      </div>
    </div>

    <!-- 完成证据 modal -->
    <div
      v-if="completionState.open"
      class="fixed inset-0 bg-slate-900/40 flex items-center justify-center z-50 p-4"
      @click.self="closeCompletion"
    >
      <div class="bg-white rounded-lg shadow-xl max-w-lg w-full p-6 space-y-4">
        <div>
          <h3 class="text-base font-semibold text-slate-800">确认完成任务</h3>
          <p class="text-sm text-slate-500 mt-1">
            完成不再只是改状态：请补一条证据或先完成至少一次打卡，便于后续诊断判断真实推进。
          </p>
        </div>
        <div v-if="completionState.task" class="rounded-md bg-slate-50 border border-slate-100 p-3">
          <div class="text-sm font-medium text-slate-800">{{ completionState.task.title }}</div>
          <div class="text-xs text-slate-500 mt-1">
            已打卡 {{ completionState.task.checkInCount }} 次，已记录投入 {{ completionState.task.actualEffortMinutes }} 分钟
          </div>
        </div>
        <div>
          <label class="block text-sm text-slate-600 mb-1">
            完成证据 / 复盘 <span v-if="completionState.task?.checkInCount === 0" class="text-red-500">*</span>
          </label>
          <textarea
            v-model="completionState.evidence"
            rows="4"
            maxlength="2000"
            placeholder="例如：提交了 GitHub 链接、完成了 20 道题、整理了笔记截图，或写下关键收获与卡点。"
            class="w-full px-3 py-2 border border-slate-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-brand-500"
          />
          <div class="text-xs text-slate-400 mt-1">若该任务没有打卡，证据至少 10 个字。</div>
        </div>
        <div>
          <label class="block text-sm text-slate-600 mb-1">本次补记投入分钟（可选）</label>
          <input
            v-model.number="completionState.effortMinutes"
            type="number"
            min="0"
            max="100000"
            class="w-full px-3 py-2 border border-slate-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-brand-500"
          />
        </div>
        <div v-if="completionState.error" class="text-sm text-red-600">{{ completionState.error }}</div>
        <div class="flex justify-end gap-2 pt-2 border-t border-slate-100">
          <button type="button" class="px-3 py-1.5 rounded-md border border-slate-300 text-sm text-slate-600 hover:bg-slate-50" @click="closeCompletion">取消</button>
          <button
            type="button"
            :disabled="completionState.submitting"
            class="px-3 py-1.5 rounded-md bg-emerald-600 text-white text-sm hover:bg-emerald-700 disabled:opacity-60"
            @click="submitCompletion"
          >
            {{ completionState.submitting ? '确认中…' : '确认完成' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import KanbanColumn from '@/components/execution/KanbanColumn.vue'
import {
  checkInTask,
  createTask,
  deleteTask,
  fetchWeeklyProgress,
  generateTaskDraft,
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

const route = useRoute()
const router = useRouter()

const tasks = ref<TaskView[]>([])
const weekly = ref<WeeklyProgressView | null>(null)
const loading = ref(true)
const loadError = ref('')
const showAbandoned = ref(false)
const suppressRequirementReset = ref(false)
const aiProgress = ref(0)
let aiProgressTimer: number | undefined

const targetOptions = ref<TargetView[]>([])
const requirementCache = ref<Record<number, RequirementView[]>>({})

const totalVisible = computed(() =>
  tasks.value.filter((t) => showAbandoned.value || t.status !== 'ABANDONED').length
)

const averageConfidence = computed(() => {
  const visible = tasks.value.filter((t) => showAbandoned.value || t.status !== 'ABANDONED')
  if (visible.length === 0) return 0
  return Math.round(visible.reduce((sum, task) => sum + executionConfidence(task), 0) / visible.length)
})

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

const draftState = reactive({
  loading: false,
  aiStatus: '' as '' | 'SUCCESS' | 'FALLBACK',
  acceptanceCriteria: [] as string[],
  checkInPlan: [] as string[],
  evidenceSuggestions: [] as string[],
  error: ''
})

const completionState = reactive({
  open: false,
  task: null as TaskView | null,
  evidence: '',
  effortMinutes: null as number | null,
  submitting: false,
  error: ''
})

const readerState = reactive({
  open: false,
  task: null as TaskView | null
})

const requirementOptionsForForm = computed(() => {
  const tid = formState.targetId
  if (!tid) return []
  return requirementCache.value[tid] ?? []
})

const taskFilterLabel = computed(() => {
  const requirementId = parsePositiveInt(route.query.requirementId)
  if (route.query.source !== 'requirement-filter' || requirementId == null) return ''
  const targetId = parsePositiveInt(route.query.targetId)
  const target = targetOptions.value.find((item) => item.id === targetId)
  const req = targetId == null ? null : requirementCache.value[targetId]?.find((item) => item.id === requirementId)
  if (target && req) return `只看：${target.title} / ${req.reqName}`
  if (req) return `只看：${req.reqName}`
  return `只看 requirement #${requirementId}`
})

const selectedTargetLabel = computed(() => {
  const target = targetOptions.value.find((item) => item.id === formState.targetId)
  return target ? `目标：${target.title}` : ''
})

const selectedRequirementLabel = computed(() => {
  const targetId = formState.targetId
  if (!targetId) return ''
  const requirement = requirementCache.value[targetId]?.find((item) => item.id === formState.requirementId)
  return requirement ? `要求：${requirement.reqName}` : ''
})

const previewParts = computed(() => splitTaskDescription(formState.description))
const previewSummary = computed(() => previewParts.value.summary)
const previewSections = computed(() => previewParts.value.sections)
const readerParts = computed(() => splitTaskDescription(readerState.task?.description))
const readerSummary = computed(() => readerParts.value.summary)
const readerSections = computed(() => readerParts.value.sections)
const doneVisibleCount = computed(() => tasks.value.filter((t) =>
  (showAbandoned.value || t.status !== 'ABANDONED') && t.status === 'DONE'
).length)
const aiProgressLabel = computed(() => {
  if (aiProgress.value < 30) return '正在理解目标与当前要求'
  if (aiProgress.value < 60) return '正在拆解验收标准'
  if (aiProgress.value < 85) return '正在规划打卡节奏'
  return '正在整理完成证据建议'
})

watch(() => formState.targetId, async (newId) => {
  if (!suppressRequirementReset.value) {
    formState.requirementId = null
  }
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
      listTasks(taskListParamsFromQuery()),
      listTargets(),
      fetchWeeklyProgress()
    ])
    tasks.value = taskList
    targetOptions.value = activeTargets
    weekly.value = weeklyData
    await hydrateRequirementFilterLabel()
    await handleTaskPrefillFromQuery()
    // 若开了"显示已放弃"，追加拉一份
    if (showAbandoned.value) {
      const abandoned = await listTasks({ ...taskListParamsFromQuery(), status: 'ABANDONED' })
      tasks.value = [...tasks.value, ...abandoned]
    }
  } catch (e) {
    loadError.value = (e as Error).message || '加载失败'
  } finally {
    loading.value = false
  }
}

function taskListParamsFromQuery() {
  if (route.query.source !== 'requirement-filter') return {}
  const targetId = parsePositiveInt(route.query.targetId)
  const requirementId = parsePositiveInt(route.query.requirementId)
  return {
    targetId: targetId ?? undefined,
    requirementId: requirementId ?? undefined
  }
}

async function hydrateRequirementFilterLabel() {
  const targetId = parsePositiveInt(route.query.targetId)
  if (targetId != null) {
    await ensureRequirementsForTarget(targetId)
  }
}

async function handleTaskPrefillFromQuery() {
  const source = typeof route.query.source === 'string' ? route.query.source : ''
  if (!['requirement', 'diagnosis-suggestion', 'diagnosis-correction'].includes(source)) return
  const targetId = parsePositiveInt(route.query.targetId)
  const requirementId = parsePositiveInt(route.query.requirementId)
  openCreate()
  formState.title = queryString(route.query.title) || ''
  formState.description = queryString(route.query.description) || ''
  suppressRequirementReset.value = true
  formState.targetId = targetId
  if (targetId != null) {
    await ensureRequirementsForTarget(targetId)
  }
  formState.requirementId = requirementId
  suppressRequirementReset.value = false
  await generateDraftForForm(sourceToDraftType(source))
}

async function ensureRequirementsForTarget(targetId: number) {
  if (requirementCache.value[targetId]) return
  try {
    const detail = await fetchTargetDetail(targetId)
    requirementCache.value[targetId] = detail.requirements
  } catch {
    requirementCache.value[targetId] = []
  }
}

function clearTaskFilter() {
  void router.push({ path: '/execution' }).then(() => load())
}

watch(showAbandoned, async (v) => {
  if (v) {
    try {
      const abandoned = await listTasks({ ...taskListParamsFromQuery(), status: 'ABANDONED' })
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

watch(() => route.query, () => {
  void load()
})

function byStatus(s: TaskStatus): TaskView[] {
  return tasks.value.filter((t) => t.status === s)
}

// ------ create / edit ------

function openCreate() {
  resetDraftState()
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
  resetDraftState()
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

function resetDraftState() {
  draftState.loading = false
  draftState.aiStatus = ''
  draftState.acceptanceCriteria = []
  draftState.checkInPlan = []
  draftState.evidenceSuggestions = []
  draftState.error = ''
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
    if (route.query.source && route.query.source !== 'requirement-filter') {
      void router.replace({ path: '/execution' })
    }
  } catch (e) {
    formState.error = (e as Error).message || '保存失败'
  } finally {
    formState.submitting = false
  }
}

async function generateDraftForForm(sourceType: 'REQUIREMENT' | 'DIAGNOSIS_SUGGESTION' | 'DIAGNOSIS_CORRECTION' | 'MANUAL') {
  draftState.loading = true
  draftState.error = ''
  startAiProgress()
  try {
    const draft = await generateTaskDraft({
      sourceType,
      targetId: formState.targetId,
      requirementId: formState.requirementId,
      title: formState.title.trim() || undefined,
      description: formState.description.trim() || undefined
    })
    formState.title = draft.title || formState.title
    formState.description = draft.description || formState.description
    formState.priority = draft.priority || formState.priority
    formState.dueDate = draft.dueDate ?? formState.dueDate
    formState.plannedEffortMinutes = draft.plannedEffortMinutes ?? formState.plannedEffortMinutes
    draftState.aiStatus = draft.aiStatus
    draftState.acceptanceCriteria = draft.acceptanceCriteria ?? []
    draftState.checkInPlan = draft.checkInPlan ?? []
    draftState.evidenceSuggestions = draft.evidenceSuggestions ?? []
  } catch (e) {
    draftState.error = (e as Error).message || 'AI 草案生成失败，可以先手动填写任务'
  } finally {
    finishAiProgress()
    draftState.loading = false
  }
}

async function regenerateDraftForTask(t: TaskView) {
  openDetail(t)
  await ensureRequirementsForTask(t)
  await generateDraftForForm(t.requirementId ? 'REQUIREMENT' : 'MANUAL')
}

function openTaskReader(t: TaskView) {
  readerState.open = true
  readerState.task = t
}

function closeTaskReader() {
  readerState.open = false
  readerState.task = null
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
  if (next === 'DONE') {
    openCompletion(t)
    return
  }
  try {
    const updated = await updateTaskStatus(t.id, { status: next })
    replaceTask(updated)
    weekly.value = await fetchWeeklyProgress()
  } catch (e) {
    loadError.value = (e as Error).message || '状态切换失败'
  }
}

function openCompletion(t: TaskView) {
  completionState.open = true
  completionState.task = t
  completionState.evidence = ''
  completionState.effortMinutes = null
  completionState.submitting = false
  completionState.error = ''
}

function closeCompletion() {
  completionState.open = false
  completionState.task = null
  completionState.error = ''
}

async function submitCompletion() {
  const task = completionState.task
  if (!task) return
  const evidence = completionState.evidence.trim()
  if (task.checkInCount === 0 && evidence.length < 10) {
    completionState.error = '没有打卡记录时，完成证据至少 10 个字'
    return
  }
  completionState.submitting = true
  completionState.error = ''
  try {
    const updated = await updateTaskStatus(task.id, {
      status: 'DONE',
      completionEvidence: evidence || undefined,
      effortMinutes: completionState.effortMinutes ?? undefined
    })
    replaceTask(updated)
    weekly.value = await fetchWeeklyProgress()
    closeCompletion()
  } catch (e) {
    completionState.error = (e as Error).message || '确认完成失败'
  } finally {
    completionState.submitting = false
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

function parsePositiveInt(raw: unknown): number | null {
  const value = Array.isArray(raw) ? raw[0] : raw
  if (typeof value !== 'string' || !/^\d+$/.test(value)) return null
  const parsed = Number(value)
  return parsed > 0 ? parsed : null
}

function queryString(raw: unknown): string {
  const value = Array.isArray(raw) ? raw[0] : raw
  return typeof value === 'string' ? value : ''
}

function sourceToDraftType(source: string) {
  if (source === 'requirement') return 'REQUIREMENT'
  if (source === 'diagnosis-suggestion') return 'DIAGNOSIS_SUGGESTION'
  if (source === 'diagnosis-correction') return 'DIAGNOSIS_CORRECTION'
  return 'MANUAL'
}

async function ensureRequirementsForTask(t: TaskView) {
  if (t.targetId != null) {
    await ensureRequirementsForTarget(t.targetId)
  }
}

function startAiProgress() {
  if (aiProgressTimer != null) window.clearInterval(aiProgressTimer)
  aiProgress.value = 8
  aiProgressTimer = window.setInterval(() => {
    const next = aiProgress.value + Math.max(1, Math.round((92 - aiProgress.value) * 0.12))
    aiProgress.value = Math.min(92, next)
  }, 450)
}

function finishAiProgress() {
  if (aiProgressTimer != null) {
    window.clearInterval(aiProgressTimer)
    aiProgressTimer = undefined
  }
  aiProgress.value = 100
  window.setTimeout(() => {
    if (!draftState.loading) aiProgress.value = 0
  }, 600)
}

function splitTaskDescription(description: string | null | undefined) {
  const text = (description ?? '').trim()
  if (!text) return { summary: '', sections: [] as { title: string; items: string[] }[] }
  const markers = ['验收标准：', '建议打卡计划：', '完成证据建议：', '完成证据（']
  const firstMarker = markers
    .map((marker) => text.indexOf(marker))
    .filter((idx) => idx >= 0)
    .sort((a, b) => a - b)[0]
  const summary = firstMarker == null ? text : text.slice(0, firstMarker).trim()
  const sections = [
    readSection(text, '验收标准：', ['建议打卡计划：', '完成证据建议：', '完成证据（']),
    readSection(text, '建议打卡计划：', ['完成证据建议：', '完成证据（']),
    readSection(text, '完成证据建议：', ['完成证据（']),
    readCompletionEvidence(text)
  ].filter((section): section is { title: string; items: string[] } => section != null)
  return { summary, sections }
}

function readSection(text: string, title: string, endMarkers: string[]) {
  const start = text.indexOf(title)
  if (start < 0) return null
  const bodyStart = start + title.length
  const end = endMarkers
    .map((marker) => text.indexOf(marker, bodyStart))
    .filter((idx) => idx >= 0)
    .sort((a, b) => a - b)[0] ?? text.length
  const items = text
    .slice(bodyStart, end)
    .split('\n')
    .map((line) => line.replace(/^\s*(\d+[.)、]|[-*•])\s*/, '').trim())
    .filter(Boolean)
  return { title: title.replace('：', ''), items }
}

function readCompletionEvidence(text: string) {
  const start = text.indexOf('完成证据（')
  if (start < 0) return null
  const items = text
    .slice(start)
    .split('\n')
    .map((line) => line.trim())
    .filter(Boolean)
  return { title: '已提交证据', items }
}

function executionConfidence(t: TaskView) {
  let score = 15
  if (t.description?.includes('验收标准：')) score += 25
  if (t.description?.includes('建议打卡计划：')) score += 20
  if (t.checkInCount > 0) score += 20
  if (t.actualEffortMinutes > 0) score += 10
  if (t.status === 'DONE' && t.description?.includes('完成证据（')) score += 10
  return Math.min(100, score)
}

function priorityBadge(p: TaskPriority) {
  if (p === 'HIGH') return 'bg-red-50 text-red-700 border border-red-100'
  if (p === 'LOW') return 'bg-slate-100 text-slate-500 border border-slate-200'
  return 'bg-brand-50 text-brand-700 border border-brand-100'
}
</script>
