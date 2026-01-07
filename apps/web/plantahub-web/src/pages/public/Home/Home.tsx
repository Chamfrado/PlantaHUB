import Header from '../../../components/layout/Header';

export default function Home() {
  return (
    <div className="min-h-screen flex flex-col bg-neutral-50">
      <Header />
      <main className="flex-1">
        <h1 className="text-7xl font-bold text-primary-500">Home Page</h1>
      </main>
    </div>
  );
}
