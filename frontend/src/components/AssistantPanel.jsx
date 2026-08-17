import { useEffect, useRef, useState } from 'react'
import { sendAssistantMessage } from '../api/assistantApi.js'

const SUGGESTIONS = [
  { icon: '✈️', text: 'Find me flights from Delhi to Mumbai tomorrow' },
  { icon: '🔁', text: 'Show me a one-way flight from Delhi to Bangalore' },
  { icon: '💬', text: 'What can you help me with?' },
]

export default function AssistantPanel({ open, onClose, onAction }) {
  const [messages, setMessages] = useState([])
  const [input, setInput] = useState('')
  const [isSending, setIsSending] = useState(false)
  const listRef = useRef(null)

  useEffect(() => {
    if (listRef.current) {
      listRef.current.scrollTop = listRef.current.scrollHeight
    }
  }, [messages, isSending])

  useEffect(() => {
    function handleEscape(event) {
      if (event.key === 'Escape') onClose()
    }
    if (open) document.addEventListener('keydown', handleEscape)
    return () => document.removeEventListener('keydown', handleEscape)
  }, [open, onClose])

  const send = async (text) => {
    const trimmed = text.trim()
    if (!trimmed || isSending) return

    setMessages((prev) => [...prev, { role: 'user', text: trimmed }])
    setInput('')
    setIsSending(true)
    try {
      const response = await sendAssistantMessage(trimmed)
      setMessages((prev) => [...prev, { role: 'assistant', text: response.reply }])
      if (response.action?.type === 'SEARCH_FLIGHTS') {
        onAction(response.action)
      }
    } catch {
      setMessages((prev) => [...prev, { role: 'assistant', text: "Sorry, I couldn't process that. Please try again." }])
    } finally {
      setIsSending(false)
    }
  }

  if (!open) return null

  return (
    <aside className="sticky top-0 flex h-screen w-[380px] shrink-0 flex-col border-r border-slate-200 bg-white shadow-lg">
      <div className="flex items-center justify-between border-b border-slate-100 px-5 py-4">
        <span className="flex items-center gap-2 text-lg font-bold text-brand-900">
          <span>✨</span> Ask AI
        </span>
        <button
          type="button"
          onClick={onClose}
          aria-label="Close assistant"
          className="flex h-8 w-8 items-center justify-center rounded-full text-slate-400 transition-colors hover:bg-slate-100 hover:text-slate-600"
        >
          ✕
        </button>
      </div>

      <div ref={listRef} className="flex-1 overflow-y-auto px-5 py-4">
        {messages.length === 0 ? (
          <div>
            <div className="mb-6 flex items-center gap-2 text-2xl font-extrabold text-brand-900">
              <span>✨</span> Ask away
            </div>
            <div className="mb-2 text-xs font-semibold uppercase tracking-wide text-slate-400">Getting started</div>
            <div className="flex flex-col gap-2">
              {SUGGESTIONS.map((suggestion) => (
                <button
                  key={suggestion.text}
                  type="button"
                  onClick={() => send(suggestion.text)}
                  className="flex items-center gap-3 rounded-xl border border-slate-100 bg-brand-50/40 px-4 py-3 text-left text-sm font-medium text-brand-900 transition-colors hover:border-accent-500 hover:bg-accent-50"
                >
                  <span>{suggestion.icon}</span>
                  {suggestion.text}
                </button>
              ))}
            </div>
          </div>
        ) : (
          <div className="flex flex-col gap-3">
            {messages.map((message, index) => (
              <div
                key={index}
                className={`max-w-[85%] rounded-2xl px-4 py-2.5 text-sm ${
                  message.role === 'user' ? 'ml-auto bg-brand-900 text-white' : 'bg-slate-100 text-brand-900'
                }`}
              >
                {message.text}
              </div>
            ))}
            {isSending && (
              <div className="max-w-[85%] rounded-2xl bg-slate-100 px-4 py-2.5 text-sm text-slate-400">Thinking…</div>
            )}
          </div>
        )}
      </div>

      <div className="border-t border-slate-100 p-4">
        <form
          onSubmit={(event) => {
            event.preventDefault()
            send(input)
          }}
          className="flex items-center gap-2 rounded-full border border-slate-200 bg-slate-50 px-4 py-2.5"
        >
          <input
            type="text"
            value={input}
            onChange={(event) => setInput(event.target.value)}
            placeholder="Where to next?"
            autoComplete="off"
            className="flex-1 border-0 bg-transparent text-sm text-brand-900 placeholder:text-slate-400 focus:outline-none focus:ring-0"
          />
          <button
            type="submit"
            disabled={!input.trim() || isSending}
            aria-label="Send"
            className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-accent-500 text-white transition-colors disabled:bg-slate-200 disabled:text-slate-400"
          >
            →
          </button>
        </form>
        <p className="mt-2 text-center text-[11px] text-slate-400">AI-powered; it can make mistakes.</p>
      </div>
    </aside>
  )
}
