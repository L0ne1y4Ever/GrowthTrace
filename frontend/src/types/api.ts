export interface ApiResult<T> {
  code: number
  message: string
  data: T
  timestamp?: number
}

export interface PageResult<T> {
  records: T[]
  total: number
  page: number
  size: number
}

export interface UserInfo {
  id: number
  username: string
  nickname?: string
  email?: string
  avatarUrl?: string
}

export interface LoginPayload {
  username: string
  password: string
}

export interface RegisterPayload {
  username: string
  password: string
  nickname?: string
  email?: string
}

export interface LoginSession {
  accessToken: string
  expiresInSeconds: number
  user: UserInfo
}
