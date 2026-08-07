import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from 'react'
import { authApi, tokenStore, workspacesApi, workspaceStore, type UserProfile } from '../lib/api'
import { useNavigate } from 'react-router-dom'

/** Inactivity timeout in milliseconds. Default: 30 minutes. */
const INACTIVITY_TIMEOUT_MS = 30 * 60 * 1000
/** How often (ms) the interval checks for inactivity. */
const INACTIVITY_CHECK_INTERVAL_MS = 60 * 1000
/** Minimum gap (ms) between activity-timestamp updates to avoid excessive writes. */
const ACTIVITY_THROTTLE_MS = 30 * 1000
const LAST_ACTIVITY_KEY = 'orbit_last_activity'

type AuthContextValue = {
  user: UserProfile | null
  isAuthenticated: boolean
  isLoading: boolean
  login: (email: string, password: string, remember?: boolean) => Promise<void>
  signup: (name: string, email: string, password: string) => Promise<void>
  logout: () => Promise<void>
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined)

function stampActivity() { sessionStorage.setItem(LAST_ACTIVITY_KEY, Date.now().toString()) }

function getLastActivity(): number {
  const raw = sessionStorage.getItem(LAST_ACTIVITY_KEY)
  return raw ? Number(raw) : Date.now()
}

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<UserProfile | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const lastThrottledRef = useRef(0)
  const navigate = useNavigate()

  // ---------- logout (stable reference) ----------
  const logout = useCallback(async () => {
    try { if (tokenStore.getRefresh()) await authApi.logout() } catch { /* best-effort server revocation */ }
    tokenStore.clear()
    workspaceStore.clear()
    sessionStorage.removeItem(LAST_ACTIVITY_KEY)
    setUser(null)
  }, [])

  // ---------- bootstrap: check existing token ----------
  useEffect(() => {
    if (!tokenStore.getAccess()) { setIsLoading(false); return }
    authApi.me()
      .then(async ({ data }) => {
        if (!workspaceStore.get()) {
          try {
            const workspacesResponse = await workspacesApi.list()
            if (workspacesResponse.data.data.length > 0) {
              workspaceStore.set(workspacesResponse.data.data[0].id)
            } else {
              navigate('/app/workspaces/create', { replace: true })
            }
          } catch { /* ignore if workspaces fetch fails on bootstrap */ }
        }
        setUser(data.data); stampActivity() 
      })
      .catch(() => { tokenStore.clear(); workspaceStore.clear(); setUser(null) })
      .finally(() => setIsLoading(false))
  }, [])

  // ---------- inactivity timeout ----------
  useEffect(() => {
    if (!user) return

    // Stamp initial activity
    stampActivity()

    // Throttled activity recorder
    function recordActivity() {
      const now = Date.now()
      if (now - lastThrottledRef.current < ACTIVITY_THROTTLE_MS) return
      lastThrottledRef.current = now
      stampActivity()
    }

    // Periodic check
    const intervalId = setInterval(() => {
      if (!tokenStore.getAccess()) return
      if (Date.now() - getLastActivity() > INACTIVITY_TIMEOUT_MS) {
        void logout()
      }
    }, INACTIVITY_CHECK_INTERVAL_MS)

    // Activity events
    const events: (keyof WindowEventMap)[] = ['mousemove', 'keydown', 'mousedown', 'touchstart', 'scroll']
    for (const event of events) window.addEventListener(event, recordActivity, { passive: true })

    return () => {
      clearInterval(intervalId)
      for (const event of events) window.removeEventListener(event, recordActivity)
    }
  }, [user, logout])

  // ---------- cross-tab logout detection ----------
  useEffect(() => {
    function onStorageChange(event: StorageEvent) {
      // Another tab cleared the remember-me flag (i.e. called tokenStore.clear())
      if (event.key === 'orbit_remember_me' && event.newValue === null) {
        tokenStore.clear()
        workspaceStore.clear()
        sessionStorage.removeItem(LAST_ACTIVITY_KEY)
        setUser(null)
      }
    }
    window.addEventListener('storage', onStorageChange)
    return () => window.removeEventListener('storage', onStorageChange)
  }, [])

  // ---------- login ----------
  async function login(email: string, password: string, remember = false) {
    const { data } = await authApi.login(email, password)
    tokenStore.set(data.data, remember)
    const profile = await authApi.me()
    try {
      const workspacesResponse = await workspacesApi.list()
      if (workspacesResponse.data.data.length > 0) {
        workspaceStore.set(workspacesResponse.data.data[0].id)
      } else {
        navigate('/app/workspaces/create', { replace: true })
      }
    } catch { /* ignore if workspaces fetch fails */ }
    setUser(profile.data.data)
    stampActivity()
  }

  // ---------- signup ----------
  async function signup(name: string, email: string, password: string) {
    await authApi.register(name, email, password)
    await login(email, password)
  }

  const value = useMemo(() => ({ user, isAuthenticated: Boolean(user), isLoading, login, signup, logout }), [user, isLoading, logout])
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

// oxlint-disable-next-line react/only-export-components
export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) throw new Error('useAuth must be used inside AuthProvider')
  return context
}
