import { dispatchSessionExpiredEvent } from './auth-events';

const API_BASE_URL = import.meta.env.VITE_API_URL;

// Sem VITE_API_URL toda requisicao vira "undefined/v1/...", que falha com um erro de rede
// generico e manda o desenvolvedor cacar o problema no lugar errado. Falha ruidosamente
// em dev (onde da para corrigir na hora) e registra em producao (onde derrubar a pagina
// inteira seria pior do que um erro por request).
if (!API_BASE_URL) {
  const message =
    'VITE_API_URL nao esta definida. Copie .env.example para .env.local e preencha a URL da API.';

  if (import.meta.env.DEV) {
    throw new Error(message);
  }

  console.error(message);
}

type HttpMethod = 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE';

type HttpOptions = {
  method?: HttpMethod;
  body?: unknown;
  headers?: Record<string, string>;
};

type HttpError = Error & {
  status?: number;
  body?: unknown;
};

export async function http<T>(path: string, options: HttpOptions = {}): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    method: options.method ?? 'GET',
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json',
      ...(options.headers ?? {}),
    },
    body: options.body ? JSON.stringify(options.body) : undefined,
  });

  const contentType = response.headers.get('content-type') ?? '';

  if (!response.ok) {
    let message = `HTTP error ${response.status}`;
    let body: unknown = undefined;

    try {
      if (contentType.includes('application/json')) {
        body = await response.json();

        if (
          body &&
          typeof body === 'object' &&
          'message' in body &&
          typeof (body as { message?: unknown }).message === 'string'
        ) {
          message = (body as { message: string }).message;
        }
      } else {
        const text = await response.text();

        if (text.trim()) {
          message = text;
          body = text;
        }
      }
    } catch {
      // ignore parse errors
    }

    if (response.status === 401 && path !== '/v1/auth/me' && path !== '/v1/auth/login') {
      dispatchSessionExpiredEvent();
    }

    const error = new Error(message) as HttpError;
    error.status = response.status;
    error.body = body;
    throw error;
  }

  if (response.status === 204) {
    return undefined as T;
  }

  if (!contentType.includes('application/json')) {
    const text = await response.text();

    if (!text.trim()) {
      return undefined as T;
    }

    return text as T;
  }

  const text = await response.text();

  if (!text.trim()) {
    return undefined as T;
  }

  return JSON.parse(text) as T;
}
