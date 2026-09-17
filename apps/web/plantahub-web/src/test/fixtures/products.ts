import type { ProductDetailResponse, ProductSummaryResponse } from '../../types/api/product';

/**
 * Produto completo: todas as seções preenchidas, como os seis produtos migrados.
 */
export const fullProduct: ProductDetailResponse = {
  id: 'casa-confort-80m2',
  category: 'casas',
  categoryName: 'Casas',
  slug: 'confort',
  name: 'Confort',
  shortDescription: 'Kitnet Moderna',
  areaM2: 80,
  heroImageUrl: 'https://cdn.test/confort.webp',
  customizable: true,
  basePriceCents: 150000,
  status: 'PUBLISHED',
  delivery: 'Download digital imediato.',
  galleryImageUrls: ['https://cdn.test/g1.webp'],
  tags: ['casas', 'kitnet'],
  fileFormats: ['PDF', 'DWG'],
  content: {
    headline: 'Confort - 80 m2',
    subheadline: 'Kitnet Moderna',
    description: 'Transforme sua experiência de construir uma casa.',
    whyChooseTitle: 'Por que escolher a Casa Confort?',
    whyChooseIntro: 'Detalhes minuciosos.',
    whyChooseFeatures: [
      { title: 'Design Compreensivo', description: 'Todas as plantas essenciais.' },
      { title: 'Acesso Imediato', description: 'Download na confirmação.' },
      { title: 'Customizável', description: null },
      // O quarto item existe de propósito: a versão anterior cortava em três.
      { title: 'Quarto diferencial', description: 'Não pode sumir.' },
    ],
    includesTitle: 'O que está incluso',
    includedItems: [{ title: 'Planta Arquitetônica', description: 'Layout detalhado.' }],
    keyFactsTitle: 'Fatos Chave',
    keyFacts: [
      { value: '80', label: 'metros quadrados' },
      { value: '5+', label: 'plantas inclusas' },
      { value: '3', label: 'terceiro fato que era cortado' },
    ],
    testimonialsTitle: 'O que dizem nossos clientes',
    testimonials: [{ quote: 'Serviço rápido.', authorName: 'James Foster' }],
    faqTitle: 'Perguntas Frequentes',
    faq: [{ question: 'Posso modificar as plantas?', answer: 'Pode sim.' }],
    finalCtaTitle: 'Pronto para construir?',
    finalCtaSubtitle: 'Compre agora.',
    tags: ['casas', 'kitnet'],
    fileFormats: ['PDF', 'DWG'],
  },
};

/**
 * Produto recém-criado no painel: só o essencial.
 *
 * É o cenário que mais importa depois desta modernização — antes, todo produto vinha de um
 * arquivo escrito à mão e sempre tinha conteúdo completo.
 */
export const sparseProduct: ProductDetailResponse = {
  id: 'casa-nova-100m2',
  category: 'casas',
  categoryName: 'Casas',
  slug: 'nova',
  name: 'Casa Nova',
  status: 'PUBLISHED',
  content: {},
};

export const productSummaries: ProductSummaryResponse[] = [
  {
    id: fullProduct.id,
    category: 'casas',
    categoryName: 'Casas',
    slug: 'confort',
    name: 'Confort',
    shortDescription: 'Kitnet Moderna',
    areaM2: 80,
    heroImageUrl: 'https://cdn.test/confort.webp',
    customizable: true,
    basePriceCents: 150000,
    tags: ['casas'],
    fileFormats: ['PDF'],
  },
  {
    id: 'chale-prime-85m2',
    category: 'chales',
    categoryName: 'Chalés',
    slug: 'prime',
    name: 'Prime',
    shortDescription: 'Chalé Familiar',
    areaM2: 85,
    heroImageUrl: null,
    customizable: true,
    basePriceCents: 375000,
    tags: [],
    fileFormats: [],
  },
];

export const categories = [
  { slug: 'casas', name: 'Casas', order: 1, featuredOnHome: true, homeOrder: 1 },
  { slug: 'chales', name: 'Chalés', order: 2, featuredOnHome: true, homeOrder: 2 },
];
