import { expect, test, type Page } from '@playwright/test';

/**
 * A proposta de valor desta modernização, expressa como teste:
 *
 * **o catálogo é administrável — criar, publicar e despublicar um produto muda o site
 * público, sem tocar em código e sem deploy.**
 *
 * Antes deste trabalho, cada passo daqui exigia editar um arquivo TypeScript, escrever uma
 * migration à mão e publicar uma nova versão do frontend.
 *
 * Os dois testes cobrem metades complementares, e a divisão não é arbitrária: publicar
 * exige imagem de capa, e subir uma capa exige armazenamento de objetos, que este ambiente
 * não tem. Então o primeiro test leva um produto novo até o portão de publicação, e o
 * segundo exercita publicação e despublicação sobre um produto semeado, que já tem capa.
 */

const API = process.env.E2E_API_URL ?? 'http://localhost:8080';

const ADMIN = {
  email: process.env.E2E_ADMIN_EMAIL ?? 'admin@plantahub.test',
  password: process.env.E2E_ADMIN_PASSWORD ?? 'senha-de-teste-123',
};

/** Produto semeado pelas migrations. Tem capa, oferta com preço e título: é publicável. */
const SEEDED = {
  id: 'casa-confort-80m2',
  category: 'casas',
  slug: 'confort',
  name: 'Confort',
  headline: 'Confort - 80 m2',
};

const unique = () => Date.now().toString(36) + Math.random().toString(36).slice(2, 6);

async function login(page: Page) {
  await page.goto('/login');
  await page.getByLabel(/e-?mail/i).fill(ADMIN.email);
  await page.getByLabel(/senha/i).fill(ADMIN.password);
  await page.getByRole('button', { name: /entrar/i }).click();
  await page.waitForURL(url => !url.pathname.includes('/login'));
}

/**
 * Cria a coleção pela API.
 *
 * O teste é sobre o fluxo de catálogo; montar uma coleção pela interface só acrescentaria
 * passos sem cobrir nada que as outras telas já não cubram.
 */
async function ensureCollection(page: Page, code: string) {
  // page.request compartilha os cookies do navegador, entao ja vai autenticado.
  await page.request.post(`${API}/v1/admin/collections`, {
    data: { code, name: `Coleção ${code}`, purchasable: true },
  });
}

/** Vai para `/produtos` e espera a lista ter carregado de verdade. */
async function openPublicCatalog(page: Page) {
  await page.goto('/produtos');

  // Sem esta espera, `toHaveCount(0)` passaria com a lista ainda vazia carregando — um
  // verde que nao prova nada. O produto semeado e o sinal de que a resposta chegou.
  await expect(page.getByText(SEEDED.name).first()).toBeVisible();
}

test.describe('catálogo administrável', () => {
  test('produto novo nasce invisível e o portão de publicação diz o que falta', async ({
    page,
  }) => {
    const suffix = unique();
    const productName = `Casa E2E ${suffix}`;
    const collectionCode = `E2E${suffix.toUpperCase().replace(/[^A-Z0-9]/g, '')}`;

    await login(page);

    // ---------- 1. Criar ----------
    await page.goto('/admin/produtos/novo');
    await page.getByLabel('Nome').fill(productName);
    await page.getByRole('button', { name: /criar rascunho/i }).click();

    // Espera a aba Geral: `/admin/produtos/novo` tambem casaria com um padrao mais
    // frouxo, e o teste extrairia "novo" como id do produto.
    await page.waitForURL(/\/admin\/produtos\/[^/]+\/geral/);
    const productId = page.url().split('/admin/produtos/')[1].split('/')[0];

    // ---------- 2. Rascunho não aparece no site ----------
    await openPublicCatalog(page);
    await expect(page.getByText(productName)).toHaveCount(0);

    // ---------- 3. A aba de imagens diz o mesmo que o portão de publicação ----------
    await page.goto(`/admin/produtos/${productId}/imagens`);
    await expect(page.getByText(/precisa de uma capa para ser publicado/i)).toBeVisible();

    // ---------- 4. Conteúdo ----------
    await page.goto(`/admin/produtos/${productId}/conteudo`);
    await page.getByLabel('Título').first().fill(`${productName} — 120 m²`);
    await page.getByRole('button', { name: /salvar conteúdo/i }).click();
    await expect(page.getByText(/conteúdo salvo/i)).toBeVisible();

    // ---------- 5. Oferta com preço ----------
    await ensureCollection(page, collectionCode);

    await page.goto(`/admin/produtos/${productId}/ofertas`);
    await page.getByRole('combobox').selectOption(collectionCode);
    await page.getByRole('button', { name: /adicionar/i }).click();

    await page.getByLabel(`Preço de Coleção ${collectionCode}`).fill('1.500,00');
    await page.getByRole('button', { name: `Salvar oferta de Coleção ${collectionCode}` }).click();

    // ---------- 6. Publicar é recusado, e o painel diz o porquê ----------
    await page.goto('/admin/produtos');
    const row = page.getByRole('row').filter({ hasText: productName });

    // `/publicar/i` tambem casaria com "Despublicar" numa linha ja publicada.
    await row.getByRole('button', { name: /^publicar$/i }).click();

    // Falta a capa. A validação devolve a lista inteira de problemas de uma vez, em vez de
    // fazer o administrador descobrir os requisitos por tentativa e erro.
    await expect(page.getByText(/imagem de capa/i)).toBeVisible();
    await expect(row.getByText('Rascunho')).toBeVisible();

    // ---------- 7. E por isso continua fora do site ----------
    await openPublicCatalog(page);
    await expect(page.getByText(productName)).toHaveCount(0);
  });

  test('publicar e despublicar muda o site público na hora', async ({ page }) => {
    await login(page);

    try {
      // ---------- Despublicar ----------
      await page.goto('/admin/produtos');
      const row = page.getByRole('row').filter({ hasText: SEEDED.id });
      await row.getByRole('button', { name: /despublicar/i }).click();
      await expect(row.getByText('Rascunho')).toBeVisible();

      await page.goto(`/produtos?category=${SEEDED.category}`);
      await expect(page.getByText(SEEDED.name)).toHaveCount(0);

      // A URL direta tambem para de resolver: o filtro de status vale para o detalhe.
      await page.goto(`/${SEEDED.category}/${SEEDED.slug}`);
      await expect(page.getByText('Produto não encontrado')).toBeVisible();

      // ---------- Publicar de volta ----------
      await page.goto('/admin/produtos');
      const rowAgain = page.getByRole('row').filter({ hasText: SEEDED.id });
      await rowAgain.getByRole('button', { name: /^publicar$/i }).click();
      await expect(rowAgain.getByText('Publicado')).toBeVisible();

      await page.goto(`/produtos?category=${SEEDED.category}`);
      await expect(page.getByText(SEEDED.name).first()).toBeVisible();

      // E a pagina dele volta a renderizar o conteudo que veio do banco.
      await page.goto(`/${SEEDED.category}/${SEEDED.slug}`);
      await expect(page.getByRole('heading', { name: SEEDED.headline })).toBeVisible();
    } finally {
      // O teste mexe num produto compartilhado: se falhar no meio, o proximo test veria um
      // catalogo diferente do que espera.
      await page.request.post(`${API}/v1/admin/products/${SEEDED.id}/publish`);
    }
  });

  test('um usuário sem permissão não entra no painel', async ({ page }) => {
    // O guard do navegador é só experiência; a autorização real é do servidor e está
    // coberta pelo AdminAuthorizationTest, que percorre todas as rotas /v1/admin.
    await page.goto('/admin/produtos');

    await expect(page).toHaveURL(/\/login/);
  });
});
