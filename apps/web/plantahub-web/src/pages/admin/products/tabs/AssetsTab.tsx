import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { RotateCcw, Trash2 } from 'lucide-react';
import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import UploadDropzone from '../../../../components/admin/upload/UploadDropzone';
import UploadQueue from '../../../../components/admin/upload/UploadQueue';
import {
  DataError,
  EmptyState,
  SecondaryButton,
  SelectInput,
  ToggleSwitch,
} from '../../../../components/admin/ui/primitives';
import { useUploadQueue } from '../../../../hooks/useUploadQueue';
import { getApiErrorMessage } from '../../../../lib/api-error';
import {
  deleteAsset,
  listAssets,
  listCollections,
  listOffers,
  moveAsset,
  restoreAsset,
} from '../../../../services/admin/admin.service';
import type { AdminAsset, AdminCollection } from '../../../../types/api/admin';

export default function AssetsTab({ productId }: { productId: string }) {
  const queryClient = useQueryClient();

  const [includeDeleted, setIncludeDeleted] = useState(false);
  const [actionError, setActionError] = useState<string | null>(null);

  const assets = useQuery({
    queryKey: ['admin', 'assets', productId, includeDeleted],
    queryFn: () => listAssets(productId, includeDeleted),
  });

  const collections = useQuery({ queryKey: ['admin', 'collections'], queryFn: listCollections });

  // As ofertas dizem quais coleções este produto já tem. Não limitam a escolha — servem
  // para avisar, antes do envio, que escolher uma coleção nova também cria o vínculo.
  const offers = useQuery({
    queryKey: ['admin', 'offers', productId],
    queryFn: () => listOffers(productId),
  });

  const collectionCodes = (collections.data ?? []).map(c => c.code);

  // Sem padrão: a versão anterior usava a primeira coleção da lista, então os arquivos
  // iam para uma coleção que ninguém escolheu — e, dependendo da ordenação, podia ser a
  // que acompanha toda compra.
  const [target, setTarget] = useState('');

  const selected = (collections.data ?? []).find(c => c.code === target) ?? null;
  const linkedCodes = new Set((offers.data ?? []).map(o => o.collectionCode));

  const queue = useUploadQueue({ productId, targetKind: 'ASSET' });

  // Quando a fila termina, a lista precisa refletir o que subiu. Num efeito, e nao durante
  // o render: invalidar cache e efeito colateral e dispararia a cada render.
  useEffect(() => {
    if (queue.status === 'finished') {
      void queryClient.invalidateQueries({ queryKey: ['admin', 'assets', productId] });
    }
  }, [queue.status, queryClient, productId]);

  function refresh() {
    void queryClient.invalidateQueries({ queryKey: ['admin', 'assets', productId] });
  }

  const move = useMutation({
    mutationFn: ({ id, code }: { id: string; code: string }) => moveAsset(id, code),
    onSuccess: refresh,
    onError: (error: unknown) =>
      setActionError(getApiErrorMessage(error, 'Não foi possível mover o arquivo.')),
  });

  const remove = useMutation({
    mutationFn: ({ id, force }: { id: string; force: boolean }) => deleteAsset(id, force),
    onSuccess: refresh,
    onError: (error: unknown) => {
      const code = (error as { body?: { error?: string } })?.body?.error;

      setActionError(
        code === 'asset_granted_to_customers_use_force'
          ? 'Este arquivo já foi entregue a clientes. Remover só é possível confirmando: ' +
              'ele sai do catálogo, mas quem já comprou continua baixando.'
          : getApiErrorMessage(error, 'Não foi possível remover o arquivo.')
      );
    },
  });

  const restore = useMutation({ mutationFn: restoreAsset, onSuccess: refresh });

  return (
    <div className="space-y-5">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <p className="max-w-2xl text-sm text-neutral-500">
          Os arquivos vêm do bucket. Mover um arquivo troca apenas a coleção a que ele
          pertence — a chave de armazenamento nunca muda, porque movê-la invalidaria links
          de download já emitidos.
        </p>

        <ToggleSwitch
          checked={includeDeleted}
          onChange={setIncludeDeleted}
          label="Mostrar removidos"
        />
      </div>

      {collections.isLoading ? (
        <div className="h-24 animate-pulse rounded-2xl bg-white" />
      ) : collectionCodes.length === 0 ? (
        <section className="rounded-2xl border border-neutral-200 bg-white p-6">
          <h2 className="text-base font-extrabold text-neutral-900">Nenhuma coleção ainda</h2>
          <p className="mt-1 text-sm text-neutral-500">
            Arquivo sempre pertence a uma coleção — é ela que define o que o cliente leva ao
            comprar. Crie a primeira para poder enviar arquivos.
          </p>
          <Link
            to="/admin/colecoes"
            className="mt-4 inline-flex rounded-xl bg-primary-500 px-4 py-2.5 text-sm font-semibold text-white transition hover:bg-primary-600"
          >
            Criar coleção
          </Link>
        </section>
      ) : (
        <>
          <section className="rounded-2xl border border-neutral-200 bg-white p-6">
            <h2 className="text-base font-extrabold text-neutral-900">Coleção de destino</h2>
            <p className="mt-1 text-xs text-neutral-500">
              Os arquivos enviados abaixo entram nesta coleção. Quem comprar essa coleção
              leva estes arquivos.
            </p>

            <div className="mt-4 max-w-md">
              <SelectInput
                value={target}
                aria-label="Coleção de destino"
                onChange={event => setTarget(event.target.value)}
              >
                <option value="">Selecione uma coleção…</option>
                {(collections.data ?? []).map(collection => (
                  <option key={collection.code} value={collection.code}>
                    {collection.name} ({collection.code})
                    {collection.bundledWithEveryOffer ? ' · acompanha toda compra' : ''}
                    {!collection.purchasable ? ' · não comprável' : ''}
                  </option>
                ))}
              </SelectInput>
            </div>

            {selected && !linkedCodes.has(selected.code) ? (
              <p className="mt-3 rounded-xl bg-amber-50 px-4 py-3 text-sm text-amber-800">
                Este produto ainda não oferece <strong>{selected.name}</strong>. Ao enviar, o
                vínculo é criado sem preço e fora de venda — defina o preço na aba Ofertas
                para começar a vender.
              </p>
            ) : null}

            {selected?.bundledWithEveryOffer ? (
              <p className="mt-3 rounded-xl bg-neutral-50 px-4 py-3 text-sm text-neutral-600">
                Atenção: esta coleção acompanha <strong>toda</strong> compra do produto. O que
                entrar aqui vai junto com qualquer oferta que o cliente escolher.
              </p>
            ) : null}
          </section>

          <UploadDropzone
            collections={collectionCodes}
            destination={selected ? { code: selected.code, label: `${selected.name} (${selected.code})` } : null}
            onEnqueue={queue.add}
          />
        </>
      )}

      <UploadQueue
        items={queue.items}
        status={queue.status}
        onStart={() => void queue.start()}
        onCancel={id => void queue.cancel(id)}
        onRemove={queue.remove}
        onClear={queue.clear}
      />

      {actionError ? <DataError message={actionError} onRetry={() => setActionError(null)} /> : null}

      {assets.isLoading ? (
        <div className="h-64 animate-pulse rounded-2xl bg-white" />
      ) : assets.isError ? (
        <DataError
          message={getApiErrorMessage(assets.error, 'Não foi possível carregar os arquivos.')}
          onRetry={() => void assets.refetch()}
        />
      ) : (assets.data ?? []).length === 0 ? (
        <EmptyState message="Nenhum arquivo associado a este produto." />
      ) : (
        <div className="overflow-x-auto rounded-2xl border border-neutral-200 bg-white">
          <table className="w-full min-w-[820px] text-sm">
            <thead className="border-b border-neutral-200 text-left text-xs uppercase tracking-wide text-neutral-500">
              <tr>
                <th className="px-4 py-3">Arquivo</th>
                <th className="px-4 py-3">Coleção</th>
                <th className="px-4 py-3">Chave no bucket</th>
                <th className="px-4 py-3 text-right">Ações</th>
              </tr>
            </thead>
            <tbody>
              {(assets.data ?? []).map(asset => (
                <AssetRow
                  key={asset.id}
                  asset={asset}
                  collections={collections.data ?? []}
                  onMove={code => move.mutate({ id: asset.id, code })}
                  onDelete={force => remove.mutate({ id: asset.id, force })}
                  onRestore={() => restore.mutate(asset.id)}
                />
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}

function AssetRow({
  asset,
  collections,
  onMove,
  onDelete,
  onRestore,
}: {
  asset: AdminAsset;
  collections: AdminCollection[];
  onMove: (code: string) => void;
  onDelete: (force: boolean) => void;
  onRestore: () => void;
}) {
  const [confirmingForce, setConfirmingForce] = useState(false);

  return (
    <tr className={['border-b border-neutral-100 last:border-0', asset.deleted ? 'opacity-50' : ''].join(' ')}>
      <td className="px-4 py-3">
        <div className="font-semibold text-neutral-900">{asset.filename}</div>
        <div className="text-xs text-neutral-400">
          {asset.relativePath ? `${asset.relativePath} · ` : ''}
          {asset.sizeBytes !== null ? `${Math.round(asset.sizeBytes / 1024)} KB` : '—'}
          {asset.keyScheme === 'LEGACY' ? ' · legado' : ''}
        </div>
      </td>

      <td className="px-4 py-3">
        <SelectInput
          value={asset.collectionCode}
          disabled={asset.deleted}
          onChange={e => onMove(e.target.value)}
        >
          {/* Mesmo rotulo do seletor de destino: ler "ARCH" num lugar e "Planta
              Arquitetonica (ARCH)" no outro faria parecer coisas diferentes. */}
          {collections.map(collection => (
            <option key={collection.code} value={collection.code}>
              {collection.name} ({collection.code})
            </option>
          ))}
        </SelectInput>
      </td>

      <td className="px-4 py-3">
        {/* Exposta de propósito: é o que permite conferir uma divergência contra o bucket. */}
        <code className="block max-w-xs truncate text-xs text-neutral-500" title={asset.storageKey}>
          {asset.storageKey}
        </code>
      </td>

      <td className="px-4 py-3">
        <div className="flex justify-end gap-2">
          {asset.deleted ? (
            <SecondaryButton onClick={onRestore}>
              <RotateCcw className="h-3.5 w-3.5" /> Restaurar
            </SecondaryButton>
          ) : confirmingForce ? (
            <>
              <button
                onClick={() => {
                  onDelete(true);
                  setConfirmingForce(false);
                }}
                className="rounded-lg bg-red-600 px-3 py-1.5 text-xs font-semibold text-white"
              >
                Confirmar remoção
              </button>
              <SecondaryButton onClick={() => setConfirmingForce(false)}>Cancelar</SecondaryButton>
            </>
          ) : (
            <>
              <SecondaryButton onClick={() => onDelete(false)}>
                <Trash2 className="h-3.5 w-3.5" /> Remover
              </SecondaryButton>
              <button
                onClick={() => setConfirmingForce(true)}
                className="text-xs font-semibold text-neutral-400 underline"
                title="Use quando o arquivo já foi entregue a clientes"
              >
                forçar
              </button>
            </>
          )}
        </div>
      </td>
    </tr>
  );
}
