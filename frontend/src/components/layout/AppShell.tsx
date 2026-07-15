import { useState } from 'react'
import { Bell, Bot, ChevronDown, CircleHelp, FileText, Home, LogOut, Menu, MessageSquare, Moon, PanelLeftClose, PanelLeftOpen, Plus, Search, Settings, Shield, Sun, Users, X } from 'lucide-react'
import { NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import { useTheme } from '../../context/ThemeContext'
import { Brand } from '../ui/Brand'
import { Button, IconButton } from '../ui/Ui'

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
  const [collapsed, setCollapsed] = useState(false)
  const [mobileOpen, setMobileOpen] = useState(false)
  const [profileOpen, setProfileOpen] = useState(false)
  const location = useLocation()
  const navigate = useNavigate()
  const isAdmin = user?.role === 'ADMIN' || user?.role === 'SUPER_ADMIN'
  const title = pageTitles[location.pathname] ?? (location.pathname.startsWith('/app/chat/') ? 'AI Chat' : location.pathname.startsWith('/app/documents/') ? 'Document workspace' : location.pathname.startsWith('/app/agents/') ? 'Agent workspace' : 'Enterprise AI')

  async function handleLogout() { await logout(); navigate('/login') }
  function closeMobile() { setMobileOpen(false) }

  return <div className="app-shell">
    {mobileOpen && <button className="sidebar-scrim" aria-label="Close navigation" onClick={closeMobile} />}
    <aside className={`app-sidebar ${collapsed ? 'is-collapsed' : ''} ${mobileOpen ? 'is-mobile-open' : ''}`}>
      <div className="sidebar-brand"><Brand compact={collapsed} /><IconButton label="Close navigation" className="mobile-close" onClick={closeMobile}><X size={19} /></IconButton></div>
      <nav className="primary-nav" aria-label="Primary navigation">
        {navItems.map(({ to, label, icon: Icon, end }) => <NavLink key={to} to={to} end={end} onClick={closeMobile} className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}><Icon size={18} /><span>{label}</span></NavLink>)}
      </nav>
      <div className="sidebar-section"><p>Workspace</p><button className="nav-item nav-item-muted" onClick={() => navigate('/app/chat')}><MessageSquare size={17} /><span>Recent chats</span></button><button className="nav-item nav-item-muted" onClick={() => navigate('/app/documents')}><FileText size={17} /><span>Recent documents</span></button></div>
      {isAdmin && <div className="sidebar-section management"><p>Management</p><NavLink to="/app/admin" onClick={closeMobile} className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}><Shield size={17} /><span>Admin</span></NavLink><NavLink to="/app/settings" onClick={closeMobile} className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}><Users size={17} /><span>Workspace</span></NavLink></div>}
      <div className="sidebar-footer"><NavLink to="/app/settings" onClick={closeMobile} className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}><Settings size={18} /><span>Settings</span></NavLink><button className="nav-item nav-item-muted"><CircleHelp size={18} /><span>Help & support</span></button>
        <div className="profile-menu-wrap"><button className="profile-summary" onClick={() => setProfileOpen((open) => !open)} aria-expanded={profileOpen}><Avatar name={user?.name} /><span className="profile-copy"><strong>{user?.name ?? 'Account'}</strong><small>{user?.role ?? 'USER'}</small></span><ChevronDown size={15} /></button>{profileOpen && <div className="profile-menu"><NavLink to="/app/settings" onClick={() => setProfileOpen(false)}>Profile</NavLink><NavLink to="/app/settings" onClick={() => setProfileOpen(false)}>Settings</NavLink><button onClick={toggleTheme}>{resolvedTheme === 'dark' ? <Sun size={15} /> : <Moon size={15} />}Use {resolvedTheme === 'dark' ? 'light' : 'dark'} theme</button><button className="profile-logout" onClick={handleLogout}><LogOut size={15} />Log out</button></div>}</div>
      </div>
    </aside>
    <div className="app-frame"><header className="app-header"><div className="header-leading"><IconButton label="Open navigation" className="mobile-menu" onClick={() => setMobileOpen(true)}><Menu size={21} /></IconButton><IconButton label={collapsed ? 'Expand sidebar' : 'Collapse sidebar'} className="desktop-collapse" onClick={() => setCollapsed((value) => !value)}>{collapsed ? <PanelLeftOpen size={19} /> : <PanelLeftClose size={19} />}</IconButton><div><p className="breadcrumb">Enterprise AI <span>/</span> Workspace</p><h1>{title}</h1></div></div><div className="header-actions"><IconButton label="Search workspace" onClick={() => navigate('/app/search')}><Search size={19} /></IconButton><IconButton label={`Switch to ${resolvedTheme === 'dark' ? 'light' : 'dark'} mode`} onClick={toggleTheme}>{resolvedTheme === 'dark' ? <Sun size={18} /> : <Moon size={18} />}</IconButton><IconButton label="Notifications"><Bell size={19} /></IconButton>{location.pathname !== '/app/chat' && <Button className="header-new-chat" onClick={() => navigate('/app/chat')}><Plus size={16} />New chat</Button>}</div></header><main className="app-content"><Outlet /></main></div>
  </div>
}

export function Avatar({ name, className = '' }: { name?: string; className?: string }) { return <span className={`avatar ${className}`}>{name?.split(' ').map((part) => part[0]).slice(0, 2).join('').toUpperCase() || 'EA'}</span> }
