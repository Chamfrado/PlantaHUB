import { http, HttpResponse } from 'msw';

import { categories, fullProduct, productSummaries, sparseProduct } from '../fixtures/products';

const API = 'http://localhost:8080';

const products = [fullProduct, sparseProduct];

/**
 * Stub no nível de requisição, e não dos módulos de serviço.
 *
 * Toda esta migração é "trocar dado local por HTTP", então mockar o transporte é o que
 * expõe erros de formato de payload. Mockar `services/*` passaria batido por eles.
 */
export const handlers = [
  http.get(`${API}/v1/categories`, () => HttpResponse.json(categories)),

  http.get(`${API}/v1/products`, ({ request }) => {
    const category = new URL(request.url).searchParams.get('category');

    return HttpResponse.json(
      category ? productSummaries.filter(p => p.category === category) : productSummaries
    );
  }),

  http.get(`${API}/v1/products/:category/:slug`, ({ params }) => {
    const found = products.find(
      p => p.category === params.category && p.slug === params.slug
    );

    return found
      ? HttpResponse.json(found)
      : HttpResponse.json({ error: 'product_not_found' }, { status: 404 });
  }),

  http.get(`${API}/v1/products/:category/:slug/plan-types`, () =>
    HttpResponse.json([
      {
        code: 'ARCH',
        name: 'Planta Arquitetônica',
        description: 'Layout completo',
        priceCents: 150000,
        includedInBundle: true,
      },
    ])
  ),

  http.get(`${API}/v1/auth/me`, () => HttpResponse.json({ error: 'unauthorized' }, { status: 401 })),
  http.get(`${API}/v1/me/cart`, () => HttpResponse.json({ error: 'unauthorized' }, { status: 401 })),
  http.get(`${API}/v1/me/library`, () => HttpResponse.json([])),
];
