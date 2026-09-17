import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';

import { mapProductSummary } from '../../mappers/product.mapper';
import { productSummaries } from '../../test/fixtures/products';
import ProductCard from './ProductCard';

const base = productSummaries[0];

describe('ProductCard', () => {
  it('anuncia o preço como ponto de partida, não como preço fechado', () => {
    render(<ProductCard product={mapProductSummary(base)} />);

    // Cada coleção tem preço próprio e o cliente monta a compra; o número sozinho passaria
    // por "preço de levar tudo".
    expect(screen.getByText('A partir de')).toBeInTheDocument();
    expect(screen.getByText('R$ 1.500,00')).toBeInTheDocument();
  });

  it('produto sem oferta à venda diz "sob consulta", e não R$ 0,00', () => {
    render(<ProductCard product={mapProductSummary({ ...base, basePriceCents: null })} />);

    // Zero significaria "de graça". Era o que a home mostrava enquanto o preço vinha de
    // product.base_price_cents, uma coluna que ninguém atualizava.
    expect(screen.getByText('Preço sob consulta')).toBeInTheDocument();
    expect(screen.queryByText('A partir de')).not.toBeInTheDocument();
    expect(screen.queryByText(/R\$\s*0,00/)).not.toBeInTheDocument();
  });
});
