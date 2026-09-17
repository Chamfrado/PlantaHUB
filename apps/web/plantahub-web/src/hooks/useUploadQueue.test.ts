import { describe, expect, it } from 'vitest';

import { subPathOf, topFolderOf } from './useUploadQueue';
import { isExpired, isRetryable, UploadAbortedError, UploadHttpError } from '../lib/upload/xhrUpload';

/** Simula o que o navegador entrega num upload de pasta. */
function fileWithPath(path: string): File {
  const file = new File(['conteudo'], path.split('/').pop() ?? 'arquivo');
  Object.defineProperty(file, 'webkitRelativePath', { value: path });
  return file;
}

describe('mapeamento de pasta', () => {
  it('a pasta escolhida vira o primeiro segmento e indica a coleção', () => {
    expect(topFolderOf(fileWithPath('ARCH/planta.dwg'))).toBe('ARCH');
    expect(topFolderOf(fileWithPath('APOIO/memorial/descritivo.pdf'))).toBe('APOIO');
  });

  it('arquivo solto não tem pasta de origem', () => {
    expect(topFolderOf(new File(['x'], 'planta.pdf'))).toBeNull();
  });

  it('a subpasta exclui o primeiro segmento, que já virou a coleção', () => {
    // Manter "ARCH" aqui duplicaria a coleção dentro do próprio caminho dela.
    expect(subPathOf(fileWithPath('ARCH/plantas/nivel-1/planta.dwg'))).toBe('plantas/nivel-1');
  });

  it('arquivo na raiz da pasta não tem subpasta', () => {
    expect(subPathOf(fileWithPath('ARCH/planta.dwg'))).toBeUndefined();
  });

  it('arquivo solto não tem subpasta', () => {
    expect(subPathOf(new File(['x'], 'planta.pdf'))).toBeUndefined();
  });
});

describe('classificação de erros de envio', () => {
  it('falha de rede e indisponibilidade valem nova tentativa', () => {
    expect(isRetryable(new UploadHttpError(0, 'network_error'))).toBe(true);
    expect(isRetryable(new UploadHttpError(500, 'boom'))).toBe(true);
    expect(isRetryable(new UploadHttpError(503, 'unavailable'))).toBe(true);
    expect(isRetryable(new UploadHttpError(408, 'timeout'))).toBe(true);
  });

  it('erro do próprio pedido não vale nova tentativa', () => {
    // Repetir um 400 só gasta tempo: o pedido está errado, não a conexão.
    expect(isRetryable(new UploadHttpError(400, 'bad request'))).toBe(false);
    expect(isRetryable(new UploadHttpError(404, 'not found'))).toBe(false);
  });

  it('cancelamento nunca é repetido', () => {
    expect(isRetryable(new UploadAbortedError())).toBe(false);
  });

  it('403 é URL vencida, e exige nova assinatura em vez de repetir a mesma', () => {
    // Repetir a URL morta queimaria as três tentativas sem chance de sucesso.
    expect(isExpired(new UploadHttpError(403, 'expired'))).toBe(true);
    expect(isExpired(new UploadHttpError(500, 'boom'))).toBe(false);
  });
});
