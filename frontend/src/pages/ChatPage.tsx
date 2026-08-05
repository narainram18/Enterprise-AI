import { Children, isValidElement, useEffect, useRef, useState, type KeyboardEvent, type ReactElement, type ReactNode } from 'react'
import { Bot, Check, CircleStop, Copy, FileText, LoaderCircle, MoreHorizontal, Paperclip, Pencil, Plus, RefreshCw, Save, Search, Send, Sparkles, Trash2, UserRound, X } from 'lucide-react'
import ReactMarkdown from 'react-markdown'
import { useNavigate, useParams } from 'react-router-dom'
import { conversationsApi, regenerateConversationMessage, streamConversationMessage, type ChatMessageResponse, type ConversationSummary, type StreamCallbacks } from '../lib/api'
import { Button, EmptyState, ErrorState, IconButton, LoadingState, TextArea } from '../components/ui/Ui'
import { AgentSelector } from '../components/AgentSelector'

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
  const [copiedMessageId, setCopiedMessageId] = useState<number | string | null>(null)
  const [selectedAgentId, setSelectedAgentId] = useState('general-assistant')
  const abortControllerRef = useRef<AbortController | null>(null)
  const copyTimeoutRef = useRef<number | null>(null)
  const requestIdRef = useRef(0)
  const currentConversationRef = useRef<number | null>(routeConversationId)
  const skipRouteLoadRef = useRef<number | null>(null)
  const messageListRef = useRef<HTMLDivElement | null>(null)
  const messagesEndRef = useRef<HTMLDivElement | null>(null)
  const shouldAutoScrollRef = useRef(true)

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
      setNotice('Conversation title cannot be empty.')
      return
    }
    if (!activeConversation) return

    try {
      const { data } = await conversationsApi.rename(activeConversation.id, title)
      setActiveConversation(data.data)
      setRenameTitle(data.data.title)
      setConversations((current) => current.map((item) => item.id === data.data.id ? data.data : item))
      setIsRenaming(false)
      setNotice('')
    } catch {
      setNotice('We couldn’t rename this conversation. Please try again.')
    }
  }

  async function deleteActiveConversation() {
    if (!activeConversation || isDeleting) return
    if (!window.confirm(`Delete “${activeConversation.title}”? This cannot be undone.`)) return

    const deletedId = activeConversation.id
    setIsDeleting(true)
    try {
      await conversationsApi.delete(deletedId)
      const remaining = conversations.filter((item) => item.id !== deletedId)
      setConversations(remaining)
      setMenuOpen(false)
      setIsRenaming(false)
      stopGeneration()

      if (routeConversationId === deletedId) {
        setActiveConversation(null)
        setMessages([])
        if (remaining[0]) navigate(`/app/chat/${remaining[0].id}`)
        else {
          currentConversationRef.current = null
          navigate('/app/chat')
        }
      }
    } catch {
      setNotice('We couldn’t delete this conversation. Please try again.')
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
      setNotice('Couldn’t copy the assistant response.')
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

  async function sendMessage() {
    const content = message.trim()
    if (!content || isStreaming) return

    const requestId = requestIdRef.current + 1
    requestIdRef.current = requestId
    const controller = new AbortController()
    abortControllerRef.current = controller
    let conversationId = currentConversationRef.current
    setMessage('')
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
      setMessage(content)
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

  function onKeyDown(event: KeyboardEvent<HTMLTextAreaElement>) {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault()
      void sendMessage()
    }
  }

  const visibleConversations = conversations.filter((conversation) => conversation.title.toLowerCase().includes(search.toLowerCase()))
  const groupedConversations = groupConversationsByTime(visibleConversations)
  const title = activeConversation?.title ?? 'New conversation'

  return <div className="chat-page">
    <aside className="chat-history">
      <Button className="chat-new" onClick={startNewConversation}><Plus size={16} />New chat</Button>
      <label className="history-search"><Search size={16} /><input value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Search conversations" aria-label="Search conversations" /></label>
      <div className="conversation-list">
        {visibleConversations.length ? groupedConversations.map((group) => <div key={group.label}>
          <div className="conversation-group-label">{group.label}</div>
          {group.conversations.map((conversation) => <button key={conversation.id} className={`conversation-list-item ${conversation.id === routeConversationId ? 'active' : ''}`} onClick={() => selectConversation(conversation.id)}>
            <MessageIcon /><span>{conversation.title}</span>
          </button>)}
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
          {menuOpen && activeConversation && <div className="conversation-menu" role="menu"><button role="menuitem" onClick={() => { setRenameTitle(activeConversation.title); setIsRenaming(true); setMenuOpen(false) }}><Pencil size={14} />Rename</button><button role="menuitem" className="is-danger" onClick={() => void deleteActiveConversation()}><Trash2 size={14} />Delete</button></div>}
        </div>
      </header>
      <div className="conversation-body">
        {isLoadingConversation ? <LoadingState label="Loading conversation…" /> : loadError ? <ErrorState message={loadError} onRetry={() => routeConversationId && navigate(`/app/chat/${routeConversationId}`)} /> : messages.length ? <div ref={messageListRef} className="message-list" onScroll={onMessageListScroll} aria-live="polite">{messages.map((item) => (
          <article key={item.id} className={`chat-message chat-message-${item.role.toLowerCase()} ${item.isStreaming || item.isThinking ? 'is-streaming' : ''}`}>
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
              ) : (
                <>
                  <p>{item.content}</p>
                  {item.streamError && <div className="message-error"><span>{item.streamError}</span><button onClick={() => void retryGeneration(item)} disabled={isStreaming}><RefreshCw size={13} />Retry</button></div>}
                </>
              )}
            </div>
          </article>
        ))}<div ref={messagesEndRef} /></div> : <EmptyState icon={<Sparkles size={23} />} title="How can Enterprise AI help?" description="Ask a question, analyze a document, or delegate a task." />}
      </div>
      <div className="composer-wrap">
        {notice && <p className="chat-error" role="alert">{notice}</p>}
        <div className="chat-composer"><IconButton label="Attach a file" disabled><Paperclip size={19} /></IconButton><TextArea value={message} onChange={(event) => { setMessage(event.target.value); setNotice('') }} onKeyDown={onKeyDown} placeholder="Message Enterprise AI…" aria-label="Message Enterprise AI" rows={1} disabled={isStreaming} /><Button onClick={() => void sendMessage()} disabled={!message.trim() || isStreaming} aria-label="Send message"><Send size={16} /></Button></div>
        {isStreaming ? <button className="chat-stop" onClick={stopGeneration}><CircleStop size={14} />Stop generation</button> : <p>AI responses may contain mistakes. Verify important information.</p>}
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

  return <div className="markdown-code-block"><div className="markdown-code-toolbar"><span>{language ?? 'Code'}</span><button onClick={() => void copyCode()}><Copy size={12} />{copied ? 'Copied' : 'Copy'}</button></div><pre><code className={className}>{code}</code></pre></div>
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
