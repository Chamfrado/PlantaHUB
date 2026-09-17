/**
 * Envio de bytes para o bucket.
 *
 * Usa `XMLHttpRequest`, e não `fetch`, por um motivo concreto: `fetch` não emite evento de
 * progresso de upload. Sem progresso, enviar um DWG de 400 MB fica indistinguível de
 * travado, e a pessoa cancela algo que estava funcionando. `xhr.upload.onprogress` e
 * `xhr.abort()` resolvem os dois problemas.
 *
 * **Isto não pode passar pelo `lib/http.ts`.** Aquele wrapper força
 * `credentials: 'include'` e `Content-Type: application/json` — mandar o cookie de sessão
 * para a AWS é errado, e um cabeçalho inesperado quebra a assinatura da URL
 * (`SignatureDoesNotMatch`).
 */

export type UploadProgress = {
  loaded: number;
  total: number;
};

export class UploadHttpError extends Error {
  readonly status: number;

  constructor(status: number, message: string) {
    super(message);
    this.name = 'UploadHttpError';
    this.status = status;
  }
}

export class UploadAbortedError extends Error {
  constructor() {
    super('upload_aborted');
    this.name = 'UploadAbortedError';
  }
}

type PutOptions = {
  url: string;
  body: Blob;
  contentType?: string | null;
  onProgress?: (progress: UploadProgress) => void;
  signal?: AbortSignal;
};

/**
 * @returns o ETag devolvido pela S3 — necessário para fechar um multipart.
 *          Só é legível se o CORS do bucket expuser o cabeçalho `ETag`.
 */
export function putWithProgress({
  url,
  body,
  contentType,
  onProgress,
  signal,
}: PutOptions): Promise<string | null> {
  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest();

    xhr.open('PUT', url, true);

    // Apenas o cabeçalho que foi assinado. Qualquer extra invalida a assinatura.
    if (contentType) {
      xhr.setRequestHeader('Content-Type', contentType);
    }

    xhr.upload.onprogress = event => {
      if (event.lengthComputable) {
        onProgress?.({ loaded: event.loaded, total: event.total });
      }
    };

    xhr.onload = () => {
      if (xhr.status >= 200 && xhr.status < 300) {
        const etag = xhr.getResponseHeader('ETag');
        resolve(etag ? etag.replace(/"/g, '') : null);
        return;
      }

      reject(new UploadHttpError(xhr.status, `upload_failed_${xhr.status}`));
    };

    xhr.onerror = () => reject(new UploadHttpError(0, 'network_error'));
    xhr.ontimeout = () => reject(new UploadHttpError(408, 'timeout'));
    xhr.onabort = () => reject(new UploadAbortedError());

    if (signal) {
      if (signal.aborted) {
        xhr.abort();
        return;
      }
      signal.addEventListener('abort', () => xhr.abort(), { once: true });
    }

    xhr.send(body);
  });
}

/** Erros que vale a pena repetir: falha de rede, indisponibilidade momentânea, timeout. */
export function isRetryable(error: unknown): boolean {
  if (error instanceof UploadAbortedError) return false;

  if (error instanceof UploadHttpError) {
    return error.status === 0 || error.status === 408 || error.status >= 500;
  }

  return false;
}

/**
 * URL assinada vencida.
 *
 * Repetir a mesma URL depois disso só queima as tentativas: é preciso pedir uma nova
 * assinatura antes de tentar de novo.
 */
export function isExpired(error: unknown): boolean {
  return error instanceof UploadHttpError && error.status === 403;
}
