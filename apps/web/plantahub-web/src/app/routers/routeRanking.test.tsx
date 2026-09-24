import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { describe, expect, it } from 'vitest';

/**
 * `/:category/:slug` é uma rota gulosa de dois segmentos: sem a classificação do React
 * Router, `/admin/produtos` cairia na página pública de produto.
 *
 * A precedência (segmento estático acima de dinâmico) é invisível no código e portante.
 * Este teste existe para que ela não se perca numa reorganização de rotas.
 */
describe('precedência de rotas', () => {
  it('/admin/produtos não cai na página pública de produto', () => {
    render(
      <MemoryRouter initialEntries={['/admin/produtos']}>
        <Routes>
          <Route path="/admin/*" element={<div>painel</div>} />
          <Route path="/:category/:slug" element={<div>produto público</div>} />
        </Routes>
      </MemoryRouter>
    );

    expect(screen.getByText('painel')).toBeInTheDocument();
    expect(screen.queryByText('produto público')).not.toBeInTheDocument();
  });

  it('uma URL de produto de verdade continua chegando na página pública', () => {
    render(
      <MemoryRouter initialEntries={['/casas/confort']}>
        <Routes>
          <Route path="/admin/*" element={<div>painel</div>} />
          <Route path="/:category/:slug" element={<div>produto público</div>} />
        </Routes>
      </MemoryRouter>
    );

    expect(screen.getByText('produto público')).toBeInTheDocument();
  });
});
