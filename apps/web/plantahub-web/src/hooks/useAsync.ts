import { useCallback, useEffect, useRef, useState } from 'react';

type AsyncState<T> = {
  data: T | null;
  loading: boolean;
  error: unknown;
};

type Settled<T> = {
  /** Qual requisição produziu este resultado. */
  requestId: string;
  data: T | null;
  error: unknown;
};

/**
 * Busca de dados para telas somente-leitura.
 *
 * Substitui o bloco `useEffect` + três `useState` que estava copiado em cinco lugares.
 * Não usamos uma biblioteca de data-fetching no site público de propósito: são duas telas
 * sem mutação e sem invalidação cruzada de cache. O painel admin, onde invalidar depois de
 * salvar custa caro, é outra história.
 *
 * A chave é uma string estável em vez de um array de dependências: evita a briga com o
 * `react-hooks/exhaustive-deps` (a função é recriada a cada render) e lê naturalmente —
 * `useAsync(`product:${category}/${slug}`, ...)`.
 */
export function useAsync<T>(
  key: string,
  fn: (signal: AbortSignal) => Promise<T>
): AsyncState<T> & { reload: () => void } {
  const [nonce, setNonce] = useState(0);
  const [settled, setSettled] = useState<Settled<T>>({ requestId: '', data: null, error: null });

  const requestId = `${key}#${nonce}`;

  // A função muda de identidade a cada render; guardá-la numa ref mantém o efeito preso
  // apenas à chave, que é o que de fato identifica a requisição.
  const fnRef = useRef(fn);

  // Atualizada num efeito, e não durante o render: escrever numa ref enquanto o React
  // renderiza é inseguro com renderização concorrente, onde um render pode ser descartado.
  useEffect(() => {
    fnRef.current = fn;
  });

  useEffect(() => {
    // O StrictMode do React 19 monta, desmonta e remonta em desenvolvimento. Sem abortar e
    // sem a trava `active`, isso gera duas requisições e um setState depois do unmount.
    const controller = new AbortController();
    let active = true;

    fnRef
      .current(controller.signal)
      .then(data => {
        if (active) setSettled({ requestId, data, error: null });
      })
      .catch(error => {
        if (!active || controller.signal.aborted) return;
        setSettled({ requestId, data: null, error });
      });

    return () => {
      active = false;
      controller.abort();
    };
  }, [requestId]);

  const reload = useCallback(() => setNonce(n => n + 1), []);

  // "Carregando" é derivado de o resultado guardado pertencer ou não à requisição atual.
  // Resetar o estado dentro do efeito produziria um render intermediário desnecessário e
  // um `setState` em efeito — que é justamente o que o React desencoraja.
  const isCurrent = settled.requestId === requestId;

  return {
    data: isCurrent ? settled.data : null,
    loading: !isCurrent,
    error: isCurrent ? settled.error : null,
    reload,
  };
}
