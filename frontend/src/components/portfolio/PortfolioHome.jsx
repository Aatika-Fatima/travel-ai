import { useRef } from 'react'
import PortfolioHeader from './PortfolioHeader.jsx'
import SocialSidebar from './SocialSidebar.jsx'
import BioSidebar from './BioSidebar.jsx'
import SearchFlightsCard from './SearchFlightsCard.jsx'
import PopularRoutes from './PopularRoutes.jsx'
import FeatureHighlights from './FeatureHighlights.jsx'
import PlatformSection from './PlatformSection.jsx'
import FeaturedProjects from './FeaturedProjects.jsx'
import AIAssistantSidebar from './AIAssistantSidebar.jsx'
import AboutSection from './AboutSection.jsx'
import SkillsSection from './SkillsSection.jsx'
import ExperienceSection from './ExperienceSection.jsx'
import ContactSection from './ContactSection.jsx'
import TechStackFooter from './TechStackFooter.jsx'
import ResultsPage from '../ResultsPage.jsx'

export default function PortfolioHome({
  criteria,
  onChange,
  onSubmit,
  onAssistantAction,
  hasSearched,
  status,
  offers,
  error,
  onRetry,
  onSelectOffer,
}) {
  const assistantRef = useRef(null)

  return (
    <div className="flex min-h-screen flex-col bg-slate-50">
      <PortfolioHeader />
      <SocialSidebar />

      <main className="mx-auto grid w-full max-w-7xl flex-1 grid-cols-1 gap-5 px-4 py-6 lg:grid-cols-[280px_minmax(0,1fr)]">
        <div className="flex flex-col gap-5">
          <BioSidebar />
          <AIAssistantSidebar ref={assistantRef} onAction={onAssistantAction} />
        </div>

        <div className="flex min-w-0 flex-col gap-5">
          <SearchFlightsCard
            criteria={criteria}
            onChange={onChange}
            onSubmit={onSubmit}
            onFocusAssistant={() => assistantRef.current?.scrollIntoView({ behavior: 'smooth', block: 'center' })}
          />

          {hasSearched ? (
            // Results replace the promo content below the search card --
            // still inside this same page shell (bio + AI assistant stay
            // put), rather than navigating away to a separate results page.
            <ResultsPage
              status={status}
              error={error}
              offers={offers}
              onRetry={onRetry}
              onSelectOffer={onSelectOffer}
              embedded
            />
          ) : (
            <>
              <PopularRoutes onSelect={(origin, destination) => onChange({ ...criteria, origin, destination })} />
              <FeatureHighlights />
            </>
          )}
        </div>
      </main>

      {/* Full-width sections -- same max-w-7xl / px-4 as <main> so Platform,
          Projects and About all line up edge to edge. */}
      <div className="mx-auto flex w-full max-w-7xl flex-col gap-5 px-4 pb-6">
        <PlatformSection />
        <FeaturedProjects />
        <AboutSection />
        <SkillsSection />
        <ExperienceSection />
        <ContactSection />
      </div>

      <TechStackFooter />
    </div>
  )
}
