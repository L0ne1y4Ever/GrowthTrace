// 与后端 journal 模块 DTO/VO 对齐。

export type JournalStatus = 'POSTED' | 'ARCHIVED'
export type MoodCode = 'GREAT' | 'GOOD' | 'NORMAL' | 'BAD' | 'BLOCKED'
export type ExtractionStatus = 'PENDING_CONFIRM' | 'CONFIRMED' | 'DISCARDED'

export interface JournalView {
  id: number
  userId: number
  content: string
  mood?: MoodCode | null
  tags: string[]
  wordCount: number
  status: JournalStatus
  createdAt?: string
  updatedAt?: string
}

export interface JournalSummary {
  id: number
  contentExcerpt: string
  mood?: MoodCode | null
  tags: string[]
  wordCount: number
  status: JournalStatus
  extractionStatus?: ExtractionStatus | null
  createdAt?: string
}

export interface ExtractionView {
  id: number
  journalId: number
  extractionStatus: ExtractionStatus
  draftNewSkills: Record<string, unknown>[]
  draftRelatedRequirements: Record<string, unknown>[]
  draftEvents: Record<string, unknown>[]
  draftBlockers: string[]
  confirmedNewSkills?: Record<string, unknown>[]
  confirmedRelatedRequirements?: Record<string, unknown>[]
  confirmedEvents?: Record<string, unknown>[]
  confirmedBlockers?: string[]
  confirmedAt?: string | null
  createdAt?: string
  updatedAt?: string
}

export interface JournalDetailView {
  journal: JournalView
  extraction?: ExtractionView | null
}

// ------- payloads -------

export interface CreateJournalPayload {
  content: string
  mood?: MoodCode | ''
  tags?: string[]
}

export interface UpdateJournalPayload {
  content: string
  mood?: MoodCode | ''
  tags?: string[]
  status?: JournalStatus
}

export interface NewSkillConfirm {
  name: string
  level: 'BEGINNER' | 'INTERMEDIATE' | 'ADVANCED'
  category?: 'LANGUAGE' | 'FRAMEWORK' | 'TOOL' | 'DOMAIN' | 'SOFT' | ''
  evidence?: string
}

export interface RequirementUpdateConfirm {
  requirementId: number
  newStatus?: 'TODO' | 'IN_PROGRESS' | 'MET' | ''
  evidence?: string
}

export interface ConfirmExtractionPayload {
  newSkills: NewSkillConfirm[]
  relatedRequirements: RequirementUpdateConfirm[]
  events: Record<string, unknown>[]
  blockers: string[]
}
