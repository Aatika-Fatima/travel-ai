import { useMemo, useState } from 'react'
import TravelerForm from './TravelerForm.jsx'
import OrderStatusWait from './OrderStatusWait.jsx'
import PaymentScreen from './PaymentScreen.jsx'
import OrderConfirmation from './OrderConfirmation.jsx'
import { createBooking, pollForConfirmedOrder } from '../api/orderSagaApi.js'
import { formatDuration, formatPrice, formatTime, stopsLabel } from '../utils/format.js'

function emptyTraveler() {
  return { title: 'mr', givenName: '', familyName: '', dateOfBirth: '', gender: 'm', email: '', phoneNumber: '' }
}

function travelerCategories(passengers) {
  const categories = []
  for (let i = 0; i < passengers.adults; i += 1) categories.push('Adult')
  for (let i = 0; i < passengers.children; i += 1) categories.push('Child')
  for (let i = 0; i < passengers.infants; i += 1) categories.push('Infant')
  return categories.length > 0 ? categories : ['Adult']
}

function validate(travelers, contact) {
  const errors = travelers.map((traveler) => {
    const fieldErrors = {}
    if (!traveler.givenName.trim()) fieldErrors.givenName = 'Required'
    if (!traveler.familyName.trim()) fieldErrors.familyName = 'Required'
    if (!traveler.dateOfBirth) fieldErrors.dateOfBirth = 'Required'
    if (!traveler.email.trim()) fieldErrors.email = 'Required'
    if (!traveler.phoneNumber.trim()) fieldErrors.phoneNumber = 'Required'
    return fieldErrors
  })

  const contactErrors = {}
  if (!contact.email.trim()) contactErrors.email = 'Required'
  if (!contact.phoneNumber.trim()) contactErrors.phoneNumber = 'Required'

  const isValid = errors.every((e) => Object.keys(e).length === 0) && Object.keys(contactErrors).length === 0
  return { errors, contactErrors, isValid }
}

function FlightSummaryBanner({ offer }) {
  return (
    <div className="rounded-2xl bg-brand-900 p-5 text-white shadow-lg">
      {offer.slices.map((slice, index) => {
        const airline = slice.segments[0]?.airline
        return (
          <div key={index} className={`flex items-center justify-between ${index > 0 ? 'mt-3 border-t border-white/10 pt-3' : ''}`}>
            <div className="flex items-center gap-3">
              <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded bg-white/10 text-xs font-bold">
                {airline?.iataCode ?? '—'}
              </div>
              <div>
                <div className="text-sm font-semibold">{airline?.name ?? 'Unknown airline'}</div>
                <div className="text-xs text-white/60">
                  {stopsLabel(slice.stops)} · {formatDuration(slice.durationMinutes)}
                </div>
              </div>
            </div>
            <div className="flex items-center gap-4 text-right sm:gap-6">
              <div>
                <div className="text-lg font-bold">{formatTime(slice.departureTime)}</div>
                <div className="text-xs text-white/60">{slice.origin.iataCode}</div>
              </div>
              <div className="text-white/40">→</div>
              <div>
                <div className="text-lg font-bold">{formatTime(slice.arrivalTime)}</div>
                <div className="text-xs text-white/60">{slice.destination.iataCode}</div>
              </div>
            </div>
          </div>
        )
      })}
    </div>
  )
}

function ContactField({ label, value, onChange, error, type = 'text' }) {
  return (
    <label>
      <span className="block text-xs font-semibold uppercase tracking-wide text-slate-500">{label}</span>
      <input
        type={type}
        value={value}
        onChange={(event) => onChange(event.target.value)}
        className={`mt-1 w-full rounded-lg border bg-white px-3 py-2 text-sm font-medium text-brand-900 focus:outline-none focus:ring-1 ${
          error ? 'border-red-300 focus:border-red-400 focus:ring-red-400' : 'border-slate-200 focus:border-accent-500 focus:ring-accent-500'
        }`}
      />
      {error && <span className="mt-1 block text-xs text-red-600">{error}</span>}
    </label>
  )
}

function FareSummaryCard({ offer, status }) {
  return (
    <div className="w-full shrink-0 lg:w-80">
      <div className="sticky top-6 rounded-2xl border border-slate-100 bg-white p-5 shadow-sm">
        <h3 className="text-sm font-bold text-brand-900">Fare Summary</h3>
        <div className="mt-3 flex items-center justify-between text-sm text-slate-600">
          <span>Base Fare</span>
          <span>{formatPrice(offer.price.amount, offer.price.currency)}</span>
        </div>
        <div className="mt-4 flex items-center justify-between border-t border-slate-100 pt-3 text-base font-bold text-brand-900">
          <span>Total Amount</span>
          <span>{formatPrice(offer.price.amount, offer.price.currency)}</span>
        </div>

        <button
          type="submit"
          disabled={status === 'submitting'}
          className="mt-4 w-full rounded-xl bg-accent-500 px-6 py-3 text-sm font-bold text-white shadow-md shadow-accent-500/30 transition-colors hover:bg-accent-600 disabled:cursor-not-allowed disabled:opacity-60"
        >
          {status === 'submitting' ? 'Booking…' : 'Continue to Payment'}
        </button>
      </div>
    </div>
  )
}

