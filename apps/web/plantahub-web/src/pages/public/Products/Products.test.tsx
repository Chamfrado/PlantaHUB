import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it } from 'vitest';

import { server } from '../../../test/msw/server';
import ProductsPage from './Products';

function renderAt(path = '/produtos') {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <ProductsPage />
    </MemoryRouter>
  );
}

describe('ProductsPage', () => {
  it('lista os produtos vindos da API', async () => {
    renderAt();

    // Esta pagina nao fazia nenhuma chamada a API: lia seis produtos de um arquivo local.
    expect(await screen.findByText('Confort')).toBeInTheDocument();
  });

  it('monta as abas a partir das categorias do banco', async () => {
    renderAt();

    expect(await screen.findByRole('button', { name: 'Casas' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Chalés' })).toBeInTheDocument();

    // "Studios" existia no codigo como categoria fixa e nunca teve produto.
    expect(screen.queryByRole('button', { name: 'Studios' })).not.toBeInTheDocument();
  });

  it('filtra ao trocar de aba', async () => {
    const user = userEvent.setup();

    renderAt();

    await screen.findByText('Confort');
    expect(screen.queryByText('Prime')).not.toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Chalés' }));

    expect(await screen.findByText('Prime')).toBeInTheDocument();
    expect(screen.queryByText('Confort')).not.toBeInTheDocument();
  });

  it('respeita a categoria vinda da URL', async () => {
    renderAt('/produtos?category=chales');

    expect(await screen.findByText('Prime')).toBeInTheDocument();
  });

  it('mostra mensagem quando a categoria nao tem produto publicado', async () => {
    server.use(
      http.get('http://localhost:8080/v1/products', () => HttpResponse.json([]))
    );

    renderAt();

    expect(
      await screen.findByText('Nenhum produto publicado nesta categoria ainda.')
    ).toBeInTheDocument();
  });

  it('categoria em breve fica desativada e so avisa ao clicar', async () => {
    const user = userEvent.setup();

    server.use(
      http.get('http://localhost:8080/v1/categories', () =>
        HttpResponse.json([
          { slug: 'sobrados', name: 'Sobrados', order: 1, featuredOnHome: false, homeOrder: 0, comingSoon: true },
          { slug: 'casas', name: 'Casas', order: 2, featuredOnHome: true, homeOrder: 1, comingSoon: false },
        ])
      )
    );

    // Nem pela URL a categoria em breve abre: cai na primeira categoria aberta.
    renderAt('/produtos?category=sobrados');

    expect(await screen.findByText('Confort')).toBeInTheDocument();

    const sobrados = screen.getByRole('button', { name: 'Sobrados' });
    expect(sobrados).toHaveAttribute('aria-disabled', 'true');
    expect(sobrados).toHaveAttribute('title', 'Em breve');

    await user.click(sobrados);

    expect(screen.getByRole('status')).toHaveTextContent('Em breve');
    expect(screen.getByText('Confort')).toBeInTheDocument();
  });

  it('oferece nova tentativa quando a API falha', async () => {
    server.use(
      http.get('http://localhost:8080/v1/products', () =>
        HttpResponse.json({ error: 'boom' }, { status: 500 })
      )
    );

    renderAt();

    expect(await screen.findByRole('button', { name: 'Tentar novamente' })).toBeInTheDocument();
  });
});
