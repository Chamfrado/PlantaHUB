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
  ToggleSwitch,
} from '../../../components/admin/ui/primitives';
import { getApiErrorMessage } from '../../../lib/api-error';
import {
  createCollection,
  listCollections,
  setCollectionActive,
} from '../../../services/admin/admin.service';

export default function CollectionListPage() {
  const queryClient = useQueryClient();

  const [creating, setCreating] = useState(false);
  const [code, setCode] = useState('');
  const [name, setName] = useState('');
  const [bundled, setBundled] = useState(false);

  const collections = useQuery({ queryKey: ['admin', 'collections'], queryFn: listCollections });

  function refresh() {
    void queryClient.invalidateQueries({ queryKey: ['admin', 'collections'] });
  }

  const create = useMutation({
    mutationFn: () =>
      createCollection({
        code: code.trim().toUpperCase(),
        name,
        bundledWithEveryOffer: bundled,
        purchasable: !bundled,
      }),
    onSuccess: () => {
      refresh();
      setCreating(false);
      setCode('');
      setName('');
      setBundled(false);
    },
  });

  const toggleActive = useMutation({
    mutationFn: ({ id, active }: { id: string; active: boolean }) => setCollectionActive(id, active),
    onSuccess: refresh,
  });

  return (
    <div className="space-y-6">
      <AdminPageHeader
        title="Coleções"
        description="Conjuntos de arquivos que um produto pode oferecer. Nada no sistema conhece nenhum código de antemão."
        actions={
          <PrimaryButton onClick={() => setCreating(v => !v)}>
            <Plus className="h-4 w-4" /> Nova coleção
          </PrimaryButton>
        }
      />

      {creating ? (
        <section className="max-w-xl space-y-5 rounded-2xl border border-neutral-200 bg-white p-6">
          <FormField
            label="Código"
            hint="Maiúsculas, sem espaço. Não pode ser alterado depois: ele está gravado nas chaves dos arquivos já existentes no bucket."
          >
            <TextInput
              value={code}
              onChange={e => setCode(e.target.value.toUpperCase())}
              placeholder="ARCH"
            />
          </FormField>

          <FormField label="Nome">
            <TextInput
              value={name}
              onChange={e => setName(e.target.value)}
              placeholder="Planta Arquitetônica"
            />
          </FormField>

          <ToggleSwitch
            checked={bundled}
            onChange={setBundled}
            label="Acompanha toda oferta (anexo, não é vendida separadamente)"
          />

          {create.isError ? (
            <DataError message={getApiErrorMessage(create.error, 'Não foi possível criar.')} />
          ) : null}

          <div className="flex justify-end gap-3">
            <SecondaryButton onClick={() => setCreating(false)}>Cancelar</SecondaryButton>
            <PrimaryButton onClick={() => create.mutate()} disabled={!code || !name || create.isPending}>
              Criar
            </PrimaryButton>
          </div>
        </section>
      ) : null}

      {collections.isLoading ? (
        <div className="h-64 animate-pulse rounded-2xl bg-white" />
      ) : collections.isError ? (
        <DataError
          message={getApiErrorMessage(collections.error, 'Não foi possível carregar as coleções.')}
          onRetry={() => void collections.refetch()}
        />
      ) : (
        <div className="overflow-x-auto rounded-2xl border border-neutral-200 bg-white">
          <table className="w-full min-w-[680px] text-sm">
            <thead className="border-b border-neutral-200 text-left text-xs uppercase tracking-wide text-neutral-500">
              <tr>
                <th className="px-4 py-3">Código</th>
                <th className="px-4 py-3">Nome</th>
                <th className="px-4 py-3">Tipo</th>
                <th className="px-4 py-3 text-right">Arquivos</th>
                <th className="px-4 py-3 text-right">Ativa</th>
              </tr>
            </thead>
            <tbody>
              {(collections.data ?? []).map(collection => (
                <tr key={collection.id} className="border-b border-neutral-100 last:border-0">
                  <td className="px-4 py-3">
                    <code className="font-bold text-neutral-900">{collection.code}</code>
                  </td>
                  <td className="px-4 py-3 text-neutral-700">{collection.name}</td>
                  <td className="px-4 py-3 text-xs text-neutral-500">
                    {collection.bundledWithEveryOffer
                      ? 'Acompanha toda oferta'
                      : collection.purchasable
                        ? 'Vendável'
                        : 'Não vendável'}
                  </td>
                  <td className="px-4 py-3 text-right font-semibold text-neutral-700">
                    {collection.assetCount}
                  </td>
                  <td className="px-4 py-3">
                    <div className="flex justify-end">
                      <ToggleSwitch
                        checked={collection.active}
                        label=""
                        onChange={next =>
                          toggleActive.mutate({ id: collection.id, active: next })
                        }
                      />
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
