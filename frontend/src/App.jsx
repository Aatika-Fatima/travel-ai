import { useState } from 'react'
import SearchForm from './components/SearchForm.jsx'
import ResultsPage from './components/ResultsPage.jsx'
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

  const runSearch = async () => {
    if (!criteria.origin || !criteria.destination || !criteria.departureDate) {
      setStatus('error')
      setError({ message: 'Please provide origin, destination and departure date.', details: [] })
      setHasSearched(true)
      return
    }

    setHasSearched(true)
    setStatus('loading')
    setError(null)
    try {
      const result = await searchFlights(toSearchRequest(criteria))
      setOffers(result.offers)
      setStatus('success')
    } catch (err) {
      setError({ message: err.message, details: err.details ?? [] })
      setStatus('error')
    }
  }

  return (
    <div className="min-h-screen bg-slate-50">
      <header className={`bg-brand-900 ${hasSearched ? 'pb-6 pt-6' : 'pb-20 pt-10'}`}>
        <div className="mx-auto max-w-6xl px-4">
          <div className="mb-6 flex items-center gap-2 text-white">
            <span className="text-2xl">✈️</span>
            <span className="text-xl font-extrabold tracking-tight">SkyFare</span>
          </div>

          {!hasSearched && (
            <div className="mb-8 text-white">
              <h1 className="text-3xl font-extrabold md:text-4xl">Find your next flight</h1>
              <p className="mt-2 text-brand-50/80">Search hundreds of airlines for the best fares, in real time.</p>
            </div>
          )}

          <SearchForm criteria={criteria} onChange={setCriteria} onSubmit={runSearch} compact={hasSearched} />
        </div>
      </header>

      {hasSearched && (
        <ResultsPage
          status={status}
          error={error}
          offers={offers}
          onRetry={runSearch}
          onSelectOffer={(offer) => window.alert(`Booking flow for offer ${offer.id} is not implemented yet.`)}
        />
      )}
    </div>
  )
}
