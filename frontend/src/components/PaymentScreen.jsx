import { useEffect, useState } from 'react'
import { confirmPayment, createPaymentOrder, openRazorpayCheckout } from '../api/paymentApi.js'
import { formatPrice } from '../utils/format.js'
import { copyText, DEFAULT_TEST_CARD, loadTestCard, saveTestCard } from '../utils/testCard.js'

// Dev-only popup for local Razorpay test-mode runs. Razorpay's hosted
// checkout is a cross-origin iframe -- our page can't pre-fill it or even
// see which field is focused -- so this walks through the values one at a
// time, in the order the checkout asks for them, auto-copying each so the
// next paste is ready. Stripped from production builds (callers gate on DEV).
const STEPS = [
  { key: 'number', label: 'Card number', target: '“Card Number”' },
  { key: 'expiry', label: 'Expiry', target: '“MM / YY”' },
  { key: 'cvv', label: 'CVV', target: '“CVV”' },
  { key: 'otp', label: 'OTP', target: 'OTP prompt' },
]

function TestCardPopup({ card, onSave, onClose }) {
  const [step, setStep] = useState(0)
  const [copied, setCopied] = useState(false)
  const [editing, setEditing] = useState(false)
  const [draft, setDraft] = useState(card)
  const [savedTick, setSavedTick] = useState(false)

  const current = STEPS[step]
  const rawValue = String(card[current.key] ?? '').replace(/\s+/g, '')

  // Copy the current step's value whenever the step changes -- ready to
  // paste as soon as you click that field in the Razorpay window.
  useEffect(() => {
    if (editing) return
    let active = true
    copyText(rawValue).then((ok) => {
      if (active) setCopied(ok)
    })
    return () => {
      active = false
    }
  }, [rawValue, editing])

  const recopy = async () => setCopied(await copyText(rawValue))

  const editField = (key, label) => (
    <label className="flex flex-col gap-1 text-xs font-sans font-semibold text-amber-700">
      {label}
      <input
        value={draft[key]}
        onChange={(e) => setDraft({ ...draft, [key]: e.target.value })}
        className="rounded-lg border border-amber-300 px-2 py-1 font-mono text-sm text-amber-900"
      />
    </label>
  )

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 p-4"
      role="dialog"
      aria-modal="true"
      aria-label="Razorpay test card details"
      onClick={onClose}
    >
      <div
        className="w-full max-w-sm rounded-2xl border border-amber-200 bg-white p-6 shadow-2xl"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex items-start justify-between">
          <h3 className="text-base font-bold text-amber-900">Test mode — domestic test card</h3>
          <button
            type="button"
            onClick={onClose}
            aria-label="Close"
            className="-mr-1 -mt-1 rounded-lg p-1 text-slate-400 hover:bg-slate-100 hover:text-slate-600"
          >
            ✕
          </button>
        </div>

        {editing ? (
          <>
            <p className="mt-1 text-xs text-slate-500">Change the saved test card.</p>
            <div className="mt-4 space-y-3 rounded-xl bg-amber-50 p-4">
              {editField('number', 'Card number')}
              {editField('expiry', 'Expiry')}
              {editField('cvv', 'CVV')}
              {editField('otp', 'OTP')}
              <div className="flex gap-2 pt-1">
                <button
                  type="button"
                  onClick={() => {
                    onSave(draft)
                    setEditing(false)
                    setStep(0)
                    setSavedTick(true)
                    setTimeout(() => setSavedTick(false), 1500)
                  }}
                  className="flex-1 rounded-lg bg-amber-500 px-3 py-1.5 text-xs font-sans font-bold text-white hover:bg-amber-600"
                >
                  Save as default
                </button>
                <button
                  type="button"
                  onClick={() => setDraft({ ...DEFAULT_TEST_CARD })}
                  className="rounded-lg border border-amber-300 px-3 py-1.5 text-xs font-sans font-semibold text-amber-700 hover:bg-amber-100"
                >
                  Reset
                </button>
              </div>
            </div>
            <button
              type="button"
              onClick={() => {
                setDraft(card)
                setEditing(false)
              }}
              className="mt-4 text-xs font-semibold text-amber-700 underline decoration-dotted underline-offset-2 hover:text-amber-800"
            >
              Cancel
            </button>
          </>
        ) : (
          <>
            <div className="mt-3 flex items-center gap-1.5">
              {STEPS.map((s, i) => (
                <span
                  key={s.key}
                  className={`h-1.5 flex-1 rounded-full ${i <= step ? 'bg-amber-500' : 'bg-amber-100'}`}
                />
              ))}
            </div>

            <div className="mt-4 rounded-xl bg-amber-50 p-4 text-center">
              <div className="text-[11px] font-semibold uppercase tracking-wide text-amber-700">
                Step {step + 1} of {STEPS.length} · {current.label}
              </div>
              <div className="mt-2 font-mono text-2xl font-bold tracking-wide text-amber-900">
                {card[current.key]}
              </div>
              <div className="mt-2 text-xs text-amber-700">
                {copied ? '✓ copied to clipboard' : (
                  <button type="button" onClick={recopy} className="underline underline-offset-2">
                    Copy to clipboard
                  </button>
                )}
              </div>
              <p className="mt-3 text-xs text-slate-500">
                Paste into Razorpay's {current.target} field.
              </p>
            </div>

            <div className="mt-4 flex gap-2">
              <button
                type="button"
                onClick={() => setStep((s) => Math.max(0, s - 1))}
                disabled={step === 0}
                className="rounded-xl border border-amber-300 px-4 py-2.5 text-sm font-bold text-amber-700 hover:bg-amber-50 disabled:cursor-not-allowed disabled:opacity-40"
              >
                Back
              </button>
              <button
                type="button"
                onClick={() => (step === STEPS.length - 1 ? onClose() : setStep((s) => s + 1))}
                className="flex-1 rounded-xl bg-amber-500 px-6 py-2.5 text-sm font-bold text-white hover:bg-amber-600"
              >
                {step === STEPS.length - 1 ? 'Done' : `Next: ${STEPS[step + 1].label} →`}
              </button>
            </div>

            <div className="mt-4 flex items-center justify-between">
              <button
                type="button"
                onClick={() => {
                  setDraft(card)
                  setEditing(true)
                }}
                className="text-xs font-semibold text-amber-700 underline decoration-dotted underline-offset-2 hover:text-amber-800"
              >
                Edit card
              </button>
              {savedTick && <span className="text-xs font-semibold text-green-600">Saved ✓</span>}
            </div>
          </>
        )}
      </div>
    </div>
  )
}

