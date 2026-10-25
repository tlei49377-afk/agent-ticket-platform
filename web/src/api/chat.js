import request from './request'

/**
 * 当前后端的 Agent 输出是原生 SSE，且鉴权依赖 token 请求头。
 * EventSource 不能自定义请求头，所以这里使用 fetch + ReadableStream 手动解析。
 */
export async function streamChat({ memoryId, message, onChunk, signal }) {
  const params = new URLSearchParams({ memoryId, message })
  const token = localStorage.getItem('atp_token') || ''
  const response = await fetch('/api/chat/stream?' + params.toString(), {
    method: 'GET',
    headers: {
      Accept: 'text/event-stream',
      token
    },
    signal
  })

  if (response.status === 401) {
    localStorage.removeItem('atp_token')
    localStorage.removeItem('atp_user')
    throw new Error('登录已过期，请重新登录')
  }
  if (!response.ok) {
    let detail = `HTTP ${response.status}`
    try {
      const body = await response.json()
      detail = body.msg || body.message || detail
    } catch (e) {
      // 非 JSON 错误响应保留 HTTP 状态
    }
    throw new Error(detail)
  }
  if (!response.body) throw new Error('浏览器不支持流式响应')

  const reader = response.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''

  const emitEvent = (rawEvent) => {
    const dataLines = rawEvent
      .split(/\r?\n/)
      .filter((line) => line.startsWith('data:'))
      .map((line) => line.slice(5).replace(/^ /, ''))
    if (dataLines.length) onChunk(dataLines.join('\n'))
  }

  while (true) {
    const { done, value } = await reader.read()
    if (done) break
    buffer += decoder.decode(value, { stream: true })

    let boundary = buffer.search(/\r?\n\r?\n/)
    while (boundary >= 0) {
      const rawEvent = buffer.slice(0, boundary)
      const separator = buffer.match(/\r?\n\r?\n/)?.[0] || '\n\n'
      buffer = buffer.slice(boundary + separator.length)
      emitEvent(rawEvent)
      boundary = buffer.search(/\r?\n\r?\n/)
    }
  }

  buffer += decoder.decode()
  if (buffer.trim()) emitEvent(buffer)
}

export const createSession = (agentId = 1) =>
  request.post('/agents/sessions', null, { params: { agentId } })

export const listSessions = () => request.get('/agents/sessions')

export const listSessionMessages = (memoryId) =>
  request.get('/agents/sessions/messages', { params: { memoryId } })

export const deleteSession = (memoryId) =>
  request.delete('/agents/sessions', { params: { memoryId } })

export const getLatestRun = (memoryId) =>
  request.get('/agents/sessions/latest-run', { params: { memoryId } })

export const getRun = (runId) => request.get('/agents/runs/' + runId)

export const getRunSteps = (runId) => request.get('/agents/runs/' + runId + '/steps')

export const getReferences = (runId) => request.get('/agents/runs/' + runId + '/references')
