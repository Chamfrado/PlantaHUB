const API_BASE_URL = import.meta.env.VITE_API_URL;

type HttpMethod = 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE';

type HttpOptions = {
  method?: HttpMethod;
  body?: unknown;
  headers?: Record<string, string>;
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

  if (!response.ok) {
    let message = `HTTP error ${response.status}`;

    try {
      const text = await response.text();
      if (text) message = text;
    } catch {
      // ignore
    }

    throw new Error(message);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return response.json() as Promise<T>;
}