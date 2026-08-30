import { forwardRef, useEffect, useRef, useState } from 'react'
import { sendAssistantMessage } from '../../api/assistantApi.js'

const SUGGESTIONS = [
  { icon: '✈️', text: 'Find cheapest flights to Goa', badge: 'bg-brand-50' },
]

// Same chat behavior as AssistantPanel (send/receive, SEARCH_FLIGHTS
// action), reimplemented here rather than shared -- AssistantPanel is a
// slide-in drawer with a close button; this is a permanently-visible
// sidebar, and forcing one component to be both isn't worth the branching.
const AIAssistantSidebar = forwardRef(function AIAssistantSidebar({ onAction }, ref) {
  const [messages, setMessages] = useState([])
  const [input, setInput] = useState('')
  const [isSending, setIsSending] = useState(false)
  const listRef = useRef(null)

  useEffect(() => {
    if (listRef.current) listRef.current.scrollTop = listRef.current.scrollHeight
  }, [messages, isSending])

  const send = async (text) => {
    const trimmed = text.trim()
    if (!trimmed || isSending) return

    setMessages((prev) => [...prev, { role: 'user', text: trimmed }])
    setInput('')
    setIsSending(true)
    try {
      const response = await sendAssistantMessage(trimmed)
      setMessages((prev) => [...prev, { role: 'assistant', text: response.reply }])
      if (response.action?.type === 'SEARCH_FLIGHTS') onAction(response.action)
    } catch {
      setMessages((prev) => [...prev, { role: 'assistant', text: "Sorry, I couldn't process that. Please try again." }])
    } finally {
      setIsSending(false)
    }
  }

  return (
    <aside
      ref={ref}
      className="flex h-[480px] min-h-[280px] flex-col overflow-hidden rounded-2xl border border-slate-100 bg-white shadow-sm lg:h-[calc(100vh-320px)]"
    >
      <div className="flex items-center justify-between border-b border-slate-100 px-5 py-4">
        <span className="flex items-center gap-2 text-sm font-bold text-brand-900">
          <span className="text-lg">✨</span> AI Assistant
          <span className="rounded-full bg-accent-50 px-2 py-0.5 text-[10px] font-bold text-accent-500">BETA</span>
        </span>
        <span className="flex items-center gap-1.5 text-xs font-medium text-emerald-600">
          <span className="h-1.5 w-1.5 rounded-full bg-emerald-500" /> Online
        </span>
      </div>

      <div ref={listRef} className="flex min-h-0 flex-1 flex-col gap-3 overflow-y-auto px-5 py-4">
        {messages.length === 0 ? (
          <>
            <div className="flex items-center justify-between px-0.5">
              <span className="text-xs font-bold uppercase tracking-wide text-slate-400">Getting started</span>
            </div>

            <div className="flex flex-col gap-2">
              {SUGGESTIONS.map((suggestion) => (
                <button
                  key={suggestion.text}
                  type="button"
                  onClick={() => send(suggestion.text)}
                  className="flex items-center gap-3 rounded-xl border border-slate-100 bg-white px-3.5 py-3 text-left transition-colors hover:border-accent-500 hover:bg-accent-50"
                >
                  <span className={`flex h-9 w-9 shrink-0 items-center justify-center rounded-lg text-base ${suggestion.badge}`}>
                    {suggestion.icon}
                  </span>
                  <span className="text-sm font-semibold text-brand-900">{suggestion.text}</span>
                </button>
              ))}
            </div>
          </>
        ) : (
          messages.map((message, index) => (
            <div
              key={index}
              className={`max-w-[85%] rounded-2xl px-4 py-2.5 text-sm ${
                message.role === 'user' ? 'ml-auto bg-brand-900 text-white' : 'bg-slate-100 text-brand-900'
              }`}
            >
              {message.text}
            </div>
          ))
        )}
        {isSending && (
          <div className="max-w-[85%] rounded-2xl bg-slate-100 px-4 py-2.5 text-sm text-slate-400">Searching the best options for you…</div>
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
            placeholder="Type your message…"
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
      </div>
    </aside>
  )
})

export default AIAssistantSidebar
