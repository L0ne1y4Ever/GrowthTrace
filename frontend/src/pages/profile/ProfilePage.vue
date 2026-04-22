<template>
  <div class="p-6 max-w-5xl mx-auto space-y-6">
    <!-- Header -->
    <header class="flex items-start justify-between gap-4">
      <div>
        <h1 class="text-2xl font-semibold text-slate-800">成长档案</h1>
        <p class="text-sm text-slate-500 mt-1">
          画像、技能、经历的读写入口。技能按 ACTIVE 优先排序，经历按开始日期倒序。
        </p>
      </div>
      <div class="flex gap-2">
        <RouterLink
          to="/onboarding"
          class="px-3 py-1.5 rounded-md border border-slate-300 text-sm text-slate-600 hover:bg-slate-50"
        >
          重新建档
        </RouterLink>
        <button
          type="button"
          :disabled="refreshing || !profile"
          class="px-3 py-1.5 rounded-md border border-slate-300 text-sm text-slate-600 hover:bg-slate-50 disabled:opacity-60"
          @click="onRefreshCompleteness"
        >
          {{ refreshing ? '重算中…' : '重算完整度' }}
        </button>
      </div>
    </header>

    <!-- Loading / empty / error -->
    <div v-if="loading" class="text-sm text-slate-500">加载中…</div>
    <div
      v-else-if="loadError"
      class="bg-amber-50 border border-amber-200 text-amber-700 text-sm rounded-md px-4 py-3"
    >
      {{ loadError }}
      <RouterLink to="/onboarding" class="ml-2 text-brand-600 hover:underline">去建档</RouterLink>
    </div>

    <template v-else-if="profile">
      <!-- 画像头部 -->
      <section class="bg-white border border-slate-200 rounded-lg p-6 space-y-4">
        <div class="flex items-center justify-between">
          <h2 class="text-sm font-medium text-slate-700">画像 · v{{ profile.version }}</h2>
          <div class="text-xs text-slate-400">source: {{ profile.source || '—' }}</div>
        </div>

        <div class="flex items-center gap-4">
          <div class="text-3xl font-semibold text-brand-600">{{ profile.completeness }}</div>
          <div class="flex-1 h-2 bg-slate-100 rounded-full overflow-hidden">
            <div
              class="h-2 bg-brand-500 rounded-full transition-all"
              :style="{ width: `${profile.completeness}%` }"
            />
          </div>
          <div class="text-xs text-slate-400">完整度 / 100</div>
        </div>

        <div v-if="profile.selfIntro" class="text-sm text-slate-700">
          <div class="text-xs text-slate-400 mb-1">一句话介绍</div>
          {{ profile.selfIntro }}
        </div>

        <div v-if="profile.summary" class="text-sm text-slate-700">
          <div class="text-xs text-slate-400 mb-1">画像总结</div>
          {{ profile.summary }}
        </div>

        <div class="grid grid-cols-1 md:grid-cols-2 gap-4 pt-2">
          <div>
            <div class="text-xs text-slate-400 mb-2">擅长</div>
            <div class="flex flex-wrap gap-1.5">
              <span
                v-for="(s, i) in profile.strengths"
                :key="`s-${i}`"
                class="px-2 py-0.5 rounded bg-emerald-50 text-emerald-700 text-xs border border-emerald-100"
              >
                {{ s }}
              </span>
              <span v-if="profile.strengths.length === 0" class="text-xs text-slate-400">（空）</span>
            </div>
          </div>
          <div>
            <div class="text-xs text-slate-400 mb-2">不擅长</div>
            <div class="flex flex-wrap gap-1.5">
              <span
                v-for="(w, i) in profile.weaknesses"
                :key="`w-${i}`"
                class="px-2 py-0.5 rounded bg-amber-50 text-amber-700 text-xs border border-amber-100"
              >
                {{ w }}
              </span>
              <span v-if="profile.weaknesses.length === 0" class="text-xs text-slate-400">（空）</span>
            </div>
          </div>
        </div>
      </section>

      <!-- 技能 -->
      <section class="bg-white border border-slate-200 rounded-lg p-6 space-y-4">
        <div class="flex items-center justify-between">
          <h2 class="text-sm font-medium text-slate-700">技能（{{ profile.skills.length }}）</h2>
          <button
            type="button"
            class="text-xs text-brand-600 hover:underline"
            @click="openSkillForm(null)"
          >
            + 新增技能
          </button>
        </div>

        <div v-if="profile.skills.length === 0" class="text-sm text-slate-400">还没有技能，点右上新增一条。</div>

        <div v-else class="grid grid-cols-1 md:grid-cols-2 gap-3">
          <div
            v-for="s in profile.skills"
            :key="s.id"
            class="border border-slate-200 rounded-md p-3 flex items-start justify-between gap-3"
            :class="s.status === 'ARCHIVED' ? 'opacity-60' : ''"
          >
            <div class="min-w-0">
              <div class="text-sm font-medium text-slate-800 truncate">{{ s.skillName }}</div>
              <div class="text-xs text-slate-500 mt-0.5 flex flex-wrap gap-2">
                <span>{{ levelLabel(s.skillLevel) }}</span>
                <span v-if="s.category">·&nbsp;{{ s.category }}</span>
                <span v-if="s.status === 'ARCHIVED'" class="text-slate-400">·&nbsp;已归档</span>
                <span v-if="s.source" class="text-slate-400">·&nbsp;{{ s.source }}</span>
              </div>
              <div v-if="s.evidence" class="text-xs text-slate-500 mt-1 line-clamp-2">{{ s.evidence }}</div>
            </div>
            <div class="flex flex-col gap-1 shrink-0">
              <button type="button" class="text-xs text-brand-600 hover:underline" @click="openSkillForm(s)">编辑</button>
              <button type="button" class="text-xs text-slate-400 hover:text-red-600" @click="onDeleteSkill(s.id)">删除</button>
            </div>
          </div>
        </div>
      </section>

      <!-- 经历 -->
      <section class="bg-white border border-slate-200 rounded-lg p-6 space-y-4">
        <div class="flex items-center justify-between">
          <h2 class="text-sm font-medium text-slate-700">经历（{{ profile.experiences.length }}）</h2>
          <button
            type="button"
            class="text-xs text-brand-600 hover:underline"
            @click="openExperienceForm(null)"
          >
            + 新增经历
          </button>
        </div>

        <div v-if="profile.experiences.length === 0" class="text-sm text-slate-400">还没有经历，点右上新增一条。</div>

        <div v-else class="space-y-3">
          <div
            v-for="e in profile.experiences"
            :key="e.id"
            class="border border-slate-200 rounded-md p-3"
          >
            <div class="flex items-start justify-between gap-3">
              <div class="min-w-0">
                <div class="flex items-center gap-2">
                  <span class="px-1.5 py-0.5 rounded bg-slate-100 text-slate-600 text-xs">{{ expTypeLabel(e.expType) }}</span>
                  <span class="text-sm font-medium text-slate-800 truncate">{{ e.title }}</span>
                </div>
                <div class="text-xs text-slate-500 mt-1 flex flex-wrap gap-2">
                  <span v-if="e.role">角色：{{ e.role }}</span>
                  <span v-if="e.startDate || e.endDate">
                    {{ e.startDate || '' }} ~ {{ e.endDate || '至今' }}
                  </span>
                </div>
                <div v-if="e.outcome" class="text-xs text-slate-600 mt-1.5">{{ e.outcome }}</div>
              </div>
              <div class="flex flex-col gap-1 shrink-0">
                <button type="button" class="text-xs text-brand-600 hover:underline" @click="openExperienceForm(e)">编辑</button>
                <button type="button" class="text-xs text-slate-400 hover:text-red-600" @click="onDeleteExperience(e.id)">删除</button>
              </div>
            </div>
          </div>
        </div>
      </section>
    </template>

    <!-- Skill modal -->
    <div
      v-if="skillForm.open"
      class="fixed inset-0 bg-slate-900/40 flex items-center justify-center z-50 p-4"
      @click.self="closeSkillForm"
    >
      <div class="bg-white rounded-lg shadow-xl max-w-md w-full p-6 space-y-4">
        <h3 class="text-base font-semibold text-slate-800">{{ skillForm.id ? '编辑技能' : '新增技能' }}</h3>

        <div>
          <label class="block text-sm text-slate-600 mb-1">名称</label>
          <input
            v-model="skillForm.skillName"
            type="text"
            maxlength="128"
            class="w-full px-3 py-2 border border-slate-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-brand-500"
          />
        </div>

        <div class="grid grid-cols-2 gap-3">
          <div>
            <label class="block text-sm text-slate-600 mb-1">熟练度</label>
            <select
              v-model="skillForm.skillLevel"
              class="w-full px-3 py-2 border border-slate-300 rounded-md text-sm bg-white focus:outline-none focus:ring-2 focus:ring-brand-500"
            >
              <option value="BEGINNER">入门</option>
              <option value="INTERMEDIATE">进阶</option>
              <option value="ADVANCED">熟练</option>
            </select>
          </div>
          <div>
            <label class="block text-sm text-slate-600 mb-1">类别</label>
            <select
              v-model="skillForm.category"
              class="w-full px-3 py-2 border border-slate-300 rounded-md text-sm bg-white focus:outline-none focus:ring-2 focus:ring-brand-500"
            >
              <option value="LANGUAGE">LANGUAGE</option>
              <option value="FRAMEWORK">FRAMEWORK</option>
              <option value="TOOL">TOOL</option>
              <option value="DOMAIN">DOMAIN</option>
              <option value="SOFT">SOFT</option>
            </select>
          </div>
        </div>

        <div>
          <label class="block text-sm text-slate-600 mb-1">说明 / 证据（可选）</label>
          <textarea
            v-model="skillForm.evidence"
            rows="3"
            maxlength="2000"
            class="w-full px-3 py-2 border border-slate-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-brand-500"
          />
        </div>

        <div v-if="skillForm.error" class="text-sm text-red-600">{{ skillForm.error }}</div>

        <div class="flex justify-end gap-2 pt-2 border-t border-slate-100">
          <button
            type="button"
            class="px-3 py-1.5 rounded-md border border-slate-300 text-sm text-slate-600 hover:bg-slate-50"
            @click="closeSkillForm"
          >
            取消
          </button>
          <button
            type="button"
            :disabled="skillForm.submitting"
            class="px-3 py-1.5 rounded-md bg-brand-600 text-white text-sm hover:bg-brand-700 disabled:opacity-60"
            @click="submitSkill"
          >
            {{ skillForm.submitting ? '保存中…' : '保存' }}
          </button>
        </div>
      </div>
    </div>

    <!-- Experience modal -->
    <div
      v-if="expForm.open"
      class="fixed inset-0 bg-slate-900/40 flex items-center justify-center z-50 p-4"
      @click.self="closeExperienceForm"
    >
      <div class="bg-white rounded-lg shadow-xl max-w-lg w-full p-6 space-y-4">
        <h3 class="text-base font-semibold text-slate-800">{{ expForm.id ? '编辑经历' : '新增经历' }}</h3>

        <div class="grid grid-cols-3 gap-3">
          <div>
            <label class="block text-sm text-slate-600 mb-1">类型</label>
            <select
              v-model="expForm.expType"
              class="w-full px-3 py-2 border border-slate-300 rounded-md text-sm bg-white focus:outline-none focus:ring-2 focus:ring-brand-500"
            >
              <option value="INTERNSHIP">实习</option>
              <option value="PROJECT">项目</option>
              <option value="AWARD">竞赛</option>
              <option value="COURSE">课程</option>
              <option value="RESEARCH">科研</option>
              <option value="OTHER">其他</option>
            </select>
          </div>
          <div class="col-span-2">
            <label class="block text-sm text-slate-600 mb-1">标题</label>
            <input
              v-model="expForm.title"
              type="text"
              maxlength="255"
              class="w-full px-3 py-2 border border-slate-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-brand-500"
            />
          </div>
        </div>

        <div class="grid grid-cols-3 gap-3">
          <div>
            <label class="block text-sm text-slate-600 mb-1">角色</label>
            <input
              v-model="expForm.role"
              type="text"
              maxlength="128"
              class="w-full px-3 py-2 border border-slate-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-brand-500"
            />
          </div>
          <div>
            <label class="block text-sm text-slate-600 mb-1">开始日期</label>
            <input
              v-model="expForm.startDate"
              type="date"
              class="w-full px-3 py-2 border border-slate-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-brand-500"
            />
          </div>
          <div>
            <label class="block text-sm text-slate-600 mb-1">结束日期</label>
            <input
              v-model="expForm.endDate"
              type="date"
              class="w-full px-3 py-2 border border-slate-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-brand-500"
            />
          </div>
        </div>

        <div>
          <label class="block text-sm text-slate-600 mb-1">描述</label>
          <textarea
            v-model="expForm.description"
            rows="2"
            maxlength="2000"
            class="w-full px-3 py-2 border border-slate-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-brand-500"
          />
        </div>

        <div>
          <label class="block text-sm text-slate-600 mb-1">产出</label>
          <textarea
            v-model="expForm.outcome"
            rows="2"
            maxlength="2000"
            class="w-full px-3 py-2 border border-slate-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-brand-500"
          />
        </div>

        <div v-if="expForm.error" class="text-sm text-red-600">{{ expForm.error }}</div>

        <div class="flex justify-end gap-2 pt-2 border-t border-slate-100">
          <button
            type="button"
            class="px-3 py-1.5 rounded-md border border-slate-300 text-sm text-slate-600 hover:bg-slate-50"
            @click="closeExperienceForm"
          >
            取消
          </button>
          <button
            type="button"
            :disabled="expForm.submitting"
            class="px-3 py-1.5 rounded-md bg-brand-600 text-white text-sm hover:bg-brand-700 disabled:opacity-60"
            @click="submitExperience"
          >
            {{ expForm.submitting ? '保存中…' : '保存' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { RouterLink } from 'vue-router'
import {
  addExperience,
  addSkill,
  deleteExperience,
  deleteSkill,
  fetchProfile,
  refreshCompleteness,
  updateExperience,
  updateSkill
} from '@/api/profile'
import type { ExperienceView, ProfileView, SkillView } from '@/types/profile'

const profile = ref<ProfileView | null>(null)
const loading = ref(true)
const loadError = ref('')
const refreshing = ref(false)

const skillForm = reactive({
  open: false,
  id: null as number | null,
  skillName: '',
  skillLevel: 'BEGINNER' as 'BEGINNER' | 'INTERMEDIATE' | 'ADVANCED',
  category: 'DOMAIN' as 'LANGUAGE' | 'FRAMEWORK' | 'TOOL' | 'DOMAIN' | 'SOFT',
  evidence: '',
  submitting: false,
  error: ''
})

const expForm = reactive({
  open: false,
  id: null as number | null,
  expType: 'PROJECT' as 'INTERNSHIP' | 'PROJECT' | 'AWARD' | 'COURSE' | 'RESEARCH' | 'OTHER',
  title: '',
  role: '',
  description: '',
  outcome: '',
  startDate: '',
  endDate: '',
  submitting: false,
  error: ''
})

onMounted(load)

async function load() {
  loading.value = true
  loadError.value = ''
  try {
    profile.value = await fetchProfile()
  } catch (e) {
    const msg = (e as Error).message || '加载失败'
    loadError.value = msg.includes('建档') ? '画像尚未建档，' : msg
  } finally {
    loading.value = false
  }
}

async function onRefreshCompleteness() {
  if (!profile.value) return
  refreshing.value = true
  try {
    const newValue = await refreshCompleteness()
    profile.value.completeness = newValue
  } catch (e) {
    loadError.value = (e as Error).message || '重算失败'
  } finally {
    refreshing.value = false
  }
}

// ---------- Skill form ----------

function openSkillForm(s: SkillView | null) {
  skillForm.open = true
  skillForm.error = ''
  if (s) {
    skillForm.id = s.id
    skillForm.skillName = s.skillName
    skillForm.skillLevel = s.skillLevel
    skillForm.category = (s.category as typeof skillForm.category) ?? 'DOMAIN'
    skillForm.evidence = s.evidence ?? ''
  } else {
    skillForm.id = null
    skillForm.skillName = ''
    skillForm.skillLevel = 'BEGINNER'
    skillForm.category = 'DOMAIN'
    skillForm.evidence = ''
  }
}

function closeSkillForm() {
  skillForm.open = false
}

async function submitSkill() {
  if (!skillForm.skillName.trim()) {
    skillForm.error = '技能名不能为空'
    return
  }
  skillForm.submitting = true
  skillForm.error = ''
  try {
    const payload = {
      skillName: skillForm.skillName.trim(),
      skillLevel: skillForm.skillLevel,
      category: skillForm.category,
      evidence: skillForm.evidence.trim() || undefined
    }
    if (skillForm.id) {
      await updateSkill(skillForm.id, payload)
    } else {
      await addSkill(payload)
    }
    closeSkillForm()
    await load()
  } catch (e) {
    skillForm.error = (e as Error).message || '保存失败'
  } finally {
    skillForm.submitting = false
  }
}

async function onDeleteSkill(id: number) {
  if (!window.confirm('确认删除这条技能？')) return
  try {
    await deleteSkill(id)
    await load()
  } catch (e) {
    loadError.value = (e as Error).message || '删除失败'
  }
}

// ---------- Experience form ----------

function openExperienceForm(e: ExperienceView | null) {
  expForm.open = true
  expForm.error = ''
  if (e) {
    expForm.id = e.id
    expForm.expType = e.expType
    expForm.title = e.title
    expForm.role = e.role ?? ''
    expForm.description = e.description ?? ''
    expForm.outcome = e.outcome ?? ''
    expForm.startDate = e.startDate ?? ''
    expForm.endDate = e.endDate ?? ''
  } else {
    expForm.id = null
    expForm.expType = 'PROJECT'
    expForm.title = ''
    expForm.role = ''
    expForm.description = ''
    expForm.outcome = ''
    expForm.startDate = ''
    expForm.endDate = ''
  }
}

function closeExperienceForm() {
  expForm.open = false
}

async function submitExperience() {
  if (!expForm.title.trim()) {
    expForm.error = '标题不能为空'
    return
  }
  expForm.submitting = true
  expForm.error = ''
  try {
    const payload = {
      expType: expForm.expType,
      title: expForm.title.trim(),
      role: expForm.role.trim() || undefined,
      description: expForm.description.trim() || undefined,
      outcome: expForm.outcome.trim() || undefined,
      startDate: expForm.startDate || undefined,
      endDate: expForm.endDate || undefined
    }
    if (expForm.id) {
      await updateExperience(expForm.id, payload)
    } else {
      await addExperience(payload)
    }
    closeExperienceForm()
    await load()
  } catch (e) {
    expForm.error = (e as Error).message || '保存失败'
  } finally {
    expForm.submitting = false
  }
}

async function onDeleteExperience(id: number) {
  if (!window.confirm('确认删除这条经历？')) return
  try {
    await deleteExperience(id)
    await load()
  } catch (e) {
    loadError.value = (e as Error).message || '删除失败'
  }
}

function levelLabel(v: string) {
  return v === 'INTERMEDIATE' ? '进阶' : v === 'ADVANCED' ? '熟练' : '入门'
}
function expTypeLabel(v: string) {
  return (
    { INTERNSHIP: '实习', PROJECT: '项目', AWARD: '竞赛', COURSE: '课程', RESEARCH: '科研', OTHER: '其他' }[v] ||
    v
  )
}
</script>
