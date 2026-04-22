// 与后端 dashboard 模块 DTO/VO 对齐。

import type { WeeklyProgressView } from '@/types/execution'

export interface HeatmapPoint {
  date: string
  score: number
}

export interface GrowthCurvePoint {
  snapshotId: number
  snapshotTime?: string
  date?: string
  profileVersion?: number
  completeness?: number | null
  taskCompletionRate?: number | null
  newSkillsCount?: number | null
}

export interface PrimaryTargetView {
  id: number
  targetType: string
  title: string
  description?: string | null
  deadline?: string | null
  requirementCount: number
  requirementMetCount: number
  metRatio: number
  updatedAt?: string
}

export interface LatestDiagnosisDigest {
  id: number
  triggerTime?: string
  stageSummaryExcerpt?: string | null
  topSuggestions: string[]
  topCorrections: string[]
  aiStatus: 'SUCCESS' | 'FALLBACK' | 'FAILED'
}

export interface JournalDigest {
  id: number
  contentExcerpt?: string | null
  mood?: string | null
  wordCount?: number | null
  createdAt?: string
  extractionStatus?: 'PENDING_CONFIRM' | 'CONFIRMED' | 'DISCARDED' | null
}

export interface DashboardOverviewView {
  profileCompleteness?: number | null
  primaryTarget?: PrimaryTargetView | null
  weeklyTask?: WeeklyProgressView | null
  latestDiagnosis?: LatestDiagnosisDigest | null
  recentJournals?: JournalDigest[] | null
  growthCurve?: GrowthCurvePoint[] | null
  heatmap?: HeatmapPoint[] | null
}
