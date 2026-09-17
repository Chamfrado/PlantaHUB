import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { AlertTriangle, Check, Copy, HelpCircle, KeyRound, RefreshCcw, X } from 'lucide-react';
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
import { http } from '../../../lib/http';

type Status = 'OK' | 'WARN' | 'FAIL' | 'UNKNOWN';

type CheckResult = {
  id: string;
  title: string;
  status: Status;
  detail: string;
  impact: string | null;
};

type Snippet = {
  id: string;
  title: string;
  description: string;
  content: string;
};

type CredentialSource = 'PANEL' | 'CONFIG' | 'DEFAULT_CHAIN';

type CredentialStatus = {
  source: CredentialSource;
  accessKeyHint: string | null;
  updatedAt: string | null;
  updatedBy: string | null;
};

type Report = {
  bucket: string;
  configuredRegion: string;
  publicBaseUrl: string;
  ranAt: string;
  checks: CheckResult[];
  snippets: Snippet[];
};

const STATUS_STYLE: Record<Status, { label: string; className: string; Icon: typeof Check }> = {
  OK: { label: 'OK', className: 'bg-emerald-50 text-emerald-700 border-emerald-200', Icon: Check },
  WARN: { label: 'Atenção', className: 'bg-amber-50 text-amber-800 border-amber-200', Icon: AlertTriangle },
  FAIL: { label: 'Problema', className: 'bg-red-50 text-red-700 border-red-200', Icon: X },
  UNKNOWN: { label: 'Não verificado', className: 'bg-neutral-50 text-neutral-600 border-neutral-200', Icon: HelpCircle },
};

/**
 * Configuração do bucket.
 *
 * <p>A tela **diagnostica e entrega o JSON pronto; quem aplica é você, no console da AWS.**
 * Não é falta de acabamento: para aplicar daqui, a aplicação precisaria de permissão para
 * reescrever CORS, lifecycle e a própria política do bucket em produção. O ganho seria
 * economizar um Ctrl+V que acontece uma vez; o custo seria permanente — "o acervo vendido
 * vazou" passaria a ser um desfecho possível de um bug, em vez de exigir uma ação
 * deliberada de uma pessoa.
 */
export default function StoragePage() {
  const report = useQuery({
    queryKey: ['admin', 'storage', 'diagnostics'],
    queryFn: () => http<Report>('/v1/admin/storage/diagnostics'),
    // Sem cache: a pergunta é "como está agora", tipicamente logo depois de alguém mexer
    // na AWS. Um resultado guardado responderia como estava antes da mudança.
    staleTime: 0,
    gcTime: 0,
  });

  const data = report.data;
  const failures = (data?.checks ?? []).filter(c => c.status === 'FAIL').length;
  const warnings = (data?.checks ?? []).filter(c => c.status === 'WARN').length;

  return (
    <div className="max-w-4xl space-y-6">
      <AdminPageHeader
        title="Armazenamento"
        description="Confere se o bucket está configurado do jeito que o painel precisa."
        actions={
          <PrimaryButton onClick={() => void report.refetch()} disabled={report.isFetching}>
            <RefreshCcw className={`h-4 w-4 ${report.isFetching ? 'animate-spin' : ''}`} />
            {report.isFetching ? 'Verificando…' : 'Verificar agora'}
          </PrimaryButton>
        }
      />

      {report.isLoading ? (
        <div className="h-64 animate-pulse rounded-2xl bg-white" />
      ) : report.isError ? (
        <DataError
          message={getApiErrorMessage(report.error, 'Não foi possível rodar o diagnóstico.')}
          onRetry={() => void report.refetch()}
        />
      ) : data ? (
        <>
          <section className="rounded-2xl border border-neutral-200 bg-white p-6">
            <dl className="grid gap-4 text-sm sm:grid-cols-3">
              <Field label="Bucket" value={data.bucket} />
              <Field label="Região" value={data.configuredRegion} />
              <Field label="Base pública" value={data.publicBaseUrl} />
            </dl>
          </section>

          <CredentialsCard />

          <Summary failures={failures} warnings={warnings} />

          <section className="space-y-3">
            {data.checks.map(check => (
              <CheckRow key={check.id} check={check} />
            ))}
          </section>

          <section className="space-y-4">
            <div>
              <h2 className="text-base font-extrabold text-neutral-900">
                Configuração para aplicar na AWS
              </h2>
              <p className="mt-1 text-sm text-neutral-500">
                Os valores abaixo já vêm com o nome do seu bucket e as origens do seu site.
                O painel não aplica nada sozinho — reescrever a segurança do bucket é uma
                permissão que a aplicação não tem, de propósito.
              </p>
            </div>

            {data.snippets.map(snippet => (
              <SnippetCard key={snippet.id} snippet={snippet} />
            ))}
          </section>
        </>
      ) : null}
    </div>
  );
}

