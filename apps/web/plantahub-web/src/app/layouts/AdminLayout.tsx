import { ExternalLink, FolderTree, HardDrive, LayoutGrid, Layers, RefreshCcw, Tag } from 'lucide-react';
import { Link, NavLink, Outlet } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';

const navItems = [
  { to: '/admin/produtos', label: 'Produtos', icon: LayoutGrid },
  { to: '/admin/colecoes', label: 'Coleções', icon: Layers },
  { to: '/admin/categorias', label: 'Categorias', icon: Tag },
  { to: '/admin/reconciliacao', label: 'Reconciliação', icon: RefreshCcw },
  { to: '/admin/armazenamento', label: 'Armazenamento', icon: HardDrive },
];

/**
 * Casca do painel.
 *
 * Não importa nada de `components/layout/Header|Footer` de propósito: uma importação
 * dessas arrastaria o cabeçalho público para dentro do chunk do painel — e, pelo caminho
 * inverso, o painel para dentro do bundle público.
 */
export default function AdminLayout() {
  const { user, logout } = useAuth();

  return (
    <div className="flex min-h-screen bg-neutral-50">
      <aside className="hidden w-60 shrink-0 border-r border-neutral-200 bg-white lg:block">
        <div className="flex h-16 items-center gap-2 border-b border-neutral-200 px-6">
          <FolderTree className="h-5 w-5 text-primary-500" />
          <span className="font-extrabold text-neutral-900">PlantaHUB</span>
          <span className="rounded bg-neutral-100 px-1.5 py-0.5 text-[10px] font-bold text-neutral-500">
            ADMIN
          </span>
        </div>

        <nav className="p-3">
          {navItems.map(item => (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) =>
                [
                  'flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-semibold transition',
                  isActive
                    ? 'bg-orange-50 text-primary-600'
                    : 'text-neutral-600 hover:bg-neutral-100',
                ].join(' ')
              }
            >
              <item.icon className="h-4 w-4" />
              {item.label}
            </NavLink>
          ))}
        </nav>
      </aside>

      <div className="flex min-w-0 flex-1 flex-col">
        <header className="flex h-16 items-center justify-between gap-4 border-b border-neutral-200 bg-white px-6">
          <div className="flex min-w-0 items-center gap-3 overflow-x-auto lg:hidden">
            {navItems.map(item => (
              <NavLink
                key={item.to}
                to={item.to}
                className={({ isActive }) =>
                  isActive ? 'text-sm font-bold text-primary-600' : 'text-sm text-neutral-500'
                }
              >
                {item.label}
              </NavLink>
            ))}
          </div>

          <div className="ml-auto flex shrink-0 items-center gap-4">
            <Link
              to="/"
              target="_blank"
              className="inline-flex items-center gap-1.5 text-sm font-semibold text-neutral-600 transition hover:text-neutral-900"
            >
              Ver site <ExternalLink className="h-3.5 w-3.5" />
            </Link>

            <span className="hidden text-sm text-neutral-500 sm:inline">{user?.email}</span>

            <button
              onClick={() => void logout()}
              className="rounded-lg border border-neutral-300 px-3 py-1.5 text-sm font-semibold text-neutral-700 transition hover:bg-neutral-50"
            >
              Sair
            </button>
          </div>
        </header>

        <main className="min-w-0 flex-1 p-6">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
