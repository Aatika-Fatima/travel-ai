const SORT_OPTIONS = [
  { id: 'BEST', label: 'Best' },
  { id: 'PRICE', label: 'Cheapest' },
  { id: 'DURATION', label: 'Fastest' },
  { id: 'DEPARTURE_TIME', label: 'Departure Time' },
]

export default function SortBar({ value, onChange, resultCount }) {
  return (
    <div className="mb-4 flex flex-wrap items-center justify-between gap-3 rounded-xl bg-white p-3 shadow-sm">
      <div className="text-sm text-slate-500">
        <span className="font-bold text-brand-900">{resultCount}</span> flights found
      </div>
      <div className="flex gap-1 rounded-full bg-slate-100 p-1">
        {SORT_OPTIONS.map((option) => (
          <button
            key={option.id}
            type="button"
            onClick={() => onChange(option.id)}
            className={`rounded-full px-3 py-1.5 text-sm font-medium transition-colors ${
              option.id === value ? 'bg-brand-900 text-white shadow-sm' : 'text-slate-600 hover:text-brand-900'
            }`}
          >
            {option.label}
          </button>
        ))}
      </div>
    </div>
  )
}
