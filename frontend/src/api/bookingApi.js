export async function bookFlight(request) {
  const res = await fetch('/api/v1/flights/book', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request),
  })

  if (!res.ok) {
    const body = await res.json().catch(() => null)
    const error = new Error(body?.message || `Booking failed (HTTP ${res.status})`)
    error.status = res.status
    error.details = body?.details ?? []
    throw error
  }

  return res.json()
}
