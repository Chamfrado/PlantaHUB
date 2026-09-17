import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { ChevronLeft, ChevronRight, ImagePlus, Star, Trash2 } from 'lucide-react';
import { useEffect, useRef, type ReactNode } from 'react';
import UploadQueue from '../../../../components/admin/upload/UploadQueue';
import {
  DataError,
  EmptyState,
  SecondaryButton,
} from '../../../../components/admin/ui/primitives';
import { useUploadQueue } from '../../../../hooks/useUploadQueue';
import { getApiErrorMessage } from '../../../../lib/api-error';
import {
  deleteMedia,
  listMedia,
  reorderMedia,
  updateMedia,
} from '../../../../services/admin/admin.service';
import type { AdminMedia } from '../../../../types/api/admin';

/**
 * Capa e galeria do produto.
 *
 * <p>Separada da aba de arquivos de propósito: arquivo é o que o cliente compra e baixa,
 * imagem é o que ele vê antes de comprar. Os dois têm limites, formatos, destino no bucket
 * e consequências diferentes — juntá-los numa tela só faria a pessoa escolher a coleção de
 * uma foto de fachada.
 */
export default function MediaTab({ productId }: { productId: string }) {
  const queryClient = useQueryClient();
  const fileInput = useRef<HTMLInputElement>(null);

  const media = useQuery({
    queryKey: ['admin', 'media', productId],
    queryFn: () => listMedia(productId),
  });

  // `collectionCode` é exigido pela fila, mas mídia não pertence a coleção nenhuma: o
  // servidor ignora o campo quando o alvo é MEDIA.
  const queue = useUploadQueue({ productId, targetKind: 'MEDIA' });

  function refresh() {
    void queryClient.invalidateQueries({ queryKey: ['admin', 'media', productId] });
    // A capa alimenta `product.hero_image_url`, que a listagem e o cabeçalho leem.
    void queryClient.invalidateQueries({ queryKey: ['admin', 'product', productId] });
    void queryClient.invalidateQueries({ queryKey: ['admin', 'products'] });
  }

  // Num efeito, e não durante o render: invalidar cache é efeito colateral.
  useEffect(() => {
    if (queue.status !== 'finished') return;

    void queryClient.invalidateQueries({ queryKey: ['admin', 'media', productId] });
    void queryClient.invalidateQueries({ queryKey: ['admin', 'product', productId] });
    void queryClient.invalidateQueries({ queryKey: ['admin', 'products'] });
  }, [queue.status, queryClient, productId]);

  const promote = useMutation({
    mutationFn: (mediaId: string) => updateMedia(mediaId, { role: 'HERO' }),
    onSuccess: refresh,
  });

  const remove = useMutation({
    mutationFn: (mediaId: string) => deleteMedia(mediaId),
    onSuccess: refresh,
  });

  const reorder = useMutation({
    mutationFn: (ids: string[]) => reorderMedia(productId, ids),
    onSuccess: refresh,
  });

  const items = media.data ?? [];
  const hero = items.find(item => item.role === 'HERO') ?? null;
  const gallery = items.filter(item => item.role === 'GALLERY');

  const busy = promote.isPending || remove.isPending || reorder.isPending;

  const actionError =
    promote.error ?? remove.error ?? reorder.error ?? null;

  /** Troca de posição na galeria e grava a ordem inteira. */
  function move(index: number, delta: number) {
    const target = index + delta;
    if (target < 0 || target >= gallery.length) return;

    const next = [...gallery];
    [next[index], next[target]] = [next[target], next[index]];

    // A capa vai na frente: o servidor ordena por papel e depois por posição, então mandar
    // só a galeria deixaria a capa com um `sort_order` maior que o da primeira foto.
    reorder.mutate([...(hero ? [hero.id] : []), ...next.map(item => item.id)]);
  }

  return (
    <div className="max-w-4xl space-y-6">
      <section className="rounded-2xl border border-dashed border-neutral-300 bg-white p-6">
        <h2 className="text-base font-extrabold text-neutral-900">Imagens do produto</h2>
        <p className="mt-1 text-xs text-neutral-500">
          A capa abre a página e aparece na vitrine; as demais formam o carrossel. Você pode
          enviar várias de uma vez. PNG, JPG, WEBP ou AVIF, até 10 MB cada.
        </p>

        <div className="mt-4">
          <SecondaryButton onClick={() => fileInput.current?.click()}>
            <ImagePlus className="h-4 w-4" /> Selecionar imagens
          </SecondaryButton>

          <input
            ref={fileInput}
            type="file"
            multiple
            hidden
            accept="image/png,image/jpeg,image/webp,image/avif"
            aria-label="Selecionar imagens"
            onChange={event => {
              const files = Array.from(event.target.files ?? []);
              if (files.length === 0) return;

              // Sem disparar o envio aqui: `start` le a fila por ref, que so e atualizada
              // no proximo render — e a fila tem o proprio botao Enviar.
              queue.add(files.map(file => ({ file, collectionCode: 'MEDIA' })));

              event.target.value = '';
            }}
          />
        </div>
      </section>

      <UploadQueue
        items={queue.items}
        status={queue.status}
        onStart={() => void queue.start()}
        onCancel={id => void queue.cancel(id)}
        onRemove={queue.remove}
        onClear={queue.clear}
      />

      {actionError ? (
        <DataError
          message={getApiErrorMessage(actionError, 'Não foi possível alterar as imagens.')}
        />
      ) : null}

      {media.isLoading ? (
        <div className="h-64 animate-pulse rounded-2xl bg-white" />
      ) : media.isError ? (
        <DataError
          message={getApiErrorMessage(media.error, 'Não foi possível carregar as imagens.')}
          onRetry={() => void media.refetch()}
        />
      ) : items.length === 0 ? (
        <EmptyState message="Nenhuma imagem ainda. Um produto precisa de uma capa para ser publicado." />
      ) : (
        <div className="space-y-6">
          <section>
            <h3 className="text-sm font-extrabold text-neutral-900">Capa</h3>

            {hero ? (
              <div className="mt-3">
                <MediaCard
                  media={hero}
                  busy={busy}
                  onDelete={() => remove.mutate(hero.id)}
                />
              </div>
            ) : (
              <p className="mt-3 rounded-xl bg-amber-50 px-4 py-3 text-sm text-amber-800">
                Sem capa definida. Escolha uma das imagens abaixo — sem ela o produto não
                pode ser publicado.
              </p>
            )}
          </section>

          <section>
            <h3 className="text-sm font-extrabold text-neutral-900">
              Carrossel{' '}
              <span className="font-semibold text-neutral-400">
                ({gallery.length} {gallery.length === 1 ? 'imagem' : 'imagens'})
              </span>
            </h3>

            {gallery.length === 0 ? (
              <p className="mt-3 text-sm text-neutral-500">
                Envie mais imagens para montar o carrossel da página do produto.
              </p>
            ) : (
              <ul className="mt-3 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
                {gallery.map((item, index) => (
                  <li key={item.id}>
                    <MediaCard
                      media={item}
                      busy={busy}
                      position={index + 1}
                      total={gallery.length}
                      onPromote={() => promote.mutate(item.id)}
                      onMoveUp={index > 0 ? () => move(index, -1) : undefined}
                      onMoveDown={index < gallery.length - 1 ? () => move(index, 1) : undefined}
                      onDelete={() => remove.mutate(item.id)}
                    />
                  </li>
                ))}
              </ul>
            )}
          </section>
        </div>
      )}
    </div>
  );
}

