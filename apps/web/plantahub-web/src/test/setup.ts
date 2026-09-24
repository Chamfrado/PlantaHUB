import '@testing-library/jest-dom/vitest';
import { cleanup } from '@testing-library/react';
import { afterAll, afterEach, beforeAll } from 'vitest';

import { resetCategoriesCache } from '../services/categories.service';
import { server } from './msw/server';

// `onUnhandledRequest: 'error'` de proposito: uma requisicao nao prevista e quase sempre
// um erro de rota ou de payload, e falhar alto e melhor do que devolver undefined.
beforeAll(() => server.listen({ onUnhandledRequest: 'error' }));

afterEach(() => {
  cleanup();
  server.resetHandlers();
  // O cache de categorias vive em modulo: sem limpar, um teste contamina o seguinte.
  resetCategoriesCache();
});

afterAll(() => server.close());
