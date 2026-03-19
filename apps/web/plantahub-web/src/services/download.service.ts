import { http } from '../lib/http';
import type {
    DownloadBundleRequest,
    DownloadBundleResponseDTO,
    DownloadResponseDTO,
} from '../types/api/download';

export async function getDownloadDetails(productId: string, planTypeCode: string) {
  return http<DownloadResponseDTO>(
    `/v1/me/downloads/products/${encodeURIComponent(productId)}/plan-types/${encodeURIComponent(
      planTypeCode.toUpperCase(),
    )}`,
  );
}

export async function createDownloadBundle(payload: DownloadBundleRequest) {
  const normalizedPayload: DownloadBundleRequest = {
    items: payload.items.map(item => ({
      productId: item.productId,
      planTypeCodes: item.planTypeCodes.map(code => code.toUpperCase()),
    })),
  };

  return http<DownloadBundleResponseDTO>('/v1/me/downloads/bundle', {
    method: 'POST',
    body: normalizedPayload,
  });
}