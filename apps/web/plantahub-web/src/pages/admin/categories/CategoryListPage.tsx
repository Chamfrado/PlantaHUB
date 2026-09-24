import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Plus } from 'lucide-react';
import { useState } from 'react';
import {
  AdminPageHeader,
  DataError,
  FormField,
  PrimaryButton,
  SecondaryButton,
  TextInput,
} from '../../../components/admin/ui/primitives';
import { getApiErrorMessage } from '../../../lib/api-error';
import { createCategory, listAdminCategories } from '../../../services/admin/admin.service';
import { resetCategoriesCache } from '../../../services/categories.service';

export default function CategoryListPage() {
  const queryClient = useQueryClient();

  const [creating, setCreating] = useState(false);
  const [slug, setSlug] = useState('');
  const [name, setName] = useState('');

  const categories = useQuery({ queryKey: ['admin', 'categories'], queryFn: listAdminCategories });

  const create = useMutation({
    mutationFn: () => createCategory({ slug, name }),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['admin', 'categories'] });
      // A lista pública é memoizada em módulo; sem limpar, o site só veria a categoria
      // nova no próximo carregamento completo.
      resetCategoriesCache();
      setCreating(false);
      setSlug('');
      setName('');
    },
  });

  return (
    <div className="space-y-6">
      <AdminPageHeader
        title="Categorias"
        description="Uma categoria pode existir antes de ter produtos — é por isso que ela é cadastrada, e não deduzida da lista."
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

          {create.isError ? (
            <DataError message={getApiErrorMessage(create.error, 'Não foi possível criar.')} />
          ) : null}

          <div className="flex justify-end gap-3">
            <SecondaryButton onClick={() => setCreating(false)}>Cancelar</SecondaryButton>
            <PrimaryButton onClick={() => create.mutate()} disabled={!slug || !name || create.isPending}>
              Criar
            </PrimaryButton>
          </div>
        </section>
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
          <table className="w-full min-w-[560px] text-sm">
            <thead className="border-b border-neutral-200 text-left text-xs uppercase tracking-wide text-neutral-500">
              <tr>
                <th className="px-4 py-3">Slug</th>
                <th className="px-4 py-3">Nome</th>
                <th className="px-4 py-3">Na home</th>
                <th className="px-4 py-3 text-right">Produtos</th>
              </tr>
            </thead>
            <tbody>
              {(categories.data ?? []).map(category => (
                <tr key={category.slug} className="border-b border-neutral-100 last:border-0">
                  <td className="px-4 py-3">
                    <code className="font-bold text-neutral-900">{category.slug}</code>
                  </td>
                  <td className="px-4 py-3 text-neutral-700">{category.name}</td>
                  <td className="px-4 py-3 text-xs text-neutral-500">
                    {category.featuredOnHome ? 'Sim' : 'Não'}
                  </td>
                  <td className="px-4 py-3 text-right font-semibold text-neutral-700">
                    {category.productCount}
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
