import { apiPost } from './http.js'

// orderId here is order-service's own order id -- the naming quirk carried
// through from payment-service's own `bookingId` field (see order-service's
// PaymentEventConsumer step-note: it's payment-service's field name for
// what is, in this saga, order-service's order id).
export function createPaymentOrder(orderId, amountPaise, currency) {
  return apiPost('/payments', { bookingId: orderId, amountPaise, currency })
}

export function confirmPayment(orderId, { razorpayOrderId, razorpayPaymentId, razorpaySignature }) {
  return apiPost(`/payments/${orderId}/confirm`, { razorpayOrderId, razorpayPaymentId, razorpaySignature })
}

let checkoutScriptPromise = null

// Razorpay's Checkout.js isn't an npm package -- it's meant to be loaded
// from their CDN so the payment form itself is always served fresh from
// Razorpay, not bundled/cached by this app.
export function loadRazorpayCheckout() {
  if (window.Razorpay) return Promise.resolve()
  if (checkoutScriptPromise) return checkoutScriptPromise

  checkoutScriptPromise = new Promise((resolve, reject) => {
    const script = document.createElement('script')
    script.src = 'https://checkout.razorpay.com/v1/checkout.js'
    script.onload = () => resolve()
    script.onerror = () => {
      checkoutScriptPromise = null
      reject(new Error('Could not load the payment provider. Check your connection and try again.'))
    }
    document.body.appendChild(script)
  })
  return checkoutScriptPromise
}

// Opens Razorpay's Checkout modal. Resolves with the raw
// { razorpay_order_id, razorpay_payment_id, razorpay_signature } response on
// success; resolves with null if the customer dismisses the modal without
// paying (not an error -- see onFailure for genuine payment failures).
export async function openRazorpayCheckout({ keyId, razorpayOrderId, amountPaise, currency, name, description, prefill }) {
  await loadRazorpayCheckout()

  return new Promise((resolve) => {
    const checkout = new window.Razorpay({
      key: keyId,
      order_id: razorpayOrderId,
      amount: amountPaise,
      currency,
      name,
      description,
      prefill,
      modal: {
        // The customer closing the modal is a normal outcome (they can
        // retry), not a thrown error.
        ondismiss: () => resolve(null),
      },
      handler: (response) => resolve(response),
    })
    // Deliberately not wired to reject the promise: Checkout.js keeps the
    // same modal open after a failed attempt so the customer can retry with
    // a different card without leaving this screen. Settling (rejecting)
    // here on the first failure would permanently discard a later success
    // in the same session -- the modal's own error state already tells the
    // customer what went wrong; ondismiss/handler are the only two outcomes
    // this promise needs to represent.
    checkout.open()
  })
}
