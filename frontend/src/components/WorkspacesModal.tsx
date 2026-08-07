import { useState, useEffect } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { Check, Edit2, Plus, Trash2, X } from 'lucide-react'
import { workspacesApi, type WorkspaceResponse, workspaceStore } from '../lib/api'
import { useConfirm } from '../context/ConfirmationContext'
import { Button, IconButton, LoadingState } from './ui/Ui'
import { useToast } from '../context/ToastContext'

export function WorkspacesModal({ isOpen, onClose }: { isOpen: boolean, onClose: () => void }) {
  const [workspaces, setWorkspaces] = useState<WorkspaceResponse[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [isCreating, setIsCreating] = useState(false)
  const [newWorkspaceName, setNewWorkspaceName] = useState('')
  const [editingId, setEditingId] = useState<number | null>(null)
  const [editingName, setEditingName] = useState('')
  const { confirm } = useConfirm()
  const { toast } = useToast()

  const currentWorkspaceId = Number(workspaceStore.get())

  useEffect(() => {
    if (isOpen) {
      loadWorkspaces()
    } else {
      setIsCreating(false)
      setEditingId(null)
    }
  }, [isOpen])

  async function loadWorkspaces() {
    setIsLoading(true)
    try {
      const { data } = await workspacesApi.list()
      setWorkspaces(data.data)
    } catch {
      toast('Failed to load workspaces', 'error')
    } finally {
      setIsLoading(false)
    }
  }

  async function handleSwitch(id: number) {
    workspaceStore.set(id)
    toast('Workspace switched', 'success')
    window.location.reload()
  }

  async function handleDelete(workspace: WorkspaceResponse) {
    if (workspaces.length <= 1) {
      await confirm({ title: 'Cannot Delete', description: 'You must have at least one workspace.', isAlert: true, variant: 'danger' })
      return
    }
    const isConfirmed = await confirm({
      title: 'Delete Workspace',
      description: `Are you sure you want to delete "${workspace.name}"? All data will be lost.`,
      confirmText: 'Delete',
      variant: 'danger'
    })
    if (!isConfirmed) return

    try {
      await workspacesApi.delete(workspace.id)
      setWorkspaces(prev => prev.filter(w => w.id !== workspace.id))
      toast('Workspace deleted', 'success')
      if (currentWorkspaceId === workspace.id) {
        const remaining = workspaces.filter(w => w.id !== workspace.id)
        if (remaining.length > 0) handleSwitch(remaining[0].id)
      }
    } catch {
      toast('Failed to delete workspace', 'error')
    }
  }

  async function handleRename(id: number) {
    if (!editingName.trim()) return
    try {
      const { data } = await workspacesApi.rename(id, editingName)
      setWorkspaces(prev => prev.map(w => w.id === id ? data.data : w))
      setEditingId(null)
      toast('Workspace renamed', 'success')
    } catch {
      toast('Failed to rename workspace', 'error')
    }
  }

  async function handleCreate(e: React.FormEvent) {
    e.preventDefault()
    if (!newWorkspaceName.trim()) return
    try {
      const { data } = await workspacesApi.create(newWorkspaceName)
      setWorkspaces(prev => [...prev, data.data])
      setIsCreating(false)
      setNewWorkspaceName('')
      toast('Workspace created', 'success')
      handleSwitch(data.data.id)
    } catch {
      toast('Failed to create workspace', 'error')
    }
  }

  return (
    <AnimatePresence>
      {isOpen && (
        <>
          <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} transition={{ duration: 0.2 }} className="modal-overlay" onClick={onClose} />
          <motion.div initial={{ opacity: 0, scale: 0.95, y: 10 }} animate={{ opacity: 1, scale: 1, y: 0 }} exit={{ opacity: 0, scale: 0.95, y: 10 }} transition={{ duration: 0.2 }} className="confirmation-modal workspaces-modal">
            <div className="confirmation-header">
              <h3>Manage Workspaces</h3>
              <IconButton label="Close" onClick={onClose}><X size={18} /></IconButton>
            </div>
            
            <div className="workspaces-list custom-scrollbar" style={{ maxHeight: '300px', overflowY: 'auto' }}>
              {isLoading ? <LoadingState label="Loading workspaces..." /> : (
                <ul style={{ listStyle: 'none', padding: 0, margin: 0, display: 'flex', flexDirection: 'column', gap: '8px' }}>
                  {workspaces.map((ws) => (
                    <li key={ws.id} style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '12px', background: 'var(--surface-hover)', borderRadius: 'var(--radius-md)' }}>
                      {editingId === ws.id ? (
                        <div style={{ display: 'flex', gap: '8px', flex: 1 }}>
                          <input type="text" autoFocus value={editingName} onChange={e => setEditingName(e.target.value)} onKeyDown={e => { if (e.key === 'Enter') void handleRename(ws.id); if (e.key === 'Escape') setEditingId(null) }} style={{ flex: 1, padding: '4px 8px', borderRadius: '4px', border: '1px solid var(--border)' }} />
                          <Button variant="ghost" onClick={() => void handleRename(ws.id)}>Save</Button>
                        </div>
                      ) : (
                        <>
                          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', cursor: 'pointer', flex: 1 }} onClick={() => ws.id !== currentWorkspaceId && handleSwitch(ws.id)}>
                            <div style={{ width: '16px', display: 'flex', justifyContent: 'center' }}>
                              {ws.id === currentWorkspaceId && <Check size={16} color="var(--primary)" />}
                            </div>
                            <span style={{ fontWeight: ws.id === currentWorkspaceId ? 600 : 400, color: 'var(--text)' }}>{ws.name}</span>
                          </div>
                          <div style={{ display: 'flex', gap: '4px' }}>
                            <IconButton label="Rename" onClick={() => { setEditingId(ws.id); setEditingName(ws.name); }}><Edit2 size={14} /></IconButton>
                            <IconButton label="Delete" onClick={() => void handleDelete(ws)} className="danger-flag"><Trash2 size={14} /></IconButton>
                          </div>
                        </>
                      )}
                    </li>
                  ))}
                </ul>
              )}
            </div>

            {isCreating ? (
              <form onSubmit={handleCreate} style={{ display: 'flex', gap: '8px', marginTop: '16px' }}>
                <input type="text" placeholder="Workspace name..." autoFocus value={newWorkspaceName} onChange={e => setNewWorkspaceName(e.target.value)} style={{ flex: 1, padding: '8px 12px', borderRadius: 'var(--radius-md)', border: '1px solid var(--border)', background: 'var(--surface)' }} />
                <Button type="submit">Create</Button>
                <Button type="button" variant="secondary" onClick={() => setIsCreating(false)}>Cancel</Button>
              </form>
            ) : (
              <div style={{ marginTop: '16px' }}>
                <Button variant="secondary" onClick={() => setIsCreating(true)} style={{ width: '100%' }}><Plus size={16} /> Create Workspace</Button>
              </div>
            )}
          </motion.div>
        </>
      )}
    </AnimatePresence>
  )
}
