import { ArrowRight, Bot, FileText, MessageSquare, Search, Sparkles, Upload, HardDrive, Clock, Activity as ActivityIcon, ArrowUpRight, CheckCircle2 } from 'lucide-react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { Button, EmptyState } from '../components/ui/Ui'
import { useEffect, useState } from 'react'
import { dashboardApi, type DashboardResponse } from '../lib/api'
import { motion } from 'framer-motion'

const actions = [
  { label: 'Start AI Chat', detail: 'Ask, analyze, and draft', icon: MessageSquare, to: '/app/chat' },
  { label: 'Upload Document', detail: 'Prepare knowledge for AI', icon: Upload, to: '/app/documents' },
  { label: 'Research a Topic', detail: 'Open the Research Agent', icon: Search, to: '/app/agents/research-assistant' },
  { label: 'Create Agent Task', detail: 'Delegate a complex workflow', icon: Bot, to: '/app/agents' },
]

function formatBytes(bytes: number, decimals = 2) {
  if (!+bytes) return '0 Bytes'
  const k = 1024
  const dm = decimals < 0 ? 0 : decimals
  const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return `${parseFloat((bytes / Math.pow(k, i)).toFixed(dm))} ${sizes[i]}`
}

function formatTimeAgo(dateString: string) {
  const date = new Date(dateString)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const seconds = Math.floor(diff / 1000)
  const minutes = Math.floor(seconds / 60)
  const hours = Math.floor(minutes / 60)
  const days = Math.floor(hours / 24)
  if (days > 0) return `${days}d ago`
  if (hours > 0) return `${hours}h ago`
  if (minutes > 0) return `${minutes}m ago`
  return 'Just now'
}

function groupActivityByTime(feed: any[]) {
  const now = new Date()
  const todayStart = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  const yesterdayStart = new Date(todayStart.getTime() - 86400000)

  const groups: { label: string; items: any[] }[] = [
    { label: 'Today', items: [] },
    { label: 'Yesterday', items: [] },
    { label: 'Earlier', items: [] },
  ]

  for (const item of feed) {
    const date = new Date(item.timestamp)
    if (date >= todayStart) groups[0].items.push(item)
    else if (date >= yesterdayStart) groups[1].items.push(item)
    else groups[2].items.push(item)
  }

  return groups.filter((group) => group.items.length > 0)
}

