import { useCallback, useRef, useState } from 'react';
import {
  isExpired,
  isRetryable,
  putWithProgress,
  UploadAbortedError,
} from '../lib/upload/xhrUpload';
import {
  abortUpload,
  completeMultipart,
  confirmUpload,
  presignUpload,
  SessionExpiredError,
  type PresignResponse,
  type TargetKind,
} from '../services/admin/upload.service';

export type UploadItemStatus =
  | 'queued'
  | 'presigning'
  | 'uploading'
  | 'confirming'
  | 'done'
  | 'failed'
  | 'canceled';

export type UploadItem = {
  id: string;
  file: File;
  /** Caminho dentro da coleção, vindo de `webkitRelativePath`. */
  relativePath?: string;
  collectionCode: string;
  status: UploadItemStatus;
  progress: number;
  error?: string;
  attempts: number;
};

export type QueueStatus = 'idle' | 'running' | 'paused' | 'finished';

/** Três arquivos em paralelo. Navegadores limitam ~6 conexões por host; sobra folga
 *  para as próprias requisições do painel continuarem respondendo. */
const MAX_PARALLEL_FILES = 3;

/** Backoff: 1s, 3s, 9s. Três tentativas automáticas. */
const RETRY_DELAYS_MS = [1000, 3000, 9000];

const sleep = (ms: number) => new Promise(resolve => setTimeout(resolve, ms));

type StartOptions = {
  productId: string;
  targetKind: TargetKind;
};

export function useUploadQueue(options: StartOptions) {
  const [items, setItems] = useState<UploadItem[]>([]);
  const [status, setStatus] = useState<QueueStatus>('idle');

  // Refs porque o laço de upload roda fora do ciclo de render e precisa ler o estado mais
  // recente, não o congelado na closure do momento em que começou.
  const pausedRef = useRef(false);
  const abortControllers = useRef(new Map<string, AbortController>());
  const uploadIds = useRef(new Map<string, string>());
  const inFlight = useRef(new Set<string>());
  const itemsRef = useRef<UploadItem[]>([]);

  // Espelho do estado para o laço assíncrono ler valores atuais em vez dos da closure.
  itemsRef.current = items;

  const patch = useCallback((id: string, partial: Partial<UploadItem>) => {
    setItems(prev => prev.map(item => (item.id === id ? { ...item, ...partial } : item)));
  }, []);

  /**
   * Enfileira arquivos já resolvidos.
   *
   * Coleção e subpasta vêm de fora porque quem decide o mapeamento é a tabela que o
   * administrador confirma antes de o envio começar — não uma regra escondida aqui.
   */
  const add = useCallback((entries: { file: File; collectionCode: string; relativePath?: string }[]) => {
    setItems(prev => [
      ...prev,
      ...entries.map(entry => ({
        id: crypto.randomUUID(),
        file: entry.file,
        relativePath: entry.relativePath,
        collectionCode: entry.collectionCode,
        status: 'queued' as const,
        progress: 0,
        attempts: 0,
      })),
    ]);
  }, []);

  const remove = useCallback((id: string) => {
    setItems(prev => prev.filter(item => item.id !== id));
  }, []);

  const cancel = useCallback(
    async (id: string) => {
      abortControllers.current.get(id)?.abort();

      const uploadId = uploadIds.current.get(id);
      if (uploadId) {
        // Sem abortar no servidor, as partes já enviadas continuam sendo cobradas.
        await abortUpload(uploadId).catch(() => undefined);
      }

      patch(id, { status: 'canceled' });
    },
    [patch]
  );

  const uploadOne = useCallback(
    async (item: UploadItem): Promise<void> => {
      const controller = new AbortController();
      abortControllers.current.set(item.id, controller);

      for (let attempt = 0; attempt <= RETRY_DELAYS_MS.length; attempt++) {
        try {
          patch(item.id, { status: 'presigning', attempts: attempt, error: undefined });

          const presigned = await presignUpload({
            targetKind: options.targetKind,
            productId: options.productId,
            collectionCode: item.collectionCode,
            filename: item.file.name,
            contentType: item.file.type || 'application/octet-stream',
            sizeBytes: item.file.size,
            relativePath: item.relativePath,
          });

          uploadIds.current.set(item.id, presigned.uploadId);

          patch(item.id, { status: 'uploading', progress: 0 });

          if (presigned.method === 'MULTIPART') {
            await uploadMultipart(item, presigned, controller.signal, patch);
          } else {
            await putWithProgress({
              url: presigned.url!,
              body: item.file,
              contentType: presigned.contentType,
              signal: controller.signal,
              onProgress: p =>
                patch(item.id, { progress: Math.round((p.loaded / p.total) * 100) }),
            });
          }

          patch(item.id, { status: 'confirming', progress: 100 });
          await confirmUpload(presigned.uploadId);
          patch(item.id, { status: 'done' });
          return;
        } catch (error) {
          if (error instanceof UploadAbortedError) {
            patch(item.id, { status: 'canceled' });
            return;
          }

          if (error instanceof SessionExpiredError) {
            // Pausa em vez de falhar: a pessoa faz login em outra aba e retoma, em vez de
            // perder uma fila inteira que já subiu metade.
            pausedRef.current = true;
            setStatus('paused');
            patch(item.id, {
              status: 'failed',
              error: 'Sessão expirada. Faça login em outra aba e clique em Retomar.',
            });
            return;
          }

          const lastAttempt = attempt === RETRY_DELAYS_MS.length;

          // URL vencida: repetir a mesma URL só queima tentativas. O laço volta ao
          // presign, que é exatamente o que resolve.
          if (!lastAttempt && (isRetryable(error) || isExpired(error))) {
            await sleep(RETRY_DELAYS_MS[attempt]);
            continue;
          }

          patch(item.id, {
            status: 'failed',
            error: error instanceof Error ? error.message : 'Falha no envio.',
          });
          return;
        } finally {
          abortControllers.current.delete(item.id);
        }
      }
    },
    [options.productId, options.targetKind, patch]
  );

  const start = useCallback(async () => {
    pausedRef.current = false;
    setStatus('running');

    const pending = () =>
      itemsRef.current.filter(i => i.status === 'queued' || i.status === 'failed');

    // Pool de tamanho fixo: cada worker puxa o próximo da fila ao terminar.
    const workers = Array.from({ length: MAX_PARALLEL_FILES }, async () => {
      for (;;) {
        if (pausedRef.current) return;

        const next = pending().find(i => !inFlight.current.has(i.id));
        if (!next) return;

        inFlight.current.add(next.id);
        try {
          await uploadOne(next);
        } finally {
          inFlight.current.delete(next.id);
        }
      }
    });

    await Promise.all(workers);

    if (!pausedRef.current) {
      setStatus('finished');
    }
  }, [uploadOne]);

  return {
    items,
    status,
    add,
    remove,
    cancel,
    start,
    retryFailed: start,
    clear: () => setItems([]),
  };
}

