import { Navigate, Route, Routes } from 'react-router-dom';
import ScrollToTop from '../../components/common/ScrollTop';
import { ToastProvider } from '../../components/ui/ToastProvider';
import CartPage from '../../pages/cart/CartPage';
import PrivacyPolicyPage from '../../pages/legal/PrivacyPolicyPage';
import TermsOfServicePage from '../../pages/legal/TermsOfServicePage';
import LibraryPage from '../../pages/Library/Library';
import LibraryItemDetailsPage from '../../pages/Library/LibraryItemDetailsPage';
import OrderDetailsPage from '../../pages/Orders/OrderDetailsPage';
import PreferencesPage from '../../pages/Preferences/Preferences';
import AboutUs from '../../pages/public/About/AboutUs';
import Carrer from '../../pages/public/Carrer/Carrer';
import ContactPage from '../../pages/public/Contact/ContactPage';
import FaqPage from '../../pages/public/Faq/Faq';
import Home from '../../pages/public/Home/Home';
import Login from '../../pages/public/Login/Login';
import ProductDetails from '../../pages/public/ProductDetails/ProductDetails';
import ProductsPage from '../../pages/public/Products/Products';
import Register from '../../pages/public/Register/Register';
import MainLayout from '../layouts/MainLayout';
import { CartProvider } from '../providers/CartProvider';
import ProtectedRoute from './ProtectedRoute';

export default function AppRoutes() {
  return (
    <ToastProvider>
      <CartProvider>
        <ScrollToTop />
        <Routes>
          <Route element={<MainLayout />}>
            <Route path="/" element={<Home />} />
            <Route path="/produtos" element={<ProductsPage />} />
            <Route path="/sobre" element={<AboutUs />} />
            <Route path="/contato" element={<ContactPage />} />
            <Route path="/:category/:slug" element={<ProductDetails />} />
            <Route path="/legal/termos" element={<TermsOfServicePage />} />
            <Route path="/legal/privacidade" element={<PrivacyPolicyPage />} />
            <Route path="/login" element={<Login />} />
            <Route path="/register" element={<Register />} />
            <Route path="/trabalhe-conosco" element={<Carrer />} />
            <Route path="/faq" element={<FaqPage />} />

            <Route
              path="/configs"
              element={
                <ProtectedRoute>
                  <PreferencesPage />
                </ProtectedRoute>
              }
            />

            <Route
              path="/biblioteca"
              element={
                <ProtectedRoute>
                  <LibraryPage />
                </ProtectedRoute>
              }
            />

            <Route
              path="/carrinho"
              element={
                <ProtectedRoute>
                  <CartPage />
                </ProtectedRoute>
              }
            />

            <Route
              path="/pedidos/:orderId"
              element={
                <ProtectedRoute>
                  <OrderDetailsPage />
                </ProtectedRoute>
              }
            />

            <Route
              path="/biblioteca/:productId/:planTypeCode"
              element={
                <ProtectedRoute>
                  <LibraryItemDetailsPage />
                </ProtectedRoute>
              }
            />

            <Route path="*" element={<Navigate to="/" replace />} />
          </Route>
        </Routes>
      </CartProvider>
    </ToastProvider>
  );
}
