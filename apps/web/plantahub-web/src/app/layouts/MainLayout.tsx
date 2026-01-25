import { Outlet } from 'react-router-dom';
import Footer from '../../components/layout/Footer';
import Header from '../../components/layout/Header';

export default function MainLayout() {
  return (
    <div className="min-h-screen flex flex-col bg-white">
      <Header />
      <main className="flex-1">
        <Outlet />
      </main>
      <Footer />
    </div>
  );
}
