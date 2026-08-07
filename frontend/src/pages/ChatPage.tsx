import { Children, isValidElement, useEffect, useRef, useState, type KeyboardEvent, type ReactElement, type ReactNode } from 'react'
import { Bot, Check, CircleStop, Copy, Download, FileText, LoaderCircle, MoreHorizontal, Paperclip, Pencil, Pin, Plus, RefreshCw, Save, Search, Send, Star, Trash2, UserRound, X, FileUp, Sparkles as SparklesIcon, MessageSquare } from 'lucide-react'
import ReactMarkdown from 'react-markdown'
import { Prism as SyntaxHighlighter } from 'react-syntax-highlighter'
import { vscDarkPlus } from 'react-syntax-highlighter/dist/esm/styles/prism'
import { useNavigate, useParams } from 'react-router-dom'
import { agentsApi, type Agent, conversationsApi, regenerateConversationMessage, streamConversationMessage, type ChatMessageResponse, type ConversationSummary, type StreamCallbacks } from '../lib/api'
import { Button, ErrorState, IconButton, LoadingState, TextArea } from '../components/ui/Ui'
import { AgentSelector } from '../components/AgentSelector'
import { motion, AnimatePresence } from 'framer-motion'
import { useToast } from '../context/ToastContext'
import { useConfirm } from '../context/ConfirmationContext'
import { eventBus } from '../lib/events'

type ChatMessage = Omit<ChatMessageResponse, 'id'> & {
  id: number | string
  isStreaming?: boolean
  isThinking?: boolean
  toolStatus?: string
  streamError?: string
}

const STREAM_ERROR_MESSAGE = 'The assistant could not finish this response.'

