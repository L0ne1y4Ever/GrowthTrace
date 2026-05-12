// 与后端 execution 模块 DTO/VO 对齐。

export type TaskStatus = 'TODO' | 'IN_PROGRESS' | 'DONE' | 'ABANDONED'
export type TaskPriority = 'HIGH' | 'MEDIUM' | 'LOW'

export interface TaskView {
  id: number
  userId: number
  targetId?: number | null
  requirementId?: number | null
  targetTitle?: string | null
  requirementName?: string | null
  requirementStatus?: string | null
  title: string
  description?: string | null
  status: TaskStatus
  priority: TaskPriority
  dueDate?: string | null
  completedAt?: string | null
  checkInDates: string[]
  checkInCount: number
  plannedEffortMinutes?: number | null
  actualEffortMinutes: number
  createdAt?: string
  updatedAt?: string
  checkedInToday: boolean
}

export interface TaskDraftView {
  aiStatus: 'SUCCESS' | 'FALLBACK'
  title: string
  description: string
  priority: TaskPriority
  dueDate?: string | null
  plannedEffortMinutes?: number | null
  acceptanceCriteria: string[]
  checkInPlan: string[]
  evidenceSuggestions: string[]
}

export interface WeeklyProgressView {
  weekStart: string
  weekEnd: string
  dueThisWeek: number
  doneThisWeek: number
  completionRate: number
  checkInPerDay: { date: string; count: number }[]
}

// ---- payloads ----

export interface CreateTaskPayload {
  title: string
  description?: string
  priority?: TaskPriority
  dueDate?: string
  targetId?: number | null
  requirementId?: number | null
  plannedEffortMinutes?: number | null
}

export interface UpdateTaskPayload {
  title: string
  description?: string
  priority?: TaskPriority
  dueDate?: string
  targetId?: number | null
  requirementId?: number | null
  plannedEffortMinutes?: number | null
}

export interface UpdateTaskStatusPayload {
  status: TaskStatus
  completionEvidence?: string
  effortMinutes?: number
}

export interface CheckInPayload {
  date?: string
  effortMinutes?: number
}

export interface GenerateTaskDraftPayload {
  sourceType?: 'REQUIREMENT' | 'DIAGNOSIS_SUGGESTION' | 'DIAGNOSIS_CORRECTION' | 'MANUAL'
  targetId?: number | null
  requirementId?: number | null
  title?: string
  description?: string
}
