import { http } from '../../lib/http';

export type TargetKind = 'ASSET' | 'MEDIA';

export type PresignedPart = { partNumber: number; url: string };

export type PresignResponse = {
  uploadId: string;
  storageKey: string;
  method: 'PUT' | 'MULTIPART';
  url: string | null;
  /** Cabeçalho que o navegador DEVE enviar, exatamente assim: ele entra na assinatura. */
  contentType: string | null;
  multipartUploadId: string | null;
  partSizeBytes: number | null;
  parts: PresignedPart[] | null;
  expiresInSeconds: number;
};

export type PresignInput = {
  targetKind: TargetKind;
  productId: string;
  collectionCode?: string;
  filename: string;
  contentType?: string;
  sizeBytes: number;
  relativePath?: string;
};

export class SessionExpiredError extends Error {
  constructor() {
    super('session_expired');
    this.name = 'SessionExpiredError';
  }
}

/**
 * Autoriza um upload.
 *
 * Trata o 401 aqui em vez de deixar o tratamento global agir. O `lib/http.ts` dispara um
 * evento de sessão expirada que navega para `/login` — o que desmontaria o painel no meio
 * de uma fila de 40 arquivos e perderia todo o progresso. Convertendo em erro tipado, a
 * fila consegue apenas pausar e oferecer retomar.
 */
export async function presignUpload(input: PresignInput): Promise<PresignResponse> {
  try {
    return await http<PresignResponse>('/v1/admin/uploads/presign', {
      method: 'POST',
      body: input,
    });
  } catch (error) {
    if ((error as { status?: number })?.status === 401) {
      throw new SessionExpiredError();
    }
    throw error;
  }
}

export function requestMoreParts(uploadId: string, fromPartNumber: number, count: number) {
  return http<PresignedPart[]>(`/v1/admin/uploads/${uploadId}/parts`, {
    method: 'POST',
    body: { fromPartNumber, count },
  });
}

export function completeMultipart(
  uploadId: string,
  parts: { partNumber: number; etag: string }[]
) {
  return http<void>(`/v1/admin/uploads/${uploadId}/complete-multipart`, {
    method: 'POST',
    body: { parts },
  });
}

export function confirmUpload(uploadId: string, checksumSha256?: string) {
  return http<{ id: string }>(`/v1/admin/uploads/${uploadId}/confirm`, {
    method: 'POST',
    body: { checksumSha256: checksumSha256 ?? null },
  });
}

export function abortUpload(uploadId: string) {
  return http<void>(`/v1/admin/uploads/${uploadId}`, { method: 'DELETE' });
}
