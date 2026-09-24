import { describe, expect, it } from 'vitest';

import { formatCentsToReais, parseReaisToCents } from './money';

const normalize = (value: string) => value.replace(/\u00a0/g, ' ');

describe('conversão de dinheiro', () => {
  it('lê o formato brasileiro com vírgula decimal', () => {
    expect(parseReaisToCents('1234,56')).toBe(123456);
  });

  it('lê com separador de milhar', () => {
    expect(parseReaisToCents('1.234,56')).toBe(123456);
    expect(parseReaisToCents('1.500,00')).toBe(150000);
  });

  it('ignora o símbolo da moeda e espaços', () => {
    expect(parseReaisToCents(' R$ 99,90 ')).toBe(9990);
  });

  it('trata campo vazio como ausência de valor, e não como zero', () => {
    expect(parseReaisToCents('')).toBeNull();
    expect(parseReaisToCents('   ')).toBeNull();
  });

  it('formata centavos de volta para reais', () => {
    expect(normalize(formatCentsToReais(123456))).toBe('1.234,56');
    expect(normalize(formatCentsToReais(0))).toBe('0,00');
    expect(formatCentsToReais(null)).toBe('');
  });

  it('sobrevive à ida e volta', () => {
    for (const cents of [0, 1, 990, 150000, 123456, 99999999]) {
      expect(parseReaisToCents(formatCentsToReais(cents))).toBe(cents);
    }
  });
});