function MediaCard({
  media,
  busy,
  position,
  total,
  onPromote,
  onMoveUp,
  onMoveDown,
  onDelete,
}: {
  media: AdminMedia;
  busy: boolean;
  position?: number;
  total?: number;
  onPromote?: () => void;
  onMoveUp?: () => void;
  onMoveDown?: () => void;
  onDelete: () => void;
}) {
  const name = media.storageKey.split('/').pop() ?? media.storageKey;
  const label = position ? `imagem ${position} de ${total}` : 'capa';

  return (
    <div className="overflow-hidden rounded-2xl border border-neutral-200 bg-white">
      <div className="aspect-video bg-neutral-100">
        {media.url ? (
          <img
            src={media.url}
            alt={media.altText ?? ''}
            className="h-full w-full object-cover"
            loading="lazy"
          />
        ) : (
          // A linha existe mas não tem endereço público: mostrar a chave do bucket como
          // `src` renderizaria um ícone quebrado sem dizer o que houve.
          <div className="flex h-full items-center justify-center px-4 text-center text-xs text-neutral-500">
            Imagem sem URL pública. Verifique a configuração do bucket.
          </div>
        )}
      </div>

      <div className="flex items-center justify-between gap-2 px-3 py-2.5">
        <span className="min-w-0 truncate text-xs text-neutral-500" title={name}>
          {media.role === 'HERO' ? (
            <span className="mr-1.5 font-bold text-primary-600">Capa ·</span>
          ) : null}
          {name}
        </span>

        <div className="flex shrink-0 gap-1">
          {onMoveUp ? (
            <IconAction label={`Mover ${label} para trás`} onClick={onMoveUp} disabled={busy}>
              <ChevronLeft className="h-4 w-4" />
            </IconAction>
          ) : null}

          {onMoveDown ? (
            <IconAction label={`Mover ${label} para frente`} onClick={onMoveDown} disabled={busy}>
              <ChevronRight className="h-4 w-4" />
            </IconAction>
          ) : null}

          {onPromote ? (
            <IconAction label={`Definir ${label} como capa`} onClick={onPromote} disabled={busy}>
              <Star className="h-4 w-4" />
            </IconAction>
          ) : null}

          <IconAction label={`Remover ${label}`} onClick={onDelete} disabled={busy} danger>
            <Trash2 className="h-4 w-4" />
          </IconAction>
        </div>
      </div>
    </div>
  );
}

function IconAction({
  label,
  onClick,
  disabled,
  danger,
  children,
}: {
  label: string;
  onClick: () => void;
  disabled?: boolean;
  danger?: boolean;
  children: ReactNode;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      disabled={disabled}
      aria-label={label}
      title={label}
      className={[
        'rounded-lg border p-1.5 transition disabled:opacity-40',
        danger
          ? 'border-neutral-300 text-red-600 hover:bg-red-50'
          : 'border-neutral-300 text-neutral-600 hover:bg-neutral-50',
      ].join(' ')}
    >
      {children}
    </button>
  );
}
