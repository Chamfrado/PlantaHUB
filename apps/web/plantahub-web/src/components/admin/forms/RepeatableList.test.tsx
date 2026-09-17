import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { useState } from 'react';
import { describe, expect, it } from 'vitest';

import RepeatableList from './RepeatableList';
import { withUid, type WithUid } from './uid';

type Item = { title: string };

function Harness({ initial }: { initial: Item[] }) {
  const [items, setItems] = useState<WithUid<Item>[]>(withUid(initial));

  return (
    <RepeatableList
      label="Itens"
      value={items}
      onChange={setItems}
      newItem={() => ({ title: '' })}
      itemTitle={item => item.title}
      renderItem={(item, patch) => (
        <input
          aria-label={`titulo-${item.title || 'vazio'}`}
          value={item.title}
          onChange={e => patch({ title: e.target.value })}
        />
      )}
    />
  );
}

describe('RepeatableList', () => {
  it('mostra o efeito de deixar a lista vazia', () => {
    render(<Harness initial={[]} />);

    expect(
      screen.getByText('Sem itens — esta seção não aparecerá na página pública.')
    ).toBeInTheDocument();
  });

  it('adiciona e remove itens', async () => {
    const user = userEvent.setup();
    render(<Harness initial={[]} />);

    await user.click(screen.getByRole('button', { name: /Adicionar/i }));
    expect(screen.getByLabelText('titulo-vazio')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Remover' }));
    expect(screen.queryByLabelText('titulo-vazio')).not.toBeInTheDocument();
  });

  it('reordenar preserva o conteúdo de cada campo', async () => {
    const user = userEvent.setup();
    render(<Harness initial={[{ title: 'primeiro' }, { title: 'segundo' }]} />);

    const inputs = () => screen.getAllByRole('textbox') as HTMLInputElement[];

    expect(inputs().map(i => i.value)).toEqual(['primeiro', 'segundo']);

    await user.click(screen.getAllByRole('button', { name: 'Mover para baixo' })[0]);

    // É aqui que o `_uid` se paga: com `key` por índice, o React reaproveitaria o nó
    // errado e os valores ficariam trocados em relação à ordem real.
    expect(inputs().map(i => i.value)).toEqual(['segundo', 'primeiro']);
  });

  it('editar depois de reordenar altera o item certo', async () => {
    const user = userEvent.setup();
    render(<Harness initial={[{ title: 'a' }, { title: 'b' }]} />);

    await user.click(screen.getAllByRole('button', { name: 'Mover para baixo' })[0]);

    const inputs = () => screen.getAllByRole('textbox') as HTMLInputElement[];

    await user.type(inputs()[0], 'x');

    expect(inputs().map(i => i.value)).toEqual(['bx', 'a']);
  });
});
