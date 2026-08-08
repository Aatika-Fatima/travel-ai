const CURRENCY_FORMATTERS = new Map()

export function formatPrice(amount, currency) {
  if (!CURRENCY_FORMATTERS.has(currency)) {
    CURRENCY_FORMATTERS.set(
      currency,
      new Intl.NumberFormat('en-US', { style: 'currency', currency, maximumFractionDigits: 2 }),
    )
  }
  return CURRENCY_FORMATTERS.get(currency).format(amount)
}

export function formatDuration(totalMinutes) {
  const hours = Math.floor(totalMinutes / 60)
  const minutes = totalMinutes % 60
  return `${hours}h ${minutes.toString().padStart(2, '0')}m`
}

export function formatTime(isoLocalDateTime) {
  const [, time] = isoLocalDateTime.split('T')
  return time.slice(0, 5)
}

export function formatDateLabel(isoLocalDateTime) {
  const date = new Date(isoLocalDateTime)
  return date.toLocaleDateString('en-US', { weekday: 'short', month: 'short', day: 'numeric' })
}

export function stopsLabel(stops) {
  if (stops === 0) return 'Non-stop'
  if (stops === 1) return '1 stop'
  return `${stops} stops`
}