async function uploadMultipart(
  item: UploadItem,
  presigned: PresignResponse,
  signal: AbortSignal,
  patch: (id: string, partial: Partial<UploadItem>) => void
) {
  const partSize = presigned.partSizeBytes!;
  const parts = presigned.parts ?? [];
  const etags: { partNumber: number; etag: string }[] = [];

  let uploaded = 0;

  for (const part of parts) {
    const start = (part.partNumber - 1) * partSize;
    const chunk = item.file.slice(start, Math.min(start + partSize, item.file.size));

    const etag = await putWithProgress({
      url: part.url,
      body: chunk,
      signal,
      onProgress: p =>
        patch(item.id, {
          progress: Math.round(((uploaded + p.loaded) / item.file.size) * 100),
        }),
    });

    if (!etag) {
      // Sem `ExposeHeaders: ["ETag"]` no CORS do bucket, o navegador não consegue ler o
      // ETag e o multipart nunca fecha. É a causa número um de "falha em silêncio".
      throw new Error(
        'O bucket não expõe o cabeçalho ETag. Configure ExposeHeaders no CORS da S3.'
      );
    }

    etags.push({ partNumber: part.partNumber, etag });
    uploaded += chunk.size;
  }

  await completeMultipart(presigned.uploadId, etags);
}

export function webkitPathOf(file: File): string | null {
  const full = (file as File & { webkitRelativePath?: string }).webkitRelativePath;
  return full && full.includes('/') ? full : null;
}

/**
 * O primeiro segmento do caminho — é ele que indica a coleção de destino.
 *
 * Em `ARCH/plantas/planta.dwg`, a pasta escolhida no seletor vira o primeiro segmento.
 */
export function topFolderOf(file: File): string | null {
  const full = webkitPathOf(file);
  return full ? (full.split('/')[0] ?? null) : null;
}

/**
 * Subpasta **dentro** da coleção.
 *
 * O primeiro segmento é descartado porque ele já foi consumido como coleção: mantê-lo
 * duplicaria `ARCH/` dentro do próprio caminho da coleção ARCH.
 */
export function subPathOf(file: File): string | undefined {
  const full = webkitPathOf(file);
  if (!full) return undefined;

  const segments = full.split('/');
  const middle = segments.slice(1, -1);

  return middle.length > 0 ? middle.join('/') : undefined;
}
