import { formatPrice } from '../utils/format.js'

export default function BookingConfirmation({ result, onBackToSearch }) {
  return (
    <div className="mx-auto max-w-2xl px-4 py-12">
      <div className="rounded-2xl border border-emerald-100 bg-white p-8 text-center shadow-xl">
        <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-emerald-50 text-3xl">✅</div>
        <h2 className="mt-4 text-2xl font-extrabold text-brand-900">Booking Confirmed!</h2>
        <p className="mt-1 text-sm text-slate-500">
          {result.emails.length > 1
            ? `A confirmation email is on its way to each traveller: ${result.emails.join(', ')}`
            : `A confirmation email is on its way to ${result.emails[0]}`}
        </p>

        <div className="mt-6 rounded-xl bg-brand-50 p-4">
          <div className="text-xs font-semibold uppercase tracking-wide text-slate-500">Booking Reference (PNR)</div>
          <div className="mt-1 text-3xl font-black tracking-[0.2em] text-brand-900">{result.bookingReference}</div>
        </div>

        <dl className="mt-6 grid grid-cols-2 gap-4 text-left text-sm">
          <div>
            <dt className="text-xs font-semibold uppercase tracking-wide text-slate-500">Order ID</dt>
            <dd className="mt-0.5 truncate font-medium text-brand-900">{result.orderId}</dd>
          </div>
          <div>
            <dt className="text-xs font-semibold uppercase tracking-wide text-slate-500">Status</dt>
            <dd className="mt-0.5 font-medium capitalize text-brand-900">{result.status}</dd>
          </div>
          <div>
            <dt className="text-xs font-semibold uppercase tracking-wide text-slate-500">Amount Paid</dt>
            <dd className="mt-0.5 font-medium text-brand-900">
              {formatPrice(result.totalAmount.amount, result.totalAmount.currency)}
            </dd>
          </div>
          {result.eTicketNumbers.length > 0 && (
            <div>
              <dt className="text-xs font-semibold uppercase tracking-wide text-slate-500">
                E-Ticket{result.eTicketNumbers.length > 1 ? 's' : ''}
              </dt>
              <dd className="mt-0.5 font-medium text-brand-900">{result.eTicketNumbers.join(', ')}</dd>
            </div>
          )}
        </dl>

        {result.passengers.length > 0 && (
          <div className="mt-6 border-t border-slate-100 pt-4 text-left">
            <div className="text-xs font-semibold uppercase tracking-wide text-slate-500">Passengers</div>
            <ul className="mt-2 space-y-1 text-sm text-brand-900">
              {result.passengers.map((passenger) => (
                <li key={passenger.id}>
                  {passenger.givenName} {passenger.familyName}
                </li>
              ))}
            </ul>
          </div>
        )}

        <button
          type="button"
          onClick={onBackToSearch}
          className="mt-8 rounded-xl bg-accent-500 px-8 py-3 text-sm font-bold text-white shadow-md shadow-accent-500/30 transition-colors hover:bg-accent-600"
        >
          Back to Search
        </button>
      </div>
    </div>
  )
}