export function ChatPage() {
  const { chatId } = useParams()
  const navigate = useNavigate()
  const { toast } = useToast()
  const { confirm } = useConfirm()
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
  const [menuOpen, setMenuOpen] = useState(false)
  const [isRenaming, setIsRenaming] = useState(false)
  const [renameTitle, setRenameTitle] = useState('')
  const [isDeleting, setIsDeleting] = useState(false)
  const [editingMessageId, setEditingMessageId] = useState<number | string | null>(null)
  const [editingContent, setEditingContent] = useState('')
  const [copiedMessageId, setCopiedMessageId] = useState<number | string | null>(null)
  const [agents, setAgents] = useState<Agent[]>([])
  const [selectedAgentId, setSelectedAgentId] = useState('general-assistant')
  const abortControllerRef = useRef<AbortController | null>(null)
  const copyTimeoutRef = useRef<number | null>(null)
  const requestIdRef = useRef(0)
  const currentConversationRef = useRef<number | null>(routeConversationId)
  const skipRouteLoadRef = useRef<number | null>(null)
  const messageListRef = useRef<HTMLDivElement | null>(null)
  const messagesEndRef = useRef<HTMLDivElement | null>(null)
  const shouldAutoScrollRef = useRef(true)
  const [hoveredConvId, setHoveredConvId] = useState<number | null>(null)

  useEffect(() => {
    agentsApi.list().then(({ data }) => setAgents(data)).catch(() => setAgents([]))
  }, [])

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
    setMenuOpen(false)
    setIsRenaming(false)
    setNotice('')
    setLoadError('')
    shouldAutoScrollRef.current = true

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
        setRenameTitle(data.data.title)
        setMessages(data.data.messages)
        if (data.data.agentId) setSelectedAgentId(data.data.agentId)
      })
      .catch(() => {
        if (currentConversationRef.current === loadId) setLoadError('We couldn’t load this conversation. Please try again.')
      })
      .finally(() => {
        if (currentConversationRef.current === loadId) setIsLoadingConversation(false)
      })
  }, [routeConversationId])

  useEffect(() => () => {
    stopGeneration()
    if (copyTimeoutRef.current !== null) window.clearTimeout(copyTimeoutRef.current)
  }, [])

  useEffect(() => {
    if (!shouldAutoScrollRef.current) return
    messagesEndRef.current?.scrollIntoView({ behavior: isStreaming ? 'auto' : 'smooth', block: 'end' })
  }, [messages, isStreaming])

  useEffect(() => {
    function handleGlobalKeyDown(e: globalThis.KeyboardEvent) {
      if (e.key === 'Escape') {
        setMenuOpen(false)
        setIsRenaming(false)
        setEditingMessageId(null)
      }
      if (e.key === '/' && (e.ctrlKey || e.metaKey)) {
        e.preventDefault()
        document.querySelector<HTMLInputElement>('.history-search input')?.focus()
      }
    }
    window.addEventListener('keydown', handleGlobalKeyDown)
    return () => window.removeEventListener('keydown', handleGlobalKeyDown)
  }, [])

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
    setMessages((current) => current.filter((item) => !item.isStreaming && !item.isThinking))
  }

  function onMessageListScroll() {
    const list = messageListRef.current
    if (!list) return
    shouldAutoScrollRef.current = list.scrollHeight - list.scrollTop - list.clientHeight < 96
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
    setMenuOpen(false)
    setIsRenaming(false)
    setSelectedAgentId('general-assistant')
    currentConversationRef.current = null
    navigate('/app/chat')
  }

  async function saveConversationRename() {
    const title = renameTitle.trim()
    if (!title) {
      toast('Conversation title cannot be empty.', 'warning')
      return
    }
    if (!activeConversation) return

    try {
      const { data } = await conversationsApi.update(activeConversation.id, { title })
      setActiveConversation(data.data)
      setRenameTitle(data.data.title)
      setConversations((current) => current.map((item) => item.id === data.data.id ? data.data : item))
      setIsRenaming(false)
      toast('Conversation renamed', 'success')
    } catch {
      toast('We couldn’t rename this conversation. Please try again.', 'error')
    }
  }

  async function toggleFlag(flag: 'isPinned' | 'isFavorite' | 'isArchived', conv: ConversationSummary) {
    const currentValue = conv[flag.replace('is', '').toLowerCase() as keyof ConversationSummary] as boolean
    const data = { [flag]: !currentValue }
    try {
      const res = await conversationsApi.update(conv.id, data)
      if (activeConversation?.id === conv.id) {
        setActiveConversation(res.data.data)
      }
      setConversations((current) => current.map((item) => item.id === res.data.data.id ? res.data.data : item))
      setMenuOpen(false)
      toast(`Conversation ${currentValue ? 'removed from' : 'added to'} ${flag.replace('is', '').toLowerCase()}`, 'success')
    } catch {
      toast(`We couldn’t update the conversation. Please try again.`, 'error')
    }
  }

  function exportConversation() {
    if (!activeConversation || !messages.length) return
    let md = `# ${activeConversation.title}\n\n`
    for (const msg of messages) {
      md += `**${msg.role === 'USER' ? 'You' : 'Enterprise AI'}**\n\n${msg.content}\n\n---\n\n`
    }
    const blob = new Blob([md], { type: 'text/markdown' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `${activeConversation.title.replace(/\s+/g, '_')}.md`
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)
    setMenuOpen(false)
  }

  async function deleteConversation(conv: ConversationSummary) {
    if (isDeleting) return
    const isConfirmed = await confirm({
      title: 'Delete Conversation',
      description: `Delete “${conv.title}”? This cannot be undone.`,
      confirmText: 'Delete',
      variant: 'danger'
    })
    if (!isConfirmed) return

    setIsDeleting(true)
    try {
      await conversationsApi.delete(conv.id)
      setConversations((prev) => prev.filter((c) => c.id !== conv.id))
      eventBus.emit('CONVERSATION_DELETED')
      toast('Conversation deleted', 'success')
      
      if (currentConversationRef.current === conv.id) {
        stopGeneration()
        setActiveConversation(null)
        setMessages([])
        currentConversationRef.current = null
        navigate('/app/chat', { replace: true })
      }
    } catch {
      toast('We couldn’t delete this conversation. Please try again.', 'error')
    } finally {
      setIsDeleting(false)
    }
  }

  function clearStreamPlaceholder() {
    setMessages((current) => current.filter((item) => !item.isStreaming && !item.isThinking))
  }

  async function copyText(text: string) {
    if (navigator.clipboard?.writeText) {
      await navigator.clipboard.writeText(text)
      return
    }
    const textarea = document.createElement('textarea')
    textarea.value = text
    textarea.style.position = 'fixed'
    textarea.style.opacity = '0'
    document.body.appendChild(textarea)
    textarea.select()
    document.execCommand('copy')
    textarea.remove()
  }

  async function copyAssistantResponse(item: ChatMessage) {
    try {
      await copyText(item.content)
      setCopiedMessageId(item.id)
      if (copyTimeoutRef.current !== null) window.clearTimeout(copyTimeoutRef.current)
      copyTimeoutRef.current = window.setTimeout(() => setCopiedMessageId(null), 1600)
    } catch {
      toast('Couldn’t copy the assistant response.', 'error')
    }
  }

  async function runAssistantStream(
    conversationId: number,
    content: string,
    requestId: number,
    controller: AbortController,
    existingUserMessageId: number | null,
  ) {
    const streamingAssistantId = `streaming-assistant-${requestId}`
    let persistedUserMessageId = existingUserMessageId

    const callbacks: StreamCallbacks = {
      onUserMessage: (userMessage) => {
        if (requestIdRef.current !== requestId) return
        persistedUserMessageId = userMessage.id
        setMessages((current) => {
          const withoutPlaceholder = current.filter((item) => item.id !== streamingAssistantId)
          const withUser = withoutPlaceholder.some((item) => item.id === userMessage.id)
            ? withoutPlaceholder.map((item) => item.id === userMessage.id ? { ...item, streamError: undefined } : item)
            : [...withoutPlaceholder, userMessage]
          return [...withUser, {
            id: streamingAssistantId,
            role: 'ASSISTANT',
            content: '',
            createdAt: new Date().toISOString(),
            isStreaming: true,
            isThinking: true,
          }]
        })
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
              isThinking: false,
            }]
          }
          return current.map((item, index) => index === existingIndex
            ? { ...item, content: item.content + token, isThinking: false, isStreaming: true }
            : item)
        })
      },
      onToolProgress: (toolName) => {
        if (requestIdRef.current !== requestId) return
        let status = 'Using tool...'
        if (toolName === 'search_documents' || toolName === 'list_documents' || toolName === 'retrieve_document') status = 'Searching documents...'
        else if (toolName === 'workspace_info') status = 'Fetching workspace info...'
        else if (toolName === 'conversation_summary') status = 'Summarizing conversation...'
        else status = `Running ${toolName}...`

        setMessages((current) => {
          const existingIndex = current.findIndex((item) => item.id === streamingAssistantId)
          if (existingIndex === -1) {
            return [...current, {
              id: streamingAssistantId,
              role: 'ASSISTANT',
              content: '',
              createdAt: new Date().toISOString(),
              isStreaming: true,
              isThinking: true,
              toolStatus: status,
            }]
          }
          return current.map((item, index) => index === existingIndex
            ? { ...item, toolStatus: status, isThinking: true }
            : item)
        })
      },
      onComplete: (assistantMessage) => {
        if (requestIdRef.current !== requestId) return
        setMessages((current) => {
          const cleaned = current
            .filter((item) => item.id !== streamingAssistantId && !item.isThinking)
            .map((item) => item.id === persistedUserMessageId ? { ...item, streamError: undefined } : item)
          return [...cleaned, assistantMessage]
        })
        void loadConversations()
        eventBus.emit('AI_RESPONDED')
      },
      onError: (error) => {
        if (requestIdRef.current !== requestId) return
        clearStreamPlaceholder()
        if (persistedUserMessageId !== null) {
          setMessages((current) => current.map((item) => item.id === persistedUserMessageId ? { ...item, streamError: error || STREAM_ERROR_MESSAGE } : item))
        } else {
          setMessage(content)
          setNotice(error || STREAM_ERROR_MESSAGE)
        }
      },
    }

    try {
      if (existingUserMessageId === null) {
        await streamConversationMessage(conversationId, content, callbacks, controller.signal)
      } else {
        await regenerateConversationMessage(conversationId, existingUserMessageId, callbacks, controller.signal)
      }
    } catch (error) {
      if (controller.signal.aborted || requestIdRef.current !== requestId) return
      clearStreamPlaceholder()
      const errorMessage = error instanceof Error ? error.message : STREAM_ERROR_MESSAGE
      if (persistedUserMessageId !== null) {
        setMessages((current) => current.map((item) => item.id === persistedUserMessageId ? { ...item, streamError: errorMessage } : item))
      } else {
        setMessage(content)
        setNotice(errorMessage)
      }
    }
  }

  async function sendMessage(overrideMsg?: string) {
    const content = (overrideMsg ?? message).trim()
    if (!content || isStreaming) return

    const requestId = requestIdRef.current + 1
    requestIdRef.current = requestId
    const controller = new AbortController()
    abortControllerRef.current = controller
    let conversationId = currentConversationRef.current
    if (!overrideMsg) setMessage('')
    setNotice('')
    setIsStreaming(true)
    shouldAutoScrollRef.current = true

    try {
      if (!conversationId) {
        const { data } = await conversationsApi.create(createConversationTitle(content), selectedAgentId)
        if (requestIdRef.current !== requestId) return
        conversationId = data.data.id
        currentConversationRef.current = conversationId
        setActiveConversation(data.data)
        setRenameTitle(data.data.title)
        setMessages([])
        skipRouteLoadRef.current = conversationId
        navigate(`/app/chat/${conversationId}`, { replace: true })
        void loadConversations()
      }

      if (!conversationId || requestIdRef.current !== requestId) return
      await runAssistantStream(conversationId, content, requestId, controller, null)
    } catch (error) {
      if (controller.signal.aborted || requestIdRef.current !== requestId) return
      clearStreamPlaceholder()
      if (!overrideMsg) setMessage(content)
      setNotice(error instanceof Error ? error.message : 'We couldn’t send your message. Please try again.')
    } finally {
      if (requestIdRef.current === requestId) {
        abortControllerRef.current = null
        setIsStreaming(false)
      }
    }
  }

  async function retryGeneration(userMessage: ChatMessage) {
    const conversationId = currentConversationRef.current
    if (isStreaming || !conversationId || typeof userMessage.id !== 'number') return

    const requestId = requestIdRef.current + 1
    requestIdRef.current = requestId
    const controller = new AbortController()
    abortControllerRef.current = controller
    setNotice('')
    setIsStreaming(true)
    shouldAutoScrollRef.current = true
    setMessages((current) => {
      const withoutPlaceholders = current.filter((item) => !item.isStreaming && !item.isThinking)
      const withUser = withoutPlaceholders.some((item) => item.id === userMessage.id)
        ? withoutPlaceholders.map((item) => item.id === userMessage.id ? { ...item, streamError: undefined } : item)
        : [...withoutPlaceholders, { ...userMessage, streamError: undefined }]
      return [...withUser, {
        id: `streaming-assistant-${requestId}`,
        role: 'ASSISTANT',
        content: '',
        createdAt: new Date().toISOString(),
        isStreaming: true,
        isThinking: true,
      }]
    })

    try {
      await runAssistantStream(conversationId, userMessage.content, requestId, controller, userMessage.id)
    } finally {
      if (requestIdRef.current === requestId) {
        abortControllerRef.current = null
        setIsStreaming(false)
      }
    }
  }

  async function saveEditedMessage(userMessage: ChatMessage) {
    const conversationId = currentConversationRef.current
    if (isStreaming || !conversationId || typeof userMessage.id !== 'number') return
    const content = editingContent.trim()
    if (!content) return

    setEditingMessageId(null)
    const requestId = requestIdRef.current + 1
    requestIdRef.current = requestId
    const controller = new AbortController()
    abortControllerRef.current = controller
    setNotice('')
    setIsStreaming(true)
    shouldAutoScrollRef.current = true

    try {
      await conversationsApi.editMessage(conversationId, userMessage.id, content)
      setMessages((current) => current.map((item) => item.id === userMessage.id ? { ...item, content, streamError: undefined } : item))
      await runAssistantStream(conversationId, content, requestId, controller, userMessage.id)
    } catch {
      if (controller.signal.aborted || requestIdRef.current !== requestId) return
      setIsStreaming(false)
      setNotice('Couldn’t save and resend the message.')
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

  function formatConvTime(dateString: string) {
    const date = new Date(dateString)
    const now = new Date()
    const isToday = date.getDate() === now.getDate() && date.getMonth() === now.getMonth() && date.getFullYear() === now.getFullYear()
    if (isToday) return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
    return date.toLocaleDateString([], { month: 'short', day: 'numeric' })
  }

  const visibleConversations = conversations.filter((conversation) => conversation.title.toLowerCase().includes(search.toLowerCase()))
  const groupedConversations = groupConversationsByTime(visibleConversations)
  const title = activeConversation?.title ?? 'New conversation'
  const selectedAgent = agents.find(a => a.id === selectedAgentId) || agents[0]

  return <div className="chat-page">
    <aside className="chat-history">
      <Button className="chat-new" onClick={startNewConversation}><Plus size={16} />New chat</Button>
      <label className="history-search"><Search size={16} /><input value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Search chats" aria-label="Search conversations" /></label>
      <div className="conversation-list">
        {visibleConversations.length ? groupedConversations.map((group) => <div key={group.label}>
          <div className="conversation-group-label">{group.label}</div>
          {group.conversations.map((conversation) => (
            <motion.div 
              key={conversation.id}
              className={`conversation-list-item ${conversation.id === routeConversationId ? 'active' : ''}`}
              onClick={() => selectConversation(conversation.id)}
              onMouseEnter={() => setHoveredConvId(conversation.id)}
              onMouseLeave={() => setHoveredConvId(null)}
            >
              <div className="conv-item-content">
                <span className="conv-item-title">{conversation.title}</span>
                <span className="conv-item-time">{formatConvTime(conversation.updatedAt)}</span>
              </div>
              <AnimatePresence>
                {(hoveredConvId === conversation.id || conversation.id === routeConversationId) && (
                  <motion.div 
                    initial={{ opacity: 0, x: 5 }} animate={{ opacity: 1, x: 0 }} exit={{ opacity: 0, x: 5 }} 
                    className="conv-item-actions"
                    onClick={(e) => e.stopPropagation()}
                  >
                    <button onClick={() => void toggleFlag('isPinned', conversation)} aria-label="Pin" className={conversation.pinned ? 'active-flag' : ''}><Pin size={13} /></button>
                    <button onClick={() => void deleteConversation(conversation)} aria-label="Delete" className="danger-flag"><Trash2 size={13} /></button>
                  </motion.div>
                )}
              </AnimatePresence>
              {conversation.pinned && hoveredConvId !== conversation.id && conversation.id !== routeConversationId && <Pin size={12} className="conversation-flag pinned" />}
            </motion.div>
          ))}
        </div>) : <div className="history-empty"><MessageIcon /><p>{search ? 'No matches' : 'No chat history'}</p><span>{search ? 'Try a different search.' : 'Your conversations will appear here.'}</span></div>}
      </div>
    </aside>
    <section className="conversation">
      <header className="conversation-header">
        <div className="conversation-heading">
          {isRenaming ? <div className="conversation-rename"><input value={renameTitle} onChange={(event) => setRenameTitle(event.target.value)} onKeyDown={(event) => { if (event.key === 'Enter') void saveConversationRename(); if (event.key === 'Escape') setIsRenaming(false) }} aria-label="Conversation title" autoFocus /><IconButton label="Save conversation title" onClick={() => void saveConversationRename()}><Save size={16} /></IconButton><IconButton label="Cancel rename" onClick={() => setIsRenaming(false)}><X size={16} /></IconButton></div> : <>
            <h2>{title}</h2>
            <AgentSelector 
              selectedAgentId={activeConversation?.agentId || selectedAgentId} 
              onSelect={setSelectedAgentId} 
              disabled={!!activeConversation} 
            />
          </>}
        </div>
        <div className="conversation-menu-wrap">
          <IconButton label="Conversation options" onClick={() => setMenuOpen((open) => !open)} disabled={!activeConversation || isDeleting}><MoreHorizontal size={19} /></IconButton>
          <AnimatePresence>
            {menuOpen && activeConversation && (
              <motion.div 
                initial={{ opacity: 0, y: 5, scale: 0.95 }} animate={{ opacity: 1, y: 0, scale: 1 }} exit={{ opacity: 0, y: 5, scale: 0.95 }}
                className="conversation-menu" role="menu"
              >
                <button role="menuitem" onClick={() => { setRenameTitle(activeConversation.title); setIsRenaming(true); setMenuOpen(false) }}><Pencil size={14} />Rename</button>
                <button role="menuitem" onClick={() => void toggleFlag('isPinned', activeConversation)}><Pin size={14} />{activeConversation.pinned ? 'Unpin' : 'Pin'}</button>
                <button role="menuitem" onClick={() => void toggleFlag('isFavorite', activeConversation)}><Star size={14} />{activeConversation.favorite ? 'Unfavorite' : 'Favorite'}</button>
                <button role="menuitem" onClick={exportConversation}><Download size={14} />Export (.md)</button>
                <div className="menu-divider"></div>
                <button role="menuitem" className="is-danger" onClick={() => void deleteConversation(activeConversation)}><Trash2 size={14} />Delete</button>
              </motion.div>
            )}
          </AnimatePresence>
        </div>
      </header>
      <div className="conversation-body">
        {isLoadingConversation ? <LoadingState label="Loading conversation…" /> : loadError ? <ErrorState message={loadError} onRetry={() => routeConversationId && navigate(`/app/chat/${routeConversationId}`)} /> : messages.length ? <div ref={messageListRef} className="message-list" onScroll={onMessageListScroll} aria-live="polite">{messages.map((item) => (
          <motion.article initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} key={item.id} className={`chat-message chat-message-${item.role.toLowerCase()} ${item.isStreaming || item.isThinking ? 'is-streaming' : ''}`}>
            <span className="message-avatar">{item.role === 'USER' ? <UserRound size={15} /> : <Bot size={15} />}</span>
            <div className="message-content">
              <span className="message-role">{item.role === 'USER' ? 'You' : 'Enterprise AI'}</span>
              {item.role === 'ASSISTANT' ? item.isThinking ? <div className="thinking-state"><LoaderCircle className="spin" size={14} />{item.toolStatus || 'Thinking…'}</div> : (
                <>
                  <div className="message-markdown">
                    <ReactMarkdown components={{ a: MarkdownLink, pre: MarkdownCodeBlock }}>{item.content}</ReactMarkdown>
                    {item.isStreaming && <span className="streaming-caret" aria-label="Generating" />}
                  </div>
                  {item.citations && item.citations.length > 0 && (
                    <div className="message-citations">
                      <p className="citations-heading">Sources</p>
                      <ul className="citations-list">
                        {item.citations.map((cit, idx) => (
                          <li key={idx} className="citation-item">
                            <FileText size={12} /> {cit.fileName}
                            <span className="citation-meta">
                               (Score: {Math.round(cit.similarityScore * 100)}%
                               {cit.pageNumber != null ? `, Page ${cit.pageNumber}` : `, Chunk ${cit.chunkIndex}`})
                            </span>
                          </li>
                        ))}
                      </ul>
                    </div>
                  )}
                  {!item.isStreaming && <button className="message-copy" onClick={() => void copyAssistantResponse(item)}>{copiedMessageId === item.id ? <Check size={13} /> : <Copy size={13} />}{copiedMessageId === item.id ? 'Copied' : 'Copy'}</button>}
                </>
              ) : editingMessageId === item.id ? (
                <div className="message-edit-mode">
                  <TextArea 
                    value={editingContent} 
                    onChange={(e) => setEditingContent(e.target.value)} 
                    rows={1}
                    className="edit-message-input"
                  />
                  <div className="edit-message-actions">
                    <Button onClick={() => void saveEditedMessage(item)} disabled={!editingContent.trim() || isStreaming}>Save & Resend</Button>
                    <button className="cancel-btn" onClick={() => setEditingMessageId(null)}>Cancel</button>
                  </div>
                </div>
              ) : (
                <>
                  <div className="message-text">
                    <p>{item.content}</p>
                    {!item.isStreaming && <button className="message-edit-btn" onClick={() => { setEditingMessageId(item.id); setEditingContent(item.content) }} aria-label="Edit message"><Pencil size={12} /></button>}
                  </div>
                  {item.streamError && <div className="message-error"><span>{item.streamError}</span><button onClick={() => void retryGeneration(item)} disabled={isStreaming}><RefreshCw size={13} />Retry</button></div>}
                </>
              )}
            </div>
          </motion.article>
        ))}<div ref={messagesEndRef} /></div> : (
          <div className="chat-welcome-wrapper">
            <motion.div initial={{ opacity: 0, scale: 0.95 }} animate={{ opacity: 1, scale: 1 }} className="chat-welcome-panel">
              <div className="chat-welcome-icon"><SparklesIcon size={28} /></div>
              <h2>{selectedAgent?.welcomeMessage || "How can Enterprise AI help?"}</h2>
              <p>{selectedAgent?.description || "Ask a question, analyze a document, or delegate a task."}</p>
              
              {selectedAgent?.suggestedPrompts && selectedAgent.suggestedPrompts.length > 0 && (
                <div className="chat-suggested-prompts">
                  {selectedAgent.suggestedPrompts.slice(0, 4).map((prompt: string, idx: number) => (
                    <button key={idx} className="suggested-prompt-card" onClick={() => void sendMessage(prompt)}>
                      <MessageSquare size={16} />
                      <span>{prompt}</span>
                    </button>
                  ))}
                </div>
              )}
              
              <div className="chat-welcome-actions">
                <button onClick={() => navigate('/app/documents')}><FileUp size={16} /> Upload Document</button>
                <button onClick={() => navigate('/app/search')}><Search size={16} /> Search Workspace</button>
              </div>
            </motion.div>
          </div>
        )}
      </div>
      <div className="composer-wrap">
        {notice && <p className="chat-error" role="alert">{notice}</p>}
        <div className={`chat-composer ${isStreaming ? 'disabled' : ''}`}>
          <IconButton label="Attach a file" disabled className="composer-attach-btn"><Paperclip size={19} /></IconButton>
          <TextArea 
            value={message} 
            onChange={(event) => { setMessage(event.target.value); setNotice('') }} 
            onKeyDown={onKeyDown} 
            placeholder="Message Enterprise AI..." 
            aria-label="Message Enterprise AI" 
            rows={1} 
            disabled={isStreaming} 
            className="composer-textarea custom-scrollbar"
          />
          <div className="composer-actions">
            <Button onClick={() => void sendMessage()} disabled={!message.trim() || isStreaming} aria-label="Send message" className="composer-send-btn">
              {isStreaming ? <LoaderCircle size={16} className="spin" /> : <Send size={16} />}
            </Button>
          </div>
        </div>
        <div className="composer-footer">
          {isStreaming ? <button className="chat-stop" onClick={stopGeneration}><CircleStop size={14} /> Stop generation</button> : <p>AI responses may contain mistakes. Verify important information.</p>}
        </div>
      </div>
    </section>
  </div>
}

