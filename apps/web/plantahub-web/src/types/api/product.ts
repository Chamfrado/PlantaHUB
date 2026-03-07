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