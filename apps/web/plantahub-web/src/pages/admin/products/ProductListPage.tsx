import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Eye, Plus } from 'lucide-react';
import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import {
  AdminPageHeader,
  DataError,
  EmptyState,
  PrimaryButton,
  StatusBadge,
} from '../../../components/admin/ui/primitives';
import { getApiErrorMessage } from '../../../lib/api-error';
import {
  archiveProduct,
  listAdminProducts,
  publishProduct,
  unpublishProduct,
} from '../../../services/admin/admin.service';
import type { AdminProductSummary } from '../../../types/api/admin';
import { formatCurrency } from '../../../utils/format';

const STATUS_TABS = [
  { value: '', label: 'Todos' },
  { value: 'DRAFT', label: 'Rascunhos' },
  { value: 'PUBLISHED', label: 'Publicados' },
  { value: 'ARCHIVED', label: 'Arquivados' },
];

export default function ProductListPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const [status, setStatus] = useState('');
  const [search, setSearch] = useState('');
  const [actionError, setActionError] = useState<string | null>(null);

  const products = useQuery({
    queryKey: ['admin', 'products', status, search],
    queryFn: () => listAdminProducts({ status: status || undefined, q: search || undefined }),
  });

  function refresh() {
    void queryClient.invalidateQueries({ queryKey: ['admin', 'products'] });
  }

  const publish = useMutation({
    mutationFn: publishProduct,
    onSuccess: refresh,
    onError: (error: unknown) =>
      // O 409 de publicação traz a lista completa do que falta; mostrar tudo de uma vez
      // evita que o admin descubra os requisitos por tentativa.
      setActionError(formatPublishError(error)),
  });

  const unpublish = useMutation({ mutationFn: unpublishProduct, onSuccess: refresh });
  const archive = useMutation({ mutationFn: archiveProduct, onSuccess: refresh });

  return (
    <div className="space-y-6">
      <AdminPageHeader
        title="Produtos"
        description="Crie, edite e publique projetos sem alterar código."
        actions={
          <PrimaryButton onClick={() => navigate('/admin/produtos/novo')}>
            <Plus className="h-4 w-4" /> Novo produto
          </PrimaryButton>
        }
      />

      <div className="flex flex-wrap items-center gap-3">
        <div className="flex max-w-full gap-1 overflow-x-auto rounded-xl border border-neutral-200 bg-white p-1">
          {STATUS_TABS.map(tab => (
            <button
              key={tab.value}
              onClick={() => setStatus(tab.value)}
              className={[
                'rounded-lg px-3 py-1.5 text-sm font-semibold transition',
                status === tab.value
                  ? 'bg-primary-500 text-white'
                  : 'text-neutral-600 hover:bg-neutral-100',
              ].join(' ')}
            >
              {tab.label}
            </button>
          ))}
        </div>

        <input
          value={search}
          onChange={event => setSearch(event.target.value)}
          placeholder="Buscar por nome, id ou slug"
          className="min-w-64 flex-1 rounded-xl border border-neutral-300 bg-white px-3 py-2 text-sm outline-none focus:border-primary-500"
        />
      </div>

      {actionError ? (
        <DataError message={actionError} onRetry={() => setActionError(null)} />
      ) : null}

      {products.isLoading ? (
        <div className="h-64 animate-pulse rounded-2xl bg-white" />
      ) : products.isError ? (
        <DataError
          message={getApiErrorMessage(products.error, 'Não foi possível carregar os produtos.')}
          onRetry={() => void products.refetch()}
        />
      ) : (products.data ?? []).length === 0 ? (
        <EmptyState message="Nenhum produto encontrado com esses filtros." />
      ) : (
        <div className="overflow-x-auto rounded-2xl border border-neutral-200 bg-white">
          <table className="w-full min-w-[720px] text-sm">
            <thead className="border-b border-neutral-200 text-left text-xs uppercase tracking-wide text-neutral-500">
              <tr>
                <th className="px-4 py-3">Produto</th>
                <th className="px-4 py-3">Categoria</th>
                <th className="px-4 py-3">Status</th>
                <th className="px-4 py-3 text-right">Preço base</th>
                <th className="px-4 py-3 text-right">Ações</th>
              </tr>
            </thead>
            <tbody>
              {(products.data ?? []).map(product => (
                <Row
                  key={product.id}
                  product={product}
                  busy={publish.isPending || unpublish.isPending || archive.isPending}
                  onPublish={() => publish.mutate(product.id)}
                  onUnpublish={() => unpublish.mutate(product.id)}
                  onArchive={() => archive.mutate(product.id)}
                />
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}

function Row({
  product,
  busy,
  onPublish,
  onUnpublish,
  onArchive,
}: {
  product: AdminProductSummary;
  busy: boolean;
  onPublish: () => void;
  onUnpublish: () => void;
  onArchive: () => void;
}) {
  return (
    <tr className="border-b border-neutral-100 last:border-0">
      <td className="px-4 py-3">
        <Link
          to={`/admin/produtos/${encodeURIComponent(product.id)}`}
          className="font-bold text-neutral-900 hover:text-primary-600"
        >
          {product.name}
        </Link>
        <div className="text-xs text-neutral-400">{product.id}</div>
      </td>
      <td className="px-4 py-3 text-neutral-600">{product.category}</td>
      <td className="px-4 py-3">
        <StatusBadge status={product.status} />
      </td>
      <td className="px-4 py-3 text-right font-semibold text-neutral-700">
        {product.basePriceCents !== null ? formatCurrency(product.basePriceCents) : '—'}
      </td>
      <td className="px-4 py-3">
        <div className="flex justify-end gap-2">
          <Link
            to={`/admin/produtos/${encodeURIComponent(product.id)}/preview`}
            className="rounded-lg border border-neutral-300 p-1.5 text-neutral-600 transition hover:bg-neutral-50"
            title="Pré-visualizar"
          >
            <Eye className="h-4 w-4" />
          </Link>

          {product.status === 'PUBLISHED' ? (
            <RowAction label="Despublicar" onClick={onUnpublish} disabled={busy} />
          ) : (
            <RowAction label="Publicar" onClick={onPublish} disabled={busy} primary />
          )}

          {product.status !== 'ARCHIVED' ? (
            <RowAction label="Arquivar" onClick={onArchive} disabled={busy} />
          ) : null}
        </div>
      </td>
    </tr>
  );
}

function RowAction({
  label,
  onClick,
  disabled,
  primary,
}: {
  label: string;
  onClick: () => void;
  disabled?: boolean;
  primary?: boolean;
}) {
  return (
    <button
      onClick={onClick}
      disabled={disabled}
      className={[
        'rounded-lg px-3 py-1.5 text-xs font-semibold transition disabled:opacity-40',
        primary
          ? 'bg-primary-500 text-white hover:bg-primary-600'
          : 'border border-neutral-300 text-neutral-700 hover:bg-neutral-50',
      ].join(' ')}
    >
      {label}
    </button>
  );
}

const PROBLEM_LABELS: Record<string, string> = {
  missing_hero_image: 'Falta a imagem de capa',
  missing_priced_offer: 'Falta ao menos uma oferta com preço maior que zero',
  missing_content_headline: 'Falta o título do conteúdo',
};

function formatPublishError(error: unknown): string {
  const body = (error as { body?: { error?: string; reasons?: string[] } })?.body;

  if (body?.reasons?.length) {
    const reasons = body.reasons.map(reason => PROBLEM_LABELS[reason] ?? reason);
    return `Não é possível publicar: ${reasons.join('; ')}.`;
  }

  return getApiErrorMessage(error, 'Não foi possível publicar o produto.');
}
