import { useState } from 'react'
import { ArrowRight, Check, Eye, EyeOff, ShieldCheck, Sparkles } from 'lucide-react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { Brand } from '../components/ui/Brand'
import { Button, TextInput } from '../components/ui/Ui'

function AuthLayout({ children }: { children: React.ReactNode }) {
  const benefits = ['Chat with enterprise AI', 'Search and understand documents', 'Automate work with specialized AI agents']
  return <main className="auth-layout"><section className="auth-intro"><Brand /><div className="auth-intro-content"><span className="auth-kicker"><Sparkles size={14} />Enterprise intelligence</span><h1>Your intelligent workspace for knowledge, research and execution.</h1><p>Bring your team’s thinking, documents, and AI workflows together in one secure workspace.</p><ul>{benefits.map((benefit) => <li key={benefit}><Check size={16} />{benefit}</li>)}</ul></div><p className="auth-security"><ShieldCheck size={15} />Secure by design. Built for enterprise teams.</p></section><section className="auth-panel"><div className="auth-mobile-brand"><Brand /></div>{children}<p className="auth-terms">By continuing, you agree to our Terms of Service and Privacy Policy.</p></section></main>
}

function PasswordField({ label, value, onChange, autoComplete, error, placeholder = 'Enter your password' }: { label: string; value: string; onChange: (value: string) => void; autoComplete: string; error?: string; placeholder?: string }) {
  const [visible, setVisible] = useState(false)
  return <label className="form-field"><span>{label}</span><span className="password-input"><input type={visible ? 'text' : 'password'} value={value} onChange={(event) => onChange(event.target.value)} placeholder={placeholder} autoComplete={autoComplete} required minLength={6} /><button type="button" onClick={() => setVisible((value) => !value)} aria-label={visible ? 'Hide password' : 'Show password'}>{visible ? <EyeOff size={17} /> : <Eye size={17} />}</button></span>{error && <small className="field-error">{error}</small>}</label>
}

function AuthError({ message }: { message: string }) { return message ? <div className="auth-error" role="alert">{message}</div> : null }

export function LoginPage() {
  const { login, isAuthenticated, isLoading: authLoading } = useAuth()
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [remember, setRemember] = useState(true)
  const [error, setError] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  if (!authLoading && isAuthenticated) return <Navigate to="/app" replace />

  async function submit(event: React.FormEvent<HTMLFormElement>) { event.preventDefault(); setError(''); setIsSubmitting(true); try { await login(email, password, remember); navigate('/app') } catch { setError('We could not sign you in with those details. Please try again.') } finally { setIsSubmitting(false) } }
  return <AuthLayout><div className="auth-form-card"><div className="auth-heading"><p>Welcome back</p><h2>Sign in to Enterprise AI</h2><span>Continue where your work left off.</span></div><form onSubmit={submit} className="auth-form" noValidate><AuthError message={error} /><TextInput label="Work email" type="email" value={email} onChange={(event) => setEmail(event.target.value)} placeholder="you@company.com" autoComplete="email" required /><PasswordField label="Password" value={password} onChange={setPassword} autoComplete="current-password" /><div className="auth-options"><label className="check-label"><input type="checkbox" checked={remember} onChange={(event) => setRemember(event.target.checked)} />Remember me</label><button type="button" className="text-link">Forgot password?</button></div><Button type="submit" className="auth-submit" isLoading={isSubmitting}>Sign in <ArrowRight size={16} /></Button></form><p className="auth-switch">New to Enterprise AI? <Link to="/register">Create an account</Link></p></div></AuthLayout>
}

export function RegisterPage() {
  const { signup, isAuthenticated, isLoading: authLoading } = useAuth()
  const navigate = useNavigate()
  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [error, setError] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  if (!authLoading && isAuthenticated) return <Navigate to="/app" replace />
  const strength = password.length < 6 ? 'Too short' : password.length < 10 ? 'Good' : 'Strong'
  const passwordError = password && password.length < 6 ? 'Use at least 6 characters.' : undefined
  const confirmError = confirmPassword && password !== confirmPassword ? 'Passwords do not match.' : undefined

  async function submit(event: React.FormEvent<HTMLFormElement>) { event.preventDefault(); if (password.length < 6 || password !== confirmPassword) return; setError(''); setIsSubmitting(true); try { await signup(name, email, password); navigate('/app') } catch { setError('We could not create your account. An account may already use this email.') } finally { setIsSubmitting(false) } }
  return <AuthLayout><div className="auth-form-card"><div className="auth-heading"><p>Create your workspace</p><h2>Start with Enterprise AI</h2><span>Set up your account in a few moments.</span></div><form onSubmit={submit} className="auth-form" noValidate><AuthError message={error} /><TextInput label="Full name" value={name} onChange={(event) => setName(event.target.value)} placeholder="Alex Morgan" autoComplete="name" required /><TextInput label="Work email" type="email" value={email} onChange={(event) => setEmail(event.target.value)} placeholder="you@company.com" autoComplete="email" required /><div><PasswordField label="Password" value={password} onChange={setPassword} autoComplete="new-password" placeholder="At least 6 characters" error={passwordError} />{password && <div className={`password-strength strength-${strength.toLowerCase().replace(' ', '-')}`}><i /><i /><i /><span>{strength}</span></div>}</div><PasswordField label="Confirm password" value={confirmPassword} onChange={setConfirmPassword} autoComplete="new-password" error={confirmError} /><Button type="submit" className="auth-submit" isLoading={isSubmitting} disabled={Boolean(passwordError || confirmError)}>Create account <ArrowRight size={16} /></Button></form><p className="auth-switch">Already have an account? <Link to="/login">Sign in</Link></p></div></AuthLayout>
}
