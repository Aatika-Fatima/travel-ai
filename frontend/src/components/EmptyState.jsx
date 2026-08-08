export default function EmptyState({ onResetFilters }) {
  return (
    <div className="flex flex-col items-center justify-center rounded-2xl border border-dashed border-slate-200 bg-white py-16 text-center shadow-sm">
      <div className="mb-3 text-5xl">🔍</div>
      <h3 className="text-lg font-bold text-brand-900">No flights match your filters</h3>
      <p className="mt-1 max-w-sm text-sm text-slate-500">
        Try widening your price range, allowing more stops, or clearing a few filters.
      </p>
      {onResetFilters && (
        <button
          type="button"
          onClick={onResetFilters}
          className="mt-4 rounded-lg bg-brand-900 px-5 py-2 text-sm font-semibold text-white hover:bg-brand-800"
        >
          Reset filters
        </button>
      )}
    </div>
  )
}
