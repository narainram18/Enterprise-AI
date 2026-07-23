import type { ButtonHTMLAttributes, InputHTMLAttributes, TextareaHTMLAttributes } from 'react'
import { AlertCircle, LoaderCircle } from 'lucide-react'

type ButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & { variant?: 'primary' | 'secondary' | 'ghost' | 'danger'; isLoading?: boolean }
export function Button({ variant = 'primary', isLoading, className = '', children, disabled, ...props }: ButtonProps) {
  return <button {...props} disabled={disabled || isLoading} className={`button button-${variant} ${className}`}>{isLoading && <LoaderCircle className="spin" size={16} />}{children}</button>
}

export function IconButton({ label, className = '', children, ...props }: ButtonHTMLAttributes<HTMLButtonElement> & { label: string }) {
  return <button {...props} className={`icon-button ${className}`} aria-label={label} title={label}>{children}</button>
}

export function TextInput({ label, error, className = '', ...props }: InputHTMLAttributes<HTMLInputElement> & { label: string; error?: string }) {
  return <label className="form-field"><span>{label}</span><input {...props} className={className} />{error && <small className="field-error">{error}</small>}</label>
}

export function TextArea({ label, className = '', ...props }: TextareaHTMLAttributes<HTMLTextAreaElement> & { label?: string }) {
  return <label className="form-field">{label && <span>{label}</span>}<textarea {...props} className={className} /></label>
}

export function ErrorState({ message, onRetry, title = 'Something went wrong' }: { message: string; onRetry?: () => void; title?: string }) {
  return <div className="state state-error"><AlertCircle size={22} /><div><strong>{title}</strong><p>{message}</p>{onRetry && <Button variant="secondary" onClick={onRetry}>Try again</Button>}</div></div>
}

export function EmptyState({ icon, title, description, action }: { icon: React.ReactNode; title: string; description: string; action?: React.ReactNode }) {
  return <div className="empty-state"><span className="empty-state-icon">{icon}</span><h2>{title}</h2><p>{description}</p>{action}</div>
}

export function LoadingState({ label = 'Loading…' }: { label?: string }) { return <div className="loading-state"><LoaderCircle className="spin" size={20} />{label}</div> }
