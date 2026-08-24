import { useState } from 'react'
import { confirmPayment, createPaymentOrder, openRazorpayCheckout } from '../api/paymentApi.js'
import { formatPrice } from '../utils/format.js'

export default function PaymentScreen({ orderId, amount, currency, contactEmail, contactPhone, onPaid }) {
  const [status, setStatus] = useState('idle') // idle | processing | error
  const [error, setError] = useState(null)

  const amountPaise = Math.round(amount * 100)

  const handlePay = async () => {
    setStatus('processing')
    setError(null)
    try {
      const paymentOrder = await createPaymentOrder(orderId, amountPaise, currency)

      const checkoutResult = await openRazorpayCheckout({
        keyId: paymentOrder.keyId,
        razorpayOrderId: paymentOrder.razorpayOrderId,
        amountPaise: paymentOrder.amountPaise,
        currency: paymentOrder.currency,
        name: 'SkyFare',
        description: 'Flight booking payment',
        prefill: { email: contactEmail, contact: contactPhone },
      })

      if (checkoutResult === null) {
        // Customer closed the Razorpay modal without paying.
        setStatus('idle')
        return
      }

      await confirmPayment(orderId, {
        razorpayOrderId: checkoutResult.razorpay_order_id,
        razorpayPaymentId: checkoutResult.razorpay_payment_id,
        razorpaySignature: checkoutResult.razorpay_signature,
      })

      onPaid()
    } catch (err) {
      setError(err.message || 'Payment failed. Please try again.')
      setStatus('error')
    }
  }

  return (
    <div className="mx-auto max-w-md px-4 py-12">
      <div className="rounded-2xl border border-slate-100 bg-white p-8 text-center shadow-xl">
        <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-brand-50 text-3xl">💳</div>
        <h2 className="mt-4 text-2xl font-extrabold text-brand-900">Your flight is confirmed</h2>
        <p className="mt-1 text-sm text-slate-500">Complete payment to receive your ticket.</p>

        <div className="mt-6 flex items-center justify-between rounded-xl bg-brand-50 p-4 text-left">
          <span className="text-sm font-semibold text-slate-600">Amount due</span>
          <span className="text-xl font-black text-brand-900">{formatPrice(amount, currency)}</span>
        </div>

        {status === 'error' && error && (
          <div className="mt-4 rounded-xl border border-red-100 bg-red-50 p-3 text-left text-sm text-red-700">{error}</div>
        )}

        <button
          type="button"
          onClick={handlePay}
          disabled={status === 'processing'}
          className="mt-6 w-full rounded-xl bg-accent-500 px-6 py-3 text-sm font-bold text-white shadow-md shadow-accent-500/30 transition-colors hover:bg-accent-600 disabled:cursor-not-allowed disabled:opacity-60"
        >
          {status === 'processing' ? 'Processing…' : 'Pay Now'}
        </button>
        <p className="mt-3 text-xs text-slate-400">Payments are securely processed by Razorpay.</p>

        {/* Dev-only: stripped from production builds by Vite. Razorpay's
            checkout is their own hosted iframe -- there's no supported way
            to pre-fill it from this page (deliberate, for PCI compliance),
            so this is just a copy-paste reference for local test-mode runs. */}
        {import.meta.env.DEV && (
          <div className="mt-4 rounded-xl border border-amber-100 bg-amber-50 p-3 text-left text-xs text-amber-800">
            <div className="font-semibold">Test mode — Razorpay sandbox card</div>
            <div className="mt-1 space-y-0.5 font-mono">
              <div>Card: 5267 3181 8797 5449 (Mastercard, domestic)</div>
              <div>Expiry: any future date · CVV: any 3 digits</div>
              <div>OTP: 1111</div>
            </div>
          </div>
        )}
      </div>
    </div>
  )
}
