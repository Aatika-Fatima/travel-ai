import { formatPrice } from '../utils/format.js'

export default function OrderConfirmation({ order, amount, currency, contactEmail, onBackToSearch }) {
  return (
    <div className="mx-auto max-w-2xl px-4 py-12">
      <div className="rounded-2xl border border-emerald-100 bg-white p-8 text-center shadow-xl">
        <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-emerald-50 text-3xl">✅</div>
        <h2 className="mt-4 text-2xl font-extrabold text-brand-900">Payment received!</h2>
        <p className="mt-1 text-sm text-slate-500">A confirmation email is on its way to {contactEmail}.</p>

        {order.bookingReference && (
          <div className="mt-6 rounded-xl bg-brand-50 p-4">
            <div className="text-xs font-semibold uppercase tracking-wide text-slate-500">Booking Reference (PNR)</div>
            <div className="mt-1 text-3xl font-black tracking-[0.2em] text-brand-900">{order.bookingReference}</div>
          </div>
        )}

        <dl className="mt-6 grid grid-cols-2 gap-4 text-left text-sm">
          <div>
            <dt className="text-xs font-semibold uppercase tracking-wide text-slate-500">Order ID</dt>
            <dd className="mt-0.5 truncate font-medium text-brand-900">{order.orderId}</dd>
          </div>
          <div>
            <dt className="text-xs font-semibold uppercase tracking-wide text-slate-500">Status</dt>
            <dd className="mt-0.5 font-medium capitalize text-brand-900">{order.status.toLowerCase()}</dd>
          </div>
          <div>
            <dt className="text-xs font-semibold uppercase tracking-wide text-slate-500">Amount Paid</dt>
            <dd className="mt-0.5 font-medium text-brand-900">{formatPrice(amount, currency)}</dd>
          </div>
        </dl>

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
