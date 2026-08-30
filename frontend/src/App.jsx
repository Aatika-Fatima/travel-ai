import { useState } from 'react'
import BookingPage from './components/BookingPage.jsx'
import PortfolioHome from './components/portfolio/PortfolioHome.jsx'
import { searchFlights } from './api/searchApi.js'

const INITIAL_CRITERIA = {
  tripType: 'ONE_WAY',
  origin: 'DEL',
  destination: 'BOM',
  departureDate: new Date().toISOString().slice(0, 10),
  returnDate: '',
  passengers: { adults: 1, children: 0, infants: 0 },
  cabinClass: 'ECONOMY',
  directOnly: false,
}

function toSearchRequest(criteria) {
  return {
    origin: criteria.origin,
    destination: criteria.destination,
    departureDate: criteria.departureDate,
    returnDate: criteria.tripType === 'ROUND_TRIP' ? criteria.returnDate || null : null,
    tripType: criteria.tripType,
    cabinClass: criteria.cabinClass,
    passengers: criteria.passengers,
    sortBy: 'BEST',
    filters: criteria.directOnly ? { maxStops: 0 } : {},
  }
}

export default function App() {
  const [criteria, setCriteria] = useState(INITIAL_CRITERIA)
  const [hasSearched, setHasSearched] = useState(false)
  const [status, setStatus] = useState('idle')
  const [offers, setOffers] = useState([])
  const [error, setError] = useState(null)
  const [selectedOffer, setSelectedOffer] = useState(null)

  const runSearch = async (overrideCriteria) => {
    const activeCriteria = overrideCriteria ?? criteria
    if (!activeCriteria.origin || !activeCriteria.destination || !activeCriteria.departureDate) {
      setStatus('error')
      setError({ message: 'Please provide origin, destination and departure date.', details: [] })
      setHasSearched(true)
      return
    }

    setSelectedOffer(null)
    setHasSearched(true)
    setStatus('loading')
    setError(null)
    try {
      const result = await searchFlights(toSearchRequest(activeCriteria))
      setOffers(result.offers)
      setStatus('success')
    } catch (err) {
      setError({ message: err.message, details: err.details ?? [] })
      setStatus('error')
    }
  }

  const handleAssistantAction = (action) => {
    const nextCriteria = {
      ...criteria,
      origin: action.origin,
      destination: action.destination,
      departureDate: action.departureDate,
      tripType: action.tripType,
      returnDate: action.tripType === 'ROUND_TRIP' ? criteria.returnDate : '',
    }
    setCriteria(nextCriteria)
    runSearch(nextCriteria)
  }

  if (selectedOffer) {
    return (
      <div className="min-h-screen bg-slate-50">
        <div className="bg-brand-900 px-4 py-4">
          <div className="mx-auto flex max-w-6xl items-center justify-between gap-2">
            <button type="button" onClick={() => setSelectedOffer(null)} className="flex items-center gap-2 text-white">
              <span className="text-2xl">🚀</span>
              <span className="text-xl font-extrabold tracking-tight">
                Aatika <span className="font-light text-white/80">FlyStack</span>
              </span>
            </button>
          </div>
        </div>
        <BookingPage offer={selectedOffer} passengers={criteria.passengers} onBackToResults={() => setSelectedOffer(null)} />
      </div>
    )
  }

  // Portfolio is the only view -- the bio / AI-assistant page shell wraps both
  // the search form and its results (results replace the promo content in the
  // center column). Booking/payment hand off to the shared flow above once an
  // offer is actually selected (see the `selectedOffer` branch).
  return (
    <PortfolioHome
      criteria={criteria}
      onChange={setCriteria}
      onSubmit={runSearch}
      onAssistantAction={handleAssistantAction}
      hasSearched={hasSearched}
      status={status}
      offers={offers}
      error={error}
      onRetry={runSearch}
      onSelectOffer={(offer) => setSelectedOffer(offer)}
    />
  )
}
