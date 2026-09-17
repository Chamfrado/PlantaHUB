import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it } from 'vitest';

import { server } from '../../../../test/msw/server';
import type { AdminCollection, AdminOffer } from '../../../../types/api/admin';
import AssetsTab from './AssetsTab';

const API = 'http://localhost:8080/v1/admin';
const PRODUCT = 'casa-confort-80m2';

function collection(over: Partial<AdminCollection> = {}): AdminCollection {
  return {
    id: crypto.randomUUID(),
    code: 'ARCH',
    name: 'Planta Arquitetônica',
    description: null,
    purchasable: true,
    bundledWithEveryOffer: false,
    active: true,
    sortOrder: 0,
    assetCount: 0,
    ...over,
  };
}

function offer(code: string): AdminOffer {
  return {
    id: crypto.randomUUID(),
    collectionCode: code,
    collectionName: code,
    purchasableCollection: true,
    bundledWithEveryOffer: false,
    priceCents: 150000,
    available: true,
    includedInBundle: false,
    sortOrder: 0,
  };
}

function mount(collections: AdminCollection[], offers: AdminOffer[] = []) {
  server.use(
    http.get(`${API}/products/:productId/assets`, () => HttpResponse.json([])),
    http.get(`${API}/collections`, () => HttpResponse.json(collections)),
    http.get(`${API}/products/:productId/offers`, () => HttpResponse.json(offers))
  );

  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });

  return render(
    <MemoryRouter>
      <QueryClientProvider client={client}>
        <AssetsTab productId={PRODUCT} />
      </QueryClientProvider>
    </MemoryRouter>
  );
}

describe('AssetsTab', () => {
  it('sem coleção nenhuma, manda criar uma antes de enviar arquivo', async () => {
    mount([]);

    expect(await screen.findByText('Nenhuma coleção ainda')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Criar coleção' })).toHaveAttribute(
      'href',
      '/admin/colecoes'
    );

    // Sem coleção não existe destino possível, então nem aparece por onde enviar.
    expect(screen.queryByRole('button', { name: /selecionar arquivos/i })).not.toBeInTheDocument();
  });

  it('não envia nada enquanto a coleção de destino não for escolhida', async () => {
    mount([collection()]);

    const button = await screen.findByRole('button', { name: /selecionar arquivos/i });

    // A versão anterior usava a primeira coleção da lista como padrão silencioso: os
    // arquivos iam para uma coleção que ninguém escolheu.
    expect(button).toBeDisabled();
    expect(
      screen.getByText(/Escolha a coleção de destino acima/)
    ).toBeInTheDocument();
  });

  it('escolhida a coleção, diz para onde os arquivos vão e libera o envio', async () => {
    mount([collection()], [offer('ARCH')]);

    await userEvent.selectOptions(
      await screen.findByLabelText('Coleção de destino'),
      'ARCH'
    );

    expect(screen.getByRole('button', { name: /selecionar arquivos/i })).toBeEnabled();

    // O nome tambem aparece no <option>; a asercao e sobre o texto do envio.
    expect(screen.getByText(/Ao enviar uma pasta/))
      .toHaveTextContent('Os arquivos vão para Planta Arquitetônica (ARCH)');
  });

  it('avisa que escolher uma coleção nova também cria o vínculo com o produto', async () => {
    // O produto não tem oferta de ARCH: o envio vai criar a linha, sem preço e fora de
    // venda. Descobrir isso depois, vendo uma oferta que ninguém criou, seria confuso.
    mount([collection()], []);

    await userEvent.selectOptions(
      await screen.findByLabelText('Coleção de destino'),
      'ARCH'
    );

    expect(screen.getByText(/ainda não oferece/)).toBeInTheDocument();
    expect(screen.getByText(/sem preço e fora de venda/)).toBeInTheDocument();
  });

  it('avisa quando a coleção escolhida acompanha toda compra', async () => {
    mount([
      collection({ code: 'APOIO', name: 'Material de apoio', purchasable: false, bundledWithEveryOffer: true }),
    ]);

    await userEvent.selectOptions(
      await screen.findByLabelText('Coleção de destino'),
      'APOIO'
    );

    // Por um arquivo aqui e decidir que ele vai junto com qualquer compra do produto.
    expect(screen.getByText(/qualquer oferta que o cliente escolher/))
      .toHaveTextContent('acompanha toda compra do produto');
  });
});
