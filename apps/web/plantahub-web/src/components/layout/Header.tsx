import { Link, NavLink } from 'react-router-dom';
import logotipo from '../../assets/logotipo.png';

export default function Header() {
  const base = 'text-sm font-semibold text-neutral-700 hover:text-primary-500 transition';
  const active = 'text-primary-600';

  return (
    <header className="w-full bg-white border-b">
      <div className="max-w-7xl mx-auto px-6 h-16 flex items-center justify-between">
        {/* Logo */}
        <Link to="/" className="flex items-center gap-3">
          <img src={logotipo} alt="PlantaHUB" className="h-9 w-9 rounded-lg" />
          <span className="font-semibold text-lg text-neutral-900">PlantaHUB</span>
        </Link>

        {/* Nav */}
        <nav className="hidden md:flex items-center gap-8">
          <NavLink to="/" className={({ isActive }) => `${base} ${isActive ? active : ''}`} end>
            Home
          </NavLink>

          <NavLink to="/products" className={({ isActive }) => `${base} ${isActive ? active : ''}`}>
            Produtos
          </NavLink>

          <NavLink to="/about" className={({ isActive }) => `${base} ${isActive ? active : ''}`}>
            Sobre
          </NavLink>

          <NavLink to="/contact" className={({ isActive }) => `${base} ${isActive ? active : ''}`}>
            Contato
          </NavLink>
        </nav>

        {/* Actions */}
        <div className="flex items-center gap-4">
          <Link
            to="/signin"
            className="text-sm font-semibold text-neutral-700 hover:text-primary-500 transition"
          >
            Entrar
          </Link>

          <Link
            to="/products"
            className="px-5 py-2 rounded-lg bg-primary-500 text-white hover:bg-primary-600 transition text-sm font-semibold"
          >
            Começar
          </Link>
        </div>
      </div>
    </header>
  );
}
