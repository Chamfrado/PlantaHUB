export type ApiErrorLike = Error & {
  status?: number;
  body?: unknown;
};

export function getApiErrorMessage(error: unknown, fallback: string): string {
  if (!error || typeof error !== 'object') {
    return fallback;
  }

  const err = error as ApiErrorLike & {
    message?: string;
    body?: {
      message?: string;
      error?: string;
    };
  };

  if (err.body && typeof err.body === 'object') {
    const body = err.body as { message?: string; error?: string };

    if (typeof body.message === 'string' && body.message.trim()) {
      return body.message;
    }

    if (typeof body.error === 'string' && body.error.trim()) {
      return body.error;
    }
  }

  if (typeof err.message === 'string' && err.message.trim()) {
    return err.message;
  }

  return fallback;
}