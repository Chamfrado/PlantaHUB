export type ProductStatus = 'DRAFT' | 'PUBLISHED' | 'ARCHIVED';

export type ProductFeatureDTO = { title: string; description?: string | null };
export type ProductIncludedItemDTO = {
  title: string;
  description?: string | null;
  imageUrl?: string | null;
};
export type ProductKeyFactDTO = { value: string; label: string };
export type ProductTestimonialDTO = {
  quote: string;
  authorName: string;
  authorTitle?: string | null;
  avatarUrl?: string | null;
};
export type ProductFaqItemDTO = { question: string; answer?: string | null };

/**
 * Texto da página, vindo do banco.
 *
 * Tudo é opcional porque o admin cria produtos incompletos — um rascunho pode ter só nome.
 * A normalização para um formato total acontece no mapper, uma vez só.
 */
export type ProductPageContentDTO = {
  headline?: string | null;
  subheadline?: string | null;
  description?: string | null;

  whyChooseTitle?: string | null;
  whyChooseIntro?: string | null;
  whyChooseFeatures?: ProductFeatureDTO[] | null;

  includesTitle?: string | null;
  includesIntro?: string | null;
  includedItems?: ProductIncludedItemDTO[] | null;

  keyFactsTitle?: string | null;
  keyFactsIntro?: string | null;
  keyFacts?: ProductKeyFactDTO[] | null;

  testimonialsTitle?: string | null;
  testimonialsIntro?: string | null;
  testimonials?: ProductTestimonialDTO[] | null;

  faqTitle?: string | null;
  faqIntro?: string | null;
  faq?: ProductFaqItemDTO[] | null;

  finalCtaTitle?: string | null;
  finalCtaSubtitle?: string | null;

  tags?: string[] | null;
  fileFormats?: string[] | null;
};

export type ProductSummaryResponse = {
  id: string;
  category: string;
  categoryName?: string | null;
  slug: string;
  name: string;
  shortDescription?: string | null;
  areaM2?: number | null;
  heroImageUrl?: string | null;
  customizable?: boolean | null;
  basePriceCents?: number | null;
  tags?: string[] | null;
  fileFormats?: string[] | null;
};

export type ProductDetailResponse = ProductSummaryResponse & {
  delivery?: string | null;
  status: ProductStatus;
  galleryImageUrls?: string[] | null;
  content?: ProductPageContentDTO | null;
};

export type PlanTypeOptionDTO = {
  code: string;
  name: string;
  description: string;
  priceCents: number;
  includedInBundle: boolean;
};

export type CategoryResponse = {
  slug: string;
  name: string;
  description?: string | null;
  order: number;
  featuredOnHome: boolean;
  homeOrder: number;
};
