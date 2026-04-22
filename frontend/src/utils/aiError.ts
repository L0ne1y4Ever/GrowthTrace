const AI_UNAVAILABLE = 50010
const AI_PARSE_ERROR = 50011
const AI_TIMEOUT = 50012

type ErrorWithCode = Error & { code?: number }

export function explainAiError(error: unknown, fallback: string): string {
  const err = error as ErrorWithCode | undefined
  const code = typeof err?.code === 'number' ? err.code : undefined
  const message = typeof err?.message === 'string' ? err.message : ''

  if (code === AI_TIMEOUT || /timeout/i.test(message) || message.includes('超时')) {
    return 'AI 调用超时，模型响应较慢。请稍后重试，或改用更快的模型。'
  }
  if (code === AI_PARSE_ERROR || message.includes('格式异常') || message.includes('缺少 choices')) {
    return 'AI 返回格式异常，当前模型可能与 OpenAI-compatible 响应不完全兼容。'
  }
  if (code === AI_UNAVAILABLE) {
    if (message.includes('apiKey') || message.includes('baseUrl') || message.includes('model')) {
      return 'AI 配置不完整，请检查 baseUrl、apiKey 和 model 是否正确。'
    }
    if (/HTTP 401|HTTP 403/.test(message)) {
      return 'AI 鉴权失败，请检查 apiKey 是否有效。'
    }
    if (/HTTP 404/.test(message)) {
      return 'AI 接口地址不正确，请检查 baseUrl 是否指向 /chat/completions 的上一级。'
    }
    return 'AI 服务暂不可用，请检查模型服务状态、baseUrl 和网络连接。'
  }

  return message || fallback
}
