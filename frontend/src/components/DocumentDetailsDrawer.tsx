import { useEffect, useState, type FormEvent } from 'react'
import { X, Download, RotateCw, Trash2, Edit2, Check, AlertCircle } from 'lucide-react'
import { documentsApi, type DocumentResponse, apiErrorMessage } from '../lib/api'
import { Button, IconButton, LoadingState, ErrorState } from './ui/Ui'

const API_URL = import.meta.env.VITE_API_URL || '/api'

type Props = {
  documentId: number
  onClose: () => void
  onUpdate: (doc: DocumentResponse) => void
  onDelete: (id: number) => void
}

export function DocumentDetailsDrawer({ documentId, onClose, onUpdate, onDelete }: Props) {
  const [doc, setDoc] = useState<DocumentResponse | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState('')
  const [isRenaming, setIsRenaming] = useState(false)
  const [newName, setNewName] = useState('')
  const [renameLoading, setRenameLoading] = useState(false)
  const [actionLoading, setActionLoading] = useState(false)
  
  const [textPreview, setTextPreview] = useState<string | null>(null)
  const [previewLoading, setPreviewLoading] = useState(false)
  const [previewError, setPreviewError] = useState('')

  useEffect(() => {
    loadDocument()
  }, [documentId])

  async function loadDocument() {
    setIsLoading(true)
    setError('')
    try {
      const { data } = await documentsApi.get(documentId)
      setDoc(data.data)
      if (data.data.documentType === 'TXT' || data.data.documentType === 'DOCX') {
         loadTextPreview()
      }
    } catch (e) {
      setError(apiErrorMessage(e, 'Could not load document details'))
    } finally {
      setIsLoading(false)
    }
  }

  async function loadTextPreview() {
    setPreviewLoading(true)
    setPreviewError('')
    try {
      const { data } = await documentsApi.getText(documentId)
      setTextPreview(data.data.text)
    } catch (e) {
      setPreviewError(apiErrorMessage(e, 'Could not load text preview'))
    } finally {
      setPreviewLoading(false)
    }
  }

  async function handleRename(e: FormEvent) {
    e.preventDefault()
    if (!newName.trim() || newName === doc?.originalFileName) {
      setIsRenaming(false)
      return
    }
    setRenameLoading(true)
    try {
      const { data } = await documentsApi.rename(documentId, newName)
      setDoc(data.data)
      onUpdate(data.data)
      setIsRenaming(false)
    } catch (e) {
      alert(apiErrorMessage(e, 'Failed to rename'))
    } finally {
      setRenameLoading(false)
    }
  }

  async function handleRetry() {
    if (!confirm('Are you sure you want to retry/reindex processing?')) return
    setActionLoading(true)
    try {
      const { data } = await documentsApi.retry(documentId)
      setDoc(data.data)
      onUpdate(data.data)
    } catch (e) {
      alert(apiErrorMessage(e, 'Failed to retry'))
    } finally {
      setActionLoading(false)
    }
  }

  async function handleDownload() {
    try {
      const blob = await documentsApi.download(documentId)
      const url = window.URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = doc?.originalFileName || 'download'
      document.body.appendChild(a)
      a.click()
      window.URL.revokeObjectURL(url)
      document.body.removeChild(a)
    } catch (e) {
      alert(apiErrorMessage(e, 'Download failed'))
    }
  }

  function renderStatusTimeline(status: string) {
    const steps = ['UPLOADED', 'EXTRACTING_TEXT', 'CHUNKING', 'CREATING_EMBEDDINGS', 'STORING_VECTORS', 'READY']
    const currentIndex = steps.indexOf(status)
    const isFailed = status === 'FAILED'
    
    return <div className="timeline">
      {steps.map((step, index) => {
        const isCompleted = index <= currentIndex && !isFailed
        const isCurrent = index === currentIndex && !isFailed
        return <div key={step} className={`timeline-step ${isCompleted ? 'completed' : ''} ${isCurrent ? 'current' : ''}`}>
           <div className="timeline-marker"></div>
           <div className="timeline-label">{step.replace(/_/g, ' ')}</div>
        </div>
      })}
      {isFailed && <div className="timeline-step failed current">
         <div className="timeline-marker"></div>
         <div className="timeline-label">FAILED</div>
      </div>}
    </div>
  }

  if (isLoading) return <div className="drawer-overlay"><div className="drawer"><LoadingState /></div></div>
  if (error || !doc) return <div className="drawer-overlay"><div className="drawer"><ErrorState message={error} onRetry={loadDocument} /></div></div>

  return (
    <div className="drawer-overlay" onClick={onClose}>
      <div className="drawer drawer-large" onClick={e => e.stopPropagation()}>
        <header className="drawer-header">
          <div className="drawer-title-row">
            {isRenaming ? (
              <form onSubmit={handleRename} className="rename-form">
                <input autoFocus value={newName} onChange={e => setNewName(e.target.value)} className="rename-input" />
                <IconButton type="submit" label="Save" disabled={renameLoading}><Check size={16} /></IconButton>
                <IconButton type="button" label="Cancel" onClick={() => setIsRenaming(false)} disabled={renameLoading}><X size={16} /></IconButton>
              </form>
            ) : (
              <>
                <h2 title={doc.originalFileName}>{doc.originalFileName}</h2>
                <IconButton label="Rename" onClick={() => { setNewName(doc.originalFileName); setIsRenaming(true) }}><Edit2 size={16} /></IconButton>
              </>
            )}
          </div>
          <div className="drawer-actions">
            <IconButton label="Close" onClick={onClose}><X size={20} /></IconButton>
          </div>
        </header>

        <div className="drawer-content">
          <div className="drawer-grid">
            <div className="drawer-main">
              <section className="drawer-section">
                <h3>Preview</h3>
                <div className="preview-container">
                  {doc.documentType === 'PDF' && (
                    <iframe src={`${API_URL.replace(/\/$/, '')}/documents/${doc.id}/download`} title="PDF Preview" className="pdf-preview" />
                  )}
                  {(doc.documentType === 'TXT' || doc.documentType === 'DOCX') && (
                    <div className="text-preview">
                       {previewLoading ? <LoadingState label="Loading preview..." /> : previewError ? <ErrorState message={previewError} /> : <pre>{textPreview}</pre>}
                    </div>
                  )}
                </div>
              </section>
            </div>

            <aside className="drawer-sidebar">
              <section className="drawer-section">
                <h3>Details</h3>
                <dl className="details-list">
                  <dt>Type</dt><dd>{doc.documentType}</dd>
                  <dt>Size</dt><dd>{formatFileSize(doc.fileSize)}</dd>
                  <dt>Workspace</dt><dd>{doc.workspaceName}</dd>
                  <dt>Uploaded by</dt><dd>{doc.uploaderName}</dd>
                  <dt>Date</dt><dd>{new Date(doc.createdAt).toLocaleString()}</dd>
                  <dt>Chunks</dt><dd>{doc.chunkCount}</dd>
                  <dt>Last Indexed</dt><dd>{doc.lastIndexedAt ? new Date(doc.lastIndexedAt).toLocaleString() : 'Never'}</dd>
                </dl>
              </section>

              <section className="drawer-section">
                <h3>Processing Status</h3>
                {renderStatusTimeline(doc.processingStatus)}
                {doc.extractionError && <div className="error-box"><AlertCircle size={14} /> {doc.extractionError}</div>}
              </section>

              <section className="drawer-section">
                <h3>Actions</h3>
                <div className="action-buttons">
                  <Button variant="secondary" onClick={handleDownload}><Download size={16} /> Download</Button>
                  <Button variant="secondary" onClick={handleRetry} isLoading={actionLoading} disabled={doc.processingStatus === 'READY' || actionLoading}><RotateCw size={16} /> Reindex</Button>
                  <Button variant="danger" onClick={() => onDelete(doc.id)}><Trash2 size={16} /> Delete</Button>
                </div>
              </section>
            </aside>
          </div>
        </div>
      </div>
    </div>
  )
}

function formatFileSize(bytes: number) {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}
