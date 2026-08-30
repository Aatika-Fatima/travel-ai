import { formatDuration, formatPrice, formatTime, stopsLabel } from '../utils/format.js'

function Badge({ children, tone = 'slate' }) {
  const tones = {
    slate: 'bg-slate-100 text-slate-600',
    green: 'bg-emerald-50 text-emerald-700',
    amber: 'bg-amber-50 text-amber-700',
  }
  return <span className={`rounded-full px-2 py-0.5 text-xs font-medium ${tones[tone]}`}>{children}</span>
}

function SliceRow({ slice }) {
  const airline = slice.segments[0]?.airline
  return (
    <div className="flex items-center gap-4">
      <div className="flex w-40 shrink-0 items-center gap-2">
        {airline?.logoUrl ? (
          <img src={airline.logoUrl} alt={airline.name} className="h-6 w-6 rounded object-contain" />
        ) : (
          <div className="flex h-6 w-6 items-center justify-center rounded bg-brand-100 text-[10px] font-bold text-brand-900">
            {airline?.iataCode ?? '—'}
          </div>
        )}
        <div className="truncate text-xs text-slate-600">{airline?.name ?? 'Unknown airline'}</div>
      </div>

      <div className="flex flex-1 items-center gap-3">
        <div className="text-center">
          <div className="text-[1.069rem] font-bold text-brand-900">{formatTime(slice.departureTime)}</div>
          <div className="text-xs text-slate-500">{slice.origin.iataCode}</div>
        </div>

        <div className="flex flex-1 flex-col items-center">
          <div className="text-xs text-slate-400">{formatDuration(slice.durationMinutes)}</div>
          <div className="relative h-px w-full bg-slate-300">
            <div className="absolute left-1/2 top-1/2 h-1.5 w-1.5 -translate-x-1/2 -translate-y-1/2 rounded-full bg-slate-400" />
          </div>
          <div className="text-xs text-slate-400">{stopsLabel(slice.stops)}</div>
        </div>

        <div className="text-center">
          <div className="text-[1.069rem] font-bold text-brand-900">{formatTime(slice.arrivalTime)}</div>
          <div className="text-xs text-slate-500">{slice.destination.iataCode}</div>
        </div>
      </div>
    </div>
  )
}

export default function FlightCard({ offer, onSelect }) {
  return (
    <div className="rounded-2xl border border-slate-100 bg-white p-5 shadow-sm transition-shadow hover:shadow-lg">
      <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
        <div className="flex-1 space-y-3">
          {offer.slices.map((slice, index) => (
            <SliceRow key={index} slice={slice} />
          ))}
        </div>

        <div className="flex shrink-0 flex-col items-end gap-2 border-t border-slate-100 pt-3 md:border-t-0 md:border-l md:pl-5 md:pt-0">
          <div className="text-[1.425rem] font-extrabold text-brand-900">{formatPrice(offer.price.amount, offer.price.currency)}</div>
          <div className="flex gap-1.5">
            {offer.refundable && <Badge tone="green">Refundable</Badge>}
            {offer.baggageAllowance && <Badge>{offer.baggageAllowance}</Badge>}
            {offer.stops === 0 && <Badge tone="amber">Non-stop</Badge>}
          </div>
          <button
            type="button"
            onClick={() => onSelect(offer)}
            className="rounded-lg bg-accent-500 px-6 py-2 text-sm font-bold text-white shadow-sm hover:bg-accent-600"
          >
            Book
          </button>
        </div>
      </div>
    </div>
  )
}
