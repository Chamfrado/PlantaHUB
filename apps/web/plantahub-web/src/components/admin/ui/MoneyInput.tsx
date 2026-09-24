import { useState } from 'react';
import { formatCentsToReais, parseReaisToCents } from '../../../utils/money';

type Props = {
  /** Sempre em centavos. Este e o unico componente do painel que converte para reais. */
  valueCents: number | null;
  onChange: (cents: number | null) => void;
  id?: string;
  disabled?: boolean;
  /** Obrigatorio quando o campo nao esta dentro de um <label>. */
  ariaLabel?: string;
};

/**
 * Entrada de dinheiro.
 *
 * Enquanto o campo tem foco, o texto digitado e exibido como esta; fora de foco, o valor
 * vem formatado do estado externo. Reformatar a cada tecla faria o cursor pular no meio da
 * digitacao, e derivar o texto em vez de sincroniza-lo num efeito evita um render extra.
 */
export default function MoneyInput({ valueCents, onChange, id, disabled, ariaLabel }: Props) {
  const [draft, setDraft] = useState<string | null>(null);

  const editing = draft !== null;
  const displayValue = editing ? draft : formatCentsToReais(valueCents);

  return (
    <div className="relative">
      <span className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-sm font-semibold text-neutral-400">
        R$
      </span>
      <input
        id={id}
        aria-label={ariaLabel}
        type="text"
        inputMode="decimal"
        disabled={disabled}
        value={displayValue}
        onFocus={() => setDraft(formatCentsToReais(valueCents))}
        onChange={event => {
          setDraft(event.target.value);
          onChange(parseReaisToCents(event.target.value));
        }}
        onBlur={() => setDraft(null)}
        placeholder="0,00"
        className="w-full rounded-xl border border-neutral-300 bg-white py-2.5 pl-10 pr-3 text-right font-semibold text-neutral-900 outline-none transition focus:border-primary-500 disabled:bg-neutral-100"
      />
    </div>
  );
}