const SOURCE_LABEL: Record<CredentialSource, string> = {
  PANEL: 'Gravada no painel',
  CONFIG: 'Variável de ambiente',
  DEFAULT_CHAIN: 'Instance role ou perfil da máquina',
};

/**
 * Credenciais da AWS.
 *
 * O formulário nunca é preenchido com o valor atual, porque a API não devolve o segredo —
 * só de qual fonte ele vem e os últimos quatro caracteres da chave pública. Um campo que
 * se preenche sozinho com a secret transformaria qualquer XSS no painel num vazamento de
 * credencial da AWS.
 */
function CredentialsCard() {
  const queryClient = useQueryClient();

  const [accessKey, setAccessKey] = useState('');
  const [secretKey, setSecretKey] = useState('');
  const [editing, setEditing] = useState(false);

  const status = useQuery({
    queryKey: ['admin', 'storage', 'credentials'],
    queryFn: () => http<CredentialStatus>('/v1/admin/storage/credentials'),
  });

  function done() {
    setAccessKey('');
    setSecretKey('');
    setEditing(false);

    void queryClient.invalidateQueries({ queryKey: ['admin', 'storage', 'credentials'] });
    // Salvar uma credencial sem reconferir o bucket deixaria a tela mostrando o
    // diagnostico de antes — justo quando a pessoa quer saber se funcionou.
    void queryClient.invalidateQueries({ queryKey: ['admin', 'storage', 'diagnostics'] });
  }

  const save = useMutation({
    mutationFn: () =>
      http<CredentialStatus>('/v1/admin/storage/credentials', {
        method: 'PUT',
        body: { accessKey, secretKey },
      }),
    onSuccess: done,
  });

  const remove = useMutation({
    mutationFn: () =>
      http<CredentialStatus>('/v1/admin/storage/credentials', { method: 'DELETE' }),
    onSuccess: done,
  });

  const current = status.data;
  const busy = save.isPending || remove.isPending;

  return (
    <section className="rounded-2xl border border-neutral-200 bg-white p-6">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h2 className="flex items-center gap-2 text-base font-extrabold text-neutral-900">
            <KeyRound className="h-4 w-4 text-neutral-400" aria-hidden="true" />
            Credenciais da AWS
          </h2>

          {current ? (
            <p className="mt-1 text-sm text-neutral-600">
              Em uso: <strong>{SOURCE_LABEL[current.source]}</strong>
              {current.accessKeyHint ? ` (${current.accessKeyHint})` : ''}
              {current.updatedBy ? `, por ${current.updatedBy}` : ''}.
            </p>
          ) : null}
        </div>

        {!editing ? (
          <SecondaryButton onClick={() => setEditing(true)}>
            {current?.source === 'PANEL' ? 'Trocar' : 'Informar credenciais'}
          </SecondaryButton>
        ) : null}
      </div>

      {current?.source === 'DEFAULT_CHAIN' ? (
        <p className="mt-3 rounded-xl bg-neutral-50 px-4 py-3 text-sm text-neutral-600">
          Numa máquina da AWS, o caminho preferido é uma <strong>instance role</strong>: não
          existe segredo de longa duração para vazar nem para rotacionar. Informe uma chave
          aqui apenas quando a role não for possível.
        </p>
      ) : null}

      {editing ? (
        <form
          className="mt-5 space-y-4"
          onSubmit={event => {
            event.preventDefault();
            save.mutate();
          }}
        >
          <FormField label="Access key ID" htmlFor="aws-access-key">
            <TextInput
              id="aws-access-key"
              value={accessKey}
              autoComplete="off"
              spellCheck={false}
              placeholder="AKIA…"
              onChange={event => setAccessKey(event.target.value)}
            />
          </FormField>

          <FormField
            label="Secret access key"
            htmlFor="aws-secret-key"
            hint="Guardada cifrada. Depois de salvar, ela nunca mais é exibida — nem aqui, nem pela API."
          >
            <TextInput
              id="aws-secret-key"
              type="password"
              value={secretKey}
              autoComplete="new-password"
              spellCheck={false}
              onChange={event => setSecretKey(event.target.value)}
            />
          </FormField>

          {save.error ? (
            <DataError
              message={getApiErrorMessage(save.error, 'Não foi possível salvar as credenciais.')}
            />
          ) : null}

          <div className="flex flex-wrap gap-2">
            <PrimaryButton type="submit" disabled={busy || !accessKey || !secretKey}>
              {save.isPending ? 'Salvando…' : 'Salvar e verificar'}
            </PrimaryButton>

            <SecondaryButton
              type="button"
              onClick={() => {
                setEditing(false);
                setAccessKey('');
                setSecretKey('');
              }}
            >
              Cancelar
            </SecondaryButton>

            {current?.source === 'PANEL' ? (
              <SecondaryButton
                type="button"
                disabled={busy}
                onClick={() => remove.mutate()}
              >
                Remover credencial salva
              </SecondaryButton>
            ) : null}
          </div>
        </form>
      ) : null}
    </section>
  );
}

