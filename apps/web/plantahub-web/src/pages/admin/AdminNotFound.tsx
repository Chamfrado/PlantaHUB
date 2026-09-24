import { Link } from 'react-router-dom';

export default function AdminNotFound() {
  return (
    <div className="rounded-2xl border border-neutral-200 bg-white px-6 py-16 text-center">
      <h1 className="text-xl font-extrabold text-neutral-900">Página não encontrada</h1>
      <p className="mt-2 text-sm text-neutral-500">Esta tela do painel não existe.</p>
      <Link
        to="/admin/produtos"
        className="mt-6 inline-flex rounded-xl bg-primary-500 px-5 py-2.5 text-sm font-semibold text-white transition hover:bg-primary-600"
      >
        Ir para produtos
      </Link>
    </div>
  );
}
