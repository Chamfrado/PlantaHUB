import { ChevronDown, ChevronUp, Plus, Trash2 } from 'lucide-react';
import type { ReactNode } from 'react';
import { attachUid, type WithUid } from './uid';

type Props<T> = {
  label: string;
  value: WithUid<T>[];
  onChange: (next: WithUid<T>[]) => void;
  newItem: () => T;
  itemTitle: (item: T, index: number) => string;
  renderItem: (item: T, patch: (partial: Partial<T>) => void, index: number) => ReactNode;
  max?: number;
  /** Mostrado quando a lista está vazia. Diga o efeito, não apenas "sem itens". */
  emptyHint?: string;
};

export default function RepeatableList<T>({
  label,
  value,
  onChange,
  newItem,
  itemTitle,
  renderItem,
  max = 30,
  emptyHint = 'Sem itens — esta seção não aparecerá na página pública.',
}: Props<T>) {
  function patchAt(index: number, partial: Partial<T>) {
    onChange(value.map((item, i) => (i === index ? { ...item, ...partial } : item)));
  }

  function removeAt(index: number) {
    onChange(value.filter((_, i) => i !== index));
  }

  function move(index: number, direction: -1 | 1) {
    const target = index + direction;
    if (target < 0 || target >= value.length) return;

    const next = [...value];
    [next[index], next[target]] = [next[target], next[index]];
    onChange(next);
  }

  return (
    <div>
      <div className="flex items-center justify-between">
        <span className="text-sm font-bold text-neutral-800">{label}</span>

        <button
          type="button"
          disabled={value.length >= max}
          onClick={() => onChange([...value, attachUid(newItem())])}
          className="inline-flex items-center gap-1.5 rounded-lg border border-neutral-300 bg-white px-3 py-1.5 text-xs font-semibold text-neutral-700 transition hover:bg-neutral-50 disabled:opacity-40"
        >
          <Plus className="h-3.5 w-3.5" /> Adicionar
        </button>
      </div>

      {value.length === 0 ? (
        <p className="mt-3 rounded-xl border border-dashed border-neutral-300 bg-neutral-50 px-4 py-6 text-center text-xs text-neutral-500">
          {emptyHint}
        </p>
      ) : (
        <div className="mt-3 space-y-3">
          {value.map((item, index) => (
            <div key={item._uid} className="rounded-xl border border-neutral-200 bg-white p-4">
              <div className="flex items-start justify-between gap-3">
                <span className="min-w-0 break-words text-xs font-bold uppercase tracking-wide text-neutral-400">
                  {index + 1}. {itemTitle(item, index) || 'Sem título'}
                </span>

                <div className="flex shrink-0 items-center gap-1">
                  {/* Botões em vez de arrastar: as listas têm poucos itens e botão é
                      acessível por teclado sem nenhuma dependência extra. */}
                  <IconButton label="Mover para cima" onClick={() => move(index, -1)} disabled={index === 0}>
                    <ChevronUp className="h-4 w-4" />
                  </IconButton>
                  <IconButton
                    label="Mover para baixo"
                    onClick={() => move(index, 1)}
                    disabled={index === value.length - 1}
                  >
                    <ChevronDown className="h-4 w-4" />
                  </IconButton>
                  <IconButton label="Remover" onClick={() => removeAt(index)} danger>
                    <Trash2 className="h-4 w-4" />
                  </IconButton>
                </div>
              </div>

              <div className="mt-3 space-y-3">
                {renderItem(item, partial => patchAt(index, partial), index)}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

function IconButton({
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
      aria-label={label}
      title={label}
      onClick={onClick}
      disabled={disabled}
      className={[
        'rounded-lg border p-1.5 transition disabled:opacity-30',
        danger
          ? 'border-red-200 text-red-600 hover:bg-red-50'
          : 'border-neutral-300 text-neutral-600 hover:bg-neutral-50',
      ].join(' ')}
    >
      {children}
    </button>
  );
}
