import ChaletsSection from '../../../components/home/ChaletsSection';
import Hero from '../../../components/home/Hero';
import ResidentialHouses from '../../../components/home/ResidentialHouses';
import WhyChoose from '../../../components/home/WhyChoose';
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
      </main>
    </div>
  );
}
