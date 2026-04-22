<template>
  <div class="p-6 max-w-3xl mx-auto space-y-6">
    <header>
      <h1 class="text-2xl font-semibold text-slate-800">建档引导</h1>
      <p class="text-sm text-slate-500 mt-1">
        写一段自然语言自述，由 AI 抽取画像草稿。草稿你可以编辑，确认后才会正式写入档案。
        <span class="text-slate-400">每次确认会把画像版本号 +1。</span>
      </p>
    </header>

    <!-- 1. 原始输入 -->
    <section class="bg-white border border-slate-200 rounded-lg p-6 space-y-4">
      <h2 class="text-sm font-medium text-slate-700">1. 自我描述</h2>

      <div>
        <label for="ob-self-intro" class="block text-sm text-slate-600 mb-1">一句话介绍自己（可选）</label>
        <input
          id="ob-self-intro"
          v-model="selfIntro"
          type="text"
          maxlength="500"
          class="w-full px-3 py-2 border border-slate-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-brand-500 focus:border-brand-500"
          placeholder="留空会自动取自述的前 200 字"
        />
      </div>

      <div>
        <label for="ob-raw-text" class="block text-sm text-slate-600 mb-1">
          完整自述 <span class="text-red-500">*</span>
          <span class="text-xs text-slate-400">（10–4000 字，越具体越好：擅长、不擅长、经历）</span>
        </label>
        <textarea
          id="ob-raw-text"
          v-model="rawText"
          rows="8"
          required
          minlength="10"
          maxlength="4000"
          class="w-full px-3 py-2 border border-slate-300 rounded-md text-sm font-sans focus:outline-none focus:ring-2 focus:ring-brand-500 focus:border-brand-500"
          placeholder="我是 XX 大学计算机系大三学生，主要方向是 Java 后端。做过一个电商秒杀的课程项目，接触过 Redis 和 MQ……"
        />
        <div class="text-xs text-slate-400 mt-1">{{ rawText.length }} / 4000</div>
      </div>

      <div class="flex items-center gap-3">
        <button
          type="button"
          :disabled="extracting || rawText.trim().length < 10"
          class="px-4 py-2 rounded-md bg-brand-600 text-white text-sm font-medium hover:bg-brand-700 disabled:opacity-60 disabled:cursor-not-allowed"
          @click="onExtract"
        >
          {{ extracting ? 'AI 抽取中…（最长 30 秒）' : (draft ? '重新抽取' : 'AI 抽取画像草稿') }}
        </button>
        <span v-if="draft" class="text-xs text-slate-400">草稿已生成，编辑确认后写入</span>
      </div>

      <div v-if="extractError" class="text-sm text-red-600 bg-red-50 border border-red-100 rounded px-3 py-2">
        {{ extractError }}
      </div>
    </section>

    <!-- 2. 草稿编辑 -->
    <section v-if="draft" class="bg-white border border-slate-200 rounded-lg p-6 space-y-5">
      <h2 class="text-sm font-medium text-slate-700">2. 编辑草稿</h2>

      <!-- summary -->
      <div>
        <label class="block text-sm text-slate-600 mb-1">画像总结</label>
        <textarea
          v-model="summary"
          rows="3"
          maxlength="800"
          class="w-full px-3 py-2 border border-slate-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-brand-500"
        />
      </div>

      <!-- strengths & weaknesses -->
      <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div class="space-y-2">
          <div class="flex items-center justify-between">
            <label class="text-sm text-slate-600">擅长</label>
            <button type="button" class="text-xs text-brand-600 hover:underline" @click="strengths.push('')">+ 添加</button>
          </div>
          <div v-for="(_, i) in strengths" :key="`s-${i}`" class="flex items-center gap-2">
            <input
              v-model="strengths[i]"
              type="text"
              maxlength="64"
              class="flex-1 px-3 py-1.5 border border-slate-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-brand-500"
            />
            <button type="button" class="text-xs text-slate-400 hover:text-red-600" @click="strengths.splice(i, 1)">删除</button>
          </div>
          <div v-if="strengths.length === 0" class="text-xs text-slate-400">（空）</div>
        </div>

        <div class="space-y-2">
          <div class="flex items-center justify-between">
            <label class="text-sm text-slate-600">不擅长</label>
            <button type="button" class="text-xs text-brand-600 hover:underline" @click="weaknesses.push('')">+ 添加</button>
          </div>
          <div v-for="(_, i) in weaknesses" :key="`w-${i}`" class="flex items-center gap-2">
            <input
              v-model="weaknesses[i]"
              type="text"
              maxlength="64"
              class="flex-1 px-3 py-1.5 border border-slate-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-brand-500"
            />
            <button type="button" class="text-xs text-slate-400 hover:text-red-600" @click="weaknesses.splice(i, 1)">删除</button>
          </div>
          <div v-if="weaknesses.length === 0" class="text-xs text-slate-400">（空）</div>
        </div>
      </div>

      <!-- skills -->
      <div class="space-y-2">
        <div class="flex items-center justify-between">
          <label class="text-sm text-slate-600">技能</label>
          <button
            type="button"
            class="text-xs text-brand-600 hover:underline"
            @click="skills.push({ skillName: '', skillLevel: 'BEGINNER', category: 'DOMAIN' })"
          >
            + 添加
          </button>
        </div>
        <div v-for="(item, i) in skills" :key="`k-${i}`" class="grid grid-cols-12 gap-2 items-center">
          <input
            v-model="item.skillName"
            type="text"
            maxlength="128"
            placeholder="技能名"
            class="col-span-5 px-3 py-1.5 border border-slate-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-brand-500"
          />
          <select
            v-model="item.skillLevel"
            class="col-span-3 px-2 py-1.5 border border-slate-300 rounded-md text-sm bg-white focus:outline-none focus:ring-2 focus:ring-brand-500"
          >
            <option value="BEGINNER">入门</option>
            <option value="INTERMEDIATE">进阶</option>
            <option value="ADVANCED">熟练</option>
          </select>
          <select
            v-model="item.category"
            class="col-span-3 px-2 py-1.5 border border-slate-300 rounded-md text-sm bg-white focus:outline-none focus:ring-2 focus:ring-brand-500"
          >
            <option value="LANGUAGE">LANGUAGE</option>
            <option value="FRAMEWORK">FRAMEWORK</option>
            <option value="TOOL">TOOL</option>
            <option value="DOMAIN">DOMAIN</option>
            <option value="SOFT">SOFT</option>
          </select>
          <button type="button" class="col-span-1 text-xs text-slate-400 hover:text-red-600" @click="skills.splice(i, 1)">删除</button>
        </div>
        <div v-if="skills.length === 0" class="text-xs text-slate-400">（空）</div>
      </div>

      <!-- experiences -->
      <div class="space-y-2">
        <div class="flex items-center justify-between">
          <label class="text-sm text-slate-600">经历</label>
          <button
            type="button"
            class="text-xs text-brand-600 hover:underline"
            @click="experiences.push({ expType: 'PROJECT', title: '', role: '', outcome: '' })"
          >
            + 添加
          </button>
        </div>
        <div
          v-for="(item, i) in experiences"
          :key="`e-${i}`"
          class="border border-slate-200 rounded-md p-3 space-y-2"
        >
          <div class="grid grid-cols-12 gap-2">
            <select
              v-model="item.expType"
              class="col-span-3 px-2 py-1.5 border border-slate-300 rounded-md text-sm bg-white focus:outline-none focus:ring-2 focus:ring-brand-500"
            >
              <option value="INTERNSHIP">实习</option>
              <option value="PROJECT">项目</option>
              <option value="AWARD">竞赛</option>
              <option value="COURSE">课程</option>
              <option value="RESEARCH">科研</option>
              <option value="OTHER">其他</option>
            </select>
            <input
              v-model="item.title"
              type="text"
              maxlength="255"
              placeholder="标题"
              class="col-span-8 px-3 py-1.5 border border-slate-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-brand-500"
            />
            <button type="button" class="col-span-1 text-xs text-slate-400 hover:text-red-600" @click="experiences.splice(i, 1)">删除</button>
          </div>
          <div class="grid grid-cols-12 gap-2">
            <input
              v-model="item.role"
              type="text"
              maxlength="128"
              placeholder="角色（可选）"
              class="col-span-6 px-3 py-1.5 border border-slate-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-brand-500"
            />
            <input
              v-model="item.startDate"
              type="date"
              class="col-span-3 px-2 py-1.5 border border-slate-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-brand-500"
            />
            <input
              v-model="item.endDate"
              type="date"
              class="col-span-3 px-2 py-1.5 border border-slate-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-brand-500"
            />
          </div>
          <textarea
            v-model="item.outcome"
            rows="2"
            maxlength="2000"
            placeholder="产出 / 学到什么（可选）"
            class="w-full px-3 py-1.5 border border-slate-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-brand-500"
          />
        </div>
        <div v-if="experiences.length === 0" class="text-xs text-slate-400">（空）</div>
      </div>

      <!-- commit -->
      <div class="flex items-center gap-3 pt-2 border-t border-slate-100">
        <button
          type="button"
          :disabled="confirming || rawText.trim().length < 10"
          class="px-4 py-2 rounded-md bg-brand-600 text-white text-sm font-medium hover:bg-brand-700 disabled:opacity-60 disabled:cursor-not-allowed"
          @click="onConfirm"
        >
          {{ confirming ? '写入中…' : '确认入档' }}
        </button>
        <button
          type="button"
          class="px-4 py-2 rounded-md border border-slate-300 text-sm text-slate-600 hover:bg-slate-50"
          @click="resetDraft"
        >
          丢弃草稿
        </button>
        <span v-if="confirmError" class="text-sm text-red-600">{{ confirmError }}</span>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, toRefs } from 'vue'
