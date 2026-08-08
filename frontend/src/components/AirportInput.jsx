export default function AirportInput({ label, value, onChange, placeholder }) {
  return (
    <label className="flex-1">
      <span className="block text-xs font-semibold uppercase tracking-wide text-slate-500">{label}</span>
      <input
        type="text"
        value={value}
        maxLength={3}
        placeholder={placeholder}
        onChange={(event) => onChange(event.target.value.toUpperCase().replace(/[^A-Z]/g, ''))}
        className="mt-1 w-full border-0 border-b-2 border-transparent bg-transparent p-0 text-2xl font-bold uppercase tracking-wide text-brand-900 placeholder:text-slate-300 focus:border-accent-500 focus:outline-none focus:ring-0"
      />
    </label>
  )
}
