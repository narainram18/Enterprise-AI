import axios, { type AxiosError, type InternalAxiosRequestConfig } from 'axios'

const ACCESS_TOKEN_KEY = 'orbit_access_token'
const REFRESH_TOKEN_KEY = 'orbit_refresh_token'
const API_URL = import.meta.env.VITE_API_URL || '/api'

export type ApiResponse<T> = { success: boolean; message: string; data: T; timestamp?: string }
export type AuthTokens = { token: string; refreshToken: string }
export type UserProfile = { id: number; name: string; email: string; role: string }
export type PageResponse<T> = { content: T[]; page: number; size: number; totalElements: number; totalPages: number; first: boolean; last: boolean }

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
  if (!refreshPromise) {
    refreshPromise = refreshClient.post<ApiResponse<AuthTokens>>('/auth/refresh', { refreshToken: tokenStore.getRefresh() })
      .then(({ data }) => { tokenStore.set(data.data); return data.data.token })
      .catch(() => { tokenStore.clear(); return null })
      .finally(() => { refreshPromise = null })
  }
  const token = await refreshPromise
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
