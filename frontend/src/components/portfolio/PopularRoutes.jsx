const ROUTES = [
  ['HYD', 'DEL'],
  ['BOM', 'BLR'],
  ['DEL', 'GOI'],
  ['MAA', 'CCU'],
  ['BLR', 'BOM'],
]

export default function PopularRoutes({ onSelect }) {
  return (
    <section className="rounded-2xl border border-slate-100 bg-white p-5 shadow-sm">
      <div className="mb-3 flex items-center justify-between">
        <h3 className="flex items-center gap-2 text-sm font-bold text-brand-900">
          <span>🧭</span> Popular Routes
        </h3>
      </div>
      <div className="flex flex-wrap gap-2">
        {ROUTES.map(([from, to]) => (
          <button
            key={`${from}-${to}`}
            type="button"
            onClick={() => onSelect(from, to)}
            className="rounded-full border border-slate-200 bg-brand-50/40 px-4 py-1.5 text-sm font-semibold text-brand-900 transition-colors hover:border-accent-500 hover:bg-accent-50 hover:text-accent-500"
          >
            {from} → {to}
          </button>
        ))}
      </div>
    </section>
  )
}
