// src/routes/AppRoutes.tsx (or wherever your routes are)
import { Navigate, Route, Routes } from 'react-router-dom';
import PrivacyPolicyPage from '../../pages/legal/PrivacyPolicyPage';
import TermsOfServicePage from '../../pages/legal/TermsOfServicePage';
import PreferencesPage from '../../pages/Preferences/Preferences';
import AboutUs from '../../pages/public/About/AboutUs';
import ContactPage from '../../pages/public/Contact/ContactPage';
import Home from '../../pages/public/Home/Home';
import Login from '../../pages/public/Login/Login';
import ProductDetails from '../../pages/public/ProductDetails/ProductDetails';
import ProductsPage from '../../pages/public/Products/Products';
import Register from '../../pages/public/Register/Register';
import MainLayout from '../layouts/MainLayout';
import ProtectedRoute from './ProtectedRoute';

export default function AppRoutes() {
  return (
    <Routes>
      <Route element={<MainLayout />}>
        {/* Public Routes */}
        <Route path="/" element={<Home />} />
        <Route path="/produtos" element={<ProductsPage />} />
        <Route path="/sobre" element={<AboutUs />} />
        <Route path="/contato" element={<ContactPage />} />
        <Route path="/:category/:slug" element={<ProductDetails />} />
        <Route path="/legal/termos" element={<TermsOfServicePage />} />
        <Route path="/legal/privacidade" element={<PrivacyPolicyPage />} />
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />

        {/* Protected Routes */}
        <Route
          path="/configs"
          element={
            <ProtectedRoute>
              <PreferencesPage />
            </ProtectedRoute>
          }
        />

        {/* fallback */}
        <Route path="*" element={<Navigate to="/" replace />} />
      </Route>
    </Routes>
  );
}
