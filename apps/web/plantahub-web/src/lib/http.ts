import { dispatchSessionExpiredEvent } from './auth-events';

const API_BASE_URL = import.meta.env.VITE_API_URL;

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

function getStoredToken() {
  return localStorage.getItem('token');
}

function getStoredTokenType() {
  return localStorage.getItem('tokenType') ?? 'Bearer';
}

export async function http<T>(path: string, options: HttpOptions = {}): Promise<T> {
  const token = getStoredToken();
  const tokenType = getStoredTokenType();

  const response = await fetch(`${API_BASE_URL}${path}`, {
    method: options.method ?? 'GET',
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `${tokenType} ${token}` } : {}),
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

    if (response.status === 401 && token) {
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