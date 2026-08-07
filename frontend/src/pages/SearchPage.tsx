import { useState, useEffect } from 'react'
import { Bot, FileText, MessageSquare, Search as SearchIcon, ArrowRight, LoaderCircle, Clock } from 'lucide-react'
import { EmptyState } from '../components/ui/Ui'
import { searchApi, type GlobalSearchResponse } from '../lib/api'
import { useNavigate } from 'react-router-dom'
import { motion } from 'framer-motion'

export function SearchPage() {
  const [query, setQuery] = useState('')
  const [filter, setFilter] = useState('All')
  const [results, setResults] = useState<GlobalSearchResponse | null>(null)
  const [isLoading, setIsLoading] = useState(false)
  const [selectedIndex, setSelectedIndex] = useState(0)
  const navigate = useNavigate()

  const [recentSearches, setRecentSearches] = useState<string[]>(JSON.parse(localStorage.getItem('recentSearches') || '[]'))
  
  // Flatten results for keyboard navigation
  const flatResults = Object.values(results?.resultsByCategory || {}).flat()

  useEffect(() => {
    const timer = setTimeout(() => {
      if (query.length > 1) {
        executeSearch()
      } else {
        setResults(null)
      }
      setSelectedIndex(0)
    }, 300)
    return () => clearTimeout(timer)
  }, [query, filter])

  function handleKeyDown(e: React.KeyboardEvent<HTMLInputElement>) {
    if (!flatResults.length) return
    if (e.key === 'ArrowDown') {
      e.preventDefault()
      setSelectedIndex((prev) => (prev + 1) % flatResults.length)
    } else if (e.key === 'ArrowUp') {
      e.preventDefault()
      setSelectedIndex((prev) => (prev - 1 + flatResults.length) % flatResults.length)
    } else if (e.key === 'Enter') {
      e.preventDefault()
      const item = flatResults[selectedIndex]
      if (item) navigate(item.url)
    }
  }

  async function executeSearch() {
    setIsLoading(true)
    try {
      const res = await searchApi.global(query, filter)
      setResults(res.data)
      if (!recentSearches.includes(query)) {
        const updated = [query, ...recentSearches].slice(0, 5)
        setRecentSearches(updated)
        localStorage.setItem('recentSearches', JSON.stringify(updated))
      }
    } catch (e) {
      console.error(e)
    } finally {
      setIsLoading(false)
    }
  }

  return (
    <motion.div initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.3 }} className="search-page">
      <div className="search-hero">
        <p className="section-kicker">Workspace discovery</p>
        <h2>Find what matters</h2>
        <label className="global-search">
          <SearchIcon size={21} />
          <input 
            placeholder="Search chats, documents and workspace knowledge…" 
            aria-label="Search workspace" 
            autoFocus 
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            onKeyDown={handleKeyDown}
          />
          {isLoading && <LoaderCircle size={18} className="spin search-spinner"/>}
        </label>
        <div className="search-filters">
          <button className={filter === 'All' ? 'active' : ''} onClick={() => setFilter('All')}>All</button>
          <button className={filter === 'Chats' ? 'active' : ''} onClick={() => setFilter('Chats')}><MessageSquare size={14} />Chats</button>
          <button className={filter === 'Documents' ? 'active' : ''} onClick={() => setFilter('Documents')}><FileText size={14} />Documents</button>
          <button className={filter === 'Agents' ? 'active' : ''} onClick={() => setFilter('Agents')}><Bot size={14} />Agents</button>
        </div>
      </div>

      <div className="search-results-area custom-scrollbar">
        {query.length <= 1 && (
          recentSearches.length > 0 ? (
            <div className="search-recent-section">
              <h3 className="search-recent-title"><Clock size={14} /> Recent Searches</h3>
              <div className="search-recent-chips">
                {recentSearches.map((s, i) => (
                  <motion.button initial={{ opacity: 0, scale: 0.9 }} animate={{ opacity: 1, scale: 1 }} transition={{ delay: i * 0.05 }} key={s} onClick={() => setQuery(s)} className="search-recent-chip">{s}</motion.button>
                ))}
              </div>
            </div>
          ) : (
            <EmptyState icon={<SearchIcon size={24} />} title="Search your workspace" description="Search results will appear here when you start typing." />
          )
        )}
        {query.length > 1 && !isLoading && results && results.totalResults === 0 && (
          <EmptyState icon={<SearchIcon size={24} />} title="No results found" description={`We couldn't find anything matching '${query}'.`} />
        )}
        {results && results.totalResults > 0 && (
          <div className="search-results-container">
            {Object.entries(results.resultsByCategory).map(([category, items]) => (
              items.length > 0 && (
                <div key={category}>
                  <h3 className="search-category-title">{category}</h3>
                  <div className="search-category-list">
                    {items.map((item, idx) => {
                      const globalIdx = flatResults.indexOf(item)
                      return (
                        <motion.div initial={{ opacity: 0, y: 5 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: idx * 0.03 }} key={idx} onClick={() => navigate(item.url)} className={`search-result-item ${selectedIndex === globalIdx ? 'selected' : ''}`} onMouseEnter={() => setSelectedIndex(globalIdx)}>
                          {item.type === 'chat' && <MessageSquare size={20} className="search-result-icon" />}
                          {item.type === 'document' && <FileText size={20} className="search-result-icon" />}
                          {item.type === 'document_content' && <FileText size={20} className="search-result-icon-accent" />}
                          {item.type === 'agent' && <Bot size={20} className="search-result-icon" />}
                          <div className="search-result-body">
                            <h4 className="search-result-title">
                              {highlightText(item.title, query)}
                            </h4>
                            {item.description && <p className="search-result-desc">
                              {highlightText(item.description, query)}
                            </p>}
                            <div className="search-result-meta">
                              <span>{item.matchReason}</span>
                              {item.timestamp && <span>&middot;</span>}
                              {item.timestamp && <span>{new Date(item.timestamp).toLocaleDateString()}</span>}
                            </div>
                          </div>
                          <ArrowRight size={16} className="search-result-arrow" />
                        </motion.div>
                      )
                    })}
                  </div>
                </div>
              )
            ))}
          </div>
        )}
      </div>
    </motion.div>
  )
}

function highlightText(text: string, query: string) {
  if (!query) return text
  const parts = text.split(new RegExp(`(${query})`, 'gi'))
  return (
    <>
      {parts.map((part, i) => 
        part.toLowerCase() === query.toLowerCase() ? <mark key={i} className="search-highlight">{part}</mark> : part
      )}
    </>
  )
}
