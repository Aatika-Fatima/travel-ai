export default function BioSidebar() {
  return (
    <aside className="flex h-fit flex-col gap-3 self-start rounded-2xl border border-slate-100 bg-white p-6 shadow-sm">
      <div className="flex items-center gap-3">
        <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-full bg-gradient-to-br from-accent-500 to-brand-900 text-lg font-black text-white shadow-md">
          A
        </div>
        <div>
          <span className="block text-xs font-semibold text-brand-900">Hi, I'm</span>
          <h1 className="text-2xl font-black leading-tight tracking-tight text-brand-900">Aatika</h1>
        </div>
      </div>

      <p className="text-sm font-semibold text-slate-500">Java Backend Engineer</p>

      <p className="line-clamp-2 text-sm leading-relaxed text-slate-600">
        I build scalable, secure and user-centric backend systems that solve real-world problems. I combine clean
        code with intelligent features to create products people actually enjoy using.
      </p>
    </aside>
  )
}
