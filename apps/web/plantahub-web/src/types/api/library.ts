export type LibraryAssetDTO = {
  id: string;
  filename: string;
  storageKey: string;
  version: number;
  sizeBytes: number;
  createdAt: string;
};

export type LibraryPlanTypeDTO = {
  code: string;
  name: string;
  assets: LibraryAssetDTO[];
};

export type LibraryProductDTO = {
  productId: string;
  category: string;
  slug: string;
  name: string;
  heroImageUrl: string;
  areaM2: number;
  purchasedAt: string;
  planTypes: LibraryPlanTypeDTO[];
};