import { useEffect, useMemo, useState } from 'react'
import { ArrowLeft, Bot, CheckCircle, Loader2, XCircle } from 'lucide-react'
import { Link, useParams } from 'react-router-dom'
import { Button, EmptyState, TextArea } from '../components/ui/Ui'
import { type Agent, agentsApi, agentTasksApi, type AgentTaskResponse } from '../lib/api'
import { AgentIcon } from '../components/AgentSelector'
import ReactMarkdown from 'react-markdown'
import remarkGfm from 'remark-gfm'

export function AgentsPage() { 
  const [agents, setAgents] = useState<Agent[]>([])
  useEffect(() => { agentsApi.list().then(({ data }) => setAgents(data)).catch(() => setAgents([])) }, [])
  
  return <div className="agents-page"><div className="page-intro"><div><p className="section-kicker">AI workforce</p><h2>AI Agents</h2><p>Specialized AI assistants for complex tasks.</p></div></div><div className="agent-catalog">{agents.map((agent) => <article key={agent.id} className="agent-card"><span className="agent-card-icon"><AgentIcon iconName={agent.icon} color={agent.color} size={21} /></span><div><h3>{agent.name}</h3><p>{agent.description}</p></div><ul>{agent.suggestedPrompts?.slice(0, 3).map((capability) => <li key={capability}>{capability}</li>)}</ul><div className="agent-card-footer"><span className="status-badge"><i />Integration ready</span><Link className="button button-secondary" to={`/app/agents/${agent.id}`}>Open agent</Link></div></article>)}</div></div> 
}

export function AgentWorkspacePage() {
  const { agentId } = useParams()
  const [agents, setAgents] = useState<Agent[]>([])
  useEffect(() => { agentsApi.list().then(({ data }) => setAgents(data)).catch(() => setAgents([])) }, [])
  const agent = useMemo(() => agents.find((item) => item.id === agentId), [agents, agentId])
  
  const [task, setTask] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [taskId, setTaskId] = useState<number | null>(null)
  const [taskState, setTaskState] = useState<AgentTaskResponse | null>(null)

  useEffect(() => {
    if (!taskId) return
    let interval = setInterval(async () => {
      try {
        const { data: { data } } = await agentTasksApi.get(taskId)
        setTaskState(data)
        if (data.status === 'COMPLETED' || data.status === 'FAILED') {
          clearInterval(interval)
        }
      } catch (err) {
        console.error(err)
      }
    }, 3000)
    return () => clearInterval(interval)
  }, [taskId])

  const handleLaunch = async () => {
    if (!task.trim() || !agentId) return
    setIsSubmitting(true)
    try {
      const { data: { data } } = await agentTasksApi.create(agentId, task)
      setTaskId(data.id)
      setTaskState(data)
    } catch (err) {
      console.error(err)
    } finally {
      setIsSubmitting(false)
    }
  }

  if (!agents.length) return null // loading
  if (!agent) return <div className="not-found"><EmptyState icon={<Bot size={23} />} title="Agent not found" description="Choose an available specialized agent from the workspace." action={<Link className="button button-primary" to="/app/agents">View agents</Link>} /></div>
  
  return (
    <div className="agent-workspace">
      <Link to="/app/agents" className="back-link"><ArrowLeft size={15} />All agents</Link>
      <header className="agent-workspace-header">
        <span className="agent-card-icon" style={{ '--agent-color': agent.color } as any}><AgentIcon iconName={agent.icon} color={agent.color} size={22} /></span>
        <div>
          <p className="section-kicker">Specialized assistant</p>
          <h2>{agent.name}</h2>
          <p>{agent.description}</p>
        </div>
        <span className="status-badge"><i />{agent.supportsRag ? 'RAG Enabled' : 'No RAG'}</span>
      </header>
      
      <div className="agent-workspace-grid">
        <section className="workspace-panel">
          <div className="section-heading">
            <div>
              <p className="section-kicker">Agent Profile</p>
              <h2>Capabilities & Context</h2>
            </div>
          </div>
          
          <div className="agent-info-grid">
            <div className="info-box">
              <h4>System Prompt</h4>
              <p className="system-prompt">{agent.systemPrompt}</p>
            </div>
            <div className="info-box">
              <h4>Best For</h4>
              <p>{agent.bestFor}</p>
            </div>
            {agent.supportedTools && agent.supportedTools.length > 0 && (
              <div className="info-box">
                <h4>Tool Permissions</h4>
                <div className="tool-tags">
                  {agent.supportedTools.map(tool => <span key={tool} className="tool-tag">{tool}</span>)}
                </div>
              </div>
            )}
            {agent.suggestedPrompts && agent.suggestedPrompts.length > 0 && (
              <div className="info-box">
                <h4>Suggested Prompts</h4>
                <ul className="prompt-list">
                  {agent.suggestedPrompts.map(prompt => <li key={prompt}>{prompt}</li>)}
                </ul>
              </div>
            )}
          </div>
        </section>
        
        <section className="workspace-panel">
          <div className="section-heading">
            <div>
              <p className="section-kicker">Task</p>
              <h2>What should this agent work on?</h2>
            </div>
          </div>
          
          {!taskId ? (
            <>
              <TextArea label="Task brief" value={task} onChange={(event) => setTask(event.target.value)} placeholder="Describe the outcome, context, and any constraints…" rows={7} />
              <Button disabled={!task.trim() || isSubmitting} onClick={handleLaunch}>
                {isSubmitting ? <Loader2 size={16} className="spin" /> : <Bot size={16} />} 
                Launch task
              </Button>
            </>
          ) : (
            <div className="task-status-container">
              <div className="status-header">
                <h3>Task Status: <span className={`status-${taskState?.status.toLowerCase()}`}>{taskState?.status}</span></h3>
                {taskState?.status === 'RUNNING' && <Loader2 size={24} className="spin" />}
                {taskState?.status === 'COMPLETED' && <CheckCircle size={24} color="green" />}
                {taskState?.status === 'FAILED' && <XCircle size={24} color="red" />}
              </div>
              <div className="task-result">
                <h4>Result:</h4>
                {taskState?.status === 'FAILED' ? (
                  <p className="error-text">{taskState?.errorMessage}</p>
                ) : (
                  <div className="markdown-body">
                    {taskState?.result ? (
                       <ReactMarkdown remarkPlugins={[remarkGfm]}>{taskState.result}</ReactMarkdown>
                    ) : (
                      <p>Waiting for output...</p>
                    )}
                  </div>
                )}
              </div>
              <Button variant="secondary" onClick={() => { setTaskId(null); setTask(''); setTaskState(null); }}>Start New Task</Button>
            </div>
          )}
        </section>
      </div>
    </div>
  )
}
