import { useMemo, useState } from 'react'
import { ArrowLeft, BookOpen, Bot, Code2, Database, FileText, ListTodo, Search } from 'lucide-react'
import { Link, useParams } from 'react-router-dom'
import { Button, EmptyState, TextArea } from '../components/ui/Ui'

const agents = [
  { id: 'research', name: 'Research Agent', description: 'Performs structured research and organizes findings into useful answers.', capabilities: ['Research plans', 'Source evaluation', 'Briefing notes'], icon: Search },
  { id: 'coding', name: 'Coding Agent', description: 'Helps understand codebases, plan changes, and generate implementation guidance.', capabilities: ['Code analysis', 'Implementation plans', 'Debugging support'], icon: Code2 },
  { id: 'sql', name: 'SQL Agent', description: 'Supports database exploration, query planning, and clear data explanations.', capabilities: ['Query drafting', 'Schema exploration', 'Data analysis'], icon: Database },
  { id: 'document', name: 'Document Agent', description: 'Works with your uploaded knowledge to summarize and locate information.', capabilities: ['Summaries', 'Key points', 'Document Q&A'], icon: FileText },
  { id: 'planner', name: 'Planner Agent', description: 'Breaks complex goals into clear, trackable execution plans.', capabilities: ['Goal breakdown', 'Task planning', 'Milestones'], icon: ListTodo },
]

export function AgentsPage() { return <div className="agents-page"><div className="page-intro"><div><p className="section-kicker">AI workforce</p><h2>AI Agents</h2><p>Specialized AI assistants for complex tasks.</p></div></div><div className="agent-catalog">{agents.map(({ id, name, description, capabilities, icon: Icon }) => <article key={id} className="agent-card"><span className="agent-card-icon"><Icon size={21} /></span><div><h3>{name}</h3><p>{description}</p></div><ul>{capabilities.map((capability) => <li key={capability}>{capability}</li>)}</ul><div className="agent-card-footer"><span className="status-badge"><i />Integration ready</span><Link className="button button-secondary" to={`/app/agents/${id}`}>Open agent</Link></div></article>)}</div></div> }

export function AgentWorkspacePage() {
  const { agentId } = useParams()
  const agent = useMemo(() => agents.find((item) => item.id === agentId), [agentId])
  const [task, setTask] = useState('')
  if (!agent) return <div className="not-found"><EmptyState icon={<Bot size={23} />} title="Agent not found" description="Choose an available specialized agent from the workspace." action={<Link className="button button-primary" to="/app/agents">View agents</Link>} /></div>
  const Icon = agent.icon
  return <div className="agent-workspace"><Link to="/app/agents" className="back-link"><ArrowLeft size={15} />All agents</Link><header className="agent-workspace-header"><span className="agent-card-icon"><Icon size={22} /></span><div><p className="section-kicker">Specialized assistant</p><h2>{agent.name}</h2><p>{agent.description}</p></div><span className="status-badge"><i />Integration ready</span></header><div className="agent-workspace-grid"><section className="workspace-panel"><div className="section-heading"><div><p className="section-kicker">Task</p><h2>What should this agent work on?</h2></div></div><TextArea label="Task brief" value={task} onChange={(event) => setTask(event.target.value)} placeholder="Describe the outcome, context, and any constraints…" rows={7} /><Button disabled={!task.trim()} title="Agent execution requires a backend endpoint"><Bot size={16} />Launch task</Button><p className="integration-note">Agent execution is not sent because this backend does not yet expose an agent task API.</p></section><section className="workspace-panel"><div className="section-heading"><div><p className="section-kicker">Output</p><h2>Task progress</h2></div></div><EmptyState icon={<BookOpen size={22} />} title="No task in progress" description="When the agent service is connected, plans, sources, and outputs will appear here." /></section></div></div>
}
