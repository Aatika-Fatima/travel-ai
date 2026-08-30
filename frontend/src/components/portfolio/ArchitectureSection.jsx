import { useState } from 'react'

// Reflects the actual module/service layout of this repo -- keep in sync
// with pom.xml modules and compose.yaml if either changes.
const LAYERS = [
  {
    label: 'Client',
    icon: '💻',
    nodes: [
      {
        name: 'React + Vite SPA',
        icon: '⚛️',
        detail: 'Flight search UI and the AI Assistant chat panel.',
        tags: ['React', 'Vite', 'Tailwind CSS'],
      },
    ],
  },
  {
    label: 'Runtime',
    icon: '⚙️',
    nodes: [
      {
        name: 'app (Spring Boot)',
        icon: '🍃',
        detail: 'Assembles every module into one runnable JVM for this environment; ArchUnit enforces module boundaries at build time.',
        tags: ['Spring Boot 4', 'Kotlin', 'ArchUnit'],
      },
    ],
  },
  {
    label: 'Domain Services',
    icon: '🧩',
    nodes: [
      { name: 'flight (duffle-api)', icon: '✈️', detail: 'Duffel flight search & booking integration.', tags: ['Kotlin', 'Duffel API'], saga: false },
      { name: 'search-service / search-ai', icon: '🧠', detail: 'Elasticsearch-backed search, ranked with Spring AI.', tags: ['Elasticsearch', 'Spring AI'], saga: false },
      { name: 'booking-service', icon: '🎫', detail: 'Owns the booking lifecycle and drives the Kafka saga.', tags: ['Kotlin', 'Outbox pattern'], saga: true },
      { name: 'order-service', icon: '📦', detail: 'Orders orchestration, payment confirmation, and reconciliation.', tags: ['Kotlin', 'Outbox pattern'], saga: true },
      { name: 'payment-service', icon: '💳', detail: 'Razorpay checkout, signature verification, and webhooks.', tags: ['Kotlin', 'Razorpay'], saga: true },
      { name: 'notification', icon: '🔔', detail: 'Email delivery triggered by booking/order events.', tags: ['Kotlin', 'RetryableTopic + DLQ'], saga: false },
    ],
  },
  {
    label: 'Event Backbone',
    icon: '📡',
    nodes: [
      {
        name: 'Kafka',
        icon: '📨',
        detail: 'booking.events · order.events · payment.events · email-events -- every producer uses a transactional outbox for at-least-once delivery.',
        tags: ['Transactional outbox', 'Idempotent producer', 'Manual-ack consumers'],
        saga: true,
      },
    ],
  },
  {
    label: 'Data & External',
    icon: '🗄️',
    nodes: [
      { name: 'PostgreSQL + pgvector', icon: '🐘', detail: 'System of record plus vector embeddings for semantic search.', tags: ['Postgres 16', 'pgvector'] },
      { name: 'Redis', icon: '🟥', detail: 'Caching layer.', tags: ['Redis 7'] },
      { name: 'Elasticsearch', icon: '🔎', detail: 'Flight and airport search index.', tags: ['Elasticsearch 9'] },
      { name: 'Duffel API', icon: '🌐', detail: 'External flight inventory & fulfillment provider.', tags: ['External'] },
      { name: 'Razorpay', icon: '💰', detail: 'External payment gateway.', tags: ['External'] },
      { name: 'MailHog / SMTP', icon: '✉️', detail: 'Local email delivery/inbox for notifications.', tags: ['SMTP'] },
    ],
  },
]

// The actual search -> book -> pay journey, annotated with the Kafka
// events each step consumes/publishes. Source: booking-service,
// order-service, payment-service, and notification module code.
const FLOW_STEPS = [
  {
    title: 'Search Flights',
    actor: 'Client + search-service + flight (duffle-api)',
    icon: '🔍',
    detail: 'Traveler searches DEL -> BOM. The request hits search-service (Elasticsearch ranking) and flight/duffle-api (live Duffel offers).',
    kafka: [],
  },
  {
    title: 'Create Booking',
    actor: 'booking-service',
    icon: '🎫',
    detail: 'Traveler selects an offer. booking-service creates the Booking (PENDING) and writes an outbox row in the same DB transaction.',
    kafka: [{ direction: 'publishes', topic: 'booking.events', event: 'BookingCreated', note: 'BookingOutboxPublisher polls every 1s, keyed by bookingId.' }],
  },
  {
    title: 'Submit Order',
    actor: 'order-service',
    icon: '📦',
    detail: 'BookingSagaConsumer (group order-service) picks up the event and submits a new Order.',
    kafka: [
      { direction: 'consumes', topic: 'booking.events', note: 'Group: order-service' },
      { direction: 'publishes', topic: 'order.events', event: 'OrderAwaitingConfirmation', note: 'OutboxRelay polls every 500ms with FOR UPDATE SKIP LOCKED, keyed by orderId.' },
    ],
  },
  {
    title: 'Sync Booking Status',
    actor: 'booking-service',
    icon: '🔄',
    detail: "OrderEventConsumer (group booking-service) advances the booking's status machine. Malformed messages are logged and skipped rather than blocking the partition.",
    kafka: [{ direction: 'consumes', topic: 'order.events', note: 'Group: booking-service' }],
  },
  {
    title: 'Checkout & Capture',
    actor: 'payment-service',
    icon: '💳',
    detail: 'Traveler completes Razorpay checkout. payment-service verifies the checkout signature and captures the payment.',
    kafka: [{ direction: 'publishes', topic: 'payment.events', event: 'payment.captured', note: 'PaymentOutboxPublisher polls every 1s, keyed by bookingId.' }],
  },
  {
    title: 'Confirm Payment',
    actor: 'order-service',
    icon: '✅',
    detail: 'PaymentEventConsumer (group order-service) marks the order PAID -- @Retryable on optimistic-lock conflicts, idempotent no-op if already paid.',
    kafka: [{ direction: 'consumes', topic: 'payment.events', note: 'Group: order-service' }],
  },
  {
    title: 'Notify Traveler',
    actor: 'notification',
    icon: '📧',
    detail: 'EmailWorker sends the booking confirmation email.',
    kafka: [{ direction: 'publishes', topic: 'email-events', event: 'send confirmation', note: '@RetryableTopic: 3 attempts, 5min -> 30min backoff, then a DLQ handler.' }],
  },
]

