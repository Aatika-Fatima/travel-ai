import { useState } from 'react'
import ThemeToggle from '../ThemeToggle.jsx'
import { DownloadIcon } from './icons.jsx'

const NAV_ITEMS = [
  { label: 'Home', href: '#' },
  { label: 'About', href: '#about' },
  { label: 'Skills', href: '#skills' },
  { label: 'Projects', href: '#featured-projects' },
  { label: 'Architecture', href: '#architecture' },
  { label: 'C4 Diagram', href: '#c4-diagram' },
  { label: 'Experience', href: '#experience' },
  { label: 'Blog', href: '#' },
  { label: 'Contact', href: '#contact' },
]

export default function PortfolioHeader({ theme, onThemeChange }) {
  const [active, setActive] = useState('Home')

  return (
    <header className="sticky top-0 z-20 bg-gradient-to-r from-brand-900 to-accent-500 px-4 py-3 shadow-md">
      <div className="mx-auto flex max-w-7xl flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-2 text-white">
          <span className="text-2xl">🚀</span>
          <div className="leading-tight">
            <div className="text-lg font-extrabold tracking-tight">
              Aatika <span className="font-light text-white/80">FlyStack</span>
            </div>
            <div className="text-[11px] text-white/70">Java Backend Engineer</div>
          </div>
        </div>

        <nav
          className="scroll-thin order-3 flex w-full items-center gap-1 overflow-x-auto rounded-full bg-white/10 p-1 lg:order-none lg:w-auto lg:overflow-visible"
          aria-label="Primary"
        >
          {NAV_ITEMS.map((item) => (
            <a
              key={item.label}
              href={item.href}
              onClick={() => setActive(item.label)}
              className={`shrink-0 whitespace-nowrap rounded-full px-3.5 py-1.5 text-sm font-semibold transition-colors ${
                active === item.label ? 'bg-white text-brand-900 shadow-sm' : 'text-white/85 hover:text-white'
              }`}
            >
              {item.label}
            </a>
          ))}
        </nav>

        <div className="flex items-center gap-3">
          <ThemeToggle theme={theme} onChange={onThemeChange} dark />
          <a
            href="/resume.pdf"
            download="Aatika_Fatima_Resume.pdf"
            aria-label="Download résumé"
            className="flex items-center gap-1.5 rounded-full bg-white/10 px-3 py-1.5 text-sm font-semibold text-white transition-colors hover:bg-white/20"
          >
            <DownloadIcon />
            <span className="hidden sm:inline">Resume</span>
          </a>
        </div>
      </div>
    </header>
  )
}
