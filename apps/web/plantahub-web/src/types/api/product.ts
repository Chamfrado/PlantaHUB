export type ProductSummaryResponse = {
  id: string;
  category: string;
  slug: string;
  name: string;
  shortDescription: string;
  areaM2: number;
  heroImageUrl: string | null;
  customizable: boolean;
  basePriceCents: number;
};

export type ProductDetailResponse = {
  id: string;
  category: string;
  slug: string;
  name: string;
  shortDescription: string;
  areaM2: number;
  heroImageUrl: string | null;
  delivery: string;
  customizable: boolean;
};

export type PlanTypeOptionDTO = {
  code: string;
  name: string;
  description: string;
  priceCents: number;
  includedInBundle: boolean;
};