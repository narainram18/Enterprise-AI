import { BrainCircuit } from 'lucide-react'
import { Link } from 'react-router-dom'

export function Brand({ compact = false }: { compact?: boolean }) {
  return <Link className="brand" to="/app" aria-label="Enterprise AI home"><span className="brand-icon"><BrainCircuit size={18} /></span>{!compact && <span>Enterprise <b>AI</b></span>}</Link>
}
