// Shared fetch helper for the booking-service / order-service / payment-service
// saga APIs -- same error shape (message + status + details) that
// searchApi.js / bookingApi.js already established for the legacy endpoint,
// so components can handle errors from either the same way.
async function request(path, options = {}) {
  const res = await fetch(path, {
    ...options,
    headers: { 'Content-Type': 'application/json', ...(options.headers ?? {}) },
  })

  if (!res.ok) {
    const body = await res.json().catch(() => null)
    const error = new Error(body?.message || `Request failed (HTTP ${res.status})`)
    error.status = res.status
    error.details = body?.details ?? []
    throw error
  }

  if (res.status === 204) return null
  return res.json()
}

export function apiGet(path) {
  return request(path, { method: 'GET' })
}

export function apiPost(path, body, headers) {
  return request(path, { method: 'POST', body: JSON.stringify(body), headers })
}

// GET that treats 404 as "not there yet" rather than an error -- the shape
// every poll loop in this app needs (an order/payment row that doesn't
// exist *yet* isn't a failure, it's a signal to keep waiting).
export async function apiGetOrNull(path) {
  const res = await fetch(path)
  if (res.status === 404) return null
  if (!res.ok) {
    const body = await res.json().catch(() => null)
    const error = new Error(body?.message || `Request failed (HTTP ${res.status})`)
    error.status = res.status
    throw error
  }
  return res.json()
}
