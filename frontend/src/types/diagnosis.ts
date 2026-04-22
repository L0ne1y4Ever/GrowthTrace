// 与后端 diagnosis 模块 DTO/VO 对齐。

export type AiStatus = 'SUCCESS' | 'FALLBACK' | 'FAILED'

/** AI 返回的结构化关键问题 / 建议 / 纠偏方向的条目。宽松类型，字段随 prompt 演进。 */
export type Insight = Record<string, unknown>

export interface RequirementProgress {
  total: number
  met: number
  in_progress: number
  todo: number
  met_ratio: number
}

export interface ActivityPoint {
  date: string
  score: number
}

export interface DiagnosisMetrics {
  journal_count?: number
  journal_streak?: number
  task_completion_rate?: number
  new_skills_count?: number
  profile_completeness?: number
  target_requirement_progress?: RequirementProgress
  activity_intensity?: ActivityPoint[]
  [k: string]: unknown
}

export interface ReviewNotes {
  wins: string[]
  learnings: string[]
  nextFocus: string[]
  userFreeform?: string | null
}

export interface DiagnosisView {
  id: number
  userId: number
  triggerTime?: string
  windowStart?: string
  windowEnd?: string
  profileVersionAtTrigger?: number
  metrics: DiagnosisMetrics
  stageSummary?: string | null
  keyProblems?: Insight[]
  suggestions?: Insight[]
  correctionDirections?: Insight[]
  reviewNotes?: ReviewNotes
  aiStatus: AiStatus
  snapshotId?: number | null
  createdAt?: string
  updatedAt?: string
}

export interface DiagnosisSummary {
  id: number
  triggerTime?: string
  windowStart?: string
  windowEnd?: string
  profileVersionAtTrigger?: number
  stageSummaryExcerpt?: string | null
  keyProblemCount: number
  suggestionCount: number
  aiStatus: AiStatus
  createdAt?: string
}

// ---- payloads ----

export interface TriggerDiagnosisPayload {
  windowDays?: number
}

export interface UpdateReviewNotesPayload {
  wins?: string[]
  learnings?: string[]
  nextFocus?: string[]
  userFreeform?: string
}
