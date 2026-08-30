const THEMES = [
  { value: 'skyfare', label: 'SkyFare' },
  { value: 'portfolio', label: 'Portfolio' },
]

export default function ThemeToggle({ theme, onChange, dark = false }) {
  return (
    <div
      className={`inline-flex rounded-full p-1 ${dark ? 'bg-white/10' : 'bg-brand-50'}`}
      role="radiogroup"
      aria-label="Theme"
    >
      {THEMES.map(({ value, label }) => (
        <button
          key={value}
          type="button"
          role="radio"
          aria-checked={theme === value}
          onClick={() => onChange(value)}
          className={`rounded-full px-3 py-1 text-xs font-semibold transition-colors ${
            theme === value
              ? 'bg-accent-500 text-white shadow-sm'
              : dark
                ? 'text-white/70 hover:text-white'
                : 'text-brand-900/70 hover:text-brand-900'
          }`}
        >
          {label}
        </button>
      ))}
    </div>
  )
}
