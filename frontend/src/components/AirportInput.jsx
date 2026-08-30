import { useEffect, useRef, useState } from 'react'
import { searchAirports } from '../api/searchApi.js'

function useDebouncedValue(value, delayMs) {
  const [debounced, setDebounced] = useState(value)
  useEffect(() => {
    const timer = setTimeout(() => setDebounced(value), delayMs)
    return () => clearTimeout(timer)
  }, [value, delayMs])
  return debounced
}

export default function AirportInput({ label, value, onChange, placeholder }) {
  const [query, setQuery] = useState(value)
  const [results, setResults] = useState([])
  const [isOpen, setIsOpen] = useState(false)
  const [isLoading, setIsLoading] = useState(false)
  const [highlightedIndex, setHighlightedIndex] = useState(-1)
  const containerRef = useRef(null)
  const debouncedQuery = useDebouncedValue(query, 250)

  // Keep in sync when the parent changes the value from outside (e.g. the swap button).
  useEffect(() => {
    setQuery(value)
  }, [value])

  useEffect(() => {
    const term = debouncedQuery.trim()
    if (term.length < 2) {
      setResults([])
      setIsLoading(false)
      return
    }

    let cancelled = false
    setIsLoading(true)
    searchAirports(term)
      .then((airports) => {
        if (!cancelled) setResults(airports)
      })
      .catch(() => {
        if (!cancelled) setResults([])
      })
      .finally(() => {
        if (!cancelled) setIsLoading(false)
      })

    return () => {
      cancelled = true
    }
  }, [debouncedQuery])

  useEffect(() => {
    function handleClickOutside(event) {
      if (containerRef.current && !containerRef.current.contains(event.target)) {
        setIsOpen(false)
      }
    }
    document.addEventListener('mousedown', handleClickOutside)
    return () => document.removeEventListener('mousedown', handleClickOutside)
  }, [])

  const selectAirport = (airport) => {
    onChange(airport.iataCode)
    setQuery(airport.iataCode)
    setResults([])
    setIsOpen(false)
    setHighlightedIndex(-1)
  }

  const handleKeyDown = (event) => {
    if (!isOpen || results.length === 0) return
    if (event.key === 'ArrowDown') {
      event.preventDefault()
      setHighlightedIndex((index) => (index + 1) % results.length)
    } else if (event.key === 'ArrowUp') {
      event.preventDefault()
      setHighlightedIndex((index) => (index - 1 + results.length) % results.length)
    } else if (event.key === 'Enter') {
      if (highlightedIndex >= 0) {
        event.preventDefault()
        selectAirport(results[highlightedIndex])
      }
    } else if (event.key === 'Escape') {
      setIsOpen(false)
    }
  }

  const showDropdown = isOpen && query.trim().length >= 2

  return (
    <div ref={containerRef} className="relative flex-1">
      <label className="block">
        <span className="block text-xs font-semibold uppercase tracking-wide text-slate-500">{label}</span>
        <input
          type="text"
          value={query}
          placeholder={placeholder}
          autoComplete="off"
          onChange={(event) => {
            const next = event.target.value
            setQuery(next)
            onChange(next.toUpperCase())
            setIsOpen(true)
            setHighlightedIndex(-1)
          }}
          onFocus={() => setIsOpen(true)}
          onKeyDown={handleKeyDown}
          className="mt-1 w-full border-0 border-b-2 border-transparent bg-transparent p-0 text-[1.425rem] font-bold uppercase tracking-wide text-brand-900 placeholder:text-slate-300 focus:border-accent-500 focus:outline-none focus:ring-0"
        />
      </label>

      {showDropdown && (
        <div className="absolute left-0 top-full z-20 mt-2 w-80 max-w-[90vw] overflow-hidden rounded-xl border border-slate-200 bg-white py-1 shadow-xl">
          {isLoading && <div className="px-4 py-3 text-sm text-slate-400">Searching…</div>}
          {!isLoading && results.length === 0 && (
            <div className="px-4 py-3 text-sm text-slate-400">No airports found</div>
          )}
          {!isLoading &&
            results.map((airport, index) => (
              <button
                type="button"
                key={airport.iataCode}
                onMouseDown={(event) => event.preventDefault()}
                onClick={() => selectAirport(airport)}
                className={`flex w-full items-center justify-between gap-3 px-4 py-2.5 text-left text-sm transition-colors ${
                  index === highlightedIndex ? 'bg-accent-50' : 'hover:bg-slate-50'
                }`}
              >
                <span className="min-w-0">
                  <span className="block truncate font-semibold text-brand-900">
                    {airport.cityName || airport.name}
                  </span>
                  <span className="block truncate text-xs text-slate-500">{airport.name}</span>
                </span>
                <span className="shrink-0 rounded bg-brand-50 px-2 py-1 text-xs font-bold text-brand-900">
                  {airport.iataCode}
                </span>
              </button>
            ))}
        </div>
      )}
    </div>
  )
}
