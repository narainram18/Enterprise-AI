import { Globe, WifiOff } from 'lucide-react'
import { motion } from 'framer-motion'
import { useWorkspace } from '../../context/WorkspaceContext'

export function ModeToggle() {
  const { currentWorkspace, toggleOnlineMode, isLoadingWorkspace } = useWorkspace()

  if (isLoadingWorkspace || !currentWorkspace) {
    return <div className="mode-toggle-skeleton" style={{ width: 120, height: 32, borderRadius: 16, background: 'var(--bg-secondary)', opacity: 0.5 }} />
  }

  const isOnline = currentWorkspace.onlineMode

  return (
    <button 
      className={`mode-toggle ${isOnline ? 'is-online' : 'is-offline'}`}
      onClick={() => void toggleOnlineMode()}
      title={isOnline ? "Switch to Offline Mode" : "Switch to Online Mode"}
      style={{
        display: 'flex',
        alignItems: 'center',
        gap: '6px',
        padding: '4px 12px',
        borderRadius: '20px',
        border: '1px solid var(--border)',
        background: isOnline ? 'rgba(16, 185, 129, 0.1)' : 'rgba(239, 68, 68, 0.1)',
        color: isOnline ? 'var(--success)' : 'var(--error)',
        fontWeight: 500,
        fontSize: '12px',
        cursor: 'pointer',
        transition: 'all 0.2s ease',
        outline: 'none',
      }}
    >
      <motion.div
        initial={false}
        animate={{ rotate: isOnline ? 0 : 360 }}
        transition={{ duration: 0.3 }}
      >
        {isOnline ? <Globe size={14} /> : <WifiOff size={14} />}
      </motion.div>
      <span>{isOnline ? 'Online' : 'Offline'}</span>
    </button>
  )
}
