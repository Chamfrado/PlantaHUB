import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';

import { CartContext } from '../../../contexts/CartContext';
import { addCartItem } from '../../../services/cart.service';
import { ToastProvider } from '../../../components/ui/ToastProvider';
import { fullProduct } from '../../../test/fixtures/products';
import { server } from '../../../test/msw/server';
import ProductDetails from './ProductDetails';

vi.mock('../../../services/cart.service', () => ({
  addCartItem: vi.fn().mockResolvedValue({}),
}));

vi.mock('../../../services/profile.service', () => ({
  getMyProfileStatus: vi.fn().mockResolvedValue({ profileCompleted: true }),
}));

vi.mock('../../../contexts/AuthContext', async () => {
  const actual = await vi.importActual<typeof import('../../../contexts/AuthContext')>(
    '../../../contexts/AuthContext'
  );
  return {
    ...actual,
    useAuth: () => ({
      isAuthenticated: true,
      isLoading: false,
      user: null,
      login: vi.fn(),
      register: vi.fn(),
      logout: vi.fn(),
      refreshSession: vi.fn(),
    }),
  };
});

function renderAt(path: string) {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <ToastProvider>
        <CartContext.Provider
          value={{
            cart: null,
            cartCount: 0,
            loadingCart: false,
            refreshCart: vi.fn(),
            setCart: vi.fn(),
          }}
        >
          <Routes>
            <Route path="/:category/:slug" element={<ProductDetails />} />
          </Routes>
        </CartContext.Provider>
      </ToastProvider>
    </MemoryRouter>
  );
}

describe('ProductDetails', () => {
  it('renderiza todas as secoes de um produto completo', async () => {
    renderAt('/casas/confort');

    expect(await screen.findByRole('heading', { name: 'Confort - 80 m2' })).toBeInTheDocument();

    expect(screen.getByText('Por que escolher a Casa Confort?')).toBeInTheDocument();
    expect(screen.getByText('O que está incluso')).toBeInTheDocument();
    expect(screen.getByText('Fatos Chave')).toBeInTheDocument();
    expect(screen.getByText('O que dizem nossos clientes')).toBeInTheDocument();
    expect(screen.getByText('Perguntas Frequentes')).toBeInTheDocument();
  });

  it('mostra todos os itens das listas, sem cortar em tres', async () => {
    renderAt('/casas/confort');

    await screen.findByRole('heading', { name: 'Confort - 80 m2' });

    // A versao anterior aplicava .slice(0, 3) e .slice(0, 2): um item a mais cadastrado
    // pelo admin sumia da pagina sem nenhum aviso.
    expect(screen.getByText('Quarto diferencial')).toBeInTheDocument();
    expect(screen.getByText('terceiro fato que era cortado')).toBeInTheDocument();
  });

  it('o carrossel mostra todas as imagens, nao so as cinco primeiras', async () => {
    const user = userEvent.setup();

    const gallery = Array.from({ length: 7 }, (_, i) => `https://cdn.test/foto-${i + 1}.webp`);

    server.use(
      http.get('http://localhost:8080/v1/products/casas/confort', () =>
        HttpResponse.json({ ...fullProduct, galleryImageUrls: gallery })
      )
    );

    renderAt('/casas/confort');

    await screen.findByRole('heading', { name: 'Confort - 80 m2' });

    // Capa + sete da galeria. O corte em cinco descartava em silencio a sexta imagem que
    // o administrador tivesse subido pelo painel.
    const thumbs = screen.getAllByRole('button', { name: /^Ver imagem \d+ de 8$/ });
    expect(thumbs).toHaveLength(8);

    // Cada miniatura tem nome proprio: com um rotulo unico repetido, quem usa leitor de
    // tela ouvia "Selecionar imagem" oito vezes sem saber qual era qual.
    expect(screen.getByRole('button', { name: 'Ver imagem 8 de 8' })).toBeInTheDocument();

    // A primeira comeca selecionada, e clicar troca a imagem grande.
    expect(thumbs[0]).toHaveAttribute('aria-pressed', 'true');

    await user.click(thumbs[4]);
    expect(thumbs[4]).toHaveAttribute('aria-pressed', 'true');
    expect(thumbs[0]).toHaveAttribute('aria-pressed', 'false');
  });

  it('renderiza produto esparso sem cabecalhos de secao vazios', async () => {
    renderAt('/casas/nova');

    // Sem conteudo editorial, o nome vira o titulo.
    expect(await screen.findByRole('heading', { name: 'Casa Nova' })).toBeInTheDocument();

    // Nenhuma secao sem conteudo pode aparecer: um rascunho renderizaria titulos orfaos.
    expect(screen.queryByText(/Por que escolher/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/O que está incluso/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/dizem nossos clientes/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/Perguntas frequentes/i)).not.toBeInTheDocument();
  });

  it('mostra pagina de nao encontrado quando a API responde 404', async () => {
    renderAt('/casas/inexistente');

    expect(await screen.findByText('Produto não encontrado')).toBeInTheDocument();
  });

  it('envia ao carrinho o id que veio da API, e nao um literal local', async () => {
    const user = userEvent.setup();

    renderAt('/casas/confort');

    await screen.findByRole('heading', { name: 'Confort - 80 m2' });

    // A opcao de compra vive dentro do seletor; o mesmo texto aparece na lista de itens
    // inclusos, entao a busca precisa ser pelo botao.
    const planButton = await screen.findByRole('button', { name: /Planta Arquitet/i });
    await user.click(planButton);

    await user.click(screen.getByRole('button', { name: /Adicionar ao carrinho/i }));

    await waitFor(() => {
      expect(addCartItem).toHaveBeenCalledWith({
        // Antes deste cutover este id vinha de src/data/products.ts, e a compra so
        // funcionava porque aquele literal era identico ao id do banco.
        productId: 'casa-confort-80m2',
        planTypeCodes: ['ARCH'],
      });
    });
  });
});
