const CHANNELS = [
  { icon: '✉️', label: 'Email', value: 'aatika08@gmail.com', href: 'mailto:aatika08@gmail.com' },
  { icon: '📞', label: 'Phone', value: '+91 89778 13009', href: 'tel:+918977813009' },
  { icon: '💼', label: 'LinkedIn', value: 'linkedin.com/in/aatikafatima', href: 'https://linkedin.com/in/aatikafatima' },
  { icon: '🐙', label: 'GitHub', value: 'github.com/Aatika-Fatima', href: 'https://github.com/Aatika-Fatima?tab=repositories' },
]

export default function ContactSection() {
  return (
    <section id="contact" className="scroll-mt-24 rounded-2xl border border-slate-100 bg-white p-5 shadow-sm">
      <h3 className="mb-4 flex items-center gap-2 text-sm font-bold text-brand-900">
        <span>📬</span> Contact
      </h3>

      <p className="mb-4 text-sm leading-relaxed text-slate-600">
        Open to senior backend and AI-assisted engineering roles. Reach out directly, or grab the full resume below.
      </p>

      <div className="grid gap-3 sm:grid-cols-2">
        {CHANNELS.map((channel) => (
          <a
            key={channel.label}
            href={channel.href}
            target={channel.href.startsWith('http') ? '_blank' : undefined}
            rel={channel.href.startsWith('http') ? 'noreferrer' : undefined}
            className="flex items-center gap-3 rounded-xl border border-slate-100 px-3.5 py-3 transition-colors hover:border-accent-500 hover:bg-accent-50"
          >
            <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-brand-50 text-base">{channel.icon}</span>
            <div className="min-w-0">
              <div className="text-[11px] font-bold uppercase tracking-wide text-slate-400">{channel.label}</div>
              <div className="truncate text-sm font-semibold text-brand-900">{channel.value}</div>
            </div>
          </a>
        ))}
      </div>

      <a
        href="/resume.pdf"
        download="Aatika_Fatima_Resume.pdf"
        className="mt-4 flex w-fit items-center gap-2 rounded-xl bg-accent-500 px-4 py-2.5 text-sm font-bold text-white shadow-md shadow-accent-500/30 transition-colors hover:bg-accent-600"
      >
        ⬇ Download Résumé
      </a>
    </section>
  )
}
