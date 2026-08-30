// Small hand-written SVG icons -- no icon package is installed in this
// project, and pulling one in for five glyphs isn't worth the dependency.
function Svg({ children, ...props }) {
  return (
    <svg viewBox="0 0 24 24" fill="currentColor" width="18" height="18" aria-hidden="true" {...props}>
      {children}
    </svg>
  )
}

// Official brand mark, badged on its own filled background (like GmailIcon)
// rather than left as a plain currentColor glyph.
export function GithubIcon(props) {
  return (
    <svg viewBox="0 0 24 24" width="18" height="18" aria-hidden="true" {...props}>
      <rect width="24" height="24" rx="5" fill="#181717" />
      <path
        fill="#fff"
        d="M12 3.5c-4.7 0-8.5 3.8-8.5 8.5 0 3.76 2.44 6.95 5.82 8.08.43.08.58-.19.58-.42v-1.63c-2.37.51-2.87-1.07-2.87-1.07-.39-.99-.95-1.25-.95-1.25-.77-.53.06-.52.06-.52.86.06 1.31.88 1.31.88.76 1.3 1.99.93 2.48.71.08-.55.3-.93.54-1.15-1.89-.22-3.88-.95-3.88-4.22 0-.93.33-1.7.87-2.29-.09-.22-.38-1.08.08-2.26 0 0 .71-.23 2.34.87a8.07 8.07 0 0 1 4.26 0c1.62-1.1 2.33-.87 2.33-.87.47 1.18.17 2.04.09 2.26.54.59.87 1.36.87 2.29 0 3.28-2 4-3.9 4.21.31.27.58.79.58 1.6v2.37c0 .23.15.5.59.42A8.51 8.51 0 0 0 20.5 12c0-4.7-3.8-8.5-8.5-8.5Z"
      />
    </svg>
  )
}

export function LinkedinIcon(props) {
  return (
    <svg viewBox="0 0 24 24" width="18" height="18" aria-hidden="true" {...props}>
      <rect width="24" height="24" rx="5" fill="#0A66C2" />
      <path
        fill="#fff"
        d="M8.34 9.5H5.7v9.13h2.64V9.5Zm-1.31-4.2a1.53 1.53 0 1 0 0 3.06 1.53 1.53 0 0 0 0-3.06ZM18.3 13.4c0-2.6-1.39-3.81-3.24-3.81-1.5 0-2.16.83-2.53 1.4h-.03V9.5h-2.54c.03.72 0 9.13 0 9.13h2.54v-5.1c0-.27.02-.55.1-.74.22-.55.72-1.13 1.57-1.13 1.11 0 1.55.85 1.55 2.09v4.88h2.58v-5.23Z"
      />
    </svg>
  )
}

export function GmailIcon(props) {
  return (
    <svg viewBox="0 0 24 24" width="18" height="18" aria-hidden="true" {...props}>
      <path fill="#fff" d="M2 6.5A2.5 2.5 0 0 1 4.5 4h15A2.5 2.5 0 0 1 22 6.5v11a2.5 2.5 0 0 1-2.5 2.5h-15A2.5 2.5 0 0 1 2 17.5v-11Z" />
      <path fill="#4285F4" d="M4.5 4h15A2.5 2.5 0 0 1 22 6.5v.4l-10 6.35L2 6.9v-.4A2.5 2.5 0 0 1 4.5 4Z" />
      <path fill="#EA4335" d="M2 6.9 6 9.65V20H4.5A2.5 2.5 0 0 1 2 17.5V6.9Z" />
      <path fill="#34A853" d="M22 6.9 18 9.65V20h1.5A2.5 2.5 0 0 0 22 17.5V6.9Z" />
      <path fill="#FBBC04" d="m6 9.65 6 3.6 6-3.6V20H6V9.65Z" />
    </svg>
  )
}

export function MailIcon(props) {
  return (
    <Svg {...props}>
      <path d="M3 5h18a1 1 0 0 1 1 1v12a1 1 0 0 1-1 1H3a1 1 0 0 1-1-1V6a1 1 0 0 1 1-1Zm9 7.5L4.2 7h15.6L12 12.5Zm-9 3.8V8.4l7.4 5.3a1 1 0 0 0 1.2 0L20 8.4v7.9H3Z" />
    </Svg>
  )
}

export function TwitterIcon(props) {
  return (
    <Svg {...props}>
      <path d="M18.9 3h3.3l-7.2 8.2L23.5 21h-6.6l-5.2-6.8L5.7 21H2.4l7.7-8.8L1.5 3h6.8l4.7 6.2L18.9 3Zm-1.16 16.2h1.83L7.35 4.7H5.38l12.36 14.5Z" />
    </Svg>
  )
}

export function DownloadIcon(props) {
  return (
    <Svg {...props}>
      <path d="M12 3a1 1 0 0 1 1 1v9.59l2.3-2.3a1 1 0 1 1 1.4 1.42l-4 4a1 1 0 0 1-1.4 0l-4-4a1 1 0 1 1 1.4-1.42l2.3 2.3V4a1 1 0 0 1 1-1ZM5 19a1 1 0 0 1 1-1h12a1 1 0 1 1 0 2H6a1 1 0 0 1-1-1Z" />
    </Svg>
  )
}
