import 'react';

/**
 * `webkitdirectory` é o único jeito de deixar o navegador selecionar uma pasta inteira,
 * mas não está nos tipos padrão do React. Declará-lo aqui evita espalhar `any` pelo
 * componente de upload só para contornar a tipagem.
 */
declare module 'react' {
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  interface InputHTMLAttributes<T> {
    webkitdirectory?: string;
    directory?: string;
  }
}
