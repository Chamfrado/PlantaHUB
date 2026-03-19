import { BookOpen, ChevronDown, LogOut, Settings } from 'lucide-react';
import { useEffect, useRef, useState } from 'react';
import { Link, NavLink, useNavigate } from 'react-router-dom';
import logotipo from '../../assets/logotipo.png';
import { useAuth } from '../../contexts/AuthContext';

export default function Header() {
  const navigate = useNavigate();
  const { isAuthenticated, user, logout } = useAuth();

  const [menuOpen, setMenuOpen] = useState(false);
  const menuRef = useRef<HTMLDivElement | null>(null);

  const base = 'text-sm font-semibold text-neutral-700 hover:text-primary-500 transition';
  const active = 'text-primary-600';

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

  function handleLogout() {
    logout();
    setMenuOpen(false);
    navigate('/login');
  }

  return (
    <header className="w-full bg-white border-b">
      <div className="max-w-7xl mx-auto px-6 h-16 flex items-center justify-between">
        <Link to="/" className="flex items-center gap-3">
          <img src={logotipo} alt="PlantaHUB" className="h-9 w-9 rounded-lg" />
          <span className="font-semibold text-lg text-neutral-900">PlantaHUB</span>
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

        <div className="flex items-center gap-4">
          {!isAuthenticated ? (
            <>
              <Link
                to="/login"
                className="text-sm font-semibold text-neutral-700 hover:text-primary-500 transition"
              >
                Entrar
              </Link>

              <Link
                to="/register"
                className="px-5 py-2 rounded-lg bg-primary-500 text-white hover:bg-primary-600 transition text-sm font-semibold"
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
