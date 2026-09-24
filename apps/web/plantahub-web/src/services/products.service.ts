import { http } from '../lib/http';
import type {
  PlanTypeOptionDTO,
  ProductDetailResponse,
  ProductSummaryResponse,
} from '../types/api/product';

type ListProductsParams = {
  category?: string;
  limit?: number;
};

export async function listProducts(params: ListProductsParams = {}) {
  const searchParams = new URLSearchParams();

  if (params.category) {
    searchParams.set('category', params.category);
  }

  if (params.limit) {
    searchParams.set('limit', String(params.limit));
  }

  const query = searchParams.toString();

  return http<ProductSummaryResponse[]>(`/v1/products${query ? `?${query}` : ''}`);
}

export async function getProduct(category: string, slug: string) {
  return http<ProductDetailResponse>(
    `/v1/products/${encodeURIComponent(category)}/${encodeURIComponent(slug)}`
  );
}

export async function getProductPlanTypes(category: string, slug: string) {
  return http<PlanTypeOptionDTO[]>(
    `/v1/products/${encodeURIComponent(category)}/${encodeURIComponent(slug)}/plan-types`
  );
}
