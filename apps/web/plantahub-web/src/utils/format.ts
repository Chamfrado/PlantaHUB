const LOCALE_BY_CURRENCY: Record<string, string> = {
  BRL: 'pt-BR',
  USD: 'en-US',
  EUR: 'de-DE',
};

/**
 * Formata um valor monetário. O argumento é SEMPRE em centavos.
 *
 * Esta é a única função de formatação de dinheiro do app. Antes existiam seis cópias
 * privadas espalhadas por componentes, e três delas não dividiam por 100 — os preços das
 * vitrines da home saíam 100× maiores. A causa raiz era um campo chamado `price.amount`,
 * sem unidade no nome: quem escrevia o componente não tinha como saber se era real ou
 * centavo. Por isso o parâmetro aqui se chama `cents` e nada mais formata dinheiro.
 */
export function formatCurrency(cents: number, currency: string = 'BRL'): string {
  const resolvedCurrency = (currency || 'BRL').toUpperCase();
  const locale = LOCALE_BY_CURRENCY[resolvedCurrency] ?? 'en-US';

  return (cents / 100).toLocaleString(locale, {
    style: 'currency',
    currency: resolvedCurrency,
    // Explícito: sem isto, um preço de R$ 1.234,56 vira "R$ 1.235" dependendo do padrão
    // do locale — um número errado diferente do bug que acabamos de corrigir.
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  });
}
