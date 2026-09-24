import react from '@vitejs/plugin-react';
import { defineConfig } from 'vitest/config';

// Config separada da vite.config.ts de proposito: o build de producao nao precisa
// carregar nada de teste, e o Tailwind nao roda nos testes.
export default defineConfig({
  plugins: [react()],
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: ['./src/test/setup.ts'],
    include: ['src/**/*.{test,spec}.{ts,tsx}'],
    // O e2e do Playwright roda por `npm run e2e`, nunca no `npm test`.
    exclude: ['node_modules', 'dist', 'e2e'],
    // Os handlers do MSW interceptam este endereco (src/test/msw/handlers.ts). Fixado
    // aqui para os testes nao dependerem de um .env.test local (ignorado pelo git) nem
    // herdarem o VITE_API_URL de producao que o workflow de deploy exporta.
    env: {
      VITE_API_URL: 'http://localhost:8080',
    },
  },
});
