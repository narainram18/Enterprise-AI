import { createContext, useContext, useEffect, useMemo, useState } from 'react'
import { authApi, tokenStore, type UserProfile } from '../lib/api'

type AuthContextValue = {
  user: UserProfile | null
  isAuthenticated: boolean
  isLoading: boolean
  login: (email: string, password: string, remember?: boolean) => Promise<void>
  signup: (name: string, email: string, password: string) => Promise<void>
  logout: () => Promise<void>
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined)

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<UserProfile | null>(null)
  const [isLoading, setIsLoading] = useState(true)

  useEffect(() => {
    if (!tokenStore.getAccess()) { setIsLoading(false); return }
    authApi.me().then(({ data }) => setUser(data.data)).catch(() => { tokenStore.clear(); setUser(null) }).finally(() => setIsLoading(false))
  }, [])

  async function login(email: string, password: string, remember = true) {
    const { data } = await authApi.login(email, password)
    tokenStore.set(data.data, remember)
    const profile = await authApi.me()
    setUser(profile.data.data)
  }

  async function signup(name: string, email: string, password: string) {
    await authApi.register(name, email, password)
    await login(email, password)
  }

  async function logout() {
    try { if (tokenStore.getRefresh()) await authApi.logout() } finally { tokenStore.clear(); setUser(null) }
  }

  const value = useMemo(() => ({ user, isAuthenticated: Boolean(user), isLoading, login, signup, logout }), [user, isLoading])
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

// oxlint-disable-next-line react/only-export-components
export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) throw new Error('useAuth must be used inside AuthProvider')
  return context
}
