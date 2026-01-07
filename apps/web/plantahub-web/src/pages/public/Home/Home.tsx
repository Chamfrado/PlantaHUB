import Hero from '../../../components/home/hero';
import Header from '../../../components/layout/Header';

export default function Home() {
  return (
    <div className="min-h-screen flex flex-col bg-neutral-50">
      <Header />
      <main className="flex-1">
        <Hero />
      </main>
    </div>
  );
}
