import request from '@/utils/request'
import type { PageResult } from '@/types/api'
import type {
  ConfirmExtractionPayload,
  CreateJournalPayload,
  ExtractionView,
  JournalDetailView,
  JournalSummary,
  JournalView,
  UpdateJournalPayload
} from '@/types/journal'

const AI_REQUEST_TIMEOUT_MS = 90_000

export interface ListJournalParams {
  status?: 'POSTED' | 'ARCHIVED'
  page?: number
  size?: number
}

export function createJournal(payload: CreateJournalPayload): Promise<JournalView> {
  return request.post('/journal', payload) as unknown as Promise<JournalView>
}

export function listJournals(params: ListJournalParams = {}): Promise<PageResult<JournalSummary>> {
  return request.get('/journal', { params }) as unknown as Promise<PageResult<JournalSummary>>
}

export function fetchJournal(id: number): Promise<JournalDetailView> {
  return request.get(`/journal/${id}`) as unknown as Promise<JournalDetailView>
}

export function updateJournal(id: number, payload: UpdateJournalPayload): Promise<JournalView> {
  return request.put(`/journal/${id}`, payload) as unknown as Promise<JournalView>
}

export function extractJournal(id: number): Promise<ExtractionView> {
  return request.post(`/journal/${id}/extract`, undefined, {
    timeout: AI_REQUEST_TIMEOUT_MS
  }) as unknown as Promise<ExtractionView>
}

export function confirmExtraction(id: number, payload: ConfirmExtractionPayload): Promise<ExtractionView> {
  return request.post(`/journal/${id}/extract/confirm`, payload) as unknown as Promise<ExtractionView>
}
