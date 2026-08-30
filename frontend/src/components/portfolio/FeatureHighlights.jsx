// Describes the actual stack behind this app -- not generic filler. Keep
// this in sync with what the backend modules really use.
const FEATURES = [
  { icon: '🧠', title: 'AI Powered', description: 'Intelligent search, recommendations and an assistant, built on Spring AI.' },
  { icon: '🧩', title: 'Microservices', description: 'Event-driven, independently scalable services behind a Kafka-based saga.' },
  { icon: '☁️', title: 'Cloud Native', description: 'Containerized and deployed on Oracle Cloud Infrastructure.' },
  { icon: '🔒', title: 'Secure & Reliable', description: 'OAuth2, JWT, RBAC, and idempotency-first design throughout.' },
  { icon: '📊', title: 'Data & Search', description: 'PostgreSQL, Redis, and Elasticsearch power storage, caching and search.' },
]

export default function FeatureHighlights() {
  return (
    <section className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-5">
      {FEATURES.map(({ icon, title, description }) => (
        <div key={title} className="rounded-2xl border border-slate-100 bg-white p-4 text-center shadow-sm">
          <div className="mx-auto mb-2 flex h-10 w-10 items-center justify-center rounded-xl bg-brand-50 text-lg">
            {icon}
          </div>
          <div className="text-xs font-bold text-brand-900">{title}</div>
          <p className="mt-1 text-[11px] leading-snug text-slate-500">{description}</p>
        </div>
      ))}
    </section>
  )
}
