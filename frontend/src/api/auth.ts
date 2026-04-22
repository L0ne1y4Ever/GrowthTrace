import request from '@/utils/request'
import type { LoginPayload, LoginSession, RegisterPayload, UserInfo } from '@/types/api'

export function login(payload: LoginPayload): Promise<LoginSession> {
  return request.post('/auth/login', payload) as unknown as Promise<LoginSession>
}

export function register(payload: RegisterPayload): Promise<void> {
  return request.post('/auth/register', payload) as unknown as Promise<void>
}

export function fetchCurrentUser(): Promise<UserInfo> {
  return request.get('/auth/me') as unknown as Promise<UserInfo>
}
