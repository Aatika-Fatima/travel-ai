import { useState } from 'react'

// A C4 "Container" diagram scoped to the booking flow only (search -> book
// -> pay -> notify). Coordinates are hand-tuned for a 1100x780 viewBox --
// adjust box positions together with their arrows if you change either.
const BOXES = [
  {
    id: 'traveler',
    kind: 'person',
    name: 'Traveler',
    icon: '🧑',
    x: 460, y: 20, w: 180, h: 55,
    detail: 'The end user searching for and booking a flight.',
    tech: 'Person',
  },
  {
    id: 'webapp',
    kind: 'container',
    name: 'Web App',
    icon: '⚛️',
    x: 430, y: 115, w: 240, h: 60,
    detail: 'Single-page app for flight search, booking, and checkout.',
    tech: 'React + Vite',
  },
  {
    id: 'flight',
    kind: 'container',
    name: 'flight (duffle-api)',
    icon: '✈️',
    x: 60, y: 250, w: 220, h: 65,
    detail: 'Searches live offers and issues the booking with the airline via Duffel.',
    tech: 'Kotlin, Spring Boot',
  },
  {
    id: 'booking',
    kind: 'container',
    name: 'booking-service',
    icon: '🎫',
    x: 310, y: 250, w: 220, h: 65,
    detail: 'Owns the Booking record and its status machine; first mover in the saga.',
    tech: 'Kotlin, Spring Boot',
  },
  {
    id: 'order',
    kind: 'container',
    name: 'order-service',
    icon: '📦',
    x: 560, y: 250, w: 220, h: 65,
    detail: 'Orchestrates the order, confirms payment, runs reconciliation.',
    tech: 'Kotlin, Spring Boot',
  },
  {
    id: 'payment',
    kind: 'container',
    name: 'payment-service',
    icon: '💳',
    x: 810, y: 250, w: 220, h: 65,
    detail: 'Runs Razorpay checkout, verifies signatures, handles webhooks.',
    tech: 'Kotlin, Spring Boot',
  },
  {
    id: 'kafka',
    kind: 'infra',
    name: 'Kafka',
    icon: '📨',
    x: 420, y: 365, w: 260, h: 80,
    detail: 'booking.events, order.events, payment.events -- every producer uses a transactional outbox for at-least-once delivery.',
    tech: 'Apache Kafka',
  },
  {
    id: 'postgres',
    kind: 'infra',
    name: 'PostgreSQL',
    icon: '🐘',
    x: 430, y: 485, w: 240, h: 65,
    detail: 'System of record for bookings, orders, payments, and each service’s outbox table.',
    tech: 'PostgreSQL 16',
  },
  {
    id: 'notification',
    kind: 'container',
    name: 'notification',
    icon: '🔔',
    x: 790, y: 485, w: 210, h: 65,
    detail: 'Sends the booking confirmation email; retries with a DLQ on failure.',
    tech: 'Kotlin, Spring Boot',
  },
  {
    id: 'duffel',
    kind: 'external',
    name: 'Duffel API',
    icon: '🌐',
    x: 60, y: 650, w: 220, h: 65,
    detail: 'External flight inventory, pricing, and fulfillment provider.',
    tech: 'External System',
  },
  {
    id: 'razorpay',
    kind: 'external',
    name: 'Razorpay',
    icon: '💰',
    x: 440, y: 650, w: 220, h: 65,
    detail: 'External payment gateway that captures the traveler’s payment.',
    tech: 'External System',
  },
  {
    id: 'smtp',
    kind: 'external',
    name: 'MailHog / SMTP',
    icon: '✉️',
    x: 790, y: 650, w: 220, h: 65,
    detail: 'Email delivery for the booking confirmation.',
    tech: 'External System',
  },
]

const boxById = Object.fromEntries(BOXES.map((b) => [b.id, b]))
const center = (b) => ({ x: b.x + b.w / 2, y: b.y + b.h / 2 })
const bottom = (b, dx = 0) => ({ x: b.x + b.w / 2 + dx, y: b.y + b.h })
const top = (b, dx = 0) => ({ x: b.x + b.w / 2 + dx, y: b.y })
const side = (b, edge, dy = 0) => ({ x: edge === 'left' ? b.x : b.x + b.w, y: b.y + b.h / 2 + dy })

