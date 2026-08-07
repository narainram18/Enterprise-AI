import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { ShieldCheck, Plus } from 'lucide-react'
import { Button, TextInput } from '../components/ui/Ui'
import { workspacesApi, workspaceStore } from '../lib/api'
import { useAuth } from '../context/AuthContext'

export function CreateWorkspacePage() {
  const [name, setName] = useState('')
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const navigate = useNavigate()
  const { user } = useAuth()

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!name.trim()) return

    setIsLoading(true)
    setError(null)

    try {
      const response = await workspacesApi.create(name.trim())
      const workspace = response.data.data
      workspaceStore.set(workspace.id)
      navigate('/app', { replace: true })
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to create workspace')
    } finally {
      setIsLoading(false)
    }
  }

  return (
    <div className="settings-page" style={{ height: '100%', display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center' }}>
      <div style={{ maxWidth: '400px', width: '100%' }}>
        <div className="settings-heading" style={{ marginBottom: '2rem' }}>
          <ShieldCheck size={28} />
          <div>
            <h2>Welcome, {user?.name}!</h2>
            <p>You need a workspace to get started.</p>
          </div>
        </div>

        <form className="settings-form" onSubmit={handleCreate}>
          {error && <div className="error-message">{error}</div>}
          <TextInput
            label="Workspace Name"
            value={name}
            onChange={(e) => setName(e.target.value)}
            placeholder="e.g. My Company Workspace"
            disabled={isLoading}
            required
          />
          <Button type="submit" disabled={isLoading || !name.trim()} style={{ width: '100%', marginTop: '1rem' }}>
            {isLoading ? 'Creating...' : <><Plus size={16} /> Create Workspace</>}
          </Button>
        </form>
      </div>
    </div>
  )
}
