import { Outlet, useLocation } from 'react-router-dom';
import Footer from '../../components/layout/Footer';
import Header from '../../components/layout/Header';

export default function MainLayout() {
  const location = useLocation();

  return (
    <div className="min-h-screen flex flex-col bg-white">
      <Header />
      <main key={`${location.pathname}${location.search}`} className="page-transition flex-1">
        <Outlet />
      </main>
      <Footer />
    </div>
  );
}
