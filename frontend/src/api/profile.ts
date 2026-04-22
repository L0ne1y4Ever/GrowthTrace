import request from '@/utils/request'
import type {
  ExperiencePayload,
  ExperienceView,
  OnboardingConfirmPayload,
  OnboardingDraft,
  OnboardingExtractPayload,
  ProfileView,
  SkillPayload,
  SkillView
} from '@/types/profile'

const AI_REQUEST_TIMEOUT_MS = 90_000

export function extractOnboarding(payload: OnboardingExtractPayload): Promise<OnboardingDraft> {
  return request.post('/profile/onboarding/extract', payload, {
    timeout: AI_REQUEST_TIMEOUT_MS
  }) as unknown as Promise<OnboardingDraft>
}

export function confirmOnboarding(payload: OnboardingConfirmPayload): Promise<void> {
  return request.post('/profile/onboarding/confirm', payload) as unknown as Promise<void>
}

export function fetchProfile(): Promise<ProfileView> {
  return request.get('/profile') as unknown as Promise<ProfileView>
}

export function refreshCompleteness(): Promise<number> {
  return request.post('/profile/completeness/refresh') as unknown as Promise<number>
}

export function addSkill(payload: SkillPayload): Promise<SkillView> {
  return request.post('/profile/skills', payload) as unknown as Promise<SkillView>
}

export function updateSkill(id: number, payload: SkillPayload): Promise<SkillView> {
  return request.put(`/profile/skills/${id}`, payload) as unknown as Promise<SkillView>
}

export function deleteSkill(id: number): Promise<void> {
  return request.delete(`/profile/skills/${id}`) as unknown as Promise<void>
}

export function addExperience(payload: ExperiencePayload): Promise<ExperienceView> {
  return request.post('/profile/experiences', payload) as unknown as Promise<ExperienceView>
}

export function updateExperience(id: number, payload: ExperiencePayload): Promise<ExperienceView> {
  return request.put(`/profile/experiences/${id}`, payload) as unknown as Promise<ExperienceView>
}

export function deleteExperience(id: number): Promise<void> {
  return request.delete(`/profile/experiences/${id}`) as unknown as Promise<void>
}
