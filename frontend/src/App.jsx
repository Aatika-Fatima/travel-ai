import { useEffect, useState } from 'react'
import SearchForm from './components/SearchForm.jsx'
import ResultsPage from './components/ResultsPage.jsx'
import AssistantPanel from './components/AssistantPanel.jsx'
import BookingPage from './components/BookingPage.jsx'
import ThemeToggle from './components/ThemeToggle.jsx'
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

function useTheme() {
  const [theme, setTheme] = useState(() => localStorage.getItem('skyfare-theme') || 'skyfare')

  useEffect(() => {
    document.documentElement.dataset.theme = theme
    localStorage.setItem('skyfare-theme', theme)
  }, [theme])

  return [theme, setTheme]
}

export default function App() {
  const [theme, setTheme] = useTheme()
  const [criteria, setCriteria] = useState(INITIAL_CRITERIA)
  const [hasSearched, setHasSearched] = useState(false)
  const [status, setStatus] = useState('idle')
  const [offers, setOffers] = useState([])
  const [error, setError] = useState(null)
  const [isAssistantOpen, setIsAssistantOpen] = useState(false)
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

  const goHome = () => {
    setSelectedOffer(null)
    setHasSearched(false)
    setStatus('idle')
    setError(null)
  }

  if (selectedOffer) {
    return (
      <div className="min-h-screen bg-slate-50">
        <div className="bg-brand-900 px-4 py-4">
          <div className="mx-auto flex max-w-6xl items-center justify-between gap-2">
            <button type="button" onClick={goHome} className="flex items-center gap-2 text-white">
              <span className="text-2xl">✈️</span>
              <span className="text-xl font-extrabold tracking-tight">SkyFare</span>
            </button>
            <ThemeToggle theme={theme} onChange={setTheme} dark />
          </div>
        </div>
        <BookingPage offer={selectedOffer} passengers={criteria.passengers} onBackToResults={() => setSelectedOffer(null)} />
      </div>
    )
  }

  // The portfolio theme keeps its own page shell (bio, AI assistant
  // sidebar, tech-stack footer) for search AND results -- results replace
  // the promo content in the center column instead of navigating to a
  // separate page. Booking/payment still hand off to the shared flow below
  // once an offer is actually selected (see the `selectedOffer` branch).
  if (theme === 'portfolio') {
    return (
      <PortfolioHome
        theme={theme}
        onThemeChange={setTheme}
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

  return (
    <div className="flex min-h-screen bg-slate-50">
      <AssistantPanel open={isAssistantOpen} onClose={() => setIsAssistantOpen(false)} onAction={handleAssistantAction} />

      <div className="min-w-0 flex-1">
        <header className={`bg-brand-900 ${hasSearched ? 'pb-6 pt-6' : 'pb-20 pt-10'}`}>
          <div className="mx-auto max-w-6xl px-4">
            <div className="mb-6 flex items-center justify-between text-white">
              <button type="button" onClick={goHome} className="flex items-center gap-2">
                <span className="text-2xl">✈️</span>
                <span className="text-xl font-extrabold tracking-tight">SkyFare</span>
              </button>
              <div className="flex items-center gap-3">
                <ThemeToggle theme={theme} onChange={setTheme} dark />
                <button
                  type="button"
                  onClick={() => setIsAssistantOpen(true)}
                  className="flex items-center gap-1.5 rounded-full border border-white/20 bg-white/10 px-4 py-1.5 text-sm font-semibold text-white transition-colors hover:bg-white/20"
                >
                  <span>✨</span> Ask AI
                </button>
              </div>
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
            onSelectOffer={(offer) => setSelectedOffer(offer)}
          />
        )}
      </div>
    </div>
  )
}
