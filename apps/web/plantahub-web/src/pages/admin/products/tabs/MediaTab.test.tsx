import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { beforeEach, describe, expect, it } from 'vitest';

import { server } from '../../../../test/msw/server';
import type { AdminMedia } from '../../../../types/api/admin';
import MediaTab from './MediaTab';

const API = 'http://localhost:8080/v1/admin';
const PRODUCT = 'casa-confort-80m2';

function media(id: string, role: AdminMedia['role'], sortOrder: number): AdminMedia {
  return {
    id,
    role,
    storageKey: `public/products/${PRODUCT}/${id}/${id}.webp`,
    url: `https://cdn.test/${id}.webp`,
    altText: null,
    sortOrder,
    sizeBytes: 2048,
  };
}

/** Estado do servidor falso, para as mutações terem efeito observável. */
let rows: AdminMedia[] = [];
let lastReorder: string[] | null = null;

function mountHandlers() {
  server.use(
    http.get(`${API}/products/:productId/media`, () => HttpResponse.json(rows)),

    http.patch(`${API}/media/:mediaId`, async ({ params, request }) => {
      const body = (await request.json()) as { role?: AdminMedia['role'] };

      rows = rows.map(row => {
        if (row.id === params.mediaId) return { ...row, role: body.role ?? row.role };
        // Espelha o rebaixamento que o servidor real faz: só pode haver uma capa.
        if (body.role === 'HERO' && row.role === 'HERO') return { ...row, role: 'GALLERY' };
        return row;
      });

      return HttpResponse.json(rows.find(row => row.id === params.mediaId));
    }),

    http.delete(`${API}/media/:mediaId`, ({ params }) => {
      rows = rows.filter(row => row.id !== params.mediaId);
      return new HttpResponse(null, { status: 204 });
    }),

    http.post(`${API}/products/:productId/media/reorder`, async ({ request }) => {
      const body = (await request.json()) as { mediaIds: string[] };
      lastReorder = body.mediaIds;

      rows = body.mediaIds
        .map((id, index) => {
          const row = rows.find(candidate => candidate.id === id);
          return row ? { ...row, sortOrder: index } : null;
        })
        .filter(Boolean) as AdminMedia[];

      return HttpResponse.json(rows);
    })
  );
}

function renderTab() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });

  return render(
    <QueryClientProvider client={client}>
      <MediaTab productId={PRODUCT} />
    </QueryClientProvider>
  );
}

describe('MediaTab', () => {
  beforeEach(() => {
    lastReorder = null;
    mountHandlers();
  });

  it('diz o que falta quando o produto nao tem imagem nenhuma', async () => {
    rows = [];
    renderTab();

    expect(
      await screen.findByText(/precisa de uma capa para ser publicado/i)
    ).toBeInTheDocument();
  });

  it('separa a capa do carrossel e conta quantas imagens ele tem', async () => {
    rows = [
      media('capa', 'HERO', 0),
      media('foto-a', 'GALLERY', 1),
      media('foto-b', 'GALLERY', 2),
    ];

    renderTab();

    expect(await screen.findByText('Capa')).toBeInTheDocument();
    expect(screen.getByText('(2 imagens)')).toBeInTheDocument();
  });

  it('avisa, em vez de publicar, quando o produto so tem fotos e nenhuma capa', async () => {
    rows = [media('foto-a', 'GALLERY', 0)];
    renderTab();

    expect(await screen.findByText(/sem capa definida/i)).toBeInTheDocument();
  });

  it('promover uma foto a capa tira a anterior do carrossel', async () => {
    rows = [media('capa', 'HERO', 0), media('foto-a', 'GALLERY', 1)];
    renderTab();

    await userEvent.click(
      await screen.findByRole('button', { name: 'Definir imagem 1 de 1 como capa' })
    );

    // A antiga capa desce para o carrossel; a nova sobe. Sem o rebaixamento, as duas
    // apareceriam como capa.
    await waitFor(() => expect(screen.getByText('(1 imagem)')).toBeInTheDocument());

    await waitFor(() =>
      expect(rows.find(row => row.role === 'HERO')?.id).toBe('foto-a')
    );
  });

  it('reordenar manda a lista inteira, com a capa na frente', async () => {
    rows = [
      media('capa', 'HERO', 0),
      media('foto-a', 'GALLERY', 1),
      media('foto-b', 'GALLERY', 2),
    ];

    renderTab();

    await userEvent.click(
      await screen.findByRole('button', { name: 'Mover imagem 2 de 2 para trás' })
    );

    // Enviar só a galeria deixaria a capa com posição maior que a da primeira foto, e o
    // carrossel público sairia fora de ordem.
    await waitFor(() =>
      expect(lastReorder).toEqual(['capa', 'foto-b', 'foto-a'])
    );
  });

  it('remove uma imagem do carrossel', async () => {
    rows = [media('capa', 'HERO', 0), media('foto-a', 'GALLERY', 1)];
    renderTab();

    await userEvent.click(
      await screen.findByRole('button', { name: 'Remover imagem 1 de 1' })
    );

    await waitFor(() =>
      expect(screen.getByText(/envie mais imagens para montar o carrossel/i)).toBeInTheDocument()
    );
  });

  it('nao renderiza a chave do bucket como endereco quando falta a URL publica', async () => {
    rows = [{ ...media('capa', 'HERO', 0), url: null }];
    renderTab();

    expect(await screen.findByText(/sem URL pública/i)).toBeInTheDocument();

    // Um `img` com `public/products/...` resolveria contra a origem do site e daria 404.
    const cover = screen.getByText('Capa').closest('section') as HTMLElement;
    expect(within(cover).queryByRole('img')).not.toBeInTheDocument();
  });
});
