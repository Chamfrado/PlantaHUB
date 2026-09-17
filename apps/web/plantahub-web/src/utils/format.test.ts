import { describe, expect, it } from 'vitest';

import { formatCurrency } from './format';

// O espaco que o Intl usa entre "R$" e o numero em pt-BR e um NBSP (U+00A0),
// nao um espaco normal. Normalizar evita um teste que quebra por um caractere invisivel.
const normalize = (value: string) => value.replace(/\u00a0/g, ' ');

describe('formatCurrency', () => {
  it('interpreta o argumento como centavos', () => {
    // Este era o bug: 123456 centavos era formatado como R$ 123.456,00
    // nas vitrines da home, 100x o valor real.
    expect(normalize(formatCurrency(123456))).toBe('R$ 1.234,56');
  });

  it('sempre mostra duas casas decimais', () => {
    expect(normalize(formatCurrency(150000))).toBe('R$ 1.500,00');
    expect(normalize(formatCurrency(1))).toBe('R$ 0,01');
  });

  it('formata zero', () => {
    expect(normalize(formatCurrency(0))).toBe('R$ 0,00');
  });

  it('formata valores negativos (estornos)', () => {
    expect(normalize(formatCurrency(-5000))).toContain('50,00');
  });

  it('respeita outras moedas', () => {
    expect(formatCurrency(123456, 'USD')).toBe('$1,234.56');
  });

  it('cai em BRL quando a moeda vem vazia', () => {
    expect(normalize(formatCurrency(1000, ''))).toBe('R$ 10,00');
  });

  it('aceita moeda em minusculas', () => {
    expect(normalize(formatCurrency(1000, 'brl'))).toBe('R$ 10,00');
  });
});
