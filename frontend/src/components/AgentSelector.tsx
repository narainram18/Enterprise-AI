import { useEffect, useState } from 'react'
import { Bot, BookOpen, Code, Database, ListTodo, Brain, Check } from 'lucide-react'
import { type Agent, agentsApi } from '../lib/api'

type AgentSelectorProps = {
  selectedAgentId: string
  onSelect: (agentId: string) => void
  disabled?: boolean
}

export function AgentSelector({ selectedAgentId, onSelect, disabled }: AgentSelectorProps) {
  const [agents, setAgents] = useState<Agent[]>([])
  const [isOpen, setIsOpen] = useState(false)
  
  useEffect(() => {
    agentsApi.list()
      .then(({ data }) => setAgents(data))
      .catch(() => setAgents([]))
  }, [])
  
  if (!agents.length) return null
  
  const selected = agents.find(a => a.id === selectedAgentId) || agents[0]
  if (!selected) return null

  return (
    <div className="agent-selector-wrapper">
      <button 
        className="agent-selector-btn" 
        onClick={() => !disabled && setIsOpen(!isOpen)}
        disabled={disabled}
        aria-label="Select Agent"
      >
        <AgentIcon iconName={selected.icon} color={selected.color} size={14} />
        <span className="agent-selector-name">{selected.name}</span>
      </button>
      
      {isOpen && (
        <div className="agent-selector-dropdown">
          <div className="agent-selector-header">Select an Agent</div>
          <div className="agent-selector-list">
            {agents.map((agent) => (
              <button 
                key={agent.id} 
                className={`agent-selector-item ${agent.id === selectedAgentId ? 'active' : ''}`}
                onClick={() => {
                  onSelect(agent.id)
                  setIsOpen(false)
                }}
              >
                <div className="agent-selector-icon-wrap">
                  <AgentIcon iconName={agent.icon} color={agent.color} />
                </div>
                <div className="agent-selector-details">
                  <div className="agent-selector-title">
                    {agent.name}
                    {agent.id === selectedAgentId && <Check size={14} className="agent-selector-check" />}
                  </div>
                  <div className="agent-selector-desc">{agent.description}</div>
                </div>
              </button>
            ))}
          </div>
        </div>
      )}
      
      {isOpen && <div className="agent-selector-backdrop" onClick={() => setIsOpen(false)} />}
    </div>
  )
}

export function AgentIcon({ iconName, color, size = 18 }: { iconName: string; color: string; size?: number }) {
  const props = { size, color }
  switch (iconName) {
    case 'Brain': return <Brain {...props} />
    case 'BookOpen': return <BookOpen {...props} />
    case 'Code': return <Code {...props} />
    case 'Database': return <Database {...props} />
    case 'ListTodo': return <ListTodo {...props} />
    default: return <Bot {...props} />
  }
}
