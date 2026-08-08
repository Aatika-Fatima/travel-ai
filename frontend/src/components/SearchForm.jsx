import AirportInput from './AirportInput.jsx'
import TravelerSelector from './TravelerSelector.jsx'

function SwapButton({ onClick }) {
  return (
    <button
      type="button"
      onClick={onClick}
      aria-label="Swap origin and destination"
      className="mx-3 flex h-9 w-9 shrink-0 items-center justify-center rounded-full border border-slate-200 bg-white text-brand-900 shadow-sm transition-transform hover:rotate-180 hover:border-accent-500 hover:text-accent-500"
    >
      ⇄
    </button>
  )
}

export default function SearchForm({ criteria, onChange, onSubmit, compact = false }) {
  const update = (patch) => onChange({ ...criteria, ...patch })

  const today = new Date().toISOString().slice(0, 10)

  return (
    <form
      onSubmit={(event) => {
        event.preventDefault()
        onSubmit()
      }}
      className={`rounded-2xl bg-white shadow-xl ${compact ? 'p-4' : 'p-6'}`}
    >
      <div className="mb-4 flex items-center justify-between">
        <TripTypeToggleWrapper value={criteria.tripType} onChange={(tripType) => update({ tripType })} />
        <label className="flex items-center gap-2 text-sm text-slate-600">
          <input
            type="checkbox"
            checked={criteria.directOnly}
            onChange={(event) => update({ directOnly: event.target.checked })}
            className="h-4 w-4 rounded border-slate-300 text-accent-500 focus:ring-accent-500"
          />
          Direct flights only
        </label>
      </div>

      <div className="flex flex-col gap-4 rounded-xl border border-slate-100 bg-brand-50/40 p-4 md:flex-row md:items-end">
        <div className="flex flex-1 items-end">
          <AirportInput label="From" placeholder="DEL" value={criteria.origin} onChange={(origin) => update({ origin })} />
          <SwapButton onClick={() => update({ origin: criteria.destination, destination: criteria.origin })} />
          <AirportInput
            label="To"
            placeholder="BOM"
            value={criteria.destination}
            onChange={(destination) => update({ destination })}
          />
        </div>

        <div className="flex flex-1 gap-4">
          <label className="flex-1">
            <span className="block text-xs font-semibold uppercase tracking-wide text-slate-500">Departure</span>
            <input
              type="date"
              min={today}
              value={criteria.departureDate}
              onChange={(event) => update({ departureDate: event.target.value })}
              className="mt-1 w-full border-0 border-b-2 border-transparent bg-transparent p-0 text-lg font-semibold text-brand-900 focus:border-accent-500 focus:outline-none focus:ring-0"
            />
          </label>

          {criteria.tripType === 'ROUND_TRIP' && (
            <label className="flex-1">
              <span className="block text-xs font-semibold uppercase tracking-wide text-slate-500">Return</span>
              <input
                type="date"
                min={criteria.departureDate || today}
                value={criteria.returnDate}
                onChange={(event) => update({ returnDate: event.target.value })}
                className="mt-1 w-full border-0 border-b-2 border-transparent bg-transparent p-0 text-lg font-semibold text-brand-900 focus:border-accent-500 focus:outline-none focus:ring-0"
              />
            </label>
          )}
        </div>

        <TravelerSelector
          passengers={criteria.passengers}
          cabinClass={criteria.cabinClass}
          onChange={(passengers, cabinClass) => update({ passengers, cabinClass })}
        />

        <button
          type="submit"
          className="rounded-xl bg-accent-500 px-8 py-3 text-base font-bold text-white shadow-md shadow-accent-500/30 transition-colors hover:bg-accent-600 md:self-stretch"
        >
          Search Flights
        </button>
      </div>
    </form>
  )
}

function TripTypeToggleWrapper({ value, onChange }) {
  return (
    <div className="rounded-full bg-brand-50 p-1">
      <div className="inline-flex rounded-full">
        {[
          ['ONE_WAY', 'One Way'],
          ['ROUND_TRIP', 'Round Trip'],
          ['MULTI_CITY', 'Multi-City'],
        ].map(([val, label]) => (
          <button
            key={val}
            type="button"
            onClick={() => onChange(val)}
            className={`rounded-full px-4 py-1.5 text-sm font-medium transition-colors ${
              val === value ? 'bg-brand-900 text-white shadow-sm' : 'text-brand-900/70 hover:text-brand-900'
            }`}
          >
            {label}
          </button>
        ))}
      </div>
    </div>
  )
}
