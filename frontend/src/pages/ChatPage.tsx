import { useState } from 'react'
import { Bot, MoreHorizontal, Paperclip, Plus, Search, Send, Sparkles } from 'lucide-react'
import { Button, EmptyState, IconButton, TextArea } from '../components/ui/Ui'

export function ChatPage() {
  const [message, setMessage] = useState('')
  const [notice, setNotice] = useState('')
  function sendMessage() { if (!message.trim()) return; setNotice('Chat execution is awaiting a backend conversation API. Your message was not sent.'); }
  function onKeyDown(event: React.KeyboardEvent<HTMLTextAreaElement>) { if (event.key === 'Enter' && !event.shiftKey) { event.preventDefault(); sendMessage() } }
  return <div className="chat-page"><aside className="chat-history"><Button className="chat-new" onClick={() => { setMessage(''); setNotice('') }}><Plus size={16} />New chat</Button><label className="history-search"><Search size={16} /><input placeholder="Search conversations" aria-label="Search conversations" /></label><div className="history-empty"><MessageIcon /><p>No chat history</p><span>Your conversations will appear here.</span></div></aside><section className="conversation"><header className="conversation-header"><div><h2>New conversation</h2><span><Bot size={14} />Enterprise AI</span></div><IconButton label="Conversation options"><MoreHorizontal size={19} /></IconButton></header><div className="conversation-body"><EmptyState icon={<Sparkles size={23} />} title="How can Enterprise AI help?" description="Ask a question, analyze a document, or delegate a task. Chat delivery will activate when the conversation API is connected." /></div><div className="composer-wrap">{notice && <p className="integration-notice">{notice}</p>}<div className="chat-composer"><IconButton label="Attach a file" disabled><Paperclip size={19} /></IconButton><TextArea value={message} onChange={(event) => { setMessage(event.target.value); setNotice('') }} onKeyDown={onKeyDown} placeholder="Message Enterprise AI…" aria-label="Message Enterprise AI" rows={1} /><Button onClick={sendMessage} disabled={!message.trim()} aria-label="Send message"><Send size={16} /></Button></div><p>AI responses may contain mistakes. Verify important information.</p></div></section></div>
}

function MessageIcon() { return <span className="history-message-icon"><Bot size={18} /></span> }
