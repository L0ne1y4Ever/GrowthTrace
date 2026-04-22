// 与后端 target 模块 DTO/VO 对齐。

export type TargetType = 'JOB_SEEKING' | 'POSTGRAD' | 'SKILL_GROWTH'
export type TargetStatus = 'ACTIVE' | 'ACHIEVED' | 'ABANDONED'
export type RequirementType = 'SKILL' | 'KNOWLEDGE' | 'EXPERIENCE' | 'OTHER'
export type RequirementStatus = 'TODO' | 'IN_PROGRESS' | 'MET'

export interface RequirementTemplateVO {
  reqName: string
  reqType: RequirementType
  description?: string
}

export interface TargetTemplateVO {
  templateKey: string
  targetType: TargetType
  title: string
  description?: string
  defaultRequirements: RequirementTemplateVO[]
}

export interface TargetView {
  id: number
  userId: number
  targetType: TargetType
  title: string
  description?: string
  templateKey?: string | null
  status: TargetStatus
  deadline?: string | null
  achievedAt?: string | null
  isPrimary: boolean
  requirementCount: number
  requirementMetCount: number
  createdAt?: string
  updatedAt?: string
}

export interface RequirementView {
  id: number
  targetId: number
  reqName: string
  reqType: RequirementType
  description?: string
  status: RequirementStatus
  sortOrder: number
  dueDate?: string | null
  linkedSkillId?: number | null
  linkedExperienceId?: number | null
  progress: number
  createdAt?: string
  updatedAt?: string
}

export interface TargetDetailView {
  target: TargetView
  requirements: RequirementView[]
}

// ------- payloads -------

export interface RequirementPayload {
  reqName: string
  reqType: RequirementType
  description?: string
  sortOrder?: number
  dueDate?: string
  status?: RequirementStatus | ''
  progress?: number
  linkedSkillId?: number | null
  linkedExperienceId?: number | null
}

export interface CreateTargetPayload {
  targetType: TargetType
  title: string
  description?: string
  templateKey?: string
  deadline?: string
  isPrimary?: boolean
  requirements?: RequirementPayload[]
}

export interface UpdateTargetPayload {
  title: string
  description?: string
  deadline?: string
}

export interface UpdateTargetStatusPayload {
  status: TargetStatus
}

export interface UpdateRequirementStatusPayload {
  status: RequirementStatus
  progress?: number
}
