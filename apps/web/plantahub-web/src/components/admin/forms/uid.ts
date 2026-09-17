/**
 * Identidade estável de um item enquanto ele está sendo editado.
 *
 * Existe só no formulário e é removida antes de enviar. É o detalhe que evita o bug
 * clássico de listas editáveis: usar o índice como `key` faz o React reaproveitar o nó
 * errado depois de reordenar (o texto digitado "pula" de um item para o outro), e usar o
 * conteúdo como `key` remonta o campo a cada tecla, derrubando o cursor.
 */
export type WithUid<T> = T & { _uid: string };

export function withUid<T>(items: T[]): WithUid<T>[] {
  return items.map(item => ({ ...item, _uid: crypto.randomUUID() }));
}

export function attachUid<T>(item: T): WithUid<T> {
  return { ...item, _uid: crypto.randomUUID() };
}

export function stripUid<T>(items: WithUid<T>[]): T[] {
  return items.map(item => {
    const copy: Record<string, unknown> = { ...item };
    delete copy._uid;
    return copy as T;
  });
}
