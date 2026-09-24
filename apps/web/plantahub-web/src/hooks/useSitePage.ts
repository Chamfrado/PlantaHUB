import { useAsync } from './useAsync';
import { getSitePage, getSiteSettings } from '../services/site.service';
import type { SitePageContent, SiteSection } from '../types/api/site';

const EMPTY: SitePageContent = {
  headline: null,
  subheadline: null,
  intro: null,
  sections: [],
  faq: [],
  ctaTitle: null,
  ctaSubtitle: null,
  ctaLabel: null,
  ctaHref: null,
};

/**
 * O texto de uma página institucional.
 *
 * Devolve `EMPTY` enquanto carrega, em vez de `null`: quem renderiza não precisa se
 * defender de ausência em cada campo, e a página aparece com a estrutura certa e os textos
 * chegando — em vez de piscar uma tela vazia.
 */
export function useSitePage(slug: string) {
  const request = useAsync(`site:${slug}`, () => getSitePage(slug));

  return {
    content: request.data?.content ?? EMPTY,
    loading: request.loading,
    error: request.error,
    reload: request.reload,
  };
}

export function useSiteSettings() {
  const request = useAsync('site:settings', getSiteSettings);
  return request.data ?? null;
}

/** A seção de nome informado, ou `null`. Páginas com layout próprio pegam pelo nome. */
export function sectionByKey(content: SitePageContent, key: string): SiteSection | null {
  return content.sections.find(section => section.key === key) ?? null;
}
