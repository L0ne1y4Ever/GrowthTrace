import request from '@/utils/request'
import type { PageResult } from '@/types/api'
import type {
  DiagnosisSummary,
  DiagnosisView,
  TriggerDiagnosisPayload,
  UpdateReviewNotesPayload
} from '@/types/diagnosis'

const AI_REQUEST_TIMEOUT_MS = 90_000

export function triggerDiagnosis(payload: TriggerDiagnosisPayload = {}): Promise<DiagnosisView> {
  return request.post('/diagnosis/trigger', payload, {
    timeout: AI_REQUEST_TIMEOUT_MS
  }) as unknown as Promise<DiagnosisView>
}

export interface ListDiagnosisParams {
  page?: number
  size?: number
}

export function listDiagnosisHistory(params: ListDiagnosisParams = {}): Promise<PageResult<DiagnosisSummary>> {
  return request.get('/diagnosis/history', { params }) as unknown as Promise<PageResult<DiagnosisSummary>>
}

export function fetchDiagnosis(id: number): Promise<DiagnosisView> {
  return request.get(`/diagnosis/${id}`) as unknown as Promise<DiagnosisView>
}

export function updateDiagnosisReview(
  id: number,
  payload: UpdateReviewNotesPayload
): Promise<DiagnosisView> {
  return request.put(`/diagnosis/${id}/review`, payload) as unknown as Promise<DiagnosisView>
}