// Each arrow: start point, end point, optional control point (quadratic
// bezier, for routing around other boxes), a label, and whether it should
// carry arrowheads on both ends (Kafka <-> service relationships publish
// AND consume).
const ARROWS = [
  { from: bottom(boxById.traveler), to: top(boxById.webapp), label: 'searches & books' },
  { from: bottom(boxById.webapp, -80), to: top(boxById.flight), ctrl: { x: 300, y: 190 }, label: 'search flights [HTTPS]' },
  { from: bottom(boxById.webapp), to: top(boxById.booking), label: 'create booking [HTTPS]' },
  { from: bottom(boxById.webapp, 60), to: top(boxById.payment), ctrl: { x: 800, y: 190 }, label: 'checkout [HTTPS]' },

  { from: bottom(boxById.flight), to: top(boxById.duffel), label: 'fetch offers [HTTPS]' },
  { from: bottom(boxById.payment), to: top(boxById.razorpay), ctrl: { x: 650, y: 560 }, label: 'capture payment [HTTPS]' },
  { from: bottom(boxById.notification), to: top(boxById.smtp), label: 'send email [SMTP]' },

  { from: bottom(boxById.booking, -20), to: side(boxById.kafka, 'left', -20), label: 'booking.events', bidirectional: true },
  { from: bottom(boxById.order, -10), to: top(boxById.kafka, 60), label: 'order.events', bidirectional: true },
  { from: bottom(boxById.payment, -60), to: side(boxById.kafka, 'right', -10), ctrl: { x: 780, y: 330 }, label: 'payment.events', bidirectional: true },
  { from: side(boxById.kafka, 'right', 20), to: top(boxById.notification, -40), ctrl: { x: 760, y: 430 }, label: 'triggers email' },

  { from: bottom(boxById.booking, 40), to: side(boxById.postgres, 'left', -15), ctrl: { x: 380, y: 460 }, label: 'JDBC' },
  { from: bottom(boxById.order, 30), to: top(boxById.postgres, 40), ctrl: { x: 640, y: 460 }, label: 'JDBC' },
  { from: bottom(boxById.payment, -100), to: side(boxById.postgres, 'right', -25), ctrl: { x: 740, y: 590 }, label: 'JDBC' },
]

const KIND_STYLE = {
  person: { fill: 'var(--color-brand-700)', text: '#fff', dash: 'none' },
  container: { fill: 'var(--color-brand-900)', text: '#fff', dash: 'none' },
  infra: { fill: 'var(--color-accent-500)', text: '#fff', dash: 'none' },
  external: { fill: '#94a3b8', text: '#fff', dash: '6 4' },
}

function pointOnQuadratic(p0, pc, p2, t) {
  const x = (1 - t) ** 2 * p0.x + 2 * (1 - t) * t * pc.x + t ** 2 * p2.x
  const y = (1 - t) ** 2 * p0.y + 2 * (1 - t) * t * pc.y + t ** 2 * p2.y
  return { x, y }
}

function Arrow({ arrow, dimmed }) {
  const { from, to, label, bidirectional } = arrow
  const ctrl = arrow.ctrl ?? { x: (from.x + to.x) / 2, y: (from.y + to.y) / 2 }
  const d = `M ${from.x} ${from.y} Q ${ctrl.x} ${ctrl.y} ${to.x} ${to.y}`
  const mid = pointOnQuadratic(from, ctrl, to, 0.5)

  return (
    <g opacity={dimmed ? 0.25 : 1} className="transition-opacity duration-200">
      <path
        d={d}
        fill="none"
        stroke="var(--color-brand-700)"
        strokeWidth="1.75"
        markerEnd="url(#c4-arrowhead)"
        markerStart={bidirectional ? 'url(#c4-arrowhead-start)' : undefined}
      />
      <rect x={mid.x - label.length * 3.1 - 4} y={mid.y - 9} width={label.length * 6.2 + 8} height="16" rx="4" fill="white" />
      <text x={mid.x} y={mid.y + 3} textAnchor="middle" fontSize="10.5" fill="var(--color-brand-900)" fontWeight="600">
        {label}
      </text>
    </g>
  )
}

function Box({ box, isSelected, dimmed, onClick }) {
  const style = KIND_STYLE[box.kind]
  return (
    <g
      role="button"
      tabIndex={0}
      onClick={() => onClick(box)}
      onKeyDown={(e) => e.key === 'Enter' && onClick(box)}
      className="cursor-pointer outline-none"
      opacity={dimmed ? 0.35 : 1}
      style={{ transition: 'opacity 200ms' }}
    >
      {isSelected && (
        <rect
          x={box.x - 4}
          y={box.y - 4}
          width={box.w + 8}
          height={box.h + 8}
          rx="13"
          fill="none"
          stroke="#facc15"
          strokeWidth="3"
        />
      )}
      <rect x={box.x} y={box.y} width={box.w} height={box.h} rx="10" fill={style.fill} strokeDasharray={style.dash} stroke={style.dash !== 'none' ? '#fff' : 'none'} strokeWidth="1.5" />
      <text x={box.x + box.w / 2} y={box.y + 24} textAnchor="middle" fontSize="15">
        {box.icon}
      </text>
      <text x={box.x + box.w / 2} y={box.y + box.h / 2 + 12} textAnchor="middle" fontSize="12.5" fontWeight="700" fill={style.text}>
        {box.name}
      </text>
    </g>
  )
}

