import type { ProductSummaryResponse } from '../types/api/product';
import type { Product } from '../types/ProductData';

export function mapProductSummaryToProduct(api: ProductSummaryResponse): Product {
  return {
    id: api.id,
    category: api.category,
    slug: api.slug,
    name: api.name,
    shortDescription: api.shortDescription,
    heroImageUrl: api.heroImageUrl ?? undefined,
    areaM2: api.areaM2,
    customizable: api.customizable,
    price: {
      amount: api.basePriceCents,
      currency: 'BRL',
    },
  } as Product;
}