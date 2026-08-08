import { useState } from 'react'

const CABIN_CLASSES = [
  { value: 'ECONOMY', label: 'Economy' },
  { value: 'PREMIUM_ECONOMY', label: 'Premium Economy' },
  { value: 'BUSINESS', label: 'Business' },
  { value: 'FIRST', label: 'First' },
]

function Counter({ label, sublabel, value, min, onChange }) {
  return (
    <div className="flex items-center justify-between py-2">
      <div>
        <div className="text-sm font-medium text-brand-900">{label}</div>
        {sublabel && <div className="text-xs text-slate-500">{sublabel}</div>}
      </div>
      <div className="flex items-center gap-3">
        <button
          type="button"
          disabled={value <= min}
          onClick={() => onChange(Math.max(min, value - 1))}
          className="flex h-7 w-7 items-center justify-center rounded-full border border-slate-300 text-brand-900 disabled:opacity-30"
        >
          −
        </button>
        <span className="w-4 text-center text-sm font-semibold">{value}</span>
        <button
          type="button"
          onClick={() => onChange(value + 1)}
          className="flex h-7 w-7 items-center justify-center rounded-full border border-slate-300 text-brand-900 hover:border-accent-500 hover:text-accent-500"
        >
          +
        </button>
      </div>
    </div>
  )
}

export default function TravelerSelector({ passengers, cabinClass, onChange }) {
  const [open, setOpen] = useState(false)
  const total = passengers.adults + passengers.children + passengers.infants
  const cabinLabel = CABIN_CLASSES.find((c) => c.value === cabinClass)?.label ?? 'Economy'

  return (
    <div className="relative flex-1">
      <span className="block text-xs font-semibold uppercase tracking-wide text-slate-500">Travelers &amp; Class</span>
      <button
        type="button"
        onClick={() => setOpen((v) => !v)}
        className="mt-1 w-full text-left text-lg font-semibold text-brand-900"
      >
        {total} Traveler{total !== 1 ? 's' : ''} <span className="text-slate-400">·</span> {cabinLabel}
      </button>

      {open && (
        <div className="absolute left-0 top-full z-20 mt-2 w-72 rounded-xl border border-slate-200 bg-white p-4 text-left shadow-xl">
          <Counter
            label="Adults"
            sublabel="12+ years"
            value={passengers.adults}
            min={1}
            onChange={(v) => onChange({ ...passengers, adults: v }, cabinClass)}
          />
          <Counter
            label="Children"
            sublabel="2-11 years"
            value={passengers.children}
            min={0}
            onChange={(v) => onChange({ ...passengers, children: v }, cabinClass)}
          />
          <Counter
            label="Infants"
            sublabel="Under 2 years"
            value={passengers.infants}
            min={0}
            onChange={(v) => onChange({ ...passengers, infants: v }, cabinClass)}
          />

          <div className="my-3 border-t border-slate-100" />

          <div className="text-sm font-medium text-brand-900">Cabin Class</div>
          <div className="mt-2 grid grid-cols-2 gap-2">
            {CABIN_CLASSES.map((option) => (
              <button
                key={option.value}
                type="button"
                onClick={() => onChange(passengers, option.value)}
                className={`rounded-lg border px-2 py-1.5 text-xs font-medium ${
                  option.value === cabinClass
                    ? 'border-accent-500 bg-accent-50 text-accent-600'
                    : 'border-slate-200 text-slate-600 hover:border-slate-300'
                }`}
              >
                {option.label}
              </button>
            ))}
          </div>

          <button
            type="button"
            onClick={() => setOpen(false)}
            className="mt-4 w-full rounded-lg bg-brand-900 py-2 text-sm font-semibold text-white hover:bg-brand-800"
          >
            Done
          </button>
        </div>
      )}
    </div>
  )
}
