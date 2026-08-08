import { formatDuration, formatPrice } from '../utils/format.js'

const TIME_BUCKETS = [
  { id: 'EARLY_MORNING', label: 'Early Morning', hint: '12am – 6am' },
  { id: 'MORNING', label: 'Morning', hint: '6am – 12pm' },
  { id: 'AFTERNOON', label: 'Afternoon', hint: '12pm – 6pm' },
  { id: 'EVENING', label: 'Evening', hint: '6pm – 12am' },
]

function Section({ title, children }) {
  return (
    <div className="border-b border-slate-100 py-4">
      <h3 className="mb-3 text-sm font-bold text-brand-900">{title}</h3>
      {children}
    </div>
  )
}

function toggleInSet(set, value) {
  const next = new Set(set)
  if (next.has(value)) next.delete(value)
  else next.add(value)
  return next
}

export default function FilterSidebar({ filters, onChange, facets }) {
  const update = (patch) => onChange({ ...filters, ...patch })

  return (
    <aside className="scroll-thin max-h-[calc(100vh-8rem)] w-full shrink-0 overflow-y-auto rounded-2xl border border-slate-100 bg-white p-5 shadow-sm md:sticky md:top-24 md:w-72">
      <div className="mb-2 flex items-center justify-between">
        <h2 className="text-base font-extrabold text-brand-900">Filters</h2>
        <button type="button" onClick={() => onChange(null)} className="text-xs font-semibold text-accent-500 hover:underline">
          Clear all
        </button>
      </div>

      <Section title="Stops">
        {[0, 1, 2].map((stops) => (
          <label key={stops} className="flex items-center gap-2 py-1 text-sm text-slate-700">
            <input
              type="checkbox"
              checked={filters.stops.has(stops)}
              onChange={() => update({ stops: toggleInSet(filters.stops, stops) })}
              className="h-4 w-4 rounded border-slate-300 text-accent-500 focus:ring-accent-500"
            />
            {stops === 0 ? 'Non-stop' : stops === 1 ? '1 stop' : '2+ stops'}
          </label>
        ))}
      </Section>

      <Section title="Price">
        <input
          type="range"
          min={facets.minPrice}
          max={facets.maxPrice}
          value={filters.maxPrice}
          onChange={(event) => update({ maxPrice: Number(event.target.value) })}
          className="w-full accent-accent-500"
        />
        <div className="flex justify-between text-xs text-slate-500">
          <span>{formatPrice(facets.minPrice, facets.currency)}</span>
          <span className="font-semibold text-brand-900">Up to {formatPrice(filters.maxPrice, facets.currency)}</span>
        </div>
      </Section>

      <Section title="Departure Time">
        {TIME_BUCKETS.map((bucket) => (
          <label key={bucket.id} className="flex items-center justify-between gap-2 py-1 text-sm text-slate-700">
            <span className="flex items-center gap-2">
              <input
                type="checkbox"
                checked={filters.timeBuckets.has(bucket.id)}
                onChange={() => update({ timeBuckets: toggleInSet(filters.timeBuckets, bucket.id) })}
                className="h-4 w-4 rounded border-slate-300 text-accent-500 focus:ring-accent-500"
              />
              {bucket.label}
            </span>
            <span className="text-xs text-slate-400">{bucket.hint}</span>
          </label>
        ))}
      </Section>

      <Section title="Duration">
        <input
          type="range"
          min={facets.minDuration}
          max={facets.maxDuration}
          value={filters.maxDuration}
          onChange={(event) => update({ maxDuration: Number(event.target.value) })}
          className="w-full accent-accent-500"
        />
        <div className="text-xs text-slate-500">Up to {formatDuration(filters.maxDuration)}</div>
      </Section>

      {facets.airlines.length > 0 && (
        <Section title="Airlines">
          {facets.airlines.map((airline) => (
            <label key={airline} className="flex items-center gap-2 py-1 text-sm text-slate-700">
              <input
                type="checkbox"
                checked={filters.airlines.has(airline)}
                onChange={() => update({ airlines: toggleInSet(filters.airlines, airline) })}
                className="h-4 w-4 rounded border-slate-300 text-accent-500 focus:ring-accent-500"
              />
              {airline}
            </label>
          ))}
        </Section>
      )}
    </aside>
  )
}
