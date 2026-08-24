import { apiGetOrNull, apiPost } from './http.js'

// booking-service's own booking id is never used again after this call --
// order-service's orders.idempotency_key is reused unchanged, end to end
// across booking-service -> Kafka -> order-service (see order-service's own
// BookingSagaConsumer step-note), so the SAME client-generated key doubles
// as the correlation id order-service is polled by below.
export function createBooking(idempotencyKey, request) {
  return apiPost('/bookings', request, { 'Idempotency-Key': idempotencyKey })
}

// null means "no order yet for this key" -- not found is the expected,
// normal state while the booking.events -> order-service leg of the saga is
// still in flight, not an error.
export function getOrderByIdempotencyKey(idempotencyKey) {
  return apiGetOrNull(`/orders/by-idempotency-key/${encodeURIComponent(idempotencyKey)}`)
}

const TERMINAL_FAILURE_STATUSES = new Set(['PAYMENT_FAILED', 'FAILED', 'CANCELLED'])

// Polls until order-service has an order for this key that's reached
// CONFIRMED (Duffel booked it, ready for payment) or a terminal failure
// status. Resolves { outcome: 'confirmed', order } | { outcome: 'failed', order }
// | { outcome: 'timeout' }.
export async function pollForConfirmedOrder(idempotencyKey, { intervalMs = 2000, timeoutMs = 120000, onTick } = {}) {
  const deadline = Date.now() + timeoutMs
  while (Date.now() < deadline) {
    const order = await getOrderByIdempotencyKey(idempotencyKey)
    onTick?.(order)
    if (order) {
      if (order.status === 'CONFIRMED') return { outcome: 'confirmed', order }
      if (TERMINAL_FAILURE_STATUSES.has(order.status)) return { outcome: 'failed', order }
    }
    await new Promise((resolve) => setTimeout(resolve, intervalMs))
  }
  return { outcome: 'timeout' }
}
