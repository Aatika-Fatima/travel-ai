const PROJECTS = [
  {
    icon: '✈️',
    iconBg: 'bg-brand-900',
    name: 'FlyStack (AI Travel Platform)',
    period: 'This project',
    description:
      "This app -- a modular-monolith flight search, booking, and payment platform with an AI assistant, an idempotent Kafka saga across booking/order/payment services, and a Razorpay checkout integration.",
    tags: ['Kotlin', 'Spring Boot', 'Spring AI', 'Kafka', 'PostgreSQL'],
  },
  {
    icon: '💳',
    iconBg: 'bg-accent-500',
    name: 'Razorpay Payment Integration',
    period: 'This project',
    description:
      'Idempotent order creation, checkout-signature verification, and webhook-driven reconciliation for customer payments, decoupled from airline fulfillment via its own state machine.',
    tags: ['Kotlin', 'Spring Boot', 'Razorpay', 'PostgreSQL'],
  },
]

const EDUCATION = [
  {
    degree: 'Bachelor of Computer Science & Engineering',
    school: 'Osmania University, Hyderabad, India',
    note: 'Sep 2004 -- May 2008',
  },
]

export default function FeaturedProjects() {
  return (
    <section id="featured-projects" className="scroll-mt-24 rounded-2xl border border-slate-100 bg-white p-5 shadow-sm">
      <h3 className="mb-4 flex items-center gap-2 text-sm font-bold text-brand-900">
        <span>💼</span> Featured Projects
      </h3>

      <div className="grid gap-4 sm:grid-cols-2">
        {PROJECTS.map((project) => (
          <div key={project.name} className="flex flex-col rounded-xl border border-slate-100 p-4">
            <div className="flex items-start gap-3">
              <div className={`flex h-9 w-9 shrink-0 items-center justify-center rounded-lg text-white ${project.iconBg}`}>
                {project.icon}
              </div>
              <div className="min-w-0">
                <div className="text-sm font-bold text-brand-900">{project.name}</div>
                <div className="text-[11px] text-slate-400">{project.period}</div>
              </div>
            </div>
            <p className="mt-3 flex-1 text-xs leading-relaxed text-slate-600">{project.description}</p>
            <div className="mt-3 flex flex-wrap gap-1.5">
              {project.tags.map((tag) => (
                <span key={tag} className="rounded-full bg-brand-50 px-2.5 py-0.5 text-[10px] font-semibold text-brand-900">
                  {tag}
                </span>
              ))}
            </div>
          </div>
        ))}
      </div>

      <div className="mt-5 border-t border-slate-100 pt-4">
        <h4 className="mb-2 text-xs font-bold uppercase tracking-wide text-slate-400">Academic Background</h4>
        {EDUCATION.map((entry) => (
          <div key={entry.degree} className="text-sm">
            <div className="font-semibold text-brand-900">{entry.degree}</div>
            <div className="text-xs text-slate-500">
              {entry.school} · {entry.note}
            </div>
          </div>
        ))}
        <div className="mt-2 text-xs text-slate-500">
          <span className="font-semibold text-brand-900">Certification:</span> AWS Certified Developer Associate
        </div>
      </div>
    </section>
  )
}
