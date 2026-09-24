import { ChevronDown, HelpCircle, Search } from 'lucide-react';
import { useMemo, useState } from 'react';
import { useSitePage } from '../../../hooks/useSitePage';

export default function FaqPage() {
  const { content } = useSitePage('faq');
  const FAQ_ITEMS = content.faq;

  const [search, setSearch] = useState('');
  const [openIndex, setOpenIndex] = useState<number | null>(0);

  const filteredItems = useMemo(() => {
    const normalized = search.trim().toLowerCase();

    if (!normalized) return FAQ_ITEMS;

    return FAQ_ITEMS.filter(item =>
      [item.question, item.answer, item.category].some(field =>
        (field ?? '').toLowerCase().includes(normalized)
      )
    );
    // FAQ_ITEMS entra na lista: sem ele, a busca continuaria filtrando a lista vazia de
    // antes do conteudo chegar.
  }, [search, FAQ_ITEMS]);

  return (
    <section className="bg-white">
      <div className="border-b border-neutral-200 bg-brand-light">
        <div className="mx-auto max-w-7xl px-6 py-16 md:py-20">
          <div className="max-w-3xl">
            <span className="inline-flex rounded-full bg-white px-4 py-2 text-sm font-semibold text-primary-600 border border-orange-100">
              FAQ
            </span>

            <h1 className="mt-5 text-4xl font-extrabold tracking-tight text-brand-black md:text-5xl">{content.headline}</h1>

            <p className="mt-5 text-lg leading-relaxed text-brand-muted">{content.intro}</p>

            <div className="mt-8 max-w-xl">
              <div className="flex items-center gap-3 rounded-2xl border border-neutral-200 bg-white px-4 py-3 shadow-sm">
                <Search className="h-5 w-5 text-neutral-400" />
                <input
                  value={search}
                  onChange={event => setSearch(event.target.value)}
                  placeholder="Buscar pergunta ou assunto"
                  className="w-full bg-transparent text-sm text-brand-black outline-none placeholder:text-neutral-400"
                />
              </div>
            </div>
          </div>
        </div>
      </div>

      <div className="mx-auto max-w-5xl px-6 py-16">
        {!filteredItems.length ? (
          <div className="rounded-3xl border border-neutral-200 bg-brand-light p-10 text-center">
            <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-2xl bg-white text-primary-600 shadow-sm">
              <HelpCircle className="h-6 w-6" />
            </div>

            <h2 className="mt-4 text-2xl font-extrabold text-brand-black">
              Nenhum resultado encontrado
            </h2>
            <p className="mt-2 text-brand-muted">
              Tente usar outras palavras-chave ou navegue pelas perguntas disponíveis.
            </p>
          </div>
        ) : (
          <div className="space-y-4">
            {filteredItems.map((item, index) => {
              const open = openIndex === index;

              return (
                <div
                  key={item.question}
                  className="overflow-hidden rounded-3xl border border-neutral-200 bg-white shadow-sm transition duration-300 hover:-translate-y-0.5 hover:border-orange-200 hover:shadow-md"
                >
                  <button
                    type="button"
                    onClick={() => setOpenIndex(prev => (prev === index ? null : index))}
                    className="flex w-full items-center justify-between gap-4 px-6 py-5 text-left transition hover:bg-brand-light"
                  >
                    <div>
                      {item.category ? (
                        <div className="text-xs font-semibold uppercase tracking-wide text-primary-600">
                          {item.category}
                        </div>
                      ) : null}
                      <div className="mt-1 text-lg font-extrabold text-brand-black">
                        {item.question}
                      </div>
                    </div>

                    <ChevronDown
                      className={[
                        'h-5 w-5 shrink-0 text-brand-muted transition duration-300',
                        open ? 'rotate-180' : 'rotate-0',
                      ].join(' ')}
                    />
                  </button>

                  <div className="accordion-panel" data-open={open} aria-hidden={!open}>
                    <div>
                      <div className="border-t border-neutral-100 px-6 py-5 text-sm leading-relaxed text-brand-muted">
                        {item.answer}
                      </div>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </section>
  );
}
