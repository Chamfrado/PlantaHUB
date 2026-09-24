import { ShieldAlert } from 'lucide-react';
import { Link } from 'react-router-dom';

/**
 * Mostrada quando um usuário autenticado sem permissão abre o painel.
 *
 * É uma página, e não um redirecionamento silencioso para a home: mandar a pessoa embora
 * sem explicação torna "por que não consigo entrar?" impossível de depurar.
 */
export default function Forbidden() {
  return (
    <div className="flex min-h-screen items-center justify-center bg-neutral-50 px-6">
      <div className="max-w-md text-center">
        <ShieldAlert className="mx-auto h-12 w-12 text-primary-500" />

        <h1 className="mt-6 text-2xl font-extrabold text-neutral-900">Acesso restrito</h1>

        <p className="mt-2 text-neutral-600">
          Sua conta não tem permissão de administrador. Se isso parece um engano, peça a um
          administrador para conceder o acesso.
        </p>

        <Link
          to="/"
          className="mt-8 inline-flex rounded-xl bg-primary-500 px-5 py-2.5 font-semibold text-white transition hover:bg-primary-600"
        >
          Voltar ao site
        </Link>
      </div>
    </div>
  );
}