function Summary({ failures, warnings }: { failures: number; warnings: number }) {
  if (failures > 0) {
    return (
      <p className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm font-semibold text-red-800">
        {failures} {failures === 1 ? 'problema encontrado' : 'problemas encontrados'}. Cada um
        abaixo explica o que quebra na prática.
      </p>
    );
  }

  if (warnings > 0) {
    return (
      <p className="rounded-xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm font-semibold text-amber-800">
        Nada quebrado, mas {warnings} {warnings === 1 ? 'ponto merece' : 'pontos merecem'} atenção.
      </p>
    );
  }

  return (
    <p className="rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm font-semibold text-emerald-800">
      Tudo que dá para verificar daqui está correto.
    </p>
  );
}

function CheckRow({ check }: { check: CheckResult }) {
  const style = STATUS_STYLE[check.status];
  const { Icon } = style;

  return (
    <div className="rounded-2xl border border-neutral-200 bg-white p-5">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div className="min-w-0">
          <h3 className="font-bold text-neutral-900">{check.title}</h3>
          <p className="mt-1 text-sm text-neutral-600">{check.detail}</p>
        </div>

        <span
          className={`inline-flex shrink-0 items-center gap-1.5 rounded-full border px-3 py-1 text-xs font-bold ${style.className}`}
        >
          <Icon className="h-3.5 w-3.5" aria-hidden="true" />
          {style.label}
        </span>
      </div>

      {/* O impacto é o que transforma uma lista de status em algo acionável: sem ele, a
          pessoa não sabe se a linha vermelha custa dinheiro, quebra upload ou vaza arquivo. */}
      {check.impact ? (
        <p className="mt-3 border-l-2 border-neutral-200 pl-3 text-sm text-neutral-500">
          {check.impact}
        </p>
      ) : null}
    </div>
  );
}

function SnippetCard({ snippet }: { snippet: Snippet }) {
  const [copied, setCopied] = useState(false);

  async function copy() {
    try {
      await navigator.clipboard.writeText(snippet.content);
      setCopied(true);
      window.setTimeout(() => setCopied(false), 2000);
    } catch {
      // Área de transferência bloqueada (contexto não seguro, permissão negada): o texto
      // está visível logo abaixo e pode ser selecionado à mão.
      setCopied(false);
    }
  }

  return (
    <div className="rounded-2xl border border-neutral-200 bg-white p-5">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div className="min-w-0">
          <h3 className="font-bold text-neutral-900">{snippet.title}</h3>
          <p className="mt-1 text-sm text-neutral-500">{snippet.description}</p>
        </div>

        <SecondaryButton onClick={() => void copy()} aria-label={`Copiar ${snippet.title}`}>
          <Copy className="h-4 w-4" />
          {copied ? 'Copiado' : 'Copiar'}
        </SecondaryButton>
      </div>

      <pre className="mt-4 overflow-x-auto rounded-xl bg-neutral-900 p-4 text-xs leading-relaxed text-neutral-100">
        <code>{snippet.content}</code>
      </pre>
    </div>
  );
}

function Field({ label, value }: { label: string; value: string }) {
  return (
    <div className="min-w-0">
      <dt className="text-xs font-semibold uppercase tracking-wide text-neutral-400">{label}</dt>
      <dd className="mt-1 truncate font-semibold text-neutral-800" title={value}>
        {value}
      </dd>
    </div>
  );
}
