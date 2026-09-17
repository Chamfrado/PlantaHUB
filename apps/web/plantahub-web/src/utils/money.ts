/**
 * Conversão entre o texto digitado e centavos.
 *
 * Fica separado do componente porque helper exportado junto com componente quebra o
 * hot-reload do Vite — e porque testar a conversão não deveria exigir montar UI.
 */

/** `1234,56` ou `1.234,56` → `123456`. */
export function parseReaisToCents(input: string): number | null {
  const cleaned = input.trim().replace(/[^\d.,]/g, '');
  if (cleaned === '') return null;

  // Ponto é separador de milhar em pt-BR; a vírgula é o decimal.
  const normalized = cleaned.replace(/\./g, '').replace(',', '.');
  const parsed = Number(normalized);

  if (!Number.isFinite(parsed) || parsed < 0) return null;

  return Math.round(parsed * 100);
}

export function formatCentsToReais(cents: number | null): string {
  if (cents === null) return '';
  return (cents / 100).toLocaleString('pt-BR', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  });
}
