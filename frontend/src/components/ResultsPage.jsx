import { useMemo, useState } from 'react'
import FilterSidebar from './FilterSidebar.jsx'
import SortBar from './SortBar.jsx'
import FlightCard from './FlightCard.jsx'
import LoadingSkeleton from './LoadingSkeleton.jsx'
import EmptyState from './EmptyState.jsx'
import ErrorState from './ErrorState.jsx'

function timeBucketFor(isoLocalDateTime) {
  const hour = Number(isoLocalDateTime.split('T')[1].slice(0, 2))
  if (hour < 6) return 'EARLY_MORNING'
  if (hour < 12) return 'MORNING'
  if (hour < 18) return 'AFTERNOON'
  return 'EVENING'
}

function buildFacets(offers) {
  const prices = offers.map((o) => o.price.amount)
  const durations = offers.map((o) => o.totalDurationMinutes)
  const airlines = [...new Set(offers.flatMap((o) => o.airlines.map((a) => a.name)))].sort()

  return {
    minPrice: Math.floor(Math.min(...prices, 0)),
    maxPrice: Math.ceil(Math.max(...prices, 1)),
    minDuration: Math.floor(Math.min(...durations, 0)),
    maxDuration: Math.ceil(Math.max(...durations, 1)),
    airlines,
    currency: offers[0]?.price.currency ?? 'USD',
  }
}

function defaultFilters(facets) {
  return {
    stops: new Set(),
    maxPrice: facets.maxPrice,
    timeBuckets: new Set(),
    maxDuration: facets.maxDuration,
    airlines: new Set(),
  }
}

function applyFilters(offers, filters) {
  return offers.filter((offer) => {
    if (filters.stops.size > 0 && !filters.stops.has(Math.min(offer.stops, 2))) return false
    if (offer.price.amount > filters.maxPrice) return false
    if (offer.totalDurationMinutes > filters.maxDuration) return false
    if (filters.airlines.size > 0 && !offer.airlines.some((a) => filters.airlines.has(a.name))) return false
    if (filters.timeBuckets.size > 0) {
      const bucket = timeBucketFor(offer.slices[0].departureTime)
      if (!filters.timeBuckets.has(bucket)) return false
    }
    return true
  })
}

function sortOffers(offers, sortBy) {
  const sorted = [...offers]
  switch (sortBy) {
    case 'PRICE':
      return sorted.sort((a, b) => a.price.amount - b.price.amount)
    case 'DURATION':
      return sorted.sort((a, b) => a.totalDurationMinutes - b.totalDurationMinutes)
    case 'DEPARTURE_TIME':
      return sorted.sort((a, b) => a.slices[0].departureTime.localeCompare(b.slices[0].departureTime))
    case 'BEST':
    default:
      // A simple, transparent "best" blend of price and duration, normalized 0-1.
      return sorted.sort((a, b) => bestScore(a, sorted) - bestScore(b, sorted))
  }
}

function bestScore(offer, all) {
  const prices = all.map((o) => o.price.amount)
  const durations = all.map((o) => o.totalDurationMinutes)
  const priceRange = Math.max(...prices) - Math.min(...prices) || 1
  const durationRange = Math.max(...durations) - Math.min(...durations) || 1
  const priceScore = (offer.price.amount - Math.min(...prices)) / priceRange
  const durationScore = (offer.totalDurationMinutes - Math.min(...durations)) / durationRange
  return priceScore * 0.6 + durationScore * 0.4 + offer.stops * 0.05
}

export default function ResultsPage({ status, error, offers, onSelectOffer, onRetry }) {
  const facets = useMemo(() => (offers.length ? buildFacets(offers) : null), [offers])
  const [filters, setFilters] = useState(null)
  const [sortBy, setSortBy] = useState('BEST')

  const effectiveFilters = filters ?? (facets ? defaultFilters(facets) : null)

  const visibleOffers = useMemo(() => {
    if (!offers.length || !effectiveFilters) return []
    return sortOffers(applyFilters(offers, effectiveFilters), sortBy)
  }, [offers, effectiveFilters, sortBy])

  if (status === 'loading') {
    return (
      <div className="mx-auto max-w-3xl px-4 py-8">
        <LoadingSkeleton />
      </div>
    )
  }

  if (status === 'error') {
    return (
      <div className="mx-auto max-w-3xl px-4 py-8">
        <ErrorState message={error?.message} details={error?.details} onRetry={onRetry} />
      </div>
    )
  }

  if (status === 'success' && offers.length === 0) {
    return (
      <div className="mx-auto max-w-3xl px-4 py-8">
        <EmptyState />
      </div>
    )
  }

  if (status !== 'success' || !facets || !effectiveFilters) return null

  return (
    <div className="mx-auto flex max-w-6xl flex-col gap-6 px-4 py-8 md:flex-row">
      <FilterSidebar
        filters={effectiveFilters}
        facets={facets}
        onChange={(next) => setFilters(next ?? defaultFilters(facets))}
      />

      <div className="flex-1">
        <SortBar value={sortBy} onChange={setSortBy} resultCount={visibleOffers.length} />

        {visibleOffers.length === 0 ? (
          <EmptyState onResetFilters={() => setFilters(defaultFilters(facets))} />
        ) : (
          <div className="space-y-4">
            {visibleOffers.map((offer) => (
              <FlightCard key={offer.id} offer={offer} onSelect={onSelectOffer} />
            ))}
          </div>
        )}
      </div>
    </div>
  )
}
