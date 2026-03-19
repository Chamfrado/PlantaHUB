import { http } from '../lib/http';
import type { PlanTypeOptionDTO, ProductSummaryResponse } from '../types/api/product';

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

export async function getProductPlanTypes(category: string, slug: string) {
  return http<PlanTypeOptionDTO[]>(`/v1/products/${category}/${slug}/plan-types`);
}