import { BookOpen, ChevronDown, LogOut, Settings, ShoppingCart } from 'lucide-react';
import { useEffect, useRef, useState } from 'react';
import { Link, NavLink, useNavigate } from 'react-router-dom';
import { useCart } from '../../app/providers/useCart';
import { useAuth } from '../../contexts/AuthContext';
import MiniCartDropdown from '../cart/MiniCartDropdown';

export default function Header() {
  const navigate = useNavigate();
  const { isAuthenticated, user, logout } = useAuth();

  const [menuOpen, setMenuOpen] = useState(false);
  const menuRef = useRef<HTMLDivElement | null>(null);

  const base = 'text-sm font-semibold text-brand-muted hover:text-primary-500 transition';
  const active = 'text-primary-600';

  const [openCart, setOpenCart] = useState(false);

  const cartRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    function handleClickOutside(e: MouseEvent) {
      if (cartRef.current && !cartRef.current.contains(e.target as Node)) {
        setOpenCart(false);
      }
    }

    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const { cartCount } = useCart();

  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (!menuRef.current) return;

      if (!menuRef.current.contains(event.target as Node)) {
        setMenuOpen(false);
      }
    }

    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  async function handleLogout() {
    await logout();
    setMenuOpen(false);
    navigate('/login');
  }

  return (
    <header className="w-screen max-w-full overflow-hidden border-b border-neutral-200 bg-white">
      <div className="mx-auto flex h-16 w-full max-w-7xl items-center justify-between px-6">
        <Link to="/" className="flex items-center">
          <img
            src="/brand/logo-horizontal.png"
            alt="PlantaHUB"
            className="hidden h-9 w-auto object-contain sm:block"
          />
          <img
            src="/brand/logo-symbol.png"
            alt="PlantaHUB"
            className="h-10 w-auto object-contain sm:hidden"
          />
        </Link>

        <nav className="hidden md:flex items-center gap-8">
          <NavLink to="/" className={({ isActive }) => `${base} ${isActive ? active : ''}`} end>
            Home
          </NavLink>

          <NavLink to="/produtos" className={({ isActive }) => `${base} ${isActive ? active : ''}`}>
            Produtos
          </NavLink>

          <NavLink to="/sobre" className={({ isActive }) => `${base} ${isActive ? active : ''}`}>
            Sobre
          </NavLink>

          <NavLink to="/contato" className={({ isActive }) => `${base} ${isActive ? active : ''}`}>
            Contato
          </NavLink>
        </nav>

        <div className="flex items-center gap-2 sm:gap-4">
          {isAuthenticated && (
            <div className="relative" ref={cartRef}>
              <button
                type="button"
                onClick={() => setOpenCart(prev => !prev)}
                className="relative rounded-xl border border-neutral-200 bg-white p-2 shadow-sm hover:bg-neutral-50 transition"
              >
                <ShoppingCart className="h-5 w-5 text-neutral-700" />

                {cartCount > 0 && (
                  <span className="absolute -top-1 -right-1 flex h-5 min-w-20px items-center justify-center rounded-full bg-primary-500 px-1 text-[11px] font-bold text-white">
                    {cartCount}
                  </span>
                )}
              </button>

              {openCart ? (
                <div className="absolute right-0 mt-2 z-50 translate-y-0 opacity-100 transition-all duration-200">
                  <MiniCartDropdown onClose={() => setOpenCart(false)} />
                </div>
              ) : null}
            </div>
          )}
          {!isAuthenticated ? (
            <>
              <Link
                to="/login"
                className="text-sm font-semibold text-brand-muted hover:text-primary-500 transition"
              >
                Entrar
              </Link>

              <Link
                to="/register"
                className="hidden px-4 py-2 rounded-lg bg-primary-500 text-white hover:bg-primary-600 transition text-sm font-semibold sm:inline-flex sm:px-5"
              >
                Criar conta
              </Link>
            </>
          ) : (
            <div className="relative" ref={menuRef}>
              <button
                type="button"
                onClick={() => setMenuOpen(prev => !prev)}
                className="inline-flex items-center gap-2 rounded-xl border border-neutral-200 bg-white px-4 py-2 text-sm font-semibold text-neutral-800 shadow-sm hover:bg-neutral-50 transition cursor-pointer"
              >
                <span>Olá, {user?.firstName ?? 'Usuário'}</span>
                <ChevronDown className="h-4 w-4" />
              </button>

              {menuOpen ? (
                <div className="absolute right-0 mt-3 w-56 rounded-2xl border border-neutral-200 bg-white shadow-lg overflow-hidden z-50">
                  <Link
                    to="/biblioteca"
                    onClick={() => setMenuOpen(false)}
                    className="flex items-center gap-3 px-4 py-3 text-sm font-medium text-neutral-700 hover:bg-neutral-50"
                  >
                    <BookOpen className="h-4 w-4" />
                    Biblioteca
                  </Link>

                  <Link
                    to="/configs"
                    onClick={() => setMenuOpen(false)}
                    className="flex items-center gap-3 px-4 py-3 text-sm font-medium text-neutral-700 hover:bg-neutral-50"
                  >
                    <Settings className="h-4 w-4" />
                    Configurações
                  </Link>

                  <button
                    type="button"
                    onClick={handleLogout}
                    className="flex w-full items-center gap-3 px-4 py-3 text-sm font-medium text-red-600 hover:bg-red-50"
                  >
                    <LogOut className="h-4 w-4" />
                    Sair
                  </button>
                </div>
              ) : null}
            </div>
          )}
        </div>
      </div>
    </header>
  );
}
