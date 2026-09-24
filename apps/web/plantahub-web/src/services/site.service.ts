import { http } from '../lib/http';
import type { SitePageResponse, SiteSettings } from '../types/api/site';

/**
 * Texto institucional e dados de contato.
 *
 * As configurações ficam memoizadas em nível de módulo pelo mesmo motivo das categorias: o
 * rodapé está em toda página e precisa dos links de redes sociais. Sem isso, cada navegação
 * renderia uma requisição idêntica.
 *
 * As páginas não são memoizadas: cada uma é visitada uma vez por sessão, e guardá-las faria
 * uma edição no painel demorar a aparecer sem ganho nenhum.
 */
let settingsInFlight: Promise<SiteSettings> | null = null;

export function getSiteSettings(): Promise<SiteSettings> {
  if (!settingsInFlight) {
    settingsInFlight = http<SiteSettings>('/v1/site/settings').catch(error => {
      // Uma falha não pode envenenar o cache: a próxima chamada tenta de novo.
      settingsInFlight = null;
      throw error;
    });
  }

  return settingsInFlight;
}

export function resetSiteSettingsCache() {
  settingsInFlight = null;
}

export function getSitePage(slug: string): Promise<SitePageResponse> {
  return http<SitePageResponse>(`/v1/site/pages/${encodeURIComponent(slug)}`);
}

/** Link de WhatsApp a partir do número cru. Nulo quando não há número cadastrado. */
export function whatsappLink(settings: SiteSettings | null): string | null {
  const digits = (settings?.whatsapp ?? '').replace(/\D/g, '');
  return digits ? `https://wa.me/${digits}` : null;
}