function Connector() {
  return (
    <div className="relative mx-auto flex h-8 w-px justify-center bg-brand-100" aria-hidden="true">
      <span className="absolute h-2 w-2 -translate-x-1/2 rounded-full bg-accent-500 animate-flow-down" style={{ left: '50%' }} />
    </div>
  )
}

function KafkaBadge({ entry }) {
  const isPublish = entry.direction === 'publishes'
  return (
    <span
      title={entry.note}
      className={`inline-flex cursor-help items-center gap-1 rounded-full px-2.5 py-1 text-[10px] font-semibold ${
        isPublish ? 'bg-accent-500 text-white' : 'border border-accent-400 bg-white text-accent-600'
      }`}
    >
      {isPublish ? '📨 publishes' : '📥 consumes'} {entry.topic}
      {entry.event ? ` · ${entry.event}` : ''}
    </span>
  )
}

function LayersDiagram({ selected, setSelected, sagaOnly, setSagaOnly }) {
  return (
    <>
      <div className="mb-3 flex justify-end">
        <button
          type="button"
          onClick={() => setSagaOnly((v) => !v)}
          className={`flex items-center gap-1.5 rounded-full px-3 py-1.5 text-xs font-bold transition-colors ${
            sagaOnly ? 'bg-accent-500 text-white shadow-sm' : 'bg-brand-50 text-brand-900 hover:bg-brand-100'
          }`}
        >
          🔀 {sagaOnly ? 'Showing Kafka saga path' : 'Highlight Kafka saga path'}
        </button>
      </div>

      <div className="flex flex-col items-stretch">
        {LAYERS.map((layer, index) => (
          <div key={layer.label}>
            <div className="rounded-xl border border-slate-100 bg-slate-50 p-3">
              <div className="mb-2 flex items-center gap-1.5 text-[11px] font-bold uppercase tracking-wide text-slate-400">
                <span>{layer.icon}</span> {layer.label}
              </div>
              <div className="flex flex-wrap gap-2">
                {layer.nodes.map((node) => {
                  const isSelected = selected?.name === node.name
                  const dimmed = sagaOnly && !node.saga
                  return (
                    <button
                      key={node.name}
                      type="button"
                      onClick={() => setSelected(isSelected ? null : node)}
                      className={`flex items-start gap-2 rounded-lg border px-3 py-2 text-left shadow-sm transition-all duration-200 ${
                        isSelected
                          ? 'scale-105 border-accent-500 bg-accent-50 ring-2 ring-accent-500'
                          : node.saga && sagaOnly
                            ? 'scale-105 border-accent-400 bg-white ring-2 ring-accent-400'
                            : 'border-brand-100 bg-white hover:-translate-y-0.5 hover:border-accent-500 hover:shadow-md'
                      } ${dimmed ? 'opacity-30' : 'opacity-100'}`}
                    >
                      <span className="text-base leading-none">{node.icon}</span>
                      <span>
                        <span className="block text-xs font-bold text-brand-900">{node.name}</span>
                        <span className="mt-0.5 block max-w-[200px] text-[11px] leading-snug text-slate-500">{node.detail}</span>
                      </span>
                    </button>
                  )
                })}
              </div>
            </div>
            {index < LAYERS.length - 1 && <Connector />}
          </div>
        ))}
      </div>

      {selected && (
        <div className="mt-4 flex items-start gap-3 rounded-xl border border-accent-500 bg-accent-50 p-4">
          <span className="text-2xl leading-none">{selected.icon}</span>
          <div className="min-w-0 flex-1">
            <div className="flex items-center justify-between gap-2">
              <span className="text-sm font-bold text-brand-900">{selected.name}</span>
              <button type="button" onClick={() => setSelected(null)} aria-label="Close details" className="text-slate-400 hover:text-brand-900">
                ✕
              </button>
            </div>
            <p className="mt-1 text-xs leading-relaxed text-slate-600">{selected.detail}</p>
            <div className="mt-2 flex flex-wrap gap-1.5">
              {selected.tags.map((tag) => (
                <span key={tag} className="rounded-full bg-white px-2.5 py-0.5 text-[10px] font-semibold text-brand-900 shadow-sm">
                  {tag}
                </span>
              ))}
            </div>
          </div>
        </div>
      )}
    </>
  )
}