export function DashboardPage() {
  const { user } = useAuth()
  const navigate = useNavigate()
  const name = user?.name?.split(' ')[0] || 'there'

  const [data, setData] = useState<DashboardResponse | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    dashboardApi.get().then((res) => {
      setData(res.data.data)
    }).catch((err) => {
      console.error(err)
    }).finally(() => {
      setLoading(false)
    })
  }, [])

  if (loading) {
    return <div className="dashboard-page dashboard-loading">
      <div className="skeleton header-skeleton" />
      <div className="skeleton cards-skeleton" />
      <div className="skeleton feed-skeleton" />
    </div>
  }

  const groupedActivity = data ? groupActivityByTime(data.activityFeed) : []

  return <motion.div 
    initial={{ opacity: 0, y: 10 }}
    animate={{ opacity: 1, y: 0 }}
    transition={{ duration: 0.3 }}
    className="dashboard-page"
  >
    <section className="dashboard-welcome">
      <p className="section-kicker">Your workspace</p>
      <h2>Good morning, {name}</h2>
      <p>What would you like to work on today?</p>
      <button className="ai-command" onClick={() => navigate('/app/chat')}>
        <span><Sparkles size={18} />Ask AI, search your workspace, or start a task...</span>
        <kbd>⌘ K</kbd>
      </button>
    </section>

    {data && (
      <section className="stat-cards">
        <motion.div whileHover={{ y: -2 }} className="stat-card">
          <div className="stat-icon"><MessageSquare size={18} /></div>
          <div className="stat-info">
            <h4>{data.stats.conversationsCount}</h4>
            <p>Conversations</p>
            <span className="stat-trend"><ArrowUpRight size={12} /> 12% this week</span>
          </div>
        </motion.div>
        <motion.div whileHover={{ y: -2 }} className="stat-card">
          <div className="stat-icon"><FileText size={18} /></div>
          <div className="stat-info">
            <h4>{data.stats.documentsCount}</h4>
            <p>Documents</p>
            <span className="stat-trend"><ArrowUpRight size={12} /> 4 added recently</span>
          </div>
        </motion.div>
        <motion.div whileHover={{ y: -2 }} className="stat-card">
          <div className="stat-icon"><Bot size={18} /></div>
          <div className="stat-info">
            <h4>{data.stats.agentsCount}</h4>
            <p>Agents</p>
            <span className="stat-trend neutral"><CheckCircle2 size={12} /> 2 active now</span>
          </div>
        </motion.div>
        <motion.div whileHover={{ y: -2 }} className="stat-card">
          <div className="stat-icon"><HardDrive size={18} /></div>
          <div className="stat-info">
            <h4>{formatBytes(data.stats.storageUsedBytes)}</h4>
            <p>Storage</p>
            <div className="stat-progress"><div className="stat-progress-fill" style={{ width: '45%' }}></div></div>
          </div>
        </motion.div>
      </section>
    )}

    <section className="quick-actions" aria-label="Quick actions">
      {actions.map(({ label, detail, icon: Icon, to }, i) => 
        <motion.button 
          key={label} 
          initial={{ opacity: 0, y: 10 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: i * 0.05 }}
          className="quick-action" 
          onClick={() => navigate(to)}
        >
          <span className="quick-action-icon"><Icon size={19} /></span>
          <span>
            <strong>{label}</strong>
            <small>{detail}</small>
          </span>
          <ArrowRight size={16} />
        </motion.button>
      )}
    </section>

    <div className="dashboard-grid">
      <section className="workspace-panel panel-fixed activity-feed">
        <div className="panel-header">
          <div>
            <p className="section-kicker">Recent Updates</p>
            <h2>Activity Feed</h2>
          </div>
        </div>
        <div className="panel-scroll-area custom-scrollbar">
          {data && data.activityFeed.length > 0 ? (
            <div className="timeline">
              {groupedActivity.map((group, groupIdx) => (
                <div key={group.label} className="timeline-group">
                  <div className="timeline-group-label">{group.label}</div>
                  {group.items.map((item, idx) => (
                    <motion.div 
                      initial={{ opacity: 0, x: -10 }} animate={{ opacity: 1, x: 0 }} transition={{ delay: (groupIdx * 0.1) + (idx * 0.05) }}
                      key={item.id} className="timeline-step completed timeline-item-interactive" onClick={() => navigate(item.url)}
                    >
                      <div className="timeline-marker-wrapper"><div className="timeline-marker"></div></div>
                      <div className="timeline-content">
                        <div className="timeline-header">
                          <span className="timeline-type-icon">{item.type === 'CONVERSATION' ? <MessageSquare size={12} /> : <FileText size={12} />}</span>
                          <span className="timeline-time">{formatTimeAgo(item.timestamp)}</span>
                        </div>
                        <p className="timeline-label">{item.title}</p>
                        <p className="timeline-desc">{item.description}</p>
                      </div>
                    </motion.div>
                  ))}
                </div>
              ))}
            </div>
          ) : (
            <EmptyState icon={<ActivityIcon size={32} />} title="No activity yet" description="Start a conversation or upload a document to see activity." />
          )}
        </div>
      </section>

      <section className="workspace-panel panel-fixed continue-working">
        <div className="panel-header">
          <div>
            <p className="section-kicker">Continue working</p>
            <h2>Recent conversations</h2>
          </div>
          <Button variant="ghost" onClick={() => navigate('/app/chat')}>View all <ArrowRight size={15} /></Button>
        </div>
        <div className="panel-scroll-area custom-scrollbar">
          {data && data.recentConversations.length > 0 ? (
            <div className="conversation-cards">
              {data.recentConversations.map((conv, i) => (
                <motion.div 
                  initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: i * 0.05 }}
                  key={conv.id} className="conversation-card" onClick={() => navigate(`/app/chat/${conv.id}`)}
                >
                  <div className="conversation-card-header">
                    <MessageSquare size={16} />
                    <span className="truncate"><strong>{conv.title || 'New Conversation'}</strong></span>
                  </div>
                  <p className="conversation-card-preview line-clamp-2">Click to continue your conversation...</p>
                  <div className="conversation-card-footer">
                    <span className="conversation-agent-tag"><Bot size={10} /> {conv.agentId || 'General Assistant'}</span>
                    <span className="time-subtle"><Clock size={10} /> {formatTimeAgo(conv.updatedAt)}</span>
                  </div>
                </motion.div>
              ))}
            </div>
          ) : (
            <EmptyState icon={<MessageSquare size={32} />} title="No conversations" description="Start an AI chat to keep your research, analysis, and decisions in one place." action={<Button variant="secondary" onClick={() => navigate('/app/chat')}>Start AI chat</Button>} />
          )}
        </div>
      </section>
    </div>

    <section className="agent-section">
      <div className="section-heading">
        <div>
          <p className="section-kicker">Specialized assistance</p>
          <h2>Recently used agents</h2>
        </div>
        <Button variant="secondary" onClick={() => navigate('/app/agents')}>Browse all agents</Button>
      </div>
      {data && data.recentAgents.length > 0 ? (
        <div className="agent-preview-grid">
          {data.recentAgents.map(({ id, name: agentName, description }) => 
            <motion.article whileHover={{ y: -2 }} key={id} className="agent-preview">
              <span><Bot size={20} /></span>
              <div>
                <h3>{agentName}</h3>
                <p className="line-clamp-2">{description}</p>
              </div>
              <button onClick={() => navigate(`/app/agents/${id}`)}>Open <ArrowRight size={14} /></button>
            </motion.article>
          )}
        </div>
      ) : (
        <EmptyState icon={<Bot size={22} />} title="No agents used yet" description="Browse the agent catalog to find specialized AI assistants." action={<Button variant="secondary" onClick={() => navigate('/app/agents')}>Browse agents</Button>} />
      )}
    </section>
  </motion.div>
}
