import type { ReactNode } from 'react';

/** Peças compartilhadas pelas telas do painel. */

export function AdminPageHeader({
  title,
  description,
  actions,
}: {
  title: string;
  description?: string;
  actions?: ReactNode;
}) {
  return (
    <div className="flex flex-wrap items-start justify-between gap-4 border-b border-neutral-200 pb-6">
      <div>
        <h1 className="text-2xl font-extrabold text-neutral-900">{title}</h1>
        {description ? <p className="mt-1 text-sm text-neutral-500">{description}</p> : null}
      </div>
      {actions ? <div className="flex flex-wrap items-center gap-2">{actions}</div> : null}
    </div>
  );
}

/**
 * Rótulo + campo.
 *
 * O campo fica **dentro** do `<label>`, o que associa os dois implicitamente. A versão
 * anterior dependia de `htmlFor`, e a maioria das chamadas não passava id nenhum — o
 * resultado eram rótulos soltos: leitores de tela não anunciavam o campo e clicar no texto
 * não focava a entrada.
 */
export function FormField({
  label,
  htmlFor,
  hint,
  children,
}: {
  label: string;
  htmlFor?: string;
  hint?: string;
  children: ReactNode;
}) {
  return (
    <div>
      <label htmlFor={htmlFor} className="block">
        <span className="block text-sm font-bold text-neutral-800">{label}</span>
        <span className="mt-1.5 block">{children}</span>
      </label>
      {hint ? <p className="mt-1.5 text-xs text-neutral-500">{hint}</p> : null}
    </div>
  );
}

const inputClass =
  'w-full rounded-xl border border-neutral-300 bg-white px-3 py-2.5 text-neutral-900 outline-none transition focus:border-primary-500 disabled:bg-neutral-100';

export function TextInput(props: React.InputHTMLAttributes<HTMLInputElement>) {
  return <input {...props} className={inputClass} />;
}

export function TextArea(props: React.TextareaHTMLAttributes<HTMLTextAreaElement>) {
  return <textarea {...props} rows={props.rows ?? 3} className={inputClass} />;
}

export function SelectInput(props: React.SelectHTMLAttributes<HTMLSelectElement>) {
  return <select {...props} className={inputClass} />;
}

export function ToggleSwitch({
  checked,
  onChange,
  label,
  disabled,
}: {
  checked: boolean;
  onChange: (next: boolean) => void;
  label: string;
  disabled?: boolean;
}) {
  return (
    <label className="inline-flex cursor-pointer items-center gap-2.5">
      <input
        type="checkbox"
        checked={checked}
        disabled={disabled}
        onChange={event => onChange(event.target.checked)}
        className="h-4 w-4 rounded border-neutral-300 accent-primary-500"
      />
      <span className="text-sm font-semibold text-neutral-700">{label}</span>
    </label>
  );
}

const statusStyles: Record<string, string> = {
  DRAFT: 'bg-neutral-100 text-neutral-600 border-neutral-200',
  PUBLISHED: 'bg-green-50 text-green-700 border-green-200',
  ARCHIVED: 'bg-amber-50 text-amber-700 border-amber-200',
};

const statusLabels: Record<string, string> = {
  DRAFT: 'Rascunho',
  PUBLISHED: 'Publicado',
  ARCHIVED: 'Arquivado',
};

export function StatusBadge({ status }: { status: string }) {
  return (
    <span
      className={[
        'inline-flex rounded-full border px-2.5 py-0.5 text-xs font-bold',
        statusStyles[status] ?? statusStyles.DRAFT,
      ].join(' ')}
    >
      {statusLabels[status] ?? status}
    </span>
  );
}

export function PrimaryButton({
  children,
  ...props
}: React.ButtonHTMLAttributes<HTMLButtonElement>) {
  return (
    <button
      {...props}
      className="inline-flex items-center gap-2 rounded-xl bg-primary-500 px-4 py-2.5 text-sm font-semibold text-white transition hover:bg-primary-600 disabled:opacity-50"
    >
      {children}
    </button>
  );
}

export function SecondaryButton({
  children,
  ...props
}: React.ButtonHTMLAttributes<HTMLButtonElement>) {
  return (
    <button
      {...props}
      className="inline-flex items-center gap-2 rounded-xl border border-neutral-300 bg-white px-4 py-2.5 text-sm font-semibold text-neutral-700 transition hover:bg-neutral-50 disabled:opacity-50"
    >
      {children}
    </button>
  );
}

export function EmptyState({ message }: { message: string }) {
  return (
    <div className="rounded-2xl border border-dashed border-neutral-300 bg-white px-6 py-12 text-center text-sm text-neutral-500">
      {message}
    </div>
  );
}

export function DataError({ message, onRetry }: { message: string; onRetry?: () => void }) {
  return (
    <div className="rounded-2xl border border-red-200 bg-red-50 px-6 py-6 text-center">
      <p className="text-sm font-semibold text-red-800">{message}</p>
      {onRetry ? (
        <button
          onClick={onRetry}
          className="mt-3 rounded-lg border border-red-300 bg-white px-4 py-2 text-xs font-semibold text-red-700 transition hover:bg-red-50"
        >
          Tentar novamente
        </button>
      ) : null}
    </div>
  );
}
