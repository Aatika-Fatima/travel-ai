// Condensed from the "Professional Experience" section of
// docs/Aatika_Fatima_Remote.docx -- the full bullet-for-bullet detail lives
// in the downloadable resume PDF; this is the portfolio-friendly summary.
const ROLES = [
  {
    title: 'AI Travel Platform (FlyStack) -- Personal Project',
    company: 'Independent',
    location: 'Remote',
    period: '2026 -- Present',
    highlights: [
      'Designed a transactional outbox pattern across booking-service, order-service, and payment-service -- outbox rows written in the same DB transaction as the domain change, then relayed to Kafka by scheduled pollers (order-service using FOR UPDATE SKIP LOCKED batch locking) for at-least-once delivery without dual-write inconsistency.',
      'Built a Kafka-driven saga orchestrating booking -> order -> payment across three services over booking.events, order.events, and payment.events, with idempotent, out-of-order-safe consumers -- state-transition validation, @Retryable optimistic-locking retry, and skip-on-parse-failure handling for poison messages.',
      'Implemented a @RetryableTopic/DLQ email pipeline with exponential backoff and a dead-letter handler for the notification service’s email-events consumer.',
      'Configured an idempotent Kafka producer (acks=all, enable.idempotence=true) and manual-ack, per-module consumer configs to avoid Spring bean collisions in a merged multi-module Boot app.',
      'Added scheduled reconciliation and booking-expiry sweep jobs as safety nets for stuck sagas, and unit-tested outbox writers, relays, and consumers with Mockito.',
    ],
  },
  {
    title: 'Senior Software Engineer -- Payments Platform',
    company: 'Lastminute.com',
    location: 'Hyderabad, India',
    period: 'Dec 2021 -- Jun 2026',
    highlights: [
      'Designed and built a distributed B2B payment platform processing 10,000+ travel transactions monthly across flights, hotels, baggage, parking, and ancillary services -- owning the full lifecycle from architecture to production deployment.',
      'Integrated 6 global payment providers (WEX, VNett, Citi, Revolut, Amadeus, Adyen) achieving 99.9% payment availability through multi-provider redundancy, and negotiated an Adyen integration with a 98% rebate rate.',
      'Designed a Payment Decision Engine that automatically selects the most profitable card from a pool of 8 providers based on FX rates, rebates, spending limits, and availability -- with failover logic that serves the next best card on failure, eliminating 100% of manual card selection and saving ~200 hours/month.',
      'Implemented low-balance detection to auto-suspend card issuance from depleted banks, preventing cascading failures and reducing payment decline rates ~40%.',
      'Architected the complete virtual card lifecycle as microservices (Card Issuance, Modification, Fetch, Cancellation), with an automated Cancellation Scheduler that cancels cards within 2 hours of booking confirmation and single-use card policy for WEX and Adyen.',
      'Encrypted all sensitive card data (PAN, CVV, expiry) at rest and in transit for full PCI DSS compliance, and built WEX authorisation workflows that block fraudulent attempts in real time, cutting fraud-related chargebacks ~30%.',
      'Built a self-serve Finance Team UI and on-demand credit card issuance, eliminating engineering dependency for card operations (~15 hours/week saved) and enabling 100+ manual bookings/month; added a bulk resubmit mechanism cutting incident recovery time ~70%.',
      'Developed a real-time Account Balance API across 8 bank accounts and rolled out Graylog + Grafana observability across 5+ payment microservices, reducing MTTD ~60%.',
      'Migrated a legacy Spring MVC monolith to Spring Boot microservices, cutting new bank integration time from 3-4 months to ~1 month (75% reduction in time-to-market).',
      'Designed multi-step AI agents (searchFlights) with Spring AI, Kotlin, and LLMs -- collecting missing trip details, validating requests, ranking flights by price/duration/rebate, and orchestrating the end-to-end search workflow with tool calling, multi-turn conversational context, and JSON-schema structured outputs.',
      'Built RAG pipelines with vector databases, embeddings, and semantic search to ground LLM responses in flight policy, fare-rule, and ancillary data, and designed MCP architectures for secure structured communication between the searchFlights agent and enterprise fare/availability/booking systems.',
      'Leveraged Claude Code across the full SDLC (code generation, refactoring, debugging, test creation) and for Graylog root-cause analysis -- cutting average feature delivery time ~30% and incident MTTR ~50% on critical payment issues.',
    ],
  },
  {
    title: 'Software Developer',
    company: 'HCL Technologies (Client: Gap IT Services)',
    location: 'Hyderabad, India',
    period: 'Oct 2020 -- Oct 2021',
    highlights: [
      'Built 5+ Spring Boot microservices and REST APIs for a BI reporting platform consolidating data from 10+ downstream systems.',
      'Implemented RabbitMQ listener retry mechanisms, achieving a 99%+ message processing success rate for async reporting pipelines.',
    ],
  },
  {
    title: 'Software Developer',
    company: 'Accenture Ltd',
    location: 'Hyderabad, India',
    period: 'Jan 2016 -- Oct 2020',
    highlights: [
      'Developed enterprise Java applications using Spring, Hibernate, and J2EE across multi-year client engagements.',
      'Implemented authentication/authorisation with Spring Security and provisioned AWS EC2, S3, and IAM for deployments.',
    ],
  },
  {
    title: 'Senior Java Instructor',
    company: 'NIIT Ltd',
    location: 'Hyderabad, India',
    period: 'Jun 2011 -- Jan 2016',
    highlights: ['Delivered advanced Java and enterprise software training; developed curriculum and mentored students in Spring and Hibernate.'],
  },
  {
    title: 'Computer Science Lecturer',
    company: "Dr. VRK Women's College of Engineering and Technology",
    location: 'Hyderabad, India',
    period: 'Jun 2009 -- Jun 2011',
    highlights: ['Lectured on core computer science and programming fundamentals for undergraduate engineering students.'],
  },
]

export default function ExperienceSection() {
  return (
    <section id="experience" className="scroll-mt-24 rounded-2xl border border-slate-100 bg-white p-5 shadow-sm">
      <h3 className="mb-4 flex items-center gap-2 text-sm font-bold text-brand-900">
        <span>📈</span> Experience
      </h3>

      <div className="flex flex-col gap-5">
        {ROLES.map((role) => (
          <div key={role.title + role.company} className="border-l-2 border-brand-50 pl-4">
            <div className="flex flex-wrap items-baseline justify-between gap-x-3 gap-y-0.5">
              <span className="text-sm font-bold text-brand-900">{role.title}</span>
              <span className="text-[11px] font-semibold text-slate-400">{role.period}</span>
            </div>
            <div className="text-xs font-semibold text-accent-500">
              {role.company} &middot; {role.location}
            </div>
            <ul className="mt-2 list-disc space-y-1 pl-4 text-xs leading-relaxed text-slate-600">
              {role.highlights.map((point) => (
                <li key={point}>{point}</li>
              ))}
            </ul>
          </div>
        ))}
      </div>
    </section>
  )
}
