import type {
  ProductDetailResponse,
  ProductPageContentDTO,
  ProductSummaryResponse,
} from '../types/api/product';
import type {
  ProductDetailView,
  ProductFaqItem,
  ProductFeature,
  ProductIncludedItem,
  ProductKeyFact,
  ProductSection,
  ProductSummaryView,
  ProductTestimonial,
} from '../types/product-view';

/**
 * A fronteira onde o opcional vira total.
 *
 * Este é o único lugar do app com `?? null` e `?? []` para produto. A versão anterior
 * terminava com `as Product`, e aquele cast escondia que `page`, `tags`, `fileFormats` e
 * `delivery` nunca chegavam aos cards da home — o app só não quebrava por causa do
 * optional chaining espalhado pelos componentes.
 */

const text = (value: string | null | undefined): string | null =>
  value === undefined || value === null || value.trim() === '' ? null : value;

const list = <T,>(value: T[] | null | undefined): T[] => value ?? [];

export function mapProductSummary(api: ProductSummaryResponse): ProductSummaryView {
  return {
    id: api.id,
    category: api.category,
    categoryName: api.categoryName ?? api.category,
    slug: api.slug,
    name: api.name,
    shortDescription: text(api.shortDescription),
    areaM2: api.areaM2 ?? null,
    heroImageUrl: text(api.heroImageUrl),
    customizable: api.customizable === true,
    basePriceCents: api.basePriceCents ?? null,
    tags: list(api.tags),
    fileFormats: list(api.fileFormats),
  };
}

export function mapProductDetail(api: ProductDetailResponse): ProductDetailView {
  const content: ProductPageContentDTO = api.content ?? {};

  return {
    ...mapProductSummary(api),

    delivery: text(api.delivery),
    status: api.status,
    galleryImageUrls: list(api.galleryImageUrls),

    // O nome do produto é o título quando o admin não escreveu um: a página nunca fica
    // sem cabeçalho, mas também não inventa texto.
    headline: text(content.headline) ?? api.name,
    subheadline: text(content.subheadline),
    description: text(content.description),

    whyChoose: section<ProductFeature>(content.whyChooseTitle, content.whyChooseIntro,
      list(content.whyChooseFeatures).map(f => ({
        title: f.title,
        description: text(f.description),
      }))),

    includes: section<ProductIncludedItem>(content.includesTitle, content.includesIntro,
      list(content.includedItems).map(i => ({
        title: i.title,
        description: text(i.description),
        imageUrl: text(i.imageUrl),
      }))),

    keyFacts: section<ProductKeyFact>(content.keyFactsTitle, content.keyFactsIntro,
      list(content.keyFacts).map(k => ({ value: k.value, label: k.label }))),

    testimonials: section<ProductTestimonial>(content.testimonialsTitle, content.testimonialsIntro,
      list(content.testimonials).map(t => ({
        quote: t.quote,
        authorName: t.authorName,
        authorTitle: text(t.authorTitle),
        avatarUrl: text(t.avatarUrl),
      }))),

    faq: section<ProductFaqItem>(content.faqTitle, content.faqIntro,
      list(content.faq).map(q => ({ question: q.question, answer: text(q.answer) }))),

    finalCta: {
      title: text(content.finalCtaTitle),
      subtitle: text(content.finalCtaSubtitle),
    },

    tags: list(content.tags).length > 0 ? list(content.tags) : list(api.tags),
    fileFormats: list(content.fileFormats).length > 0 ? list(content.fileFormats) : list(api.fileFormats),
  };
}

function section<T>(
  title: string | null | undefined,
  intro: string | null | undefined,
  items: T[]
): ProductSection<T> {
  return { title: text(title), intro: text(intro), items };
}
