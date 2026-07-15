import { ArrowRight, Bot, FileText, MessageSquare, Search, Sparkles, Upload } from 'lucide-react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { Button, EmptyState } from '../components/ui/Ui'

const actions = [
  { label: 'Start AI Chat', detail: 'Ask, analyze, and draft', icon: MessageSquare, to: '/app/chat' },
  { label: 'Upload Document', detail: 'Prepare knowledge for AI', icon: Upload, to: '/app/documents' },
  { label: 'Research a Topic', detail: 'Open the Research Agent', icon: Search, to: '/app/agents/research' },
  { label: 'Create Agent Task', detail: 'Delegate a complex workflow', icon: Bot, to: '/app/agents' },
]
const agents = [
  { name: 'Research Agent', description: 'Structures research and source-based analysis.', icon: Search },
  { name: 'Coding Agent', description: 'Helps understand, plan, and generate code.', icon: Bot },
  { name: 'Document Agent', description: 'Works with your connected knowledge sources.', icon: FileText },
]

export function DashboardPage() {
  const { user } = useAuth()
  const navigate = useNavigate()
  const name = user?.name?.split(' ')[0] || 'there'
  return <div className="dashboard-page"><section className="dashboard-welcome"><p className="section-kicker">Your workspace</p><h2>Good morning, {name}</h2><p>What would you like to work on today?</p><button className="ai-command" onClick={() => navigate('/app/chat')}><span><Sparkles size={18} />Ask AI, search your workspace, or start a task...</span><kbd>⌘ K</kbd></button></section><section className="quick-actions" aria-label="Quick actions">{actions.map(({ label, detail, icon: Icon, to }) => <button key={label} className="quick-action" onClick={() => navigate(to)}><span className="quick-action-icon"><Icon size={19} /></span><span><strong>{label}</strong><small>{detail}</small></span><ArrowRight size={16} /></button>)}</section><div className="dashboard-grid"><section className="workspace-panel"><div className="section-heading"><div><p className="section-kicker">Continue working</p><h2>Recent conversations</h2></div><Button variant="ghost" onClick={() => navigate('/app/chat')}>View chat <ArrowRight size={15} /></Button></div><EmptyState icon={<MessageSquare size={22} />} title="No conversations yet" description="Start an AI chat to keep your research, analysis, and decisions in one place." action={<Button variant="secondary" onClick={() => navigate('/app/chat')}>Start AI chat</Button>} /></section><section className="workspace-panel"><div className="section-heading"><div><p className="section-kicker">Knowledge</p><h2>Recent documents</h2></div><Button variant="ghost" onClick={() => navigate('/app/documents')}>View documents <ArrowRight size={15} /></Button></div><EmptyState icon={<FileText size={22} />} title="No documents yet" description="Upload a PDF, DOCX, or XLSX when the document service is available." action={<Button variant="secondary" onClick={() => navigate('/app/documents')}>Go to documents</Button>} /></section></div><section className="agent-section"><div className="section-heading"><div><p className="section-kicker">Specialized assistance</p><h2>AI agents</h2></div><Button variant="secondary" onClick={() => navigate('/app/agents')}>Browse agents</Button></div><div className="agent-preview-grid">{agents.map(({ name: agentName, description, icon: Icon }) => <article key={agentName} className="agent-preview"><span><Icon size={20} /></span><div><h3>{agentName}</h3><p>{description}</p></div><button onClick={() => navigate(`/app/agents/${agentName.split(' ')[0].toLowerCase()}`)}>Open <ArrowRight size={14} /></button></article>)}</div></section></div>
}
