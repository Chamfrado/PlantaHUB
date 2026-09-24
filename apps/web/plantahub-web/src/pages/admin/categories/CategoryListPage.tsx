import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { ArrowDown, ArrowUp, ListOrdered, Plus, Trash2 } from 'lucide-react';
import { useState } from 'react';
import {
  AdminPageHeader,
  DataError,
  FormField,
  PrimaryButton,
  SecondaryButton,
  StatusBadge,
  TextInput,
  ToggleSwitch,
} from '../../../components/admin/ui/primitives';
import { getApiErrorMessage } from '../../../lib/api-error';
import {
  createCategory,
  deleteCategory,
  listAdminCategories,
  listCategoryProducts,
  reorderCategories,
  reorderCategoryProducts,
  updateCategory,
} from '../../../services/admin/admin.service';
import { resetCategoriesCache } from '../../../services/categories.service';
import type { AdminCategory } from '../../../types/api/admin';

const ERROR_MESSAGES: Record<string, string> = {
  category_has_products_deactivate_instead:
    'Esta categoria tem produtos. Mova os produtos para outra categoria ou apenas oculte-a.',
};

function errorMessage(error: unknown, fallback: string) {
  const message = getApiErrorMessage(error, fallback);
  return ERROR_MESSAGES[message] ?? message;
}

/** Troca o item de posição com o vizinho; devolve a lista nova ou null se já está na ponta. */
function move<T>(list: T[], index: number, delta: -1 | 1): T[] | null {
  const target = index + delta;
  if (target < 0 || target >= list.length) return null;
  const next = [...list];
  [next[index], next[target]] = [next[target], next[index]];
  return next;
}

