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
      'Designed and built a distributed B2B payment platform processing 10,000+ travel transactions monthly, integrating 6 global payment providers (WEX, VNett, Citi, Revolut, Amadeus, Adyen) for 99.9% availability.',
      'Designed a Payment Decision Engine that auto-selects the most profitable card across 8 providers with built-in failover, eliminating 100% of manual card selection and saving ~200 hours/month.',
      'Delivered the full virtual card lifecycle (issuance, modification, fetch, cancellation) with PCI DSS-compliant encryption and real-time fraud detection, cutting fraud-related chargebacks ~30%.',
      'Designed multi-step AI agents with Spring AI and LLMs for an AI-powered flight search assistant -- agentic tool-calling, RAG-grounded responses, and MCP-based service integration.',
      'Used Claude Code across the SDLC, cutting average feature delivery time ~30% and incident MTTR ~50% on critical payment issues.',
      'Migrated a legacy Spring MVC monolith to Spring Boot microservices, cutting new bank integration time from 3-4 months to ~1 month.',
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
