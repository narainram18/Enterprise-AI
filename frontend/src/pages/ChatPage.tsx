import { useEffect, useRef, useState, type KeyboardEvent } from 'react'
import { Bot, CircleStop, MoreHorizontal, Paperclip, Plus, Search, Send, Sparkles, UserRound } from 'lucide-react'
import ReactMarkdown from 'react-markdown'
import { useNavigate, useParams } from 'react-router-dom'
import { conversationsApi, streamConversationMessage, type ChatMessageResponse, type ConversationSummary } from '../lib/api'
import { Button, EmptyState, ErrorState, IconButton, LoadingState, TextArea } from '../components/ui/Ui'

type ChatMessage = Omit<ChatMessageResponse, 'id'> & { id: number | string; isStreaming?: boolean }

export function ChatPage() {
  const { chatId } = useParams()
  const navigate = useNavigate()
  const routeConversationId = chatId ? Number(chatId) : null
  const [conversations, setConversations] = useState<ConversationSummary[]>([])
  const [activeConversation, setActiveConversation] = useState<ConversationSummary | null>(null)
  const [messages, setMessages] = useState<ChatMessage[]>([])
  const [message, setMessage] = useState('')
  const [search, setSearch] = useState('')
  const [notice, setNotice] = useState('')
  const [loadError, setLoadError] = useState('')
  const [isLoadingConversation, setIsLoadingConversation] = useState(false)
  const [isStreaming, setIsStreaming] = useState(false)
  const abortControllerRef = useRef<AbortController | null>(null)
  const requestIdRef = useRef(0)
  const currentConversationRef = useRef<number | null>(routeConversationId)
  const skipRouteLoadRef = useRef<number | null>(null)
  const messagesEndRef = useRef<HTMLDivElement | null>(null)

  useEffect(() => {
    void loadConversations()
  }, [])

  useEffect(() => {
    if (skipRouteLoadRef.current === routeConversationId) {
      skipRouteLoadRef.current = null
      return
    }

    stopGeneration()
    currentConversationRef.current = routeConversationId
    setNotice('')
    setLoadError('')

    if (!routeConversationId || !Number.isInteger(routeConversationId)) {
      setActiveConversation(null)
      setMessages([])
      setIsLoadingConversation(false)
      return
    }

    const loadId = routeConversationId
    setIsLoadingConversation(true)
    conversationsApi.get(loadId)
      .then(({ data }) => {
        if (currentConversationRef.current !== loadId) return
        setActiveConversation(data.data)
        setMessages(data.data.messages)
      })
      .catch(() => {
        if (currentConversationRef.current === loadId) setLoadError('We couldn’t load this conversation. Please try again.')
      })
      .finally(() => {
        if (currentConversationRef.current === loadId) setIsLoadingConversation(false)
      })
  }, [routeConversationId])

  useEffect(() => () => stopGeneration(), [])

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth', block: 'end' })
  }, [messages])

  async function loadConversations() {
    try {
      const { data } = await conversationsApi.list({ page: 0, size: 50 })
      setConversations(data.data.content)
    } catch {
      setConversations([])
    }
  }

  function stopGeneration() {
    requestIdRef.current += 1
    abortControllerRef.current?.abort()
    abortControllerRef.current = null
    setIsStreaming(false)
    setMessages((current) => current.filter((item) => !item.isStreaming))
  }

  function selectConversation(id: number) {
    if (id === currentConversationRef.current && routeConversationId === id) return
    stopGeneration()
    navigate(`/app/chat/${id}`)
  }

  function startNewConversation() {
    stopGeneration()
    setMessage('')
    setNotice('')
    setLoadError('')
    setActiveConversation(null)
    setMessages([])
    currentConversationRef.current = null
    navigate('/app/chat')
  }

  async function sendMessage() {
    const content = message.trim()
    if (!content || isStreaming) return

    const requestId = requestIdRef.current + 1
    requestIdRef.current = requestId
    const controller = new AbortController()
    abortControllerRef.current = controller
    let userPersisted = false
    let conversationId = currentConversationRef.current
    setMessage('')
    setNotice('')
    setIsStreaming(true)

    try {
      if (!conversationId) {
        const title = content.length > 60 ? `${content.slice(0, 57)}…` : content
        const { data } = await conversationsApi.create(title)
        if (requestIdRef.current !== requestId) return
        conversationId = data.data.id
        currentConversationRef.current = conversationId
        setActiveConversation(data.data)
        setMessages([])
        skipRouteLoadRef.current = conversationId
        navigate(`/app/chat/${conversationId}`, { replace: true })
        void loadConversations()
      }

      if (!conversationId || requestIdRef.current !== requestId) return
      const streamingAssistantId = `streaming-assistant-${requestId}`
      await streamConversationMessage(
        conversationId,
        content,
        {
          onUserMessage: (userMessage) => {
            if (requestIdRef.current !== requestId) return
            userPersisted = true
            setMessages((current) => current.some((item) => item.id === userMessage.id)
              ? current
              : [...current, userMessage])
          },
          onToken: (token) => {
            if (requestIdRef.current !== requestId) return
            setMessages((current) => {
              const existingIndex = current.findIndex((item) => item.id === streamingAssistantId)
              if (existingIndex === -1) {
                return [...current, {
                  id: streamingAssistantId,
                  role: 'ASSISTANT',
                  content: token,
                  createdAt: new Date().toISOString(),
                  isStreaming: true,
                }]
              }
              return current.map((item, index) => index === existingIndex
                ? { ...item, content: item.content + token }
                : item)
            })
          },
          onComplete: (assistantMessage) => {
            if (requestIdRef.current !== requestId) return
            setMessages((current) => [
              ...current.filter((item) => item.id !== streamingAssistantId),
              assistantMessage,
            ])
            void loadConversations()
          },
          onError: (error) => {
            if (requestIdRef.current !== requestId) return
            setMessages((current) => current.filter((item) => item.id !== streamingAssistantId))
            setNotice(error)
          },
        },
        controller.signal,
      )
    } catch (error) {
      if (controller.signal.aborted || requestIdRef.current !== requestId) return
      setMessages((current) => current.filter((item) => !item.isStreaming))
      if (!userPersisted) setMessage(content)
      setNotice(error instanceof Error ? error.message : 'We couldn’t send your message. Please try again.')
    } finally {
      if (requestIdRef.current === requestId) {
        abortControllerRef.current = null
        setIsStreaming(false)
      }
    }
  }

  function onKeyDown(event: KeyboardEvent<HTMLTextAreaElement>) {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault()
      void sendMessage()
    }
  }

  const visibleConversations = conversations.filter((conversation) => conversation.title.toLowerCase().includes(search.toLowerCase()))
  const title = activeConversation?.title ?? 'New conversation'

  return <div className="chat-page">
    <aside className="chat-history">
      <Button className="chat-new" onClick={startNewConversation}><Plus size={16} />New chat</Button>
      <label className="history-search"><Search size={16} /><input value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Search conversations" aria-label="Search conversations" /></label>
      <div className="conversation-list">
        {visibleConversations.length ? visibleConversations.map((conversation) => <button key={conversation.id} className={`conversation-list-item ${conversation.id === routeConversationId ? 'active' : ''}`} onClick={() => selectConversation(conversation.id)}>
          <MessageIcon /><span>{conversation.title}</span>
        </button>) : <div className="history-empty"><MessageIcon /><p>{search ? 'No matches' : 'No chat history'}</p><span>{search ? 'Try a different search.' : 'Your conversations will appear here.'}</span></div>}
      </div>
    </aside>
    <section className="conversation">
      <header className="conversation-header"><div><h2>{title}</h2><span><Bot size={14} />Enterprise AI</span></div><IconButton label="Conversation options"><MoreHorizontal size={19} /></IconButton></header>
      <div className="conversation-body">
        {isLoadingConversation ? <LoadingState label="Loading conversation…" /> : loadError ? <ErrorState message={loadError} onRetry={() => routeConversationId && navigate(`/app/chat/${routeConversationId}`)} /> : messages.length ? <div className="message-list" aria-live="polite">{messages.map((item) => <article key={item.id} className={`chat-message chat-message-${item.role.toLowerCase()} ${item.isStreaming ? 'is-streaming' : ''}`}><span className="message-avatar">{item.role === 'USER' ? <UserRound size={15} /> : <Bot size={15} />}</span><div className="message-content"><span className="message-role">{item.role === 'USER' ? 'You' : 'Enterprise AI'}</span>{item.role === 'ASSISTANT' ? <div className="message-markdown"><ReactMarkdown components={{ a: ({ children, href }) => <a href={href} target="_blank" rel="noreferrer">{children}</a> }}>{item.content}</ReactMarkdown>{item.isStreaming && <span className="streaming-caret" aria-label="Generating" />}</div> : <p>{item.content}</p>}</div></article>)}<div ref={messagesEndRef} /></div> : <EmptyState icon={<Sparkles size={23} />} title="How can Enterprise AI help?" description="Ask a question, analyze a document, or delegate a task." />}
      </div>
      <div className="composer-wrap">
        {notice && <p className="chat-error" role="alert">{notice}</p>}
        <div className="chat-composer"><IconButton label="Attach a file" disabled><Paperclip size={19} /></IconButton><TextArea value={message} onChange={(event) => { setMessage(event.target.value); setNotice('') }} onKeyDown={onKeyDown} placeholder="Message Enterprise AI…" aria-label="Message Enterprise AI" rows={1} disabled={isStreaming} /><Button onClick={() => void sendMessage()} disabled={!message.trim() || isStreaming} aria-label="Send message"><Send size={16} /></Button></div>
        {isStreaming ? <button className="chat-stop" onClick={stopGeneration}><CircleStop size={14} />Stop generation</button> : <p>AI responses may contain mistakes. Verify important information.</p>}
      </div>
    </section>
  </div>
}

function MessageIcon() { return <span className="history-message-icon"><Bot size={18} /></span> }