function createConversationTitle(content: string) {
  const firstSentence = content.replace(/\s+/g, ' ').trim().split(/[.!?](?:\s|$)/)[0] ?? ''
  const cleaned = firstSentence
    .replace(/^\s*(please\s+)?(explain|describe|tell me about|help me understand|write about|give me an overview of)\s+/i, '')
    .replace(/\s+(in detail|briefly|in two short sentences|in a few sentences)\s*$/i, '')
    .replace(/,\s*and\s+/gi, ' & ')
    .trim()
  const title = cleaned || content.replace(/\s+/g, ' ').trim()
  return title.length > 60 ? `${title.slice(0, 57).trimEnd()}…` : title
}

function MessageIcon() { return <span className="history-message-icon"><Bot size={18} /></span> }

function MarkdownLink({ children, href }: { children?: ReactNode; href?: string }) {
  return <a href={href} target="_blank" rel="noreferrer">{children}</a>
}

function textFromChildren(children: ReactNode) {
  return Children.toArray(children).map((child) => typeof child === 'string' || typeof child === 'number' ? String(child) : '').join('')
}

function MarkdownCodeBlock({ children }: { children?: ReactNode }) {
  const child = Children.toArray(children).find(isValidElement) as ReactElement<{ className?: string; children?: ReactNode }> | undefined
  const className = child?.props.className ?? ''
  const language = className.match(/language-([\w-]+)/)?.[1]
  const code = textFromChildren(child?.props.children ?? children).replace(/\n$/, '')
  const [copied, setCopied] = useState(false)

  async function copyCode() {
    try {
      if (navigator.clipboard?.writeText) await navigator.clipboard.writeText(code)
      else {
        const textarea = document.createElement('textarea')
        textarea.value = code
        textarea.style.position = 'fixed'
        textarea.style.opacity = '0'
        document.body.appendChild(textarea)
        textarea.select()
        document.execCommand('copy')
        textarea.remove()
      }
      setCopied(true)
      window.setTimeout(() => setCopied(false), 1600)
    } catch {
      setCopied(false)
    }
  }

  return (
    <div className="markdown-code-block">
      <div className="markdown-code-toolbar">
        <span>{language ?? 'Code'}</span>
        <button onClick={() => void copyCode()}>
          <Copy size={12} />
          {copied ? 'Copied' : 'Copy'}
        </button>
      </div>
      <SyntaxHighlighter
        language={language || 'text'}
        style={vscDarkPlus}
        customStyle={{ margin: 0, borderTopLeftRadius: 0, borderTopRightRadius: 0 }}
      >
        {code}
      </SyntaxHighlighter>
    </div>
  )
}

function groupConversationsByTime(conversations: ConversationSummary[]) {
  const now = new Date()
  const todayStart = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  const yesterdayStart = new Date(todayStart.getTime() - 86400000)
  const weekStart = new Date(todayStart.getTime() - 7 * 86400000)

  const groups: { label: string; conversations: ConversationSummary[] }[] = [
    { label: 'Today', conversations: [] },
    { label: 'Yesterday', conversations: [] },
    { label: 'This Week', conversations: [] },
    { label: 'Older', conversations: [] },
  ]

  for (const conversation of conversations) {
    const date = new Date(conversation.createdAt)
    if (date >= todayStart) groups[0].conversations.push(conversation)
    else if (date >= yesterdayStart) groups[1].conversations.push(conversation)
    else if (date >= weekStart) groups[2].conversations.push(conversation)
    else groups[3].conversations.push(conversation)
  }

  return groups.filter((group) => group.conversations.length > 0)
}
