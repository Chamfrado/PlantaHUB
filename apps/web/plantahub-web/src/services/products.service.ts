import { http } from '../lib/http';
import type { ProductSummaryResponse } from '../types/api/product';

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