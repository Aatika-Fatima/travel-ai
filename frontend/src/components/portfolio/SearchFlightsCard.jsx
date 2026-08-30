import { useState } from 'react'
import SearchForm from '../SearchForm.jsx'

const TABS = [
  { id: 'flights', icon: '✈️', label: 'Flights' },
  { id: 'hotels', icon: '🏨', label: 'Hotels' },
  { id: 'assistant', icon: '✨', label: 'AI Assistant' },
]

export default function SearchFlightsCard({ criteria, onChange, onSubmit, onFocusAssistant }) {
  const [tab, setTab] = useState('flights')

  return (
    <section className="rounded-2xl border border-slate-100 bg-white shadow-sm">
      <div className="flex items-center gap-2 rounded-t-2xl border-b border-slate-100 bg-brand-50/50 px-5 py-3">
        <span className="text-lg">🛫</span>
        <div>
          <h2 className="text-sm font-bold text-brand-900">Search Flights</h2>
          <p className="text-xs text-slate-500">Find the best fares for your next journey</p>
        </div>
      </div>

      <div className="flex gap-1 border-b border-slate-100 px-5 pt-3">
        {TABS.map(({ id, icon, label }) => (
          <button
            key={id}
            type="button"
            onClick={() => (id === 'assistant' ? onFocusAssistant() : setTab(id))}
            className={`flex items-center gap-1.5 rounded-t-lg px-4 py-2 text-sm font-semibold transition-colors ${
              tab === id ? 'border-b-2 border-accent-500 text-accent-500' : 'text-slate-500 hover:text-brand-900'
            }`}
          >
            <span>{icon}</span>
            {label}
          </button>
        ))}
      </div>

      <div className="p-1">
        {tab === 'flights' && <SearchForm criteria={criteria} onChange={onChange} onSubmit={onSubmit} />}
        {tab === 'hotels' && (
          <div className="p-6 text-center text-sm text-slate-500">
            🏨 Hotel search isn't wired up yet — this tab's here for the layout, not the backend.
          </div>
        )}
      </div>
    </section>
  )
}