import { useRouter } from 'vue-router'
import {
  confirmOnboarding as apiConfirm,
  extractOnboarding as apiExtract
} from '@/api/profile'
import type {
  ExperiencePayload,
  OnboardingDraft,
  SkillPayload
} from '@/types/profile'

const router = useRouter()

const selfIntro = ref('')
const rawText = ref('')

const draft = ref<OnboardingDraft | null>(null)

const summary = ref('')
const strengths = reactive<string[]>([])
const weaknesses = reactive<string[]>([])
const skills = reactive<SkillPayload[]>([])
const experiences = reactive<ExperiencePayload[]>([])

const extracting = ref(false)
const extractError = ref('')
const confirming = ref(false)
const confirmError = ref('')

async function onExtract() {
  if (rawText.value.trim().length < 10) {
    extractError.value = '自述至少 10 字'
    return
  }
  extractError.value = ''
  extracting.value = true
  try {
    const res = await apiExtract({ rawText: rawText.value.trim() })
    applyDraft(res)
  } catch (e) {
    extractError.value = (e as Error).message || 'AI 抽取失败，请检查 AI_API_KEY 或稍后重试'
  } finally {
    extracting.value = false
  }
}

function applyDraft(d: OnboardingDraft) {
  draft.value = d
  summary.value = d.summary ?? ''
  strengths.splice(0, strengths.length, ...(d.strengths ?? []))
  weaknesses.splice(0, weaknesses.length, ...(d.weaknesses ?? []))

  skills.splice(
    0,
    skills.length,
    ...(d.skills ?? []).map((s) => ({
      skillName: (s.name ?? '').trim(),
      skillLevel: normalizeLevel(s.level),
      category: normalizeCategory(s.category)
    }))
  )

  experiences.splice(
    0,
    experiences.length,
    ...(d.experiences ?? []).map((e) => ({
      expType: normalizeExpType(e.type),
      title: (e.title ?? '').trim(),
      role: e.role ?? '',
      outcome: e.outcome ?? '',
      description: e.description ?? '',
      startDate: e.startDate ?? '',
      endDate: e.endDate ?? ''
    }))
  )
}

