import axios, { type AxiosError, type InternalAxiosRequestConfig } from 'axios'

const ACCESS_TOKEN_KEY = 'orbit_access_token'
const REFRESH_TOKEN_KEY = 'orbit_refresh_token'
const API_URL = import.meta.env.VITE_API_URL || '/api'

export type ApiResponse<T> = { success: boolean; message: string; data: T; timestamp?: string }
export type AuthTokens = { token: string; refreshToken: string }
export type UserProfile = { id: number; name: string; email: string; role: string }
export type PageResponse<T> = { content: T[]; page: number; size: number; totalElements: number; totalPages: number; first: boolean; last: boolean }
export type MessageRole = 'USER' | 'ASSISTANT' | 'SYSTEM'
export type ChatMessageResponse = { id: number; role: MessageRole; content: string; createdAt: string }
export type ConversationSummary = { id: number; title: string; createdAt: string; updatedAt: string }
export type ConversationResponse = ConversationSummary & { messages: ChatMessageResponse[] }
export type AiChatTurnResponse = { userMessage: ChatMessageResponse; assistantMessage: ChatMessageResponse }
export type DocumentType = 'PDF' | 'DOCX' | 'TXT'
export type DocumentProcessingStatus = 'UPLOADED' | 'PROCESSING' | 'READY' | 'FAILED'
export type DocumentResponse = {
  id: number
  originalFileName: string
  contentType: string
  fileSize: number
  documentType: DocumentType
  processingStatus: DocumentProcessingStatus
  extractionError: string | null
  createdAt: string
  updatedAt: string
}
export type DocumentTextResponse = {
  id: number
  originalFileName: string
  processingStatus: DocumentProcessingStatus
  text: string | null
}

export type StreamCallbacks = {
  onUserMessage: (message: ChatMessageResponse) => void
  onToken: (token: string) => void
  onComplete: (message: ChatMessageResponse) => void
  onError: (message: string) => void
}

export type SseEvent = { event: string; data: string }

export class ApiRequestError extends Error {
  status: number

  constructor(message: string, status: number) {
    super(message)
    this.name = 'ApiRequestError'
    this.status = status
  }
}

export class SseParseError extends Error {
  constructor(message: string) {
    super(message)
    this.name = 'SseParseError'
  }
}

function usesPersistentStorage() { return localStorage.getItem(REFRESH_TOKEN_KEY) !== null }

export const tokenStore = {
  getAccess: () => localStorage.getItem(ACCESS_TOKEN_KEY) || sessionStorage.getItem(ACCESS_TOKEN_KEY),
  getRefresh: () => localStorage.getItem(REFRESH_TOKEN_KEY) || sessionStorage.getItem(REFRESH_TOKEN_KEY),
  set: (tokens: AuthTokens, persistent = usesPersistentStorage()) => { const storage = persistent ? localStorage : sessionStorage; const otherStorage = persistent ? sessionStorage : localStorage; otherStorage.removeItem(ACCESS_TOKEN_KEY); otherStorage.removeItem(REFRESH_TOKEN_KEY); storage.setItem(ACCESS_TOKEN_KEY, tokens.token); storage.setItem(REFRESH_TOKEN_KEY, tokens.refreshToken) },
  clear: () => { localStorage.removeItem(ACCESS_TOKEN_KEY); localStorage.removeItem(REFRESH_TOKEN_KEY); sessionStorage.removeItem(ACCESS_TOKEN_KEY); sessionStorage.removeItem(REFRESH_TOKEN_KEY) },
}

export const api = axios.create({ baseURL: API_URL, headers: { 'Content-Type': 'application/json' } })
const refreshClient = axios.create({ baseURL: API_URL, headers: { 'Content-Type': 'application/json' } })
let refreshPromise: Promise<string | null> | null = null

async function refreshAccessToken() {
  const refreshToken = tokenStore.getRefresh()
  if (!refreshToken) return null
  if (!refreshPromise) {
    refreshPromise = refreshClient.post<ApiResponse<AuthTokens>>('/auth/refresh', { refreshToken })
      .then(({ data }) => { tokenStore.set(data.data); return data.data.token })
      .catch(() => { tokenStore.clear(); return null })
      .finally(() => { refreshPromise = null })
  }
  return refreshPromise
}