export default function CategoryListPage() {
  const queryClient = useQueryClient();

  const [creating, setCreating] = useState(false);
  const [slug, setSlug] = useState('');
  const [name, setName] = useState('');
  const [comingSoon, setComingSoon] = useState(false);
  const [ordering, setOrdering] = useState<string | null>(null);

  const categories = useQuery({ queryKey: ['admin', 'categories'], queryFn: listAdminCategories });

  function refresh() {
    void queryClient.invalidateQueries({ queryKey: ['admin', 'categories'] });
    // A lista pública é memoizada em módulo; sem limpar, o site só veria a mudança
    // no próximo carregamento completo.
    resetCategoriesCache();
  }

  const create = useMutation({
    mutationFn: () => createCategory({ slug, name, comingSoon }),
    onSuccess: () => {
      refresh();
      setCreating(false);
      setSlug('');
      setName('');
      setComingSoon(false);
    },
  });

  const update = useMutation({
    mutationFn: ({ slug, input }: { slug: string; input: Parameters<typeof updateCategory>[1] }) =>
      updateCategory(slug, input),
    onSuccess: refresh,
  });

  const reorder = useMutation({ mutationFn: reorderCategories, onSuccess: refresh });

  const remove = useMutation({ mutationFn: deleteCategory, onSuccess: refresh });

  const list = categories.data ?? [];
  const busy = update.isPending || reorder.isPending || remove.isPending;
  const actionError = update.error ?? reorder.error ?? remove.error;

  function onMove(index: number, delta: -1 | 1) {
    const next = move(list, index, delta);
    if (next) reorder.mutate(next.map(c => c.slug));
  }

  function onDelete(category: AdminCategory) {
    if (window.confirm(`Excluir a categoria "${category.name}"? Esta ação não pode ser desfeita.`)) {
      remove.mutate(category.slug);
    }
  }

  return (
    <div className="space-y-6">
      <AdminPageHeader
        title="Categorias"
        description="A ordem desta lista é a ordem das abas na página de produtos. Categorias ocultas somem do site; as marcadas como “em breve” aparecem desativadas, com o aviso “Em breve”."
        actions={
          <PrimaryButton onClick={() => setCreating(v => !v)}>
            <Plus className="h-4 w-4" /> Nova categoria
          </PrimaryButton>
        }
      />

      {creating ? (
        <section className="max-w-xl space-y-5 rounded-2xl border border-neutral-200 bg-white p-6">
          <FormField label="Slug" hint="Minúsculas e hifens. Aparece na URL pública dos produtos.">
            <TextInput
              value={slug}
              onChange={e => setSlug(e.target.value.toLowerCase())}
              placeholder="studios"
            />
          </FormField>

          <FormField label="Nome">
            <TextInput value={name} onChange={e => setName(e.target.value)} placeholder="Studios" />
          </FormField>

          <ToggleSwitch
            checked={comingSoon}
            onChange={setComingSoon}
            label="Em breve (aparece no site desativada, com o aviso “Em breve”)"
          />

          {create.isError ? (
            <DataError message={errorMessage(create.error, 'Não foi possível criar.')} />
          ) : null}

          <div className="flex justify-end gap-3">
            <SecondaryButton onClick={() => setCreating(false)}>Cancelar</SecondaryButton>
            <PrimaryButton onClick={() => create.mutate()} disabled={!slug || !name || create.isPending}>
              Criar
            </PrimaryButton>
          </div>
        </section>
      ) : null}

      {actionError ? (
        <DataError message={errorMessage(actionError, 'Não foi possível salvar a alteração.')} />
      ) : null}

      {categories.isLoading ? (
        <div className="h-48 animate-pulse rounded-2xl bg-white" />
      ) : categories.isError ? (
        <DataError
          message={getApiErrorMessage(categories.error, 'Não foi possível carregar as categorias.')}
          onRetry={() => void categories.refetch()}
        />
      ) : (
        <div className="overflow-x-auto rounded-2xl border border-neutral-200 bg-white">
          <table className="w-full min-w-[880px] text-sm">
            <thead className="border-b border-neutral-200 text-left text-xs uppercase tracking-wide text-neutral-500">
              <tr>
                <th className="px-4 py-3">Ordem</th>
                <th className="px-4 py-3">Slug</th>
                <th className="px-4 py-3">Nome</th>
                <th className="px-4 py-3 text-right">Produtos</th>
                <th className="px-4 py-3">Em breve</th>
                <th className="px-4 py-3">Na home</th>
                <th className="px-4 py-3">Visível</th>
                <th className="px-4 py-3 text-right">Ações</th>
              </tr>
            </thead>
            <tbody>
              {list.map((category, index) => (
                <CategoryRow
                  key={category.slug}
                  category={category}
                  first={index === 0}
                  last={index === list.length - 1}
                  busy={busy}
                  ordering={ordering === category.slug}
                  onMoveUp={() => onMove(index, -1)}
                  onMoveDown={() => onMove(index, 1)}
                  onUpdate={input => update.mutate({ slug: category.slug, input })}
                  onToggleOrdering={() =>
                    setOrdering(current => (current === category.slug ? null : category.slug))
                  }
                  onDelete={() => onDelete(category)}
                />
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}

function CategoryRow({
  category,
  first,
  last,
  busy,
  ordering,
  onMoveUp,
  onMoveDown,
  onUpdate,
  onToggleOrdering,
  onDelete,
}: {
  category: AdminCategory;
  first: boolean;
  last: boolean;
  busy: boolean;
  ordering: boolean;
  onMoveUp: () => void;
  onMoveDown: () => void;
  onUpdate: (input: Parameters<typeof updateCategory>[1]) => void;
  onToggleOrdering: () => void;
  onDelete: () => void;
}) {
  const hasProducts = category.productCount > 0;

  return (
    <>
      <tr
        className={[
          'border-b border-neutral-100 last:border-0',
          category.active ? '' : 'bg-neutral-50 text-neutral-400',
        ].join(' ')}
      >
        <td className="px-4 py-3">
          <OrderButtons first={first} last={last} disabled={busy} onUp={onMoveUp} onDown={onMoveDown} />
        </td>
        <td className="px-4 py-3">
          <code className="font-bold text-neutral-900">{category.slug}</code>
        </td>
        <td className="px-4 py-3 text-neutral-700">{category.name}</td>
        <td className="px-4 py-3 text-right font-semibold text-neutral-700">
          {category.productCount}
        </td>
        <td className="px-4 py-3">
          <ToggleSwitch
            checked={category.comingSoon}
            disabled={busy}
            label=""
            onChange={next => onUpdate({ comingSoon: next })}
          />
        </td>
        <td className="px-4 py-3">
          <ToggleSwitch
            checked={category.featuredOnHome}
            disabled={busy}
            label=""
            onChange={next => onUpdate({ featuredOnHome: next })}
          />
        </td>
        <td className="px-4 py-3">
          <ToggleSwitch
            checked={category.active}
            disabled={busy}
            label=""
            onChange={next => onUpdate({ active: next })}
          />
        </td>
        <td className="px-4 py-3">
          <div className="flex justify-end gap-2">
            <button
              onClick={onToggleOrdering}
              disabled={!hasProducts}
              title={hasProducts ? 'Ordenar produtos desta categoria' : 'Sem produtos para ordenar'}
              className="inline-flex items-center gap-1.5 rounded-lg border border-neutral-300 px-2.5 py-1.5 text-xs font-semibold text-neutral-700 transition hover:bg-neutral-50 disabled:opacity-40"
            >
              <ListOrdered className="h-3.5 w-3.5" /> Produtos
            </button>
            <button
              onClick={onDelete}
              disabled={busy || hasProducts}
              title={
                hasProducts
                  ? 'Só é possível excluir uma categoria sem produtos. Oculte-a em vez disso.'
                  : 'Excluir categoria'
              }
              className="inline-flex items-center rounded-lg border border-red-200 px-2 py-1.5 text-red-600 transition hover:bg-red-50 disabled:opacity-40"
            >
              <Trash2 className="h-3.5 w-3.5" />
            </button>
          </div>
        </td>
      </tr>

      {ordering ? (
        <tr className="border-b border-neutral-100 bg-neutral-50">
          <td colSpan={8} className="px-4 py-4">
            <ProductOrder slug={category.slug} />
          </td>
        </tr>
      ) : null}
    </>
  );
}

/** Ordem dos produtos dentro da aba da categoria, na página pública. */
function ProductOrder({ slug }: { slug: string }) {
  const queryClient = useQueryClient();
  const queryKey = ['admin', 'categories', slug, 'products'];

  const products = useQuery({ queryKey, queryFn: () => listCategoryProducts(slug) });

  const reorder = useMutation({
    mutationFn: (ids: string[]) => reorderCategoryProducts(slug, ids),
    onSuccess: data => {
      queryClient.setQueryData(queryKey, data);
      void queryClient.invalidateQueries({ queryKey: ['admin', 'products'] });
    },
  });

  if (products.isLoading) return <div className="h-24 animate-pulse rounded-xl bg-white" />;

  if (products.isError) {
    return (
      <DataError
        message={getApiErrorMessage(products.error, 'Não foi possível carregar os produtos.')}
        onRetry={() => void products.refetch()}
      />
    );
  }

  const list = products.data ?? [];

  return (
    <div className="space-y-2">
      <p className="text-xs text-neutral-500">
        Ordem em que os produtos aparecem nesta aba da página de produtos. Rascunhos e
        arquivados não aparecem no site, mas mantêm sua posição.
      </p>

      {reorder.isError ? (
        <DataError message={getApiErrorMessage(reorder.error, 'Não foi possível reordenar.')} />
      ) : null}

      <ol className="divide-y divide-neutral-100 rounded-xl border border-neutral-200 bg-white">
        {list.map((product, index) => (
          <li key={product.id} className="flex items-center gap-3 px-3 py-2">
            <span className="w-6 text-right text-xs font-semibold text-neutral-400">{index + 1}</span>
            <OrderButtons
              first={index === 0}
              last={index === list.length - 1}
              disabled={reorder.isPending}
              onUp={() => {
                const next = move(list, index, -1);
                if (next) reorder.mutate(next.map(p => p.id));
              }}
              onDown={() => {
                const next = move(list, index, 1);
                if (next) reorder.mutate(next.map(p => p.id));
              }}
            />
            <span className="flex-1 font-medium text-neutral-800">{product.name}</span>
            <StatusBadge status={product.status} />
          </li>
        ))}
      </ol>
    </div>
  );
}

function OrderButtons({
  first,
  last,
  disabled,
  onUp,
  onDown,
}: {
  first: boolean;
  last: boolean;
  disabled: boolean;
  onUp: () => void;
  onDown: () => void;
}) {
  const className =
    'rounded-md border border-neutral-200 p-1 text-neutral-600 transition hover:bg-neutral-100 disabled:opacity-30';

  return (
    <div className="flex gap-1">
      <button onClick={onUp} disabled={disabled || first} className={className} aria-label="Subir">
        <ArrowUp className="h-3.5 w-3.5" />
      </button>
      <button onClick={onDown} disabled={disabled || last} className={className} aria-label="Descer">
        <ArrowDown className="h-3.5 w-3.5" />
      </button>
    </div>
  );
}
