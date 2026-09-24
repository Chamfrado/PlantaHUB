/** Um item de lista. `title` nulo é marcador simples; preenchido, é cartão com descrição. */
export type SiteItem = {
  title: string | null;
  text: string | null;
  bullets: string[];
};

export type SiteSection = {
  /** Nome do bloco, para páginas com layout próprio pegarem o que precisam pela chave. */
  key: string | null;
  title: string | null;
  body: string | null;
  items: SiteItem[];
};

export type SiteFaqItem = {
  category: string | null;
  question: string;
  answer: string;
};

export type SitePageContent = {
  headline: string | null;
  subheadline: string | null;
  intro: string | null;
  sections: SiteSection[];
  faq: SiteFaqItem[];
  ctaTitle: string | null;
  ctaSubtitle: string | null;
  ctaLabel: string | null;
  ctaHref: string | null;
};

export type SitePageResponse = {
  slug: string;
  title: string;
  content: SitePageContent;
  updatedAt: string;
  updatedBy: string | null;
};

export type SitePageSummary = {
  slug: string;
  title: string;
  updatedAt: string;
  updatedBy: string | null;
};

export type SiteSettings = {
  email: string | null;
  partnershipsEmail: string | null;
  phone: string | null;
  /** Só dígitos, formato internacional. O link é montado por quem renderiza. */
  whatsapp: string | null;
  address: string | null;
  businessHours: string | null;
  instagramUrl: string | null;
  facebookUrl: string | null;
  linkedinUrl: string | null;
  youtubeUrl: string | null;
};
