import { useQuery } from '@tanstack/react-query';
import { ExternalLink } from 'lucide-react';
import { Link } from 'react-router-dom';
import {
  AdminPageHeader,
  DataError,
} from '../../../components/admin/ui/primitives';
import { getApiErrorMessage } from '../../../lib/api-error';
import { listSitePages } from '../../../services/admin/admin.service';

/** Onde cada página institucional aparece no site. */
const PUBLIC_PATH: Record<string, string> = {
  sobre: '/sobre',
  contato: '/contato',
  faq: '/faq',
  'trabalhe-conosco': '/trabalhe-conosco',
  termos: '/legal/termos',
  privacidade: '/legal/privacidade',
};

function formatDate(iso: string) {
  return new Date(iso).toLocaleDateString('pt-BR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

/**
 * As páginas institucionais.
 *
 * <p>Não há "nova página" nem "excluir", de propósito: cada slug corresponde a um
 * componente com layout próprio no site. Criar uma linha aqui produziria uma página que não
 * renderiza em lugar nenhum, e apagar uma derrubaria uma rota que está no ar.
 */
export default function SitePageListPage() {
  const pages = useQuery({ queryKey: ['admin', 'site', 'pages'], queryFn: listSitePages });

  return (
    <div className="max-w-4xl space-y-6">
      <AdminPageHeader
        title="Páginas do site"
        description="Edite o texto de Sobre, Contato, FAQ e das páginas legais sem publicar uma nova versão."
      />

      {pages.isLoading ? (
        <div className="h-64 animate-pulse rounded-2xl bg-white" />
      ) : pages.isError ? (
        <DataError
          message={getApiErrorMessage(pages.error, 'Não foi possível carregar as páginas.')}
          onRetry={() => void pages.refetch()}
        />
      ) : (
        <div className="overflow-hidden rounded-2xl border border-neutral-200 bg-white">
          <table className="w-full text-sm">
            <thead className="border-b border-neutral-200 text-left text-xs uppercase tracking-wide text-neutral-500">
              <tr>
                <th className="px-4 py-3">Página</th>
                <th className="px-4 py-3">Última edição</th>
                <th className="px-4 py-3 text-right">Ações</th>
              </tr>
            </thead>

            <tbody>
              {(pages.data ?? []).map(page => (
                <tr key={page.slug} className="border-b border-neutral-100 last:border-0">
                  <td className="px-4 py-3">
                    <Link
                      to={`/admin/paginas/${page.slug}`}
                      className="font-bold text-neutral-900 transition hover:text-primary-600"
                    >
                      {page.title}
                    </Link>
                    <div className="text-xs text-neutral-400">{PUBLIC_PATH[page.slug] ?? page.slug}</div>
                  </td>

                  <td className="px-4 py-3 text-neutral-600">
                    {formatDate(page.updatedAt)}
                    {page.updatedBy ? (
                      <div className="text-xs text-neutral-400">por {page.updatedBy}</div>
                    ) : null}
                  </td>

                  <td className="px-4 py-3">
                    <div className="flex justify-end gap-2">
                      {PUBLIC_PATH[page.slug] ? (
                        <a
                          href={PUBLIC_PATH[page.slug]}
                          target="_blank"
                          rel="noreferrer"
                          className="inline-flex items-center gap-1.5 rounded-lg border border-neutral-300 px-3 py-1.5 text-xs font-semibold text-neutral-700 transition hover:bg-neutral-50"
                        >
                          <ExternalLink className="h-3.5 w-3.5" /> Ver no site
                        </a>
                      ) : null}

                      <Link
                        to={`/admin/paginas/${page.slug}`}
                        className="rounded-lg bg-primary-500 px-3 py-1.5 text-xs font-semibold text-white transition hover:bg-primary-600"
                      >
                        Editar
                      </Link>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
