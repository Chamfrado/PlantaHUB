import { Loader2 } from 'lucide-react';

/**
 * Espera enquanto a sessão resolve ou um chunk carrega.
 *
 * O `ProtectedRoute` devolve `null` nesse intervalo, o que pisca uma tela branca —
 * tolerável numa página pequena, ruim numa rota pesada que ainda vai baixar código.
 */
export default function RouteFallback({ label = 'Carregando…' }: { label?: string }) {
  return (
    <div className="flex min-h-[50vh] items-center justify-center" role="status" aria-live="polite">
      <div className="flex items-center gap-3 text-brand-muted">
        <Loader2 className="h-5 w-5 animate-spin" />
        <span className="text-sm font-semibold">{label}</span>
      </div>
    </div>
  );
}
