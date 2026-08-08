# Engineering Prompt: Duffel-Powered Flight Search (Multi-Module Maven Project)

Use this as the prompt/spec to hand to a developer or coding assistant (e.g. Claude Code) to scaffold and implement the project.

---

## Objective

Build a multi-module Maven project that integrates with the **Duffel Flights API** to search for flights and expose that data through a professional, MakeMyTrip-style web UI. Shared models/DTOs used by both the Duffel integration and the search module must live in a dedicated `common` module to avoid duplication and circular dependencies.

## Project / Module Structure

```
flight-platform (parent pom, packaging=pom)
├── common                  (shared DTOs, enums, constants, mappers, utils)
├── duffel-flight-search    (integration layer with Duffel API)
└── flight-search           (consumer module: search UI + orchestration/API layer)
```

Parent `pom.xml` should declare:
- `<modules>` for `common`, `duffel-flight-search`, `flight-search`
- Shared dependency management (Spring Boot BOM, Jackson, Lombok, MapStruct, JUnit5, WireMock/Mockito for tests)
- Common plugin management (compiler, surefire, spring-boot-maven-plugin where applicable)
- Consistent Java version (Java 17+) and project properties across modules

---

## Module 1: `common`

Purpose: hold everything shared between `duffel-flight-search` and `flight-search` so neither module depends on the other's internals.