function resetDraft() {
  draft.value = null
  summary.value = ''
  strengths.splice(0, strengths.length)
  weaknesses.splice(0, weaknesses.length)
  skills.splice(0, skills.length)
  experiences.splice(0, experiences.length)
}

async function onConfirm() {
  confirmError.value = ''
  confirming.value = true
  try {
    await apiConfirm({
      rawText: rawText.value.trim(),
      selfIntro: selfIntro.value.trim() || undefined,
      summary: summary.value.trim() || undefined,
      strengths: strengths.filter((s) => s.trim() !== ''),
      weaknesses: weaknesses.filter((s) => s.trim() !== ''),
      skills: skills
        .filter((s) => s.skillName.trim() !== '')
        .map((s) => ({
          skillName: s.skillName.trim(),
          skillLevel: s.skillLevel,
          category: s.category || undefined,
          evidence: s.evidence
        })),
      experiences: experiences
        .filter((e) => e.title.trim() !== '')
        .map((e) => ({
          expType: e.expType,
          title: e.title.trim(),
          role: e.role || undefined,
          description: e.description || undefined,
          outcome: e.outcome || undefined,
          startDate: e.startDate || undefined,
          endDate: e.endDate || undefined
        }))
    })
    router.push('/profile')
  } catch (e) {
    confirmError.value = (e as Error).message || '入档失败，请稍后重试'
  } finally {
    confirming.value = false
  }
}

// 用 toRefs 仅为消除未使用变量 lint（strengths/weaknesses 在模板中用到）
void toRefs

function normalizeLevel(raw: string | undefined): 'BEGINNER' | 'INTERMEDIATE' | 'ADVANCED' {
  const v = String(raw ?? '').toUpperCase()
  if (v === 'INTERMEDIATE' || v === 'ADVANCED') return v
  return 'BEGINNER'
}
function normalizeCategory(raw: string | undefined): '' | 'LANGUAGE' | 'FRAMEWORK' | 'TOOL' | 'DOMAIN' | 'SOFT' {
  const v = String(raw ?? '').toUpperCase()
  if (v === 'LANGUAGE' || v === 'FRAMEWORK' || v === 'TOOL' || v === 'DOMAIN' || v === 'SOFT') return v
  return 'DOMAIN'
}
function normalizeExpType(raw: string | undefined) {
  const v = String(raw ?? '').toUpperCase()
  const allowed = ['INTERNSHIP', 'PROJECT', 'AWARD', 'COURSE', 'RESEARCH', 'OTHER'] as const
  return (allowed as readonly string[]).includes(v) ? (v as (typeof allowed)[number]) : 'PROJECT'
}
</script>