export default function C4DiagramSection() {
  const [selected, setSelected] = useState(boxById.kafka)
  const [hoverGroup, setHoverGroup] = useState(null)

  const dimBox = (box) => {
    if (!hoverGroup) return false
    if (hoverGroup === 'saga') return !['booking', 'order', 'payment', 'kafka'].includes(box.id)
    return false
  }

  return (
    <section id="c4-diagram" className="scroll-mt-24 rounded-2xl border border-slate-100 bg-white p-5 shadow-sm">
      <h3 className="mb-1 flex items-center gap-2 text-sm font-bold text-brand-900">
        <span>🗺️</span> C4 Diagram -- Booking Flow
      </h3>
      <p className="mb-3 text-sm leading-relaxed text-slate-600">
        A C4 Container diagram of the search -&gt; book -&gt; pay -&gt; notify path. Click any box for detail.
      </p>

      <div className="mb-3 flex flex-wrap items-center gap-3 text-[11px] font-semibold text-slate-500">
        <span className="flex items-center gap-1.5">
          <span className="h-3 w-3 rounded-sm" style={{ background: 'var(--color-brand-700)' }} /> Person
        </span>
        <span className="flex items-center gap-1.5">
          <span className="h-3 w-3 rounded-sm" style={{ background: 'var(--color-brand-900)' }} /> Container
        </span>
        <span className="flex items-center gap-1.5">
          <span className="h-3 w-3 rounded-sm" style={{ background: 'var(--color-accent-500)' }} /> Broker / Database
        </span>
        <span className="flex items-center gap-1.5">
          <span className="h-3 w-3 rounded-sm border border-dashed border-slate-400" style={{ background: '#94a3b8' }} /> External System
        </span>
        <button
          type="button"
          onMouseEnter={() => setHoverGroup('saga')}
          onMouseLeave={() => setHoverGroup(null)}
          onClick={() => setHoverGroup((g) => (g === 'saga' ? null : 'saga'))}
          className={`ml-auto flex items-center gap-1.5 rounded-full px-3 py-1.5 font-bold transition-colors ${
            hoverGroup === 'saga' ? 'bg-accent-500 text-white' : 'bg-brand-50 text-brand-900 hover:bg-brand-100'
          }`}
        >
          🔀 Focus Kafka saga
        </button>
      </div>

      <div className="overflow-x-auto rounded-xl border border-slate-100 bg-slate-50">
        <svg viewBox="0 0 1100 730" className="w-full min-w-[860px]" role="img" aria-label="C4 container diagram of the booking flow">
          <defs>
            <marker id="c4-arrowhead" markerWidth="8" markerHeight="8" refX="6" refY="3" orient="auto">
              <path d="M0,0 L6,3 L0,6 Z" fill="var(--color-brand-700)" />
            </marker>
            <marker id="c4-arrowhead-start" markerWidth="8" markerHeight="8" refX="0" refY="3" orient="auto-start-reverse">
              <path d="M0,0 L6,3 L0,6 Z" fill="var(--color-brand-700)" />
            </marker>
          </defs>

          <rect x="30" y="200" width="1040" height="390" rx="14" fill="none" stroke="var(--color-brand-100)" strokeWidth="2" strokeDasharray="8 6" />
          <text x="46" y="222" fontSize="11" fontWeight="700" fill="var(--color-brand-700)">
            FlyStack -- Kafka-based booking saga
          </text>

          {ARROWS.map((arrow, i) => (
            <Arrow key={i} arrow={arrow} dimmed={false} />
          ))}

          {BOXES.map((box) => (
            <Box key={box.id} box={box} isSelected={selected?.id === box.id} dimmed={dimBox(box)} onClick={setSelected} />
          ))}
        </svg>
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
            <span className="mt-2 inline-block rounded-full bg-white px-2.5 py-0.5 text-[10px] font-semibold text-brand-900 shadow-sm">
              {selected.tech}
            </span>
          </div>
        </div>
      )}
    </section>
  )
}
