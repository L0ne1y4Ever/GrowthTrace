import request from '@/utils/request'
import type {
  CreateTargetPayload,
  RequirementPayload,
  RequirementView,
  TargetDetailView,
  TargetTemplateVO,
  TargetView,
  UpdateRequirementStatusPayload,
  UpdateTargetPayload,
  UpdateTargetStatusPayload
} from '@/types/target'

// ---- templates ----

export function fetchTemplates(): Promise<TargetTemplateVO[]> {
  return request.get('/target/templates') as unknown as Promise<TargetTemplateVO[]>
}

// ---- targets ----

export function createTarget(payload: CreateTargetPayload): Promise<TargetDetailView> {
  return request.post('/target', payload) as unknown as Promise<TargetDetailView>
}

export function listTargets(params?: { status?: 'ACTIVE' | 'ACHIEVED' | 'ABANDONED' }): Promise<TargetView[]> {
  return request.get('/target', { params }) as unknown as Promise<TargetView[]>
}

export function fetchTargetDetail(id: number): Promise<TargetDetailView> {
  return request.get(`/target/${id}`) as unknown as Promise<TargetDetailView>
}

export function updateTarget(id: number, payload: UpdateTargetPayload): Promise<TargetView> {
  return request.put(`/target/${id}`, payload) as unknown as Promise<TargetView>
}

export function updateTargetStatus(id: number, payload: UpdateTargetStatusPayload): Promise<TargetView> {
  return request.put(`/target/${id}/status`, payload) as unknown as Promise<TargetView>
}

export function setPrimaryTarget(id: number): Promise<TargetView> {
  return request.post(`/target/${id}/primary`) as unknown as Promise<TargetView>
}

export function deleteTarget(id: number): Promise<void> {
  return request.delete(`/target/${id}`) as unknown as Promise<void>
}

// ---- requirements ----

export function addRequirement(targetId: number, payload: RequirementPayload): Promise<RequirementView> {
  return request.post(`/target/${targetId}/requirements`, payload) as unknown as Promise<RequirementView>
}

export function updateRequirement(targetId: number, reqId: number, payload: RequirementPayload): Promise<RequirementView> {
  return request.put(`/target/${targetId}/requirements/${reqId}`, payload) as unknown as Promise<RequirementView>
}

export function updateRequirementStatus(targetId: number, reqId: number, payload: UpdateRequirementStatusPayload): Promise<RequirementView> {
  return request.put(`/target/${targetId}/requirements/${reqId}/status`, payload) as unknown as Promise<RequirementView>
}

export function deleteRequirement(targetId: number, reqId: number): Promise<void> {
  return request.delete(`/target/${targetId}/requirements/${reqId}`) as unknown as Promise<void>
}