function BookingFlowDiagram() {
  const [expanded, setExpanded] = useState(FLOW_STEPS[0].title)

  return (
    <>
      <p className="mb-4 text-xs text-slate-500">Click a step to expand it. 📨 = publishes an event, 📥 = consumes one.</p>
      <ol className="flex flex-col">
        {FLOW_STEPS.map((step, index) => {
          const isOpen = expanded === step.title
          return (
            <li key={step.title} className="flex gap-3">
              <div className="flex flex-col items-center">
                <button
                  type="button"
                  onClick={() => setExpanded(isOpen ? null : step.title)}
                  aria-label={`Toggle ${step.title}`}
                  className={`flex h-8 w-8 shrink-0 items-center justify-center rounded-full text-sm font-bold transition-colors ${
                    isOpen ? 'bg-accent-500 text-white' : 'bg-brand-900 text-white hover:bg-accent-500'
                  }`}
                >
                  {index + 1}
                </button>
                {index < FLOW_STEPS.length - 1 && (
                  <div className="relative w-px flex-1 bg-brand-100" aria-hidden="true">
                    <span className="absolute h-2 w-2 -translate-x-1/2 rounded-full bg-accent-500 animate-flow-down" style={{ left: '50%' }} />
                  </div>
                )}
              </div>

              <button type="button" onClick={() => setExpanded(isOpen ? null : step.title)} className="min-w-0 flex-1 pb-5 text-left">
                <div className="flex flex-wrap items-center gap-2">
                  <span className="text-lg leading-none">{step.icon}</span>
                  <span className="text-sm font-bold text-brand-900">{step.title}</span>
                  <span className="text-[11px] font-semibold text-accent-500">{step.actor}</span>
                </div>

                {isOpen && (
                  <>
                    <p className="mt-1.5 max-w-2xl text-xs leading-relaxed text-slate-600">{step.detail}</p>
                    {step.kafka.length > 0 && (
                      <div className="mt-2 flex flex-wrap gap-1.5">
                        {step.kafka.map((entry) => (
                          <KafkaBadge key={entry.direction + entry.topic} entry={entry} />
                        ))}
                      </div>
                    )}
                  </>
                )}
              </button>
            </li>
          )
        })}
      </ol>

      <p className="mt-1 text-[11px] leading-relaxed text-slate-400">
        Safety nets: a scheduled BookingExpirySweep (booking-service) and ReconciliationJob (order-service) catch stuck sagas independent of Kafka
        delivery.
      </p>
    </>
  )
}

export default function ArchitectureSection() {
  const [view, setView] = useState('flow')
  const [selected, setSelected] = useState(null)
  const [sagaOnly, setSagaOnly] = useState(false)

  return (
    <section id="architecture" className="scroll-mt-24 rounded-2xl border border-slate-100 bg-white p-5 shadow-sm">
      <div className="mb-1 flex flex-wrap items-center justify-between gap-3">
        <h3 className="flex items-center gap-2 text-sm font-bold text-brand-900">
          <span>🏗️</span> Architecture
        </h3>

        <div className="flex gap-1 rounded-full bg-brand-50 p-1">
          <button
            type="button"
            onClick={() => setView('flow')}
            className={`rounded-full px-3 py-1.5 text-xs font-bold transition-colors ${
              view === 'flow' ? 'bg-white text-brand-900 shadow-sm' : 'text-slate-500 hover:text-brand-900'
            }`}
          >
            🔁 Booking Flow
          </button>
          <button
            type="button"
            onClick={() => setView('layers')}
            className={`rounded-full px-3 py-1.5 text-xs font-bold transition-colors ${
              view === 'layers' ? 'bg-white text-brand-900 shadow-sm' : 'text-slate-500 hover:text-brand-900'
            }`}
          >
            🏗️ System Layers
          </button>
        </div>
      </div>

      <p className="mb-5 text-sm leading-relaxed text-slate-600">
        {view === 'flow'
          ? 'How a booking actually moves through the system, from search to payment, and the Kafka events fired along the way.'
          : 'A modular-monolith deployment of independently-boundaried Spring Boot/Kotlin services. Click any node for details.'}
      </p>

      {view === 'flow' ? (
        <BookingFlowDiagram />
      ) : (
        <LayersDiagram selected={selected} setSelected={setSelected} sagaOnly={sagaOnly} setSagaOnly={setSagaOnly} />
      )}
    </section>
  )
}
