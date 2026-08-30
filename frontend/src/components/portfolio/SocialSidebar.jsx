import { GithubIcon, LinkedinIcon, GmailIcon } from './icons.jsx'

const LINKS = [
  { Icon: GithubIcon, label: 'GitHub', href: 'https://github.com/Aatika-Fatima?tab=repositories' },
  { Icon: LinkedinIcon, label: 'LinkedIn', href: 'https://linkedin.com/in/aatikafatima' },
  { Icon: GmailIcon, label: 'Gmail', href: 'mailto:aatika08@gmail.com' },
]

// Pinned to the left edge of the viewport, independent of scroll position --
// a common portfolio pattern so these links stay reachable without competing
// for space in the header or bio card.
export default function SocialSidebar() {
  return (
    <div className="fixed left-4 top-1/2 z-10 hidden -translate-y-1/2 flex-col items-center gap-3 lg:flex">
      {LINKS.map(({ Icon, label, href }) => (
        <a
          key={label}
          href={href}
          target="_blank"
          rel="noreferrer"
          aria-label={label}
          className="flex h-9 w-9 items-center justify-center rounded-full border border-slate-200 bg-white text-slate-500 shadow-sm transition-colors hover:border-accent-500 hover:text-accent-500"
        >
          <Icon />
        </a>
      ))}
      <span className="h-16 w-px bg-slate-300" />
    </div>
  )
}
