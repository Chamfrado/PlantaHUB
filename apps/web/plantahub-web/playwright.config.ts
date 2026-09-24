import { defineConfig, devices } from '@playwright/test';

const baseURL = process.env.E2E_BASE_URL ?? 'http://localhost:4173';

/**
 * Teste ponta a ponta.
 *
 * Exige a API e o banco rodando, então não faz parte do `npm test`: um teste que precisa
 * de infraestrutura externa não pode bloquear o ciclo rápido de desenvolvimento. Roda por
 * `npm run e2e`, e na CI num job próprio.
 */
export default defineConfig({
  testDir: './e2e',
  timeout: 60_000,
  expect: { timeout: 10_000 },
  fullyParallel: false,
  // Um único worker: o teste cria e publica produtos reais no catálogo, e execuções
  // concorrentes disputariam o mesmo estado.
  workers: 1,
  retries: process.env.CI ? 1 : 0,
  reporter: process.env.CI ? 'list' : [['list']],

  use: {
    baseURL,
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },

  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
});
