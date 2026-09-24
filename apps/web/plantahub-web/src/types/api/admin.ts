import type { ProductPageContentDTO, ProductStatus } from './product';

export type AdminProductSummary = {
  id: string;
  category: string;
  slug: string;
  name: string;
  status: ProductStatus;
  basePriceCents: number | null;
  heroImageUrl: string | null;
  publishedAt: string | null;
  updatedAt: string | null;
};

export type CreateProductInput = {
  id?: string;
  category: string;
  slug?: string;
  name: string;
  shortDescription?: string;
  areaM2?: number;
  basePriceCents?: number;
  delivery?: string;
  customizable?: boolean;
};

export type UpdateProductInput = Partial<Omit<CreateProductInput, 'id'>>;

export type ProductContentInput = ProductPageContentDTO;

/** Resposta de `publish-check`: o que ainda falta, sem tentar publicar. */
export type PublishCheck = {
  publishable: boolean;
  problems: string[];
};

export type AdminCollection = {
  id: string;
  code: string;
  name: string;
  description: string | null;
  purchasable: boolean;
  bundledWithEveryOffer: boolean;
  active: boolean;
  sortOrder: number;
  assetCount: number;
};

export type CreateCollectionInput = {
  code: string;
  name: string;
  description?: string;
  purchasable?: boolean;
  bundledWithEveryOffer?: boolean;
  sortOrder?: number;
};

export type AdminOffer = {
  id: string;
  collectionCode: string;
  collectionName: string;
  purchasableCollection: boolean;
  bundledWithEveryOffer: boolean;
  priceCents: number | null;
  available: boolean;
  includedInBundle: boolean;
  sortOrder: number;
};

export type OfferInput = {
  priceCents?: number;
  available?: boolean;
  includedInBundle?: boolean;
  sortOrder?: number;
};

export type AdminAsset = {
  id: string;
  collectionCode: string;
  filename: string;
  /** Exposta de propósito: é o que permite conferir contra o bucket. */
  storageKey: string;
  relativePath: string | null;
  fileExt: string | null;
  sizeBytes: number | null;
  sortOrder: number;
  keyScheme: string;
  reconciliationStatus: string;
  deleted: boolean;
  createdAt: string;
};

export type AdminMedia = {
  id: string;
  role: 'HERO' | 'GALLERY';
  storageKey: string;
  /** Nulo quando a linha nao tem URL publica: a chave do bucket nao e endereco. */
  url: string | null;
  altText: string | null;
  sortOrder: number;
  sizeBytes: number | null;
};

export type AdminCategory = {
  slug: string;
  name: string;
  description: string | null;
  sortOrder: number;
  featuredOnHome: boolean;
  homeOrder: number;
  comingSoon: boolean;
  active: boolean;
  productCount: number;
};

/** Produto como aparece na ordenação da vitrine de uma categoria. */
export type AdminCategoryProduct = {
  id: string;
  name: string;
  status: string;
};
