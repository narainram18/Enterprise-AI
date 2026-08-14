import { Navigate, Outlet, Route, Routes, useLocation } from 'react-router-dom'
import { AppShell } from './components/layout/AppShell'
import { useAuth } from './context/AuthContext'
import { LoginPage, RegisterPage } from './pages/AuthPages'
import { DashboardPage } from './pages/DashboardPage'
import { AgentWorkspacePage, AgentsPage } from './pages/AgentsPage'
import { ChatPage } from './pages/ChatPage'
import { DocumentsPage } from './pages/DocumentsPage'
import { SearchPage } from './pages/SearchPage'
import { SettingsPage } from './pages/SettingsPage'
import { AdminPage } from './pages/AdminPage'
import { CreateWorkspacePage } from './pages/CreateWorkspacePage'
import { ResearchAgentPage } from './pages/ResearchAgentPage'
import { LoadingState } from './components/ui/Ui'
import './App.css'

function ProtectedRoute() {
  const { isAuthenticated, isLoading } = useAuth()
  const location = useLocation()
  if (isLoading) return <div className="route-loading"><LoadingState label="Loading your workspace…" /></div>
  return isAuthenticated ? <Outlet /> : <Navigate to="/login" replace state={{ from: location }} />
}

function AdminRoute() {
  const { user } = useAuth()
  return user?.role === 'ADMIN' || user?.role === 'SUPER_ADMIN' ? <Outlet /> : <Navigate to="/app" replace />
}

function App() {
  return <Routes>
    <Route path="/login" element={<LoginPage />} />
    <Route path="/register" element={<RegisterPage />} />
    <Route path="/signup" element={<Navigate to="/register" replace />} />
    <Route element={<ProtectedRoute />}>
      <Route path="/app" element={<AppShell />}>
        <Route index element={<DashboardPage />} />
        <Route path="chat" element={<ChatPage />} />
        <Route path="chat/:chatId" element={<ChatPage />} />
        <Route path="documents" element={<DocumentsPage />} />
        <Route path="documents/:documentId" element={<DocumentsPage />} />
        <Route path="agents" element={<AgentsPage />} />
        <Route path="agents/research-assistant" element={<ResearchAgentPage />} />
        <Route path="agents/:agentId" element={<AgentWorkspacePage />} />
        <Route path="search" element={<SearchPage />} />
        <Route path="settings" element={<SettingsPage />} />
        <Route path="workspaces/create" element={<CreateWorkspacePage />} />
        <Route element={<AdminRoute />}><Route path="admin" element={<AdminPage />} /></Route>
      </Route>
    </Route>
    <Route path="/dashboard/*" element={<Navigate to="/app" replace />} />
    <Route path="*" element={<Navigate to="/app" replace />} />
  </Routes>
}

export default App
