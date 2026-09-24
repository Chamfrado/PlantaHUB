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
    const body = err.body as {
      message?: string;
      error?: string;
      fields?: { field?: string; message?: string }[];
      reasons?: string[];
    };

    if (typeof body.message === 'string' && body.message.trim()) {
      return body.message;
    }

    // Erros de validação trazem a lista de campos. Mostrar só "validation_failed" deixaria
    // quem está preenchendo o formulário sem saber o que corrigir.
    if (Array.isArray(body.fields) && body.fields.length > 0) {
      const details = body.fields
        .map(field => [field.field, field.message].filter(Boolean).join(': '))
        .filter(Boolean)
        .join('; ');

      if (details) return details;
    }

    if (Array.isArray(body.reasons) && body.reasons.length > 0) {
      return body.reasons.join('; ');
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