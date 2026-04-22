import axios, {
  AxiosError,
  type AxiosInstance,
  type AxiosResponse,
  type InternalAxiosRequestConfig
} from 'axios'
import { useAuthStore } from '@/stores/auth'
import type { ApiResult } from '@/types/api'

const SUCCESS_CODE = 0

const request: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 30_000
})

request.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const auth = useAuthStore()
  if (auth.token && config.headers) {
    config.headers.Authorization = `Bearer ${auth.token}`
  }
  return config
})

// 约定：后端 R<T> 成功时 code===0，拦截器把 R.data 直接抛给上层；
// 失败时抛 Error（带 code）。Axios 1.7+ 要求 success 回调返回 AxiosResponse，
// 这里用 `as unknown as AxiosResponse` 让类型过关，实际运行时就是返回 data.data。
request.interceptors.response.use(
  (response: AxiosResponse<ApiResult<unknown>>) => {
    const body = response.data
    if (body && typeof body === 'object' && 'code' in body && 'data' in body) {
      if (body.code === SUCCESS_CODE) {
        return body.data as unknown as AxiosResponse
      }
      const err = new Error(body.message || '请求失败') as Error & { code?: number }
      err.code = body.code
      return Promise.reject(err)
    }
    return body as unknown as AxiosResponse
  },
  (error: AxiosError<ApiResult<unknown>>) => {
    const status = error.response?.status
    if (status === 401) {
      const auth = useAuthStore()
      auth.clear()
      if (typeof window !== 'undefined' && !window.location.pathname.startsWith('/login')) {
        const redirect = encodeURIComponent(window.location.pathname + window.location.search)
        window.location.href = `/login?redirect=${redirect}`
      }
    }
    const body = error.response?.data
    const message =
      (body && typeof body === 'object' && 'message' in body ? (body as { message?: string }).message : error.message) ||
      '网络异常'
    return Promise.reject(new Error(String(message)))
  }
)

export default request
