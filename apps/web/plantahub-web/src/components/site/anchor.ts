/**
 * Transforma um título em âncora estável.
 *
 * Vive aqui, e não em cada página, porque o índice lateral e o cabeçalho de cada seção
 * precisam chegar ao mesmo valor — calcular de formas diferentes produziria links que não
 * levam a lugar nenhum.
 */
export function anchorOf(title: string) {
  return title
    .toLowerCase()
    .normalize('NFD')
    .replace(/[̀-ͯ]/g, '')
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/(^-|-$)/g, '');
}

