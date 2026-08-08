export default function ErrorState({ message, details = [], onRetry }) {
  return (
    <div className="flex flex-col items-center justify-center rounded-2xl border border-red-100 bg-red-50 py-16 text-center">
      <div className="mb-3 text-5xl">⚠️</div>
      <h3 className="text-lg font-bold text-red-700">We couldn't complete that search</h3>
      <p className="mt-1 max-w-md text-sm text-red-600">{message}</p>
      {details.length > 0 && (
        <ul className="mt-2 text-xs text-red-500">
          {details.map((detail) => (
            <li key={detail}>{detail}</li>
          ))}
        </ul>
      )}
      {onRetry && (
        <button
          type="button"
          onClick={onRetry}
          className="mt-4 rounded-lg bg-accent-500 px-5 py-2 text-sm font-semibold text-white hover:bg-accent-600"
        >
          Try Again
        </button>
      )}
    </div>
  )
}
