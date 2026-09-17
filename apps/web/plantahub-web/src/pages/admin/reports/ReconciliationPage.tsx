import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import {
  AdminPageHeader,
  DataError,
  EmptyState,
  PrimaryButton,
  SecondaryButton,
} from '../../../components/admin/ui/primitives';
import { http } from '../../../lib/http';
import { getApiErrorMessage } from '../../../lib/api-error';

type Run = {
  id: string;
  dryRun: boolean;
  status: string;
  startedAt: string;
  finishedAt: string | null;
  objectsScanned: number;
  assetsCreated: number;
  assetsUpdated: number;
  assetsMatched: number;
  findingsCount: number;
  errorMessage: string | null;
};

type Finding = {
  id: string;
  type: string;
  severity: string;
  storageKey: string | null;
  productId: string | null;
  planTypeCode: string | null;
  detail: string | null;
};

const FINDING_LABELS: Record<string, string> = {
  CREATED: 'Arquivo registrado',
  METADATA_UPDATED: 'Metadado preenchido',
  MISSING_PRODUCT_PLAN_TYPE: 'Vínculo produto-coleção criado',
  UNKNOWN_PRODUCT: 'Produto inexistente',
  UNKNOWN_COLLECTION: 'Pasta sem coleção correspondente',
  MEDIA_CANDIDATE: 'Imagem de vitrine (não é arquivo de entrega)',
  UNPARSEABLE_KEY: 'Chave fora do formato',
  ORPHAN_DB: 'Registro sem arquivo no bucket',
  DUPLICATE_FILENAME: 'Nomes repetidos na mesma coleção',
};

export default function ReconciliationPage() {
  const queryClient = useQueryClient();
  const [selectedRun, setSelectedRun] = useState<string | null>(null);

  const runs = useQuery({
    queryKey: ['admin', 'reconciliation'],
    queryFn: () => http<{ content: Run[] }>('/v1/admin/reconciliation/runs'),
  });

  const start = useMutation({
    mutationFn: (dryRun: boolean) =>
      http<{ runId: string }>(`/v1/admin/reconciliation/runs?dryRun=${dryRun}`, {
        method: 'POST',
      }),
    onSuccess: result => {
      setSelectedRun(result.runId);
      void queryClient.invalidateQueries({ queryKey: ['admin', 'reconciliation'] });
    },
  });

  const findings = useQuery({
    queryKey: ['admin', 'reconciliation', selectedRun],
    queryFn: () =>
      http<{ content: Finding[] }>(
        `/v1/admin/reconciliation/runs/${selectedRun}/findings?size=200`
      ),
    enabled: !!selectedRun,
  });

  const list = runs.data?.content ?? [];

  return (
    <div className="space-y-6">
      <AdminPageHeader
        title="Reconciliação"
        description="Compara o bucket com o que o banco conhece. Nunca apaga nada: divergências viram achados para você decidir."
        actions={
          <>
            <SecondaryButton onClick={() => start.mutate(true)} disabled={start.isPending}>
              Ensaiar
            </SecondaryButton>
            <PrimaryButton onClick={() => start.mutate(false)} disabled={start.isPending}>
              Executar
            </PrimaryButton>
          </>
        }
      />

      <p className="max-w-3xl rounded-xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-800">
        Sempre rode o ensaio primeiro e trate os achados marcados como{' '}
        <strong>pasta sem coleção correspondente</strong> antes de executar — arquivos
        naquelas pastas são ignorados até a coleção existir.
      </p>

      {start.isError ? (
        <DataError message={getApiErrorMessage(start.error, 'Não foi possível iniciar.')} />
      ) : null}

      {runs.isLoading ? (
        <div className="h-48 animate-pulse rounded-2xl bg-white" />
      ) : list.length === 0 ? (
        <EmptyState message="Nenhuma varredura executada ainda." />
      ) : (
        <div className="overflow-x-auto rounded-2xl border border-neutral-200 bg-white">
          <table className="w-full min-w-[720px] text-sm">
            <thead className="border-b border-neutral-200 text-left text-xs uppercase tracking-wide text-neutral-500">
              <tr>
                <th className="px-4 py-3">Quando</th>
                <th className="px-4 py-3">Modo</th>
                <th className="px-4 py-3">Status</th>
                <th className="px-4 py-3 text-right">Objetos</th>
                <th className="px-4 py-3 text-right">Criados</th>
                <th className="px-4 py-3 text-right">Achados</th>
              </tr>
            </thead>
            <tbody>
              {list.map(run => (
                <tr
                  key={run.id}
                  onClick={() => setSelectedRun(run.id)}
                  className={[
                    'cursor-pointer border-b border-neutral-100 last:border-0',
                    selectedRun === run.id ? 'bg-orange-50' : 'hover:bg-neutral-50',
                  ].join(' ')}
                >
                  <td className="px-4 py-3 text-neutral-600">
                    {new Date(run.startedAt).toLocaleString('pt-BR')}
                  </td>
                  <td className="px-4 py-3 text-neutral-600">
                    {run.dryRun ? 'Ensaio' : 'Execução'}
                  </td>
                  <td className="px-4 py-3 text-neutral-600">{run.status}</td>
                  <td className="px-4 py-3 text-right">{run.objectsScanned}</td>
                  <td className="px-4 py-3 text-right">{run.assetsCreated}</td>
                  <td className="px-4 py-3 text-right">{run.findingsCount}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {selectedRun ? (
        <section className="rounded-2xl border border-neutral-200 bg-white p-6">
          <h2 className="text-base font-extrabold text-neutral-900">Achados</h2>

          {findings.isLoading ? (
            <div className="mt-4 h-32 animate-pulse rounded-xl bg-neutral-100" />
          ) : (findings.data?.content ?? []).length === 0 ? (
            <p className="mt-4 text-sm text-neutral-500">Nenhum achado nesta varredura.</p>
          ) : (
            <ul className="mt-4 space-y-2">
              {(findings.data?.content ?? []).map(finding => (
                <li
                  key={finding.id}
                  className="rounded-xl border border-neutral-200 px-4 py-3 text-sm"
                >
                  <div className="flex flex-wrap items-center gap-2">
                    <span
                      className={[
                        'rounded-full px-2 py-0.5 text-xs font-bold',
                        finding.severity === 'WARN'
                          ? 'bg-amber-100 text-amber-800'
                          : 'bg-neutral-100 text-neutral-600',
                      ].join(' ')}
                    >
                      {FINDING_LABELS[finding.type] ?? finding.type}
                    </span>
                    {finding.storageKey ? (
                      <code className="text-xs text-neutral-500">{finding.storageKey}</code>
                    ) : null}
                  </div>
                  {finding.detail ? (
                    <p className="mt-1.5 text-xs text-neutral-600">{finding.detail}</p>
                  ) : null}
                </li>
              ))}
            </ul>
          )}
        </section>
      ) : null}
    </div>
  );
}
