const STATUS_LABELS = {
  PENDING_SUBMISSION: 'Sending your booking to the airline…',
  AWAITING_AIRLINE_CONFIRMATION: 'Waiting on the airline to confirm…',
}

export default function OrderStatusWait({ orderStatus }) {
  const label = STATUS_LABELS[orderStatus] ?? 'Confirming your flight…'

  return (
    <div className="mx-auto max-w-md px-4 py-16 text-center">
      <div className="mx-auto h-12 w-12 animate-spin rounded-full border-4 border-brand-100 border-t-accent-500" />
      <h2 className="mt-6 text-xl font-bold text-brand-900">{label}</h2>
      <p className="mt-2 text-sm text-slate-500">This can take up to a minute. Please don't close this page.</p>
    </div>
  )
}