Should contain:
- **DTOs / POJOs** representing a normalized flight search request and response (independent of Duffel's raw API shape), e.g.:
  - `FlightSearchRequest` — origin, destination, departureDate, returnDate (nullable for one-way), tripType (ONE_WAY/ROUND_TRIP/MULTI_CITY), cabinClass (ECONOMY/PREMIUM_ECONOMY/BUSINESS/FIRST), passengers (adults, children, infants), currency, maxConnections, preferredAirlines (optional), sortBy (PRICE/DURATION/DEPARTURE_TIME), filters (stops, airlines, price range, departure/arrival time windows, baggage included)
  - `FlightOffer` — offer id, price (amount + currency), airline(s), flight number(s), origin/destination airports (IATA codes + names), departure/arrival timestamps, duration, number of stops, layover details, cabin class, baggage allowance, fare rules/refundability, seats remaining
  - `Segment` / `Slice` — per-leg detail (used for multi-city / connections)
  - `Airport`, `Airline`, `Passenger` value objects
- **Enums**: `TripType`, `CabinClass`, `PassengerType`, `SortOption`
- **Common exceptions**: `FlightSearchException`, `ExternalApiException`, `ValidationException`
- **Utility classes**: date/time formatting (ISO 8601 handling), currency formatting, IATA code validators
- **Constants**: default currency, default page size, timeout values
- No Spring Boot web/controller code here — this module should be a plain library (packaging=jar) with minimal dependencies (Lombok, Jackson annotations, validation annotations).

---

## Module 2: `duffel-flight-search`

Purpose: encapsulate all interaction with the **Duffel API** (https://duffel.com/docs/api) and translate Duffel's raw request/response format into the shared `common` DTOs.

Requirements:
- **Duffel API client** using `RestTemplate` / `WebClient` / `Retrofit` (pick one — WebClient recommended for non-blocking calls), configured with:
  - Base URL: `https://api.duffel.com`
  - Auth header: `Authorization: Bearer <DUFFEL_ACCESS_TOKEN>` (token from application config/environment variable, never hardcoded)
  - Required headers: `Duffel-Version`, `Content-Type: application/json`, `Accept: application/json`
- **Offer Requests flow** (per Duffel's two-step search):
  1. `POST /air/offer_requests` — create an offer request from search criteria (slices, passengers, cabin_class)
  2. `GET /air/offer_requests/{id}` or `GET /air/offers?offer_request_id={id}` — retrieve resulting offers, with pagination (`limit`, `after` cursor)
- **Mapping layer** (MapStruct or manual mappers) converting:
  - `common.FlightSearchRequest` → Duffel `CreateOfferRequestPayload`
  - Duffel `Offer` response JSON → `common.FlightOffer`
- **Error handling**: map Duffel error responses (400/401/422/429/5xx) to the `common` exception types; implement retry/backoff for rate limiting (429) and idempotency key usage on POST requests
- **Config**: externalize base URL, API token, timeout, retry count via `application.yml` / environment variables; provide a sandbox/test mode toggle (Duffel provides a test API)
- **Caching (optional but recommended)**: short-lived cache of offer requests to avoid duplicate calls for identical search criteria within a session
- **Unit/integration tests**: mock Duffel responses (WireMock) covering successful search, no-results, validation error, rate-limit, and timeout scenarios

Expose a single clean service interface, e.g.:
```java
public interface DuffelFlightSearchService {
    FlightSearchResult search(FlightSearchRequest request);
}
```
so `flight-search` never talks to Duffel directly — only through this service.

---

## Module 3: `flight-search`

Purpose: the consumer-facing module — accepts search parameters, calls `duffel-flight-search` (via the `common` DTOs), and renders results through a **professional UI modeled after MakeMyTrip's flight search experience.**

### Functional requirements
- **Search form** capturing *all* standard search parameters:
  - Trip type toggle: One Way / Round Trip / Multi-City
  - From / To city-airport autocomplete (IATA code + city + airport name, swap icon between them)
  - Departure date picker (and return date for round trip; multiple date+city rows for multi-city)
  - Traveler & class selector: Adults / Children / Infants counters + cabin class dropdown (Economy, Premium Economy, Business, First)
  - "Search Flights" primary CTA button
  - Optional: fare type toggle (Regular / Student / Senior Citizen / Armed Forces — MakeMyTrip-style), direct-flights-only checkbox
- **Results page**:
  - Filter sidebar: stops (non-stop/1-stop/2+), price range slider, departure time buckets (early morning/morning/afternoon/evening/night), airlines (multi-select checkboxes), duration slider
  - Sort bar: Cheapest, Fastest, Best (default), Departure time, Arrival time
  - Flight cards: airline logo/name, flight number, departure/arrival time + airport codes, duration, stops/layover info, price, "Book" / "View Details" CTA, baggage/refundability badges
  - Loading skeleton states, empty-results state, error state
  - Responsive design (desktop + mobile breakpoints)
- **API/backend layer**: REST controller(s) in this module exposing endpoints like `POST /api/v1/flights/search`, accepting `FlightSearchRequest` (from `common`), delegating to `DuffelFlightSearchService`, returning `FlightSearchResult`

### Look & feel — match makemytrip.com
- Color palette: MakeMyTrip's signature deep blue/navy (#0F1245-ish) primary with red/orange (#E01F4C-ish) accent CTA buttons — use as inspiration, not pixel-for-pixel copy of their branding/logo
- Clean card-based layout, rounded corners, subtle shadows
- Sticky/prominent search bar at top of results page (editable inline to re-search)
- Iconography for airlines, baggage, meals, wifi
- Micro-interactions: hover states on cards, animated counters, smooth filter application
- Typography: modern sans-serif (e.g., Inter/Roboto), clear hierarchy between price (large, bold) and metadata (smaller, muted)
- **Do not copy MakeMyTrip's logo, trademarks, or literal brand assets** — replicate the UX pattern and visual polish, not the trademarked identity

### Suggested tech stack
- Backend: Spring Boot (REST controllers, validation via `jakarta.validation`)
- Frontend: React (or Thymeleaf if a server-rendered Java-only stack is preferred) + Tailwind CSS for MakeMyTrip-style styling
- API contract: OpenAPI/Swagger spec generated from the controllers

---

## Cross-cutting requirements
- All three modules build via a single `mvn clean install` from the parent POM
- No module reaches "sideways" — `duffel-flight-search` and `flight-search` both depend on `common`; `flight-search` depends on `duffel-flight-search`; `duffel-flight-search` never depends on `flight-search`
- Centralized logging (SLF4J) and correlation ID propagation across the search request lifecycle
- Environment-based config profiles: `local`, `test` (Duffel sandbox), `prod` (Duffel live)
- README per module explaining setup, required env vars (`DUFFEL_ACCESS_TOKEN`, etc.), and how to run

## Deliverables checklist
- [ ] Parent `pom.xml` with 3 modules wired correctly
- [ ] `common` module: DTOs, enums, exceptions, utils
- [ ] `duffel-flight-search` module: Duffel client, mappers, service interface + impl, tests
- [ ] `flight-search` module: search form UI, results UI, REST controller, service integration
- [ ] End-to-end test: submit a search from the UI → Duffel sandbox → results rendered correctly
