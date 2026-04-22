// 与后端 profile 模块 DTO/VO 对齐。字段一一对照 schema.sql + ProfileServiceImpl。

export type SkillLevel = 'BEGINNER' | 'INTERMEDIATE' | 'ADVANCED'
export type SkillCategory = 'LANGUAGE' | 'FRAMEWORK' | 'TOOL' | 'DOMAIN' | 'SOFT'
export type SkillStatus = 'ACTIVE' | 'ARCHIVED'
export type ExperienceType =
  | 'INTERNSHIP'
  | 'PROJECT'
  | 'AWARD'
  | 'COURSE'
  | 'RESEARCH'
  | 'OTHER'

export interface SkillView {
  id: number
  skillName: string
  skillLevel: SkillLevel
  category?: SkillCategory
  addedInProfileVersion?: number
  source?: string
  status?: SkillStatus
  evidence?: string
  createdAt?: string
  updatedAt?: string
}

export interface ExperienceView {
  id: number
  expType: ExperienceType
  title: string
  description?: string
  role?: string
  outcome?: string
  startDate?: string
  endDate?: string
  addedInProfileVersion?: number
  source?: string
  createdAt?: string
  updatedAt?: string
}

export interface ProfileView {
  id: number
  userId: number
  version: number
  selfIntro?: string
  strengths: string[]
  weaknesses: string[]
  summary?: string
  completeness: number
  source?: string
  createdAt?: string
  updatedAt?: string
  skills: SkillView[]
  experiences: ExperienceView[]
}

// ------- draft / payload -------

export interface SkillDraft {
  name?: string
  level?: string
  category?: string
}

export interface ExperienceDraft {
  type?: string
  title?: string
  role?: string
  outcome?: string
  description?: string
  startDate?: string
  endDate?: string
}

/** 后端 AI 返回的画像草稿；字段与 ProfileExtractResult 对齐。 */
export interface OnboardingDraft {
  summary?: string
  strengths?: string[]
  weaknesses?: string[]
  skills?: SkillDraft[]
  experiences?: ExperienceDraft[]
  completenessHint?: number
}

export interface SkillPayload {
  skillName: string
  skillLevel: SkillLevel
  category?: SkillCategory | ''
  evidence?: string
}

export interface ExperiencePayload {
  expType: ExperienceType
  title: string
  role?: string
  description?: string
  outcome?: string
  startDate?: string
  endDate?: string
}

export interface OnboardingExtractPayload {
  rawText: string
}

export interface OnboardingConfirmPayload {
  rawText: string
  selfIntro?: string
  summary?: string
  strengths: string[]
  weaknesses: string[]
  skills: SkillPayload[]
  experiences: ExperiencePayload[]
}
