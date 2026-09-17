/**
 * O contrato que os componentes de produto consomem.
 *
 * Invariante: **array é sempre array, string é `string | null`, nunca `undefined`.**
 * A API devolve campos opcionais porque o admin cria produtos incompletos; normalizar isso
 * uma única vez, na fronteira (os mappers), evita que `?? []` e `?.` se espalhem por dez
 * componentes — e evita que um produto esparso renderize seções vazias com título e sem
 * conteúdo.
 */

export type ProductFeature = { title: string; description: string | null };
export type ProductIncludedItem = { title: string; description: string | null; imageUrl: string | null };
export type ProductKeyFact = { value: string; label: string };
export type ProductTestimonial = {
  quote: string;
  authorName: string;
  authorTitle: string | null;
  avatarUrl: string | null;
};
export type ProductFaqItem = { question: string; answer: string | null };

/** Uma seção da página: título, introdução e itens. Vazia significa "não renderize". */
export type ProductSection<T> = {
  title: string | null;
  intro: string | null;
  items: T[];
};

export type ProductSummaryView = {
  id: string;
  category: string;
  categoryName: string;
  slug: string;
  name: string;
  shortDescription: string | null;
  areaM2: number | null;
  heroImageUrl: string | null;
  customizable: boolean;
  /**
   * Sempre em centavos, sempre com a unidade no nome.
   *
   * O campo anterior chamava-se `price.amount`, sem unidade — e foi exatamente essa
   * ambiguidade que produziu preços 100× maiores nas vitrines.
   */
  basePriceCents: number | null;
  tags: string[];
  fileFormats: string[];
};

export type ProductDetailView = ProductSummaryView & {
  delivery: string | null;
  status: string;
  galleryImageUrls: string[];

  headline: string;
  subheadline: string | null;
  description: string | null;

  whyChoose: ProductSection<ProductFeature>;
  includes: ProductSection<ProductIncludedItem>;
  keyFacts: ProductSection<ProductKeyFact>;
  testimonials: ProductSection<ProductTestimonial>;
  faq: ProductSection<ProductFaqItem>;

  finalCta: { title: string | null; subtitle: string | null };
};
