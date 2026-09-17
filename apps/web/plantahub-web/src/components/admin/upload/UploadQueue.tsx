import { CheckCircle2, Loader2, X, XCircle } from 'lucide-react';
import type { UploadItem, QueueStatus } from '../../../hooks/useUploadQueue';
import { PrimaryButton, SecondaryButton } from '../ui/primitives';

type Props = {
  items: UploadItem[];
  status: QueueStatus;
  onStart: () => void;
  onCancel: (id: string) => void;
  onRemove: (id: string) => void;
  onClear: () => void;
};

const STATUS_LABELS: Record<UploadItem['status'], string> = {
  queued: 'Na fila',
  presigning: 'Autorizando',
  uploading: 'Enviando',
  confirming: 'Registrando',
  done: 'Concluído',
  failed: 'Falhou',
  canceled: 'Cancelado',
};

export default function UploadQueue({
  items,
  status,
  onStart,
  onCancel,
  onRemove,
  onClear,
}: Props) {
  if (items.length === 0) return null;

  const done = items.filter(i => i.status === 'done').length;
  const failed = items.filter(i => i.status === 'failed').length;
  const running = status === 'running';

  return (
    <section className="rounded-2xl border border-neutral-200 bg-white p-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h3 className="text-base font-extrabold text-neutral-900">
            Fila de envio ({done}/{items.length})
          </h3>
          {failed > 0 ? (
            <p className="mt-1 text-xs font-semibold text-red-600">
              {failed} arquivo(s) falharam.
            </p>
          ) : null}
        </div>

        <div className="flex gap-2">
          <SecondaryButton onClick={onClear} disabled={running}>
            Limpar
          </SecondaryButton>
          <PrimaryButton onClick={onStart} disabled={running}>
            {status === 'paused'
              ? 'Retomar'
              : failed > 0
                ? 'Tentar novamente'
                : 'Enviar'}
          </PrimaryButton>
        </div>
      </div>

      {status === 'paused' ? (
        <p className="mt-4 rounded-xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-800">
          Envio pausado. A fila foi preservada — resolva o problema indicado e clique em
          Retomar.
        </p>
      ) : null}

      <ul className="mt-4 space-y-2">
        {items.map(item => (
          <li
            key={item.id}
            className="rounded-xl border border-neutral-200 px-4 py-3"
          >
            <div className="flex flex-wrap items-center justify-between gap-3">
              <div className="min-w-0 flex-1">
                <div className="flex items-center gap-2">
                  <StatusIcon status={item.status} />
                  <span className="truncate text-sm font-semibold text-neutral-900">
                    {item.relativePath ? `${item.relativePath}/` : ''}
                    {item.file.name}
                  </span>
                </div>

                <div className="mt-1 text-xs text-neutral-400">
                  {item.collectionCode} · {formatBytes(item.file.size)} ·{' '}
                  {STATUS_LABELS[item.status]}
                  {item.attempts > 0 && item.status !== 'done'
                    ? ` · tentativa ${item.attempts + 1}`
                    : ''}
                </div>

                {item.error ? (
                  <p className="mt-1 text-xs font-semibold text-red-600">{item.error}</p>
                ) : null}
              </div>

              <div className="flex items-center gap-3">
                {item.status === 'uploading' || item.status === 'confirming' ? (
                  <div className="h-1.5 w-32 overflow-hidden rounded-full bg-neutral-200">
                    <div
                      className="h-full bg-primary-500 transition-all"
                      style={{ width: `${item.progress}%` }}
                    />
                  </div>
                ) : null}

                <button
                  onClick={() =>
                    item.status === 'uploading' || item.status === 'presigning'
                      ? onCancel(item.id)
                      : onRemove(item.id)
                  }
                  className="rounded-lg p-1.5 text-neutral-400 transition hover:bg-neutral-100 hover:text-neutral-700"
                  aria-label="Remover da fila"
                >
                  <X className="h-4 w-4" />
                </button>
              </div>
            </div>
          </li>
        ))}
      </ul>
    </section>
  );
}

function StatusIcon({ status }: { status: UploadItem['status'] }) {
  if (status === 'done') return <CheckCircle2 className="h-4 w-4 shrink-0 text-green-600" />;
  if (status === 'failed') return <XCircle className="h-4 w-4 shrink-0 text-red-600" />;
  if (status === 'canceled') return <XCircle className="h-4 w-4 shrink-0 text-neutral-400" />;
  if (status === 'queued') return <span className="h-4 w-4 shrink-0" />;

  return <Loader2 className="h-4 w-4 shrink-0 animate-spin text-primary-500" />;
}

function formatBytes(bytes: number) {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${Math.round(bytes / 1024)} KB`;
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`;
}