export default function PaymentScreen({ orderId, amount, currency, contactEmail, contactPhone, onPaid }) {
  const [status, setStatus] = useState('idle') // idle | processing | error
  const [error, setError] = useState(null)
  const [showTestCard, setShowTestCard] = useState(import.meta.env.DEV)
  const [testCard, setTestCard] = useState(() => (import.meta.env.DEV ? loadTestCard() : DEFAULT_TEST_CARD))

  const amountPaise = Math.round(amount * 100)

  const handleSaveCard = (card) => {
    saveTestCard(card)
    setTestCard(card)
  }

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
        // `method: 'card'` opens Razorpay straight on the card form, skipping
        // the payment-method chooser. Card number itself can't be prefilled.
        prefill: { email: contactEmail, contact: contactPhone, method: 'card' },
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

        {import.meta.env.DEV && (
          <button
            type="button"
            onClick={() => setShowTestCard(true)}
            className="mt-4 text-xs font-semibold text-amber-700 underline decoration-dotted underline-offset-2 hover:text-amber-800"
          >
            Show test card details
          </button>
        )}
      </div>

      {import.meta.env.DEV && showTestCard && (
        <TestCardPopup card={testCard} onSave={handleSaveCard} onClose={() => setShowTestCard(false)} />
      )}
    </div>
  )
}
