import { useState, useEffect } from 'react'
import { Bell, Bot, ChevronDown, CircleHelp, FileText, Home, LogOut, Menu, MessageSquare, Moon, PanelLeftClose, PanelLeftOpen, Plus, Search, Settings, Shield, Sun, Users, X } from 'lucide-react'
import { NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import { useTheme } from '../../context/ThemeContext'
import { Brand } from '../ui/Brand'
import { Button, IconButton } from '../ui/Ui'
import { motion, AnimatePresence } from 'framer-motion'
import { workspacesApi, type WorkspaceResponse, workspaceStore } from '../../lib/api'
import { WorkspacesModal } from '../WorkspacesModal'
import { eventBus } from '../../lib/events'

export type AppNotification = { id: string; message: string; time: Date; read: boolean }

const navItems = [
  { to: '/app', label: 'Home', icon: Home, end: true },
  { to: '/app/chat', label: 'AI Chat', icon: MessageSquare },
  { to: '/app/documents', label: 'Documents', icon: FileText },
  { to: '/app/agents', label: 'Agents', icon: Bot },
  { to: '/app/search', label: 'Search', icon: Search },
]

const pageTitles: Record<string, string> = { '/app': 'Home', '/app/chat': 'AI Chat', '/app/documents': 'Documents', '/app/agents': 'AI Agents', '/app/search': 'Search', '/app/settings': 'Settings', '/app/admin': 'Admin' }

export function AppShell() {
  const { user, logout } = useAuth()
  const { resolvedTheme, toggleTheme } = useTheme()
  const [collapsed, setCollapsed] = useState(() => localStorage.getItem('orbit_sidebar_collapsed') === 'true')
  const [mobileOpen, setMobileOpen] = useState(false)
  const [profileOpen, setProfileOpen] = useState(false)
  const [workspacesOpen, setWorkspacesOpen] = useState(false)
  const [notificationsOpen, setNotificationsOpen] = useState(false)
  const [notifications, setNotifications] = useState<AppNotification[]>([])
  const [currentWorkspace, setCurrentWorkspace] = useState<WorkspaceResponse | null>(null)
  const location = useLocation()
  const navigate = useNavigate()
  
  useEffect(() => { localStorage.setItem('orbit_sidebar_collapsed', String(collapsed)) }, [collapsed])
  
  useEffect(() => {
    const handleNotify = (msg: string) => {
      setNotifications(prev => [{ id: Math.random().toString(), message: msg, time: new Date(), read: false }, ...prev].slice(0, 10))
    }
    const unsubUpload = eventBus.on('DOCUMENT_UPLOADED', () => handleNotify('Document uploaded successfully'))
    const unsubFailed = eventBus.on('DOCUMENT_FAILED', () => handleNotify('Document upload failed'))
    const unsubDeleted = eventBus.on('CONVERSATION_DELETED', () => handleNotify('Conversation deleted'))
    const unsubAi = eventBus.on('AI_RESPONDED', () => handleNotify('AI finished responding'))
    
    return () => { unsubUpload(); unsubFailed(); unsubDeleted(); unsubAi(); }
  }, [])

  useEffect(() => {
    const wsId = workspaceStore.get()
    if (wsId) {
      workspacesApi.get(Number(wsId))
        .then(res => setCurrentWorkspace(res.data.data))
        .catch(() => {})
    }
  }, [])

  const isAdmin = user?.role === 'ADMIN' || user?.role === 'SUPER_ADMIN'
  const title = pageTitles[location.pathname] ?? (location.pathname.startsWith('/app/chat/') ? 'AI Chat' : location.pathname.startsWith('/app/documents/') ? 'Document workspace' : location.pathname.startsWith('/app/agents/') ? 'Agent workspace' : 'Enterprise AI')

  async function handleLogout() { await logout(); navigate('/login') }
  function closeMobile() { setMobileOpen(false) }

  return <div className="app-shell">
    <AnimatePresence>
      {mobileOpen && (
        <motion.button 
          initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
          className="sidebar-scrim" aria-label="Close navigation" onClick={closeMobile} 
        />
      )}
    </AnimatePresence>
    <motion.aside 
      initial={false}
      animate={{ width: collapsed ? 68 : 260 }}
      transition={{ type: "spring", bounce: 0, duration: 0.3 }}
      className={`app-sidebar ${collapsed ? 'is-collapsed' : ''} ${mobileOpen ? 'is-mobile-open' : ''}`}
    >
      <div className="sidebar-brand">
        <Brand compact={collapsed} />
        <IconButton label="Close navigation" className="mobile-close" onClick={closeMobile}><X size={19} /></IconButton>
      </div>
      <nav className="primary-nav" aria-label="Primary navigation">
        {navItems.map(({ to, label, icon: Icon, end }) => 
          <NavLink key={to} to={to} end={end} onClick={closeMobile} className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}>
            <Icon size={18} />
            <motion.span animate={{ opacity: collapsed ? 0 : 1 }} transition={{ duration: 0.1 }}>{label}</motion.span>
          </NavLink>
        )}
      </nav>
      <div className="sidebar-section">
        <motion.p animate={{ opacity: collapsed ? 0 : 1 }}>Workspace</motion.p>
        <button className="nav-item nav-item-muted" onClick={() => navigate('/app/chat')}><MessageSquare size={17} /><motion.span animate={{ opacity: collapsed ? 0 : 1 }}>Recent chats</motion.span></button>
        <button className="nav-item nav-item-muted" onClick={() => navigate('/app/documents')}><FileText size={17} /><motion.span animate={{ opacity: collapsed ? 0 : 1 }}>Recent documents</motion.span></button>
      </div>
      {isAdmin && (
        <div className="sidebar-section management">
          <motion.p animate={{ opacity: collapsed ? 0 : 1 }}>Management</motion.p>
          <NavLink to="/app/admin" onClick={closeMobile} className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}><Shield size={17} /><motion.span animate={{ opacity: collapsed ? 0 : 1 }}>Admin</motion.span></NavLink>
          <NavLink to="/app/settings" onClick={closeMobile} className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}><Users size={17} /><motion.span animate={{ opacity: collapsed ? 0 : 1 }}>Workspace</motion.span></NavLink>
        </div>
      )}
      <div className="sidebar-footer">
        <div className="profile-menu-wrap">
          <button className="profile-summary" onClick={() => setProfileOpen((open) => !open)} aria-expanded={profileOpen}>
            <Avatar name={user?.name} />
            <motion.span className="profile-copy" animate={{ opacity: collapsed ? 0 : 1 }}>
              <strong>{user?.name ?? 'Account'}</strong>
              <small>{user?.role ?? 'USER'}</small>
            </motion.span>
            {!collapsed && <ChevronDown size={15} style={{ transform: profileOpen ? 'rotate(180deg)' : 'none', transition: 'transform 0.2s' }} />}
          </button>
          <AnimatePresence>
            {profileOpen && (
              <motion.div 
                initial={{ opacity: 0, y: 10, scale: 0.95 }}
                animate={{ opacity: 1, y: 0, scale: 1 }}
                exit={{ opacity: 0, y: 10, scale: 0.95 }}
                transition={{ duration: 0.15 }}
                className="profile-menu"
              >
                <div className="profile-menu-header">
                  <Avatar name={user?.name} className="profile-menu-avatar" />
                  <div className="profile-menu-info">
                    <strong>{user?.name}</strong>
                    <small>{user?.email}</small>
                    <div style={{ marginTop: '4px', fontSize: '11px', color: 'var(--primary)', fontWeight: 600 }}>
                      {currentWorkspace?.name || 'Workspace'}
                    </div>
                  </div>
                </div>
                <div className="menu-divider"></div>
                <button onClick={() => { setProfileOpen(false); setWorkspacesOpen(true); }}><Users size={14} />Switch workspace</button>
                <button onClick={() => { setProfileOpen(false); toggleTheme(); }}>
                  {resolvedTheme === 'dark' ? <><Sun size={14} /> Light mode</> : <><Moon size={14} /> Dark mode</>}
                </button>
                <NavLink to="/app/settings" onClick={() => setProfileOpen(false)}><Settings size={14} />Settings</NavLink>
                <button onClick={() => setProfileOpen(false)}><CircleHelp size={14} />Help & support</button>
                <div className="menu-divider"></div>
                <button className="profile-logout" onClick={handleLogout}><LogOut size={14} />Log out</button>
              </motion.div>
            )}
          </AnimatePresence>
        </div>
      </div>
    </motion.aside>
    <div className="app-frame">
      <header className="app-header">
        <div className="header-leading">
          <IconButton label="Open navigation" className="mobile-menu" onClick={() => setMobileOpen(true)}><Menu size={21} /></IconButton>
          <IconButton label={collapsed ? 'Expand sidebar' : 'Collapse sidebar'} className="desktop-collapse" onClick={() => setCollapsed((value) => !value)}>
            {collapsed ? <PanelLeftOpen size={19} /> : <PanelLeftClose size={19} />}
          </IconButton>
          <div>
            <p className="breadcrumb">{currentWorkspace?.name || 'Enterprise AI'} <span>/</span> Workspace</p>
            <h1>{title}</h1>
          </div>
        </div>
        <div className="header-actions">
          <IconButton label="Search workspace" onClick={() => navigate('/app/search')}><Search size={19} /></IconButton>
          
          <div className="profile-menu-wrap">
            <IconButton label="Notifications" onClick={() => { setNotificationsOpen(!notificationsOpen); setNotifications(prev => prev.map(n => ({...n, read: true}))); }}>
              <div style={{ position: 'relative' }}>
                <Bell size={19} />
                {notifications.some(n => !n.read) && <span style={{ position: 'absolute', top: -2, right: -2, width: 8, height: 8, background: 'var(--error)', borderRadius: '50%' }} />}
              </div>
            </IconButton>
            <AnimatePresence>
              {notificationsOpen && (
                <motion.div initial={{ opacity: 0, y: 10, scale: 0.95 }} animate={{ opacity: 1, y: 0, scale: 1 }} exit={{ opacity: 0, y: 10, scale: 0.95 }} transition={{ duration: 0.15 }} className="profile-menu" style={{ width: '280px', right: 0 }}>
                  <div className="profile-menu-header"><strong>Notifications</strong></div>
                  <div className="menu-divider"></div>
                  <div className="custom-scrollbar" style={{ maxHeight: '300px', overflowY: 'auto' }}>
                    {notifications.length === 0 ? (
                      <div style={{ padding: '16px', textAlign: 'center', color: 'var(--text-secondary)', fontSize: '12px' }}>No new notifications</div>
                    ) : (
                      notifications.map(n => (
                        <div key={n.id} style={{ padding: '8px 16px', borderBottom: '1px solid var(--border)', fontSize: '13px' }}>
                          <div style={{ color: 'var(--text)' }}>{n.message}</div>
                          <div style={{ color: 'var(--text-secondary)', fontSize: '11px', marginTop: '4px' }}>{n.time.toLocaleTimeString()}</div>
                        </div>
                      ))
                    )}
                  </div>
                </motion.div>
              )}
            </AnimatePresence>
          </div>

          {location.pathname !== '/app/chat' && <Button className="header-new-chat" onClick={() => navigate('/app/chat')}><Plus size={16} />New chat</Button>}
        </div>
      </header>
      <main className="app-content"><Outlet /></main>
    </div>
    
    <WorkspacesModal isOpen={workspacesOpen} onClose={() => setWorkspacesOpen(false)} />
  </div>
}

export function Avatar({ name, className = '' }: { name?: string; className?: string }) { 
  return <span className={`avatar ${className}`}>{name?.split(' ').map((part) => part[0]).slice(0, 2).join('').toUpperCase() || 'EA'}</span> 
}
