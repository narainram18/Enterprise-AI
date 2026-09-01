import { createContext, useContext, useEffect, useState, useMemo } from 'react'
import { workspacesApi, workspaceStore, type WorkspaceResponse } from '../lib/api'
import { useAuth } from './AuthContext'

type WorkspaceContextValue = {
  currentWorkspace: WorkspaceResponse | null
  setCurrentWorkspace: (workspace: WorkspaceResponse | null) => void
  isLoadingWorkspace: boolean
  toggleOnlineMode: () => Promise<void>
}

const WorkspaceContext = createContext<WorkspaceContextValue | undefined>(undefined)

export function WorkspaceProvider({ children }: { children: React.ReactNode }) {
  const { isAuthenticated } = useAuth()
  const [currentWorkspace, setCurrentWorkspace] = useState<WorkspaceResponse | null>(null)
  const [isLoadingWorkspace, setIsLoadingWorkspace] = useState(true)

  useEffect(() => {
    const wsId = workspaceStore.get()
    if (isAuthenticated && wsId) {
      setIsLoadingWorkspace(true)
      workspacesApi.get(Number(wsId))
        .then(res => setCurrentWorkspace(res.data.data))
        .catch(() => {})
        .finally(() => setIsLoadingWorkspace(false))
    } else {
      setIsLoadingWorkspace(false)
      setCurrentWorkspace(null)
    }
  }, [isAuthenticated])

  // Optional: listen to workspace changes if we switch workspaces in the UI
  // The AppShell currently just reloads or sets it locally.

  const toggleOnlineMode = async () => {
    if (!currentWorkspace) return
    const newMode = !currentWorkspace.onlineMode
    try {
      const res = await workspacesApi.updateMode(currentWorkspace.id, newMode)
      setCurrentWorkspace(res.data.data)
    } catch (e) {
      console.error('Failed to toggle online mode', e)
      throw e
    }
  }

  const value = useMemo(() => ({
    currentWorkspace,
    setCurrentWorkspace,
    isLoadingWorkspace,
    toggleOnlineMode
  }), [currentWorkspace, isLoadingWorkspace])

  return <WorkspaceContext.Provider value={value}>{children}</WorkspaceContext.Provider>
}

export function useWorkspace() {
  const context = useContext(WorkspaceContext)
  if (!context) throw new Error('useWorkspace must be used inside WorkspaceProvider')
  return context
}