api.interceptors.request.use((config) => {
  const token = tokenStore.getAccess()
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

api.interceptors.response.use((response) => response, async (error: AxiosError) => {
  const original = error.config as (InternalAxiosRequestConfig & { _retry?: boolean }) | undefined
  const isAuthRequest = original?.url?.includes('/auth/')
  if (error.response?.status !== 401 || !original || original._retry || isAuthRequest || !tokenStore.getRefresh()) return Promise.reject(error)
  original._retry = true
  const token = await refreshAccessToken()
  if (!token) return Promise.reject(error)
  original.headers.Authorization = `Bearer ${token}`
  return api(original)
})

export const authApi = {
  login: (email: string, password: string) => api.post<ApiResponse<AuthTokens>>('/auth/login', { email, password }),
  register: (name: string, email: string, password: string) => api.post<ApiResponse<null>>('/auth/register', { name, email, password }),
  refresh: () => refreshClient.post<ApiResponse<AuthTokens>>('/auth/refresh', { refreshToken: tokenStore.getRefresh() }),
  logout: () => refreshClient.post<ApiResponse<null>>('/auth/logout', { refreshToken: tokenStore.getRefresh() }),
  me: () => api.get<ApiResponse<UserProfile>>('/users/me'),
}

export const usersApi = {
  list: (params: { page?: number; size?: number; search?: string; role?: string } = {}) => api.get<ApiResponse<PageResponse<UserProfile>>>('/users', { params }),
}

export const conversationsApi = {
  list: (params: { page?: number; size?: number } = {}) => api.get<ApiResponse<PageResponse<ConversationSummary>>>('/conversations', { params }),
  get: (conversationId: number) => api.get<ApiResponse<ConversationResponse>>(`/conversations/${conversationId}`),
  create: (title?: string) => api.post<ApiResponse<ConversationSummary>>('/conversations', { title }),
  rename: (conversationId: number, title: string) => api.patch<ApiResponse<ConversationSummary>>(`/conversations/${conversationId}`, { title }),
  delete: (conversationId: number) => api.delete<ApiResponse<null>>(`/conversations/${conversationId}`),
}

export const documentsApi = {
  upload: async (file: File) => {
    const formData = new FormData()
    formData.append('file', file)
    const response = await authenticatedFetch('/documents', { method: 'POST', body: formData })
    if (!response.ok) throw new ApiRequestError(await readErrorMessage(response), response.status)
    return { data: await response.json() as ApiResponse<DocumentResponse> }
  },
  list: async (params: { page?: number; size?: number } = {}) => {
    const query = new URLSearchParams()
    if (params.page !== undefined) query.set('page', params.page.toString())
    if (params.size !== undefined) query.set('size', params.size.toString())
    const qs = query.toString()
    const response = await authenticatedFetch(`/documents${qs ? `?${qs}` : ''}`, { method: 'GET' })
    if (!response.ok) throw new ApiRequestError(await readErrorMessage(response), response.status)
    return { data: await response.json() as ApiResponse<PageResponse<DocumentResponse>> }
  },
  get: async (documentId: number) => {
    const response = await authenticatedFetch(`/documents/${documentId}`, { method: 'GET' })
    if (!response.ok) throw new ApiRequestError(await readErrorMessage(response), response.status)
    return { data: await response.json() as ApiResponse<DocumentResponse> }
  },
  getText: async (documentId: number) => {
    const response = await authenticatedFetch(`/documents/${documentId}/text`, { method: 'GET' })
    if (!response.ok) throw new ApiRequestError(await readErrorMessage(response), response.status)
    return { data: await response.json() as ApiResponse<DocumentTextResponse> }
  },
  delete: async (documentId: number) => {
    const response = await authenticatedFetch(`/documents/${documentId}`, { method: 'DELETE' })
    if (!response.ok) throw new ApiRequestError(await readErrorMessage(response), response.status)
    return { data: await response.json() as ApiResponse<null> }
  },
}

export function apiErrorMessage(error: unknown, fallback: string) {
  if (axios.isAxiosError(error)) {
    const message = error.response?.data && typeof error.response.data === 'object'
      ? (error.response.data as { message?: unknown }).message
      : undefined
    if (typeof message === 'string' && message.trim()) return message
  }
  return error instanceof Error && error.message.trim() ? error.message : fallback
}

function parseSseBlock(block: string): SseEvent | null {
  let event = 'message'
  const data: string[] = []
  const lines = block.split('\n')

  for (const line of lines) {
    if (!line || line.startsWith(':')) continue
    const separator = line.indexOf(':')
    const field = separator === -1 ? line : line.slice(0, separator)
    let value = separator === -1 ? '' : line.slice(separator + 1)
    if (value.startsWith(' ')) value = value.slice(1)
    if (field === 'event') event = value
    if (field === 'data') data.push(value)
  }

  return data.length ? { event, data: data.join('\n') } : null
}

export function parseSseEvents(buffer: string): { events: SseEvent[]; remainder: string } {
  const normalized = buffer.replace(/\r\n/g, '\n').replace(/\r/g, '\n')
  const blocks = normalized.split('\n\n')
  const remainder = blocks.pop() ?? ''
  return {
    events: blocks.map(parseSseBlock).filter((event): event is SseEvent => Boolean(event)),
    remainder,
  }
}

async function readErrorMessage(response: Response) {
  try {
    const body = await response.json() as { message?: string }
    return body.message || `Request failed with status ${response.status}`
  } catch {
    return `Request failed with status ${response.status}`
  }
}

async function authenticatedFetch(path: string, init: RequestInit, retry = true) {
  const headers = new Headers(init.headers)
  const accessToken = tokenStore.getAccess()
  if (accessToken) headers.set('Authorization', `Bearer ${accessToken}`)

  let response = await fetch(`${API_URL.replace(/\/$/, '')}${path}`, { ...init, headers })
  if (response.status === 401 && retry && tokenStore.getRefresh()) {
    const token = await refreshAccessToken()
    if (token) {
      headers.set('Authorization', `Bearer ${token}`)
      response = await fetch(`${API_URL.replace(/\/$/, '')}${path}`, { ...init, headers })
    }
  }
  return response
}

function parseJsonPayload<T>(event: SseEvent): T {
  try {
    return JSON.parse(event.data) as T
  } catch {
    throw new SseParseError(`The server returned an invalid ${event.event} event.`)
  }
}

async function consumeConversationStream(
  path: string,
  body: BodyInit | undefined,
  callbacks: StreamCallbacks,
  signal: AbortSignal,
) {
  const response = await authenticatedFetch(
    path,
    {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', Accept: 'text/event-stream' },
      body,
      signal,
    },
  )

  if (!response.ok) throw new ApiRequestError(await readErrorMessage(response), response.status)
  if (!response.body) throw new SseParseError('The server did not provide a streaming response.')

  console.debug('[SSE] response received', { status: response.status, contentType: response.headers.get('content-type') })

  const reader = response.body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''
  let completed = false
  let serverError = false

  const dispatch = (event: SseEvent) => {
    if (event.event === 'user_message') callbacks.onUserMessage(parseJsonPayload<ChatMessageResponse>(event))
    else if (event.event === 'token') callbacks.onToken(event.data)
    else if (event.event === 'complete') { completed = true; console.debug('[SSE] complete received'); callbacks.onComplete(parseJsonPayload<ChatMessageResponse>(event)) }
    else if (event.event === 'error') {
      serverError = true
      try { callbacks.onError(parseJsonPayload<{ message?: string }>(event).message || 'AI generation failed.') } catch { callbacks.onError('AI generation failed.') }
    }
  }

  try {
    while (true) {
      const { done, value } = await reader.read()
      if (done) { console.debug('[SSE] reader done'); break }
      const decoded = decoder.decode(value, { stream: true })
      buffer += decoded
      const parsed = parseSseEvents(buffer)
      buffer = parsed.remainder
      parsed.events.forEach(dispatch)
      if (serverError || completed) break
    }
    buffer += decoder.decode()
    if (!signal.aborted && !serverError && !completed) throw new SseParseError('The AI stream ended before completion.')
  } catch (error) {
    if (!signal.aborted) { console.error('[SSE] stream error', error); throw error }
  } finally {
    reader.releaseLock()
  }
}

export function streamConversationMessage(
  conversationId: number,
  content: string,
  callbacks: StreamCallbacks,
  signal: AbortSignal,
) {
  return consumeConversationStream(
    `/conversations/${conversationId}/messages/stream`,
    JSON.stringify({ content }),
    callbacks,
    signal,
  )
}

export function regenerateConversationMessage(
  conversationId: number,
  messageId: number,
  callbacks: StreamCallbacks,
  signal: AbortSignal,
) {
  return consumeConversationStream(
    `/conversations/${conversationId}/messages/${messageId}/regenerate`,
    undefined,
    callbacks,
    signal,
  )
}
