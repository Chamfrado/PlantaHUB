import { http } from '../../lib/http';
import type {
  SitePageContent,
  SitePageResponse,
  SitePageSummary,
  SiteSettings,
} from '../../types/api/site';
import type {
  AdminAsset,
  AdminCategory,
  AdminCollection,
  AdminMedia,
  AdminOffer,
  AdminProductSummary,
  CreateCollectionInput,
  CreateProductInput,
  OfferInput,
  ProductContentInput,
  PublishCheck,
  UpdateProductInput,
} from '../../types/api/admin';
import type { PlanTypeOptionDTO, ProductDetailResponse } from '../../types/api/product';

const base = '/v1/admin';

// ---------------- Produtos ----------------

export function listAdminProducts(params: { status?: string; category?: string; q?: string } = {}) {
  const search = new URLSearchParams();
  if (params.status) search.set('status', params.status);
  if (params.category) search.set('category', params.category);
  if (params.q) search.set('q', params.q);

  const query = search.toString();
  return http<AdminProductSummary[]>(`${base}/products${query ? `?${query}` : ''}`);
}

export function getAdminProduct(id: string) {
  return http<ProductDetailResponse>(`${base}/products/${encodeURIComponent(id)}`);
}

export function createProduct(input: CreateProductInput) {
  return http<ProductDetailResponse>(`${base}/products`, { method: 'POST', body: input });
}

export function updateProduct(id: string, input: UpdateProductInput) {
  return http<ProductDetailResponse>(`${base}/products/${encodeURIComponent(id)}`, {
    method: 'PUT',
    body: input,
  });
}

/** Substitui o documento de conteúdo inteiro: o admin salva a página de uma vez. */
export function replaceProductContent(id: string, content: ProductContentInput) {
  return http<ProductDetailResponse>(`${base}/products/${encodeURIComponent(id)}/content`, {
    method: 'PUT',
    body: content,
  });
}

export function publishProduct(id: string) {
  return http<ProductDetailResponse>(`${base}/products/${encodeURIComponent(id)}/publish`, {
    method: 'POST',
  });
}

export function unpublishProduct(id: string) {
  return http<ProductDetailResponse>(`${base}/products/${encodeURIComponent(id)}/unpublish`, {
    method: 'POST',
  });
}

export function archiveProduct(id: string) {
  return http<ProductDetailResponse>(`${base}/products/${encodeURIComponent(id)}/archive`, {
    method: 'POST',
  });
}

export function checkPublishable(id: string) {
  return http<PublishCheck>(`${base}/products/${encodeURIComponent(id)}/publish-check`);
}

export function previewProduct(id: string) {
  return http<{ product: ProductDetailResponse; planTypes: PlanTypeOptionDTO[] }>(
    `${base}/products/${encodeURIComponent(id)}/preview`
  );
}

// ---------------- Coleções ----------------

export function listCollections() {
  return http<AdminCollection[]>(`${base}/collections`);
}

export function createCollection(input: CreateCollectionInput) {
  return http<AdminCollection>(`${base}/collections`, { method: 'POST', body: input });
}

export function updateCollection(id: string, input: Partial<CreateCollectionInput>) {
  return http<AdminCollection>(`${base}/collections/${id}`, { method: 'PUT', body: input });
}

export function setCollectionActive(id: string, active: boolean) {
  return http<AdminCollection>(`${base}/collections/${id}/${active ? 'activate' : 'deactivate'}`, {
    method: 'POST',
  });
}

// ---------------- Ofertas ----------------

export function listOffers(productId: string) {
  return http<AdminOffer[]>(`${base}/products/${encodeURIComponent(productId)}/offers`);
}

export function upsertOffer(productId: string, collectionCode: string, input: OfferInput) {
  return http<AdminOffer>(
    `${base}/products/${encodeURIComponent(productId)}/offers/${encodeURIComponent(collectionCode)}`,
    { method: 'PUT', body: input }
  );
}

export function removeOffer(productId: string, collectionCode: string) {
  return http<void>(
    `${base}/products/${encodeURIComponent(productId)}/offers/${encodeURIComponent(collectionCode)}`,
    { method: 'DELETE' }
  );
}

// ---------------- Arquivos ----------------

export function listAssets(productId: string, includeDeleted = false) {
  const query = includeDeleted ? '?includeDeleted=true' : '';
  return http<AdminAsset[]>(`${base}/products/${encodeURIComponent(productId)}/assets${query}`);
}

/** Associar e desassociar: troca o vínculo, nunca a chave no bucket. */
export function moveAsset(assetId: string, collectionCode: string) {
  return http<AdminAsset>(`${base}/assets/${assetId}/move`, {
    method: 'POST',
    body: { collectionCode },
  });
}

export function deleteAsset(assetId: string, force = false) {
  return http<void>(`${base}/assets/${assetId}${force ? '?force=true' : ''}`, { method: 'DELETE' });
}

export function restoreAsset(assetId: string) {
  return http<AdminAsset>(`${base}/assets/${assetId}/restore`, { method: 'POST' });
}

// ---------------- Mídia ----------------

export function listMedia(productId: string) {
  return http<AdminMedia[]>(`${base}/products/${encodeURIComponent(productId)}/media`);
}

export function updateMedia(
  mediaId: string,
  input: { role?: 'HERO' | 'GALLERY'; altText?: string; sortOrder?: number }
) {
  return http<AdminMedia>(`${base}/media/${mediaId}`, { method: 'PATCH', body: input });
}

/**
 * Grava a ordem inteira de uma vez.
 *
 * Enviar a lista completa, em vez de "esta subiu uma posicao", deixa o servidor com a
 * ordem final que o administrador ve na tela — sem depender de os dois lados calcularem o
 * mesmo resultado a partir de uma sequencia de movimentos.
 */
export function reorderMedia(productId: string, mediaIds: string[]) {
  return http<AdminMedia[]>(
    `${base}/products/${encodeURIComponent(productId)}/media/reorder`,
    { method: 'POST', body: { mediaIds } }
  );
}

export function deleteMedia(mediaId: string) {
  return http<void>(`${base}/media/${mediaId}`, { method: 'DELETE' });
}

// ---------------- Categorias ----------------

export function listAdminCategories() {
  return http<AdminCategory[]>(`${base}/categories`);
}

export function createCategory(input: { slug: string; name: string; description?: string }) {
  return http<AdminCategory>(`${base}/categories`, { method: 'POST', body: input });
}

// ---------------- Páginas institucionais ----------------

export function listSitePages() {
  return http<SitePageSummary[]>(`${base}/site/pages`);
}

export function getSitePageForEdit(slug: string) {
  return http<SitePageResponse>(`${base}/site/pages/${encodeURIComponent(slug)}`);
}

export function updateSitePage(slug: string, input: { title: string; content: SitePageContent }) {
  return http<SitePageResponse>(`${base}/site/pages/${encodeURIComponent(slug)}`, {
    method: 'PUT',
    body: input,
  });
}

export function getAdminSiteSettings() {
  return http<SiteSettings>(`${base}/site/settings`);
}

export function updateSiteSettings(settings: SiteSettings) {
  return http<SiteSettings>(`${base}/site/settings`, { method: 'PUT', body: { settings } });
}
