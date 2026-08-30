const CAPABILITIES = [
  {
    icon: '✈️',
    name: 'Flight Search',
    detail: 'Live Duffel offers plus a Postgres/pg_trgm airport index with typo-tolerant matching. Ranked by price, duration, and stops.',
  },
  {
    icon: '✨',
    name: 'AI Travel Assistant',
    detail: 'A Spring AI multi-step agent that collects missing trip details, validates the request, and drives the search via tool calls and structured JSON output.',
  },
  {
    icon: '🏨',
    name: 'Travel Services',
    detail: 'Flights today, with the booking/order/payment split built to fold in hotels, baggage, and ancillaries behind the same saga.',
  },
  {
    icon: '🧾',
    name: 'Booking Lifecycle',
    detail: 'booking-service owns the booking state machine end to end -- PENDING through CONFIRMED or EXPIRED -- with a scheduled sweep for stuck bookings.',
  },
  {
    icon: '💳',
    name: 'Payments & Webhooks',
    detail: 'Razorpay checkout with signature verification and webhook-driven reconciliation, on its own state machine decoupled from airline fulfillment.',
  },
  {
    icon: '⚡',
    name: 'Events & Saga',
    detail: 'A Kafka saga across booking → order → payment, every producer backed by a transactional outbox for at-least-once delivery with no dual-write.',
  },
]

export default function PlatformSection() {
  return (
    <section id="platform" className="scroll-mt-24 rounded-2xl border border-slate-100 bg-white p-5 shadow-sm">
      <div className="mb-1 text-[11px] font-bold uppercase tracking-[0.2em] text-accent-500">FlyStack Platform</div>
      <h3 className="text-xl font-black tracking-tight text-brand-900">AI-powered, event-driven travel platform</h3>
      <p className="mt-2 max-w-2xl text-sm leading-relaxed text-slate-600">
        A modular monolith of independently-boundaried Kotlin / Spring Boot services, assembled into one JVM and wired together
        over Kafka. Six capabilities, one deployable.
      </p>

      <div className="mt-5 grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
        {CAPABILITIES.map((cap) => (
          <div
            key={cap.name}
            className="flex flex-col rounded-xl border border-brand-100 bg-brand-50/50 p-4 transition-all duration-200 hover:-translate-y-0.5 hover:border-accent-500 hover:shadow-md"
          >
            <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-white text-lg shadow-sm">{cap.icon}</div>
            <div className="mt-3 text-sm font-bold text-brand-900">{cap.name}</div>
            <p className="mt-1 text-xs leading-relaxed text-slate-600">{cap.detail}</p>
          </div>
        ))}
      </div>
    </section>
  )
}
