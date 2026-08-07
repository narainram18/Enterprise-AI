import { createContext, useContext, useState, type ReactNode, useCallback, useEffect } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { AlertTriangle, Info, X } from 'lucide-react'
import { Button, IconButton } from '../components/ui/Ui'

type ConfirmOptions = {
  title: string
  description: string
  confirmText?: string
  cancelText?: string
  variant?: 'danger' | 'primary'
  isAlert?: boolean
}

type ConfirmContextType = {
  confirm: (options: ConfirmOptions) => Promise<boolean>
}

const ConfirmationContext = createContext<ConfirmContextType | undefined>(undefined)

export function useConfirm() {
  const context = useContext(ConfirmationContext)
  if (!context) throw new Error('useConfirm must be used within ConfirmationProvider')
  return context
}

export function ConfirmationProvider({ children }: { children: ReactNode }) {
  const [isOpen, setIsOpen] = useState(false)
  const [options, setOptions] = useState<ConfirmOptions | null>(null)
  const [resolver, setResolver] = useState<(value: boolean) => void>()

  const confirm = useCallback((opts: ConfirmOptions) => {
    setOptions(opts)
    setIsOpen(true)
    return new Promise<boolean>((resolve) => {
      setResolver(() => resolve)
    })
  }, [])

  const handleConfirm = useCallback(() => {
    setIsOpen(false)
    if (resolver) resolver(true)
  }, [resolver])

  const handleCancel = useCallback(() => {
    setIsOpen(false)
    if (resolver) resolver(false)
  }, [resolver])

  useEffect(() => {
    function handleKeyDown(e: KeyboardEvent) {
      if (!isOpen) return
      if (e.key === 'Escape') handleCancel()
      if (e.key === 'Enter') handleConfirm()
    }
    window.addEventListener('keydown', handleKeyDown)
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [isOpen, handleCancel, handleConfirm])

  return (
    <ConfirmationContext.Provider value={{ confirm }}>
      {children}
      <AnimatePresence>
        {isOpen && options && (
          <>
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              transition={{ duration: 0.2 }}
              className="modal-overlay"
              onClick={handleCancel}
            />
            <motion.div
              initial={{ opacity: 0, scale: 0.95, y: 10 }}
              animate={{ opacity: 1, scale: 1, y: 0 }}
              exit={{ opacity: 0, scale: 0.95, y: 10 }}
              transition={{ duration: 0.2, type: 'spring', bounce: 0.2 }}
              className="confirmation-modal"
            >
              <div className="confirmation-header">
                <div className={`confirmation-icon ${options.variant === 'danger' ? 'danger' : 'info'}`}>
                  {options.variant === 'danger' ? <AlertTriangle size={24} /> : <Info size={24} />}
                </div>
                <IconButton label="Close" onClick={handleCancel}><X size={18} /></IconButton>
              </div>
              <div className="confirmation-body">
                <h3>{options.title}</h3>
                <p>{options.description}</p>
              </div>
              <div className="confirmation-actions">
                {!options.isAlert && (
                  <Button variant="secondary" onClick={handleCancel}>
                    {options.cancelText || 'Cancel'}
                  </Button>
                )}
                <Button 
                  variant={options.variant === 'danger' ? 'danger' : 'primary'} 
                  onClick={handleConfirm}
                >
                  {options.confirmText || (options.isAlert ? 'OK' : 'Confirm')}
                </Button>
              </div>
            </motion.div>
          </>
        )}
      </AnimatePresence>
    </ConfirmationContext.Provider>
  )
}
