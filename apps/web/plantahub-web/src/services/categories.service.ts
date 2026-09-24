import { http } from '../lib/http';
import type { CategoryResponse } from '../types/api/product';

/**
 * Categorias do catálogo.
 *
 * A promise fica memoizada em nível de módulo porque o Footer (presente em toda página), o
 * Hero, a listagem e o cabeçalho do produto precisam da mesma lista: sem isso seriam quatro
 * requisições idênticas por navegação. Categoria muda raramente e nunca durante uma
 * sessão, então uma busca por carregamento é o comportamento certo.
 */
let inFlight: Promise<CategoryResponse[]> | null = null;

export function listCategories(): Promise<CategoryResponse[]> {
  if (!inFlight) {
    inFlight = http<CategoryResponse[]>('/v1/categories').catch(error => {
      // Uma falha não pode envenenar o cache: a próxima chamada tenta de novo.
      inFlight = null;
      throw error;
    });
  }

  return inFlight;
}

/** Descarta o cache. Existe para os testes e para o painel, depois de editar categorias. */
export function resetCategoriesCache() {
  inFlight = null;
}
