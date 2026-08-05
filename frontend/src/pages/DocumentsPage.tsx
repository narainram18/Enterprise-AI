import { useEffect, useRef, useState } from 'react'
import { AlertCircle, CheckCircle2, FileText, FolderUp, LoaderCircle, Search, SlidersHorizontal, Upload } from 'lucide-react'
import { apiErrorMessage, documentsApi, type DocumentProcessingStatus, type DocumentResponse } from '../lib/api'
import { Button, EmptyState, ErrorState, LoadingState } from '../components/ui/Ui'
import { DocumentDetailsDrawer } from '../components/DocumentDetailsDrawer'

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
  const [isDragging, setIsDragging] = useState(false)
  const [isUploading, setIsUploading] = useState(false)
  
  const [selectedDocumentId, setSelectedDocumentId] = useState<number | null>(null)
  
  const inputRef = useRef<HTMLInputElement | null>(null)

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
    try {
      const { data } = await documentsApi.upload(file)
      setDocuments((current) => [data.data, ...current.filter((item) => item.id !== data.data.id)])
      setNotice(data.data.processingStatus === 'FAILED'
        ? data.data.extractionError || 'The file was uploaded, but its text could not be extracted.'
        : 'Document uploaded successfully.')
    } catch (error) {
      setNotice(apiErrorMessage(error, 'We couldn’t upload that document. Please try again.'))
    } finally {
      setIsUploading(false)
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

  const visibleDocuments = documents.filter((document) => {
    // search and type are handled by server, but we can do client side status filter
    const matchesStatus = statusFilter === 'ALL' || document.processingStatus === statusFilter
    return matchesStatus
  })

  return <div className="documents-page">
    <input ref={inputRef} className="document-file-input" type="file" accept=".pdf,.docx,.txt,application/pdf,application/vnd.openxmlformats-officedocument.wordprocessingml.document,text/plain" onChange={onFileChange} />
    <div className="page-intro"><div><p className="section-kicker">Knowledge base</p><h2>Documents</h2><p>Organize the sources your team uses to think, decide, and execute.</p></div><Button onClick={openFilePicker} isLoading={isUploading}><Upload size={16} />{isUploading ? 'Processing…' : 'Upload document'}</Button></div>
    <div className="document-toolbar"><label><Search size={17} /><input value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Search documents" aria-label="Search documents" /></label><label className="document-filter"><SlidersHorizontal size={15} /><select value={typeFilter} onChange={(event) => setTypeFilter(event.target.value)} aria-label="Filter by document type"><option value="ALL">All types</option><option value="PDF">PDF</option><option value="DOCX">DOCX</option><option value="TXT">TXT</option></select></label><label className="document-filter"><select value={statusFilter} onChange={(event) => setStatusFilter(event.target.value)} aria-label="Filter by processing status"><option value="ALL">All statuses</option><option value="UPLOADED">UPLOADED</option><option value="PROCESSING">PROCESSING</option><option value="READY">READY</option><option value="FAILED">FAILED</option></select></label></div>
    {notice && <p className="integration-notice" role="status">{notice}</p>}
    <section className={`document-dropzone ${isDragging ? 'is-dragging' : ''}`} onClick={openFilePicker} onDragEnter={(event) => { event.preventDefault(); setIsDragging(true) }} onDragOver={(event) => event.preventDefault()} onDragLeave={() => setIsDragging(false)} onDrop={onDrop} onKeyDown={(event) => { if (event.key === 'Enter' || event.key === ' ') openFilePicker() }} role="button" tabIndex={0} aria-label="Upload a document"><span><FolderUp size={26} /></span><h3>{isUploading ? 'Processing your document…' : 'Upload knowledge for your workspace'}</h3><p>{isUploading ? 'The file is being stored and its text is being extracted.' : 'Drag and drop a file here, or click to browse.'}</p><small>Supported formats: PDF, DOCX, TXT · Maximum size: 10 MB</small></section>
    <section className="workspace-panel document-list-panel"><div className="section-heading"><div><p className="section-kicker">Your library</p><h2>All documents</h2></div><span>{documents.length} document{documents.length === 1 ? '' : 's'}</span></div>
      {isLoading ? <LoadingState label="Loading documents…" /> : loadError ? <ErrorState title="Unable to load documents" message={loadError} onRetry={() => void loadDocuments()} /> : visibleDocuments.length ? <div className="document-list">{visibleDocuments.map((document) => <DocumentRow key={document.id} document={document} onClick={() => setSelectedDocumentId(document.id)} />)}</div> : documents.length ? <EmptyState icon={<Search size={22} />} title="No matching documents" description="Try a different search or filter." /> : <EmptyState icon={<FileText size={22} />} title="Your document library is empty" description="Upload a PDF, DOCX, or TXT file to start building your private library." action={<Button onClick={openFilePicker}><Upload size={16} />Upload document</Button>} />}
    </section>
    
    {selectedDocumentId !== null && (
      <DocumentDetailsDrawer 
        documentId={selectedDocumentId} 
        onClose={() => setSelectedDocumentId(null)} 
        onUpdate={(doc) => setDocuments(docs => docs.map(d => d.id === doc.id ? doc : d))}
        onDelete={(id) => {
          documentsApi.delete(id).then(() => {
            setDocuments(docs => docs.filter(d => d.id !== id))
            setSelectedDocumentId(null)
          }).catch(e => alert(apiErrorMessage(e, 'Delete failed')))
        }}
      />
    )}
  </div>
}

function DocumentRow({ document, onClick }: { document: DocumentResponse; onClick: () => void }) {
  return <article className="document-row" onClick={onClick} style={{cursor: 'pointer'}}><span className="document-row-icon"><FileText size={19} /></span><div className="document-row-main"><h3 title={document.originalFileName}>{document.originalFileName}</h3><p>{document.documentType} · {formatFileSize(document.fileSize)} · Uploaded by {document.uploaderName}</p></div><StatusBadge status={document.processingStatus} /></article>
}

function StatusBadge({ status }: { status: DocumentProcessingStatus }) {
  const isProcessing = !['READY', 'FAILED', 'UPLOADED'].includes(status)
  return <span className={`document-status document-status-${status.toLowerCase()}`}>
    {status === 'READY' && <CheckCircle2 size={13} />}
    {status === 'FAILED' && <AlertCircle size={13} />}
    {isProcessing && <LoaderCircle className="spin" size={13} />}
    {status.replace(/_/g, ' ')}
  </span>
}

function formatFileSize(bytes: number) {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}
