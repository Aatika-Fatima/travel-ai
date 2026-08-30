import { useEffect, useState } from 'react'

const STACK = [
  { icon: '🅺', label: 'Kotlin' },
  { icon: '🍃', label: 'Spring Boot' },
  { icon: '🧠', label: 'Spring AI' },
  { icon: '📨', label: 'Kafka' },
  { icon: '🐘', label: 'PostgreSQL' },
  { icon: '🟥', label: 'Redis' },
  { icon: '🔍', label: 'Elasticsearch' },
  { icon: '🐳', label: 'Docker' },
  { icon: '☁️', label: 'OCI' },
]

function useClock() {
  const [now, setNow] = useState(() => new Date())
  useEffect(() => {
    const id = setInterval(() => setNow(new Date()), 1000)
    return () => clearInterval(id)
  }, [])
  return now
}

export default function TechStackFooter() {
  const now = useClock()

  return (
    <footer className="bg-brand-950 px-4 py-3">
      <div className="mx-auto flex max-w-7xl flex-wrap items-center justify-between gap-3 text-white">
        <div className="flex flex-wrap items-center gap-2">
          <span className="mr-1 text-xs font-bold uppercase tracking-wide text-white/50">Tech Stack</span>
          {STACK.map(({ icon, label }) => (
            <span
              key={label}
              className="flex items-center gap-1.5 rounded-full bg-white/10 px-3 py-1 text-xs font-medium"
            >
              <span>{icon}</span>
              {label}
            </span>
          ))}
        </div>
        <div className="text-right text-xs text-white/70">
          <div className="font-semibold text-white">
            {now.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
          </div>
          <div>{now.toLocaleDateString([], { month: 'short', day: 'numeric', year: 'numeric' })}</div>
        </div>
      </div>
    </footer>
  )
}
