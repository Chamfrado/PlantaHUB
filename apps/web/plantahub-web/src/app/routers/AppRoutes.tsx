// src/routes/AppRoutes.tsx (or wherever your routes are)
import { Navigate, Route, Routes } from 'react-router-dom';
import AboutUs from '../../pages/public/About/AboutUs';
import Home from '../../pages/public/Home/Home';
import ProductDetails from '../../pages/public/ProductDetails/ProductDetails';
import ProductsPage from '../../pages/public/Products/Products';
import MainLayout from '../layouts/MainLayout';

function Contact() {
  return <div className="max-w-7xl mx-auto px-6 py-12">Contact</div>;
}

export default function AppRoutes() {
  return (
    <Routes>
      <Route element={<MainLayout />}>
        <Route path="/" element={<Home />} />
        <Route path="/products" element={<ProductsPage />} />
        <Route path="/about" element={<AboutUs />} />
        <Route path="/contact" element={<Contact />} />
        <Route path="/:category/:slug" element={<ProductDetails />} />

        {/* fallback */}
        <Route path="*" element={<Navigate to="/" replace />} />
      </Route>
    </Routes>
  );
}
