const STATS = [
  { value: '12+', label: 'Years of experience' },
  { value: '6', label: 'Payment providers integrated' },
  { value: '10,000+', label: 'Monthly transactions processed' },
  { value: '99.9%', label: 'Payment availability' },
]

// Sourced from docs/Aatika_Fatima_Remote.docx -- keep in sync if the resume changes.
export default function AboutSection() {
  return (
    <section id="about" className="scroll-mt-24 rounded-2xl border border-slate-100 bg-white p-5 shadow-sm">
      <h3 className="mb-4 flex items-center gap-2 text-sm font-bold text-brand-900">
        <span>🙋</span> About
      </h3>

      <p className="text-sm leading-relaxed text-slate-600">
        Senior Software Engineer with 12+ years of experience specialising in B2B payment platforms, virtual card
        lifecycle management, and enterprise microservices. Deep expertise integrating global payment providers,
        designing intelligent card-selection engines, and building fraud-prevention systems at scale. Proven ability
        to own solutions end-to-end -- from architecture and development through deployment and production
        operations -- reducing manual workload and maximising payment profitability. Actively expanding into applied
        AI engineering, incorporating Spring AI and Claude-based tooling into the Java/Spring ecosystem to
        accelerate delivery and code quality.
      </p>

      <div className="mt-5 grid grid-cols-2 gap-3 sm:grid-cols-4">
        {STATS.map((stat) => (
          <div key={stat.label} className="rounded-xl bg-brand-50 p-3 text-center">
            <div className="text-lg font-black text-brand-900">{stat.value}</div>
            <div className="mt-0.5 text-[11px] leading-snug text-slate-500">{stat.label}</div>
          </div>
        ))}
      </div>
    </section>
  )
}
