import ChaletsSection from '../../../components/home/ChaletsSection';
import FinalCTA from '../../../components/home/FinalCTA';
import Hero from '../../../components/home/Hero';
import HowItWorks from '../../../components/home/HowItWorks';
import ResidentialHouses from '../../../components/home/ResidentialHouses';
import WhyChoose from '../../../components/home/WhyChoose';
import Footer from '../../../components/layout/Footer';
import Header from '../../../components/layout/Header';

export default function Home() {
  return (
    <div className="min-h-screen flex flex-col bg-neutral-50">
      <Header />
      <main className="flex-1">
        <Hero />
        <WhyChoose />
        <ResidentialHouses />
        <ChaletsSection />
        <HowItWorks />
        <FinalCTA />
      </main>
      <Footer />
    </div>
  );
}