export default function BookingPage({ offer, passengers, onBackToResults }) {
  const categories = useMemo(() => travelerCategories(passengers), [passengers])
  const [travelers, setTravelers] = useState(() => categories.map(() => emptyTraveler()))
  const [contact, setContact] = useState({ email: '', phoneNumber: '' })
  const [sameAsFirstTraveler, setSameAsFirstTraveler] = useState(true)
  const [status, setStatus] = useState('form')
  const [validation, setValidation] = useState(null)
  const [bookingError, setBookingError] = useState(null)
  const [orderStatus, setOrderStatus] = useState(null)
  const [order, setOrder] = useState(null)
  // Generated once per mount, not per submit -- a retry after a transient
  // network error re-sends the SAME key, so booking-service and
  // order-service both dedupe the retry instead of creating a second order.
  const [idempotencyKey] = useState(() => crypto.randomUUID())

  const updateTraveler = (index, next) => {
    setTravelers((prev) => prev.map((traveler, i) => (i === index ? next : traveler)))
  }

  const effectiveContact =
    sameAsFirstTraveler && travelers[0]
      ? { email: travelers[0].email, phoneNumber: travelers[0].phoneNumber }
      : contact

  const handleSubmit = async (event) => {
    event.preventDefault()
    const { errors, contactErrors, isValid } = validate(travelers, effectiveContact)
    setValidation({ errors, contactErrors })
    if (!isValid) return

    setStatus('submitting')
    setBookingError(null)
    try {
      await createBooking(idempotencyKey, {
        offerId: offer.id,
        passengers: travelers.map((traveler) => ({
          title: traveler.title,
          givenName: traveler.givenName,
          familyName: traveler.familyName,
          dateOfBirth: traveler.dateOfBirth,
          gender: traveler.gender,
          email: traveler.email,
          phoneNumber: traveler.phoneNumber,
        })),
        contact: effectiveContact,
      })

      setStatus('waiting-order')
      const outcome = await pollForConfirmedOrder(idempotencyKey, {
        onTick: (polledOrder) => setOrderStatus(polledOrder?.status ?? null),
      })

      if (outcome.outcome === 'confirmed') {
        setOrder(outcome.order)
        setStatus('payment')
      } else if (outcome.outcome === 'failed') {
        setBookingError({
          message: `The airline could not confirm this booking (${outcome.order.status.toLowerCase().replaceAll('_', ' ')}).`,
          details: [],
        })
        setStatus('error')
      } else {
        setBookingError({ message: 'This is taking longer than expected. Please try again shortly.', details: [] })
        setStatus('error')
      }
    } catch (err) {
      setBookingError({ message: err.message, details: err.details ?? [] })
      setStatus('error')
    }
  }

  if (status === 'waiting-order') {
    return <OrderStatusWait orderStatus={orderStatus} />
  }

  if (status === 'payment' && order) {
    return (
      <PaymentScreen
        orderId={order.orderId}
        amount={offer.price.amount}
        currency={offer.price.currency}
        contactEmail={effectiveContact.email}
        contactPhone={effectiveContact.phoneNumber}
        onPaid={() => setStatus('paid')}
      />
    )
  }

  if (status === 'paid' && order) {
    return (
      <OrderConfirmation
        order={order}
        amount={offer.price.amount}
        currency={offer.price.currency}
        contactEmail={effectiveContact.email}
        onBackToSearch={onBackToResults}
      />
    )
  }

  return (
    <div className="mx-auto max-w-6xl px-4 py-8">
      <button
        type="button"
        onClick={onBackToResults}
        className="mb-4 text-sm font-semibold text-brand-900 hover:text-accent-500"
      >
        ← Back to results
      </button>

      <FlightSummaryBanner offer={offer} />

      <form onSubmit={handleSubmit} className="mt-6 flex flex-col gap-6 lg:flex-row">
        <div className="flex-1 space-y-4">
          <h2 className="text-lg font-bold text-brand-900">Traveller Details</h2>
          {travelers.map((traveler, index) => (
            <TravelerForm
              key={index}
              index={index}
              category={categories[index]}
              value={traveler}
              onChange={(next) => updateTraveler(index, next)}
              errors={validation?.errors[index] ?? {}}
            />
          ))}

          <div className="rounded-2xl border border-slate-100 bg-white p-5 shadow-sm">
            <div className="mb-3 flex items-center justify-between">
              <h3 className="text-sm font-bold text-brand-900">Contact Details</h3>
              <label className="flex items-center gap-1.5 text-xs text-slate-500">
                <input
                  type="checkbox"
                  checked={sameAsFirstTraveler}
                  onChange={(event) => setSameAsFirstTraveler(event.target.checked)}
                  className="h-3.5 w-3.5 rounded border-slate-300 text-accent-500 focus:ring-accent-500"
                />
                Same as Traveller 1
              </label>
            </div>
            {sameAsFirstTraveler ? (
              <p className="text-sm text-slate-500">Booking confirmation will be sent to the first traveller's email.</p>
            ) : (
              <div className="grid grid-cols-2 gap-4">
                <ContactField
                  label="Email"
                  type="email"
                  value={contact.email}
                  onChange={(v) => setContact((c) => ({ ...c, email: v }))}
                  error={validation?.contactErrors.email}
                />
                <ContactField
                  label="Phone Number"
                  type="tel"
                  value={contact.phoneNumber}
                  onChange={(v) => setContact((c) => ({ ...c, phoneNumber: v }))}
                  error={validation?.contactErrors.phoneNumber}
                />
              </div>
            )}
          </div>

          {status === 'error' && bookingError && (
            <div className="rounded-xl border border-red-100 bg-red-50 p-4 text-sm text-red-700">
              <div className="font-semibold">Booking failed</div>
              <div className="mt-1">{bookingError.message}</div>
              {bookingError.details.length > 0 && (
                <ul className="mt-1 list-disc pl-5 text-xs text-red-600">
                  {bookingError.details.map((detail) => (
                    <li key={detail}>{detail}</li>
                  ))}
                </ul>
              )}
            </div>
          )}
        </div>

        <FareSummaryCard offer={offer} status={status} />
      </form>
    </div>
  )
}
