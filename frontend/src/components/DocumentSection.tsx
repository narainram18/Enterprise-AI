import { useState } from 'react'
import { ChevronDown, FileText, AlertCircle, RefreshCw, Eye, Trash2, Download } from 'lucide-react'
import { Button } from './ui/Ui'
import { type DocumentResponse } from '../lib/api'
import { motion, AnimatePresence } from 'framer-motion'

export function DocumentSection({ title, count, documents, onSelect, onDelete, onRetry }: { title: string, count: number, documents: DocumentResponse[], onSelect: (id: number) => void, onDelete: (id: number) => void, onRetry?: (id: number) => void }) {
  const [isOpen, setIsOpen] = useState(true)
  if (count === 0) return null

  return (
    <div className="document-group">
      <div className="document-group-header" onClick={() => setIsOpen(!isOpen)}>
        <motion.div animate={{ rotate: isOpen ? 0 : -90 }} transition={{ duration: 0.2 }}>
          <ChevronDown size={18} />
        </motion.div>
        <span>{title}</span>
        <span className="document-group-badge">{count}</span>
      </div>
      <AnimatePresence initial={false}>
        {isOpen && (
          <motion.div 
            initial={{ height: 0, opacity: 0 }}
            animate={{ height: 'auto', opacity: 1 }}
            exit={{ height: 0, opacity: 0 }}
            transition={{ duration: 0.3, ease: 'easeInOut' }}
            style={{ overflow: 'hidden' }}
          >
            <div className="document-group-content custom-scrollbar">
              {documents.map((doc, idx) => (
                <motion.div 
                  initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: idx * 0.05 }}
                  key={doc.id} className="document-card"
                >
                  <div className="document-card-info">
                    <div className="document-card-icon-wrap"><FileText size={20} className="document-card-icon" /></div>
                    <div className="document-card-meta">
                      <h4 className="document-card-name" title={doc.originalFileName}>{doc.originalFileName}</h4>
                      <div className="document-card-details">
                        <span className="doc-type-badge">{doc.documentType}</span>
                        {doc.version > 1 && <span className="doc-type-badge">v{doc.version}</span>}
                        <span>&middot;</span>
                        <span>{formatFileSize(doc.fileSize)}</span>
                        <span>&middot;</span>
                        <span>{new Date(doc.createdAt).toLocaleDateString()}</span>
                        {doc.uploaderName && <><span>&middot;</span><span>By {doc.uploaderName}</span></>}
                      </div>
                      {doc.processingStatus === 'FAILED' && doc.extractionError && (
                        <div className="document-card-error">
                          <AlertCircle size={12} /> {doc.extractionError}
                        </div>
                      )}
                    </div>
                  </div>
                  <div className="document-card-actions">
                    {doc.processingStatus === 'FAILED' && onRetry && (
                      <Button variant="ghost" onClick={() => onRetry(doc.id)}><RefreshCw size={14} /> Retry</Button>
                    )}
                    {doc.processingStatus === 'READY' && (
                      <Button variant="ghost" onClick={() => window.open(`/api/documents/${doc.id}/download`, '_blank')}><Download size={14} /> Download</Button>
                    )}
                    <Button variant="ghost" onClick={() => onSelect(doc.id)}><Eye size={14} /> View</Button>
                    <Button variant="danger" onClick={() => onDelete(doc.id)}><Trash2 size={14} /> Delete</Button>
                  </div>
                </motion.div>
              ))}
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  )
}

function formatFileSize(bytes: number) {
  if (!bytes) return '0 B'
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}
