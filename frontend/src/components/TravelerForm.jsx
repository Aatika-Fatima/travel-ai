const TITLES = [
  { value: 'mr', label: 'Mr' },
  { value: 'mrs', label: 'Mrs' },
  { value: 'ms', label: 'Ms' },
]

function Field({ label, value, onChange, error, type = 'text', placeholder, className = '' }) {
  return (
    <label className={className}>
      <span className="block text-xs font-semibold uppercase tracking-wide text-slate-500">{label}</span>
      <input
        type={type}
        value={value}
        placeholder={placeholder}
        onChange={(event) => onChange(event.target.value)}
        className={`mt-1 w-full rounded-lg border bg-white px-3 py-2 text-sm font-medium text-brand-900 placeholder:text-slate-300 focus:outline-none focus:ring-1 ${
          error ? 'border-red-300 focus:border-red-400 focus:ring-red-400' : 'border-slate-200 focus:border-accent-500 focus:ring-accent-500'
        }`}
      />
      {error && <span className="mt-1 block text-xs text-red-600">{error}</span>}
    </label>
  )
}

export default function TravelerForm({ index, category, value, onChange, errors = {} }) {
  const update = (patch) => onChange({ ...value, ...patch })

  return (
    <div className="rounded-2xl border border-slate-100 bg-white p-5 shadow-sm">
      <div className="mb-4 flex items-center gap-2">
        <span className="flex h-7 w-7 items-center justify-center rounded-full bg-brand-100 text-xs font-bold text-brand-900">
          {index + 1}
        </span>
        <h3 className="text-sm font-bold text-brand-900">
          Traveller {index + 1} <span className="font-normal text-slate-400">· {category}</span>
        </h3>
      </div>

      <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
        <label className="col-span-2 sm:col-span-1">
          <span className="block text-xs font-semibold uppercase tracking-wide text-slate-500">Title</span>
          <select
            value={value.title}
            onChange={(event) => update({ title: event.target.value })}
            className="mt-1 w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm font-medium text-brand-900 focus:border-accent-500 focus:outline-none focus:ring-1 focus:ring-accent-500"
          >
            {TITLES.map((title) => (
              <option key={title.value} value={title.value}>
                {title.label}
              </option>
            ))}
          </select>
        </label>

        <Field
          className="col-span-2 sm:col-span-1"
          label="First Name"
          value={value.givenName}
          onChange={(v) => update({ givenName: v })}
          error={errors.givenName}
        />
        <Field
          className="col-span-2 sm:col-span-1"
          label="Last Name"
          value={value.familyName}
          onChange={(v) => update({ familyName: v })}
          error={errors.familyName}
        />

        <label className="col-span-2 sm:col-span-1">
          <span className="block text-xs font-semibold uppercase tracking-wide text-slate-500">Date of Birth</span>
          <input
            type="date"
            value={value.dateOfBirth}
            max={new Date().toISOString().slice(0, 10)}
            onChange={(event) => update({ dateOfBirth: event.target.value })}
            className={`mt-1 w-full rounded-lg border bg-white px-3 py-2 text-sm font-medium text-brand-900 focus:outline-none focus:ring-1 ${
              errors.dateOfBirth
                ? 'border-red-300 focus:border-red-400 focus:ring-red-400'
                : 'border-slate-200 focus:border-accent-500 focus:ring-accent-500'
            }`}
          />
          {errors.dateOfBirth && <span className="mt-1 block text-xs text-red-600">{errors.dateOfBirth}</span>}
        </label>

        <div className="col-span-2 sm:col-span-1">
          <span className="block text-xs font-semibold uppercase tracking-wide text-slate-500">Gender</span>
          <div className="mt-2.5 flex gap-4">
            {[
              ['m', 'Male'],
              ['f', 'Female'],
            ].map(([val, label]) => (
              <label key={val} className="flex items-center gap-1.5 text-sm text-brand-900">
                <input
                  type="radio"
                  name={`gender-${index}`}
                  checked={value.gender === val}
                  onChange={() => update({ gender: val })}
                  className="h-4 w-4 border-slate-300 text-accent-500 focus:ring-accent-500"
                />
                {label}
              </label>
            ))}
          </div>
        </div>

        <Field
          className="col-span-2"
          label="Email"
          type="email"
          value={value.email}
          onChange={(v) => update({ email: v })}
          error={errors.email}
        />
        <Field
          className="col-span-2 sm:col-span-1"
          label="Phone Number"
          type="tel"
          placeholder="+91 98765 43210"
          value={value.phoneNumber}
          onChange={(v) => update({ phoneNumber: v })}
          error={errors.phoneNumber}
        />
      </div>
    </div>
  )
}
