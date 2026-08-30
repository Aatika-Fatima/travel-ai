// Categories mirror the "Skills & Competencies" and "Technical Skills"
// sections of docs/Aatika_Fatima_Remote.docx, plus one category for the
// stack actually exercised by this repo (kept separate so resume-verified
// skills aren't mixed with what's merely demonstrated here).
const CATEGORIES = [
  {
    title: 'Core Competencies',
    skills: [
      'B2B Credit Payments',
      'Multi-Bank API Integration',
      'Fraud Detection & Prevention',
      'Card Selection Optimisation',
      'Authorization Systems Design',
      'Chargeback & Rebate Strategy',
      'Payment Architecture',
      'Prompt Engineering',
      'Generative AI & Agentic Systems',
      'Retrieval-Augmented Generation (RAG)',
      'Model Context Protocol (MCP)',
    ],
  },
  { title: 'Languages', skills: ['Java', 'Kotlin', 'SQL'] },
  {
    title: 'Frameworks',
    skills: ['Spring Boot', 'Spring MVC', 'Spring Cloud', 'Spring Data JPA', 'Spring Security', 'Spring AI', 'Hibernate', 'JPA'],
  },
  {
    title: 'Architecture',
    skills: ['Microservices', 'REST APIs', 'Distributed Systems', 'Event-Driven Architecture', 'Enterprise Design Patterns'],
  },
  { title: 'Databases', skills: ['Oracle', 'SQL', 'Azure SQL'] },
  { title: 'Messaging', skills: ['RabbitMQ'] },
  { title: 'Monitoring', skills: ['Graylog', 'Grafana', 'New Relic'] },
  { title: 'Testing', skills: ['JUnit', 'Spring Test', 'Unit Testing', 'Integration Testing'] },
  {
    title: 'AI / GenAI Tooling',
    skills: [
      'Spring AI',
      'Large Language Models (LLM)',
      'Claude API',
      'Claude Code',
      'Agentic AI',
      'RAG',
      'Vector Database',
      'Semantic Search',
      'Embeddings',
      'Model Context Protocol (MCP)',
      'Tool / Function Calling',
      'Conversational AI',
      'NLP',
    ],
  },
  { title: 'Cloud', skills: ['Azure', 'AWS'] },
  { title: 'Practices', skills: ['Agile', 'CI/CD', 'AI-Assisted Development'] },
  {
    title: 'This Project (FlyStack)',
    skills: [
      'Kotlin',
      'Spring Boot 4',
      'Spring AI',
      'Kafka',
      'PostgreSQL + pgvector',
      'Redis',
      'Elasticsearch',
      'React',
      'Vite',
      'Tailwind CSS',
      'Duffel API',
      'Razorpay',
      'Docker Compose',
      'Maven Multi-Module',
      'ArchUnit',
    ],
  },
]

export default function SkillsSection() {
  return (
    <section id="skills" className="scroll-mt-24 rounded-2xl border border-slate-100 bg-white p-5 shadow-sm">
      <h3 className="mb-4 flex items-center gap-2 text-sm font-bold text-brand-900">
        <span>🛠️</span> Skills
      </h3>

      <div className="flex flex-col gap-4">
        {CATEGORIES.map((category) => (
          <div key={category.title}>
            <h4 className="mb-2 text-xs font-bold uppercase tracking-wide text-slate-400">{category.title}</h4>
            <div className="flex flex-wrap gap-1.5">
              {category.skills.map((skill) => (
                <span key={skill} className="rounded-full bg-brand-50 px-2.5 py-1 text-xs font-semibold text-brand-900">
                  {skill}
                </span>
              ))}
            </div>
          </div>
        ))}
      </div>
    </section>
  )
}
