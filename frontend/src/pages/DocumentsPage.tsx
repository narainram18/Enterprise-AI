import { useEffect, useRef, useState } from 'react'
import { FolderUp, Search, SlidersHorizontal, Upload, FileText } from 'lucide-react'
import { apiErrorMessage, documentsApi, type DocumentResponse } from '../lib/api'
import { Button, EmptyState, ErrorState, LoadingState } from '../components/ui/Ui'
import { DocumentDetailsDrawer } from '../components/DocumentDetailsDrawer'
import { DocumentSection } from '../components/DocumentSection'
import { useConfirm } from '../context/ConfirmationContext'
import { eventBus } from '../lib/events'

const MAX_FILE_SIZE = 10 * 1024 * 1024
const ACCEPTED_EXTENSIONS = ['.pdf', '.docx', '.txt']

export function DocumentsPage() {
  const [documents, setDocuments] = useState<DocumentResponse[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [loadError, setLoadError] = useState('')
  const [notice, setNotice] = useState('')
  const [search, setSearch] = useState('')
  const [typeFilter, setTypeFilter] = useState<any>('ALL')
  const [statusFilter, setStatusFilter] = useState('ALL')
  const [sortBy, setSortBy] = useState('DATE_DESC')
  const [isDragging, setIsDragging] = useState(false)
  const [isUploading, setIsUploading] = useState(false)
  const [uploadProgress, setUploadProgress] = useState(0)
  
  const [selectedDocumentId, setSelectedDocumentId] = useState<number | null>(null)
  
  const { confirm } = useConfirm()
  
  const inputRef = useRef<HTMLInputElement | null>(null)
  const progressIntervalRef = useRef<number | null>(null)

  useEffect(() => {
    const timer = setTimeout(() => {
       void loadDocuments()
    }, 300)
    return () => clearTimeout(timer)
  }, [search, typeFilter])

  async function loadDocuments() {
    setIsLoading(true)
    setLoadError('')
    try {
      const { data } = await documentsApi.list({ 
         page: 0, 
         size: 100,
         originalFileName: search,
         documentType: typeFilter
      })
      setDocuments(data.data.content)
    } catch (error) {
      setLoadError(apiErrorMessage(error, 'We couldn’t load your documents. Please try again.'))
    } finally {
      setIsLoading(false)
    }
  }

  function openFilePicker() {
    if (!isUploading) inputRef.current?.click()
  }

  function validateFile(file: File) {
    const extension = `.${file.name.split('.').pop()?.toLowerCase() ?? ''}`
    if (!ACCEPTED_EXTENSIONS.includes(extension)) return 'Only PDF, DOCX, and TXT files are supported.'
    if (file.size === 0) return 'The selected file is empty.'
    if (file.size > MAX_FILE_SIZE) return 'Files must be 10 MB or smaller.'
    return ''
  }

  async function uploadSelectedFile(file?: File) {
    if (!file || isUploading) return
    setNotice('')
    const validationError = validateFile(file)
    if (validationError) { setNotice(validationError); return }

    setIsUploading(true)
    setUploadProgress(0)
    
    // Simulate upload progress
    progressIntervalRef.current = window.setInterval(() => {
      setUploadProgress(p => {
        if (p >= 90) return p
        return p + Math.floor(Math.random() * 10) + 1
      })
    }, 500)

    try {
      const { data } = await documentsApi.upload(file)
      setUploadProgress(100)
      setDocuments((current) => [data.data, ...current.filter((item) => item.id !== data.data.id)])
      eventBus.emit('DOCUMENT_UPLOADED')
      setNotice(data.data.processingStatus === 'FAILED'
        ? data.data.extractionError || 'The file was uploaded, but its text could not be extracted.'
        : 'Document uploaded successfully.')
    } catch (error) {
      eventBus.emit('DOCUMENT_FAILED')
      setNotice(apiErrorMessage(error, 'We couldn’t upload that document. Please try again.'))
    } finally {
      setIsUploading(false)
      if (progressIntervalRef.current) clearInterval(progressIntervalRef.current)
      setTimeout(() => setUploadProgress(0), 1000)
      if (inputRef.current) inputRef.current.value = ''
    }
  }

  function onFileChange(event: React.ChangeEvent<HTMLInputElement>) {
    void uploadSelectedFile(event.target.files?.[0])
  }

  function onDrop(event: React.DragEvent<HTMLElement>) {
    event.preventDefault()
    setIsDragging(false)
    void uploadSelectedFile(event.dataTransfer.files[0])
  }

  async function deleteDocument(id: number) {
    const isConfirmed = await confirm({
      title: 'Delete Document',
      description: 'Are you sure you want to delete this document? This cannot be undone.',
      confirmText: 'Delete',
      variant: 'danger'
    })
    if (!isConfirmed) return
    try {
      await documentsApi.delete(id)
      setDocuments(docs => docs.filter(d => d.id !== id))
      setSelectedDocumentId(null)
    } catch (e) {
      await confirm({ title: 'Error', description: apiErrorMessage(e, 'Delete failed'), isAlert: true, variant: 'danger' })
    }
  }

  let visibleDocuments = documents.filter((document) => {
    return statusFilter === 'ALL' || document.processingStatus === statusFilter
  })
  
  visibleDocuments.sort((a, b) => {
    if (sortBy === 'DATE_DESC') return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
    if (sortBy === 'DATE_ASC') return new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime()
    if (sortBy === 'SIZE_DESC') return b.fileSize - a.fileSize
    if (sortBy === 'NAME_ASC') return a.originalFileName.localeCompare(b.originalFileName)
    return 0
  })

  const readyDocs = visibleDocuments.filter(d => d.processingStatus === 'READY')
  const processingDocs = visibleDocuments.filter(d => !['READY', 'FAILED'].includes(d.processingStatus))
  const failedDocs = visibleDocuments.filter(d => d.processingStatus === 'FAILED')
  
  const totalSize = documents.reduce((sum, doc) => sum + doc.fileSize, 0)
  
  function formatFileSize(bytes: number) {
    if (!bytes) return '0 B'
    if (bytes < 1024) return `${bytes} B`
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
  }

  return <div className="documents-page">
    <input ref={inputRef} className="document-file-input" type="file" accept=".pdf,.docx,.txt,application/pdf,application/vnd.openxmlformats-officedocument.wordprocessingml.document,text/plain" onChange={onFileChange} />
    <div className="page-intro"><div><p className="section-kicker">Knowledge base</p><h2>Documents</h2><p>Organize the sources your team uses to think, decide, and execute.</p></div><Button onClick={openFilePicker} isLoading={isUploading}><Upload size={16} />{isUploading ? 'Processing…' : 'Upload document'}</Button></div>
    <div className="document-toolbar">
      <label><Search size={17} /><input value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Search documents" aria-label="Search documents" /></label>
      <label className="document-filter"><SlidersHorizontal size={15} /><select value={typeFilter} onChange={(event) => setTypeFilter(event.target.value)} aria-label="Filter by document type"><option value="ALL">All types</option><option value="PDF">PDF</option><option value="DOCX">DOCX</option><option value="TXT">TXT</option></select></label>
      <label className="document-filter"><select value={statusFilter} onChange={(event) => setStatusFilter(event.target.value)} aria-label="Filter by processing status"><option value="ALL">All statuses</option><option value="UPLOADED">UPLOADED</option><option value="PROCESSING">PROCESSING</option><option value="READY">READY</option><option value="FAILED">FAILED</option></select></label>
      <label className="document-filter"><select value={sortBy} onChange={(event) => setSortBy(event.target.value)} aria-label="Sort documents"><option value="DATE_DESC">Newest first</option><option value="DATE_ASC">Oldest first</option><option value="SIZE_DESC">Largest first</option><option value="NAME_ASC">Name (A-Z)</option></select></label>
    </div>
    {notice && <p className="integration-notice" role="status">{notice}</p>}
    <section className={`document-dropzone ${isDragging ? 'is-dragging' : ''}`} onClick={openFilePicker} onDragEnter={(event) => { event.preventDefault(); setIsDragging(true) }} onDragOver={(event) => event.preventDefault()} onDragLeave={() => setIsDragging(false)} onDrop={onDrop} onKeyDown={(event) => { if (event.key === 'Enter' || event.key === ' ') openFilePicker() }} role="button" tabIndex={0} aria-label="Upload a document">
      {isUploading ? (
        <div className="upload-progress-container">
          <FolderUp size={26} />
          <h3>Processing your document…</h3>
          <div className="upload-progress-bar"><div className="upload-progress-fill" style={{ width: `${uploadProgress}%` }}></div></div>
          <small>{uploadProgress}%</small>
        </div>
      ) : (
        <>
          <span><FolderUp size={26} /></span>
          <h3>Upload knowledge for your workspace</h3>
          <p>Drag and drop a file here, or click to browse.</p>
          <small>Supported formats: PDF, DOCX, TXT · Maximum size: 10 MB</small>
        </>
      )}
    </section>
    
    {documents.length > 0 && (
      <div className="document-stats-bar">
        <span><strong>Total Documents:</strong> {documents.length}</span>
        <span><strong>Total Storage:</strong> {formatFileSize(totalSize)}</span>
        <span><strong>Ready:</strong> {readyDocs.length}</span>
      </div>
    )}
    
    <section className="workspace-panel document-list-panel document-list-panel-transparent">
      <div className="section-heading"><div><p className="section-kicker">Your library</p><h2>All documents</h2></div><span>{documents.length} document{documents.length === 1 ? '' : 's'}</span></div>
      {isLoading ? <LoadingState label="Loading documents…" /> : loadError ? <ErrorState title="Unable to load documents" message={loadError} onRetry={() => void loadDocuments()} /> : visibleDocuments.length > 0 ? <div className="document-list">
        <DocumentSection title="READY" count={readyDocs.length} documents={readyDocs} onSelect={setSelectedDocumentId} onDelete={deleteDocument} />
        <DocumentSection title="PROCESSING" count={processingDocs.length} documents={processingDocs} onSelect={setSelectedDocumentId} onDelete={deleteDocument} />
        <DocumentSection title="FAILED" count={failedDocs.length} documents={failedDocs} onSelect={setSelectedDocumentId} onDelete={deleteDocument} />
      </div> : documents.length > 0 ? <EmptyState icon={<Search size={32} />} title="No matching documents" description="Try a different search or filter." /> : <EmptyState icon={<FileText size={32} />} title="Your document library is empty" description="Upload a PDF, DOCX, or TXT file to start building your private library." action={<Button onClick={openFilePicker}><Upload size={16} />Upload document</Button>} />}
    </section>
    
    {selectedDocumentId !== null && (
      <DocumentDetailsDrawer 
        documentId={selectedDocumentId} 
        onClose={() => setSelectedDocumentId(null)} 
        onUpdate={(doc) => setDocuments(docs => docs.map(d => d.id === doc.id ? doc : d))}
        onDelete={(id) => deleteDocument(id)}
      />
    )}
  </div>
}
