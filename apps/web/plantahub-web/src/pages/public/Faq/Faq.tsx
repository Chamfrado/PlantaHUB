import { ChevronDown, HelpCircle, Search } from 'lucide-react';
import { useMemo, useState } from 'react';

type FaqItem = {
  question: string;
  answer: string;
  category: string;
};

const FAQ_ITEMS: FaqItem[] = [
  {
    category: 'Compras',
    question: 'Como funciona a compra de uma planta?',
    answer:
      'Você escolhe o produto, seleciona os tipos de planta desejados e conclui o pedido. Após o pagamento, os arquivos ficam disponíveis na sua biblioteca para download.',
  },
  {
    category: 'Compras',
    question: 'Posso comprar apenas algumas disciplinas do projeto?',
    answer:
      'Sim. A Plataforma permite adquirir somente os plan types desejados, como arquitetônica, hidráulica, elétrica, estrutural ou paisagística, conforme disponibilidade do produto.',
  },
  {
    category: 'Biblioteca',
    question: 'Onde encontro meus arquivos após a compra?',
    answer:
      'Todos os itens adquiridos ficam disponíveis na sua biblioteca. Lá você pode acessar cada tipo de planta individualmente, visualizar os arquivos liberados e iniciar os downloads.',
  },
  {
    category: 'Downloads',
    question: 'Posso baixar vários arquivos de uma vez?',
    answer:
      'Sim. A biblioteca permite selecionar várias plantas adquiridas e gerar um bundle ZIP com os arquivos correspondentes para facilitar o download em lote.',
  },
  {
    category: 'Conta',
    question: 'Preciso completar meu perfil para comprar?',
    answer:
      'Sim. Algumas informações obrigatórias de perfil são exigidas antes de finalizar a compra, para garantir segurança, rastreabilidade e consistência no fluxo da plataforma.',
  },
  {
    category: 'Parcerias',
    question: 'Como posso vender minhas plantas na PlantaHUB?',
    answer:
      'Você pode acessar a página Trabalhe Conosco e enviar seu portfólio para análise. Nossa equipe avalia os materiais e negocia a margem de venda conforme o modelo de parceria.',
  },
  {
    category: 'Suporte',
    question: 'O que faço se um arquivo não estiver disponível?',
    answer:
      'Caso um item adquirido não esteja disponível para download, entre em contato com o suporte da plataforma para verificação e regularização do material.',
  },
];

export default function FaqPage() {
  const [search, setSearch] = useState('');
  const [openIndex, setOpenIndex] = useState<number | null>(0);

  const filteredItems = useMemo(() => {
    const normalized = search.trim().toLowerCase();

    if (!normalized) return FAQ_ITEMS;

    return FAQ_ITEMS.filter(item =>
      [item.question, item.answer, item.category].some(field =>
        field.toLowerCase().includes(normalized)
      )
    );
  }, [search]);

  return (
    <section className="bg-white">
      <div className="border-b border-neutral-200 bg-neutral-50">
        <div className="mx-auto max-w-7xl px-6 py-16 md:py-20">
          <div className="max-w-3xl">
            <span className="inline-flex rounded-full bg-orange-50 px-4 py-2 text-sm font-semibold text-primary-600">
              FAQ
            </span>

            <h1 className="mt-5 text-4xl font-extrabold tracking-tight text-neutral-900 md:text-5xl">
              Perguntas frequentes sobre compras, biblioteca, downloads e parcerias
            </h1>

            <p className="mt-5 text-lg leading-relaxed text-neutral-600">
              Reunimos aqui as dúvidas mais comuns para ajudar clientes e parceiros a entender
              melhor como a PlantaHUB funciona.
            </p>

            <div className="mt-8 max-w-xl">
              <div className="flex items-center gap-3 rounded-2xl border border-neutral-200 bg-white px-4 py-3 shadow-sm">
                <Search className="h-5 w-5 text-neutral-400" />
                <input
                  value={search}
                  onChange={event => setSearch(event.target.value)}
                  placeholder="Buscar pergunta ou assunto"
                  className="w-full bg-transparent text-sm text-neutral-800 outline-none placeholder:text-neutral-400"
                />
              </div>
            </div>
          </div>
        </div>
      </div>

      <div className="mx-auto max-w-5xl px-6 py-16">
        {!filteredItems.length ? (
          <div className="rounded-3xl border border-neutral-200 bg-neutral-50 p-10 text-center">
            <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-2xl bg-white text-primary-600 shadow-sm">
              <HelpCircle className="h-6 w-6" />
            </div>

            <h2 className="mt-4 text-2xl font-extrabold text-neutral-900">
              Nenhum resultado encontrado
            </h2>
            <p className="mt-2 text-neutral-600">
              Tente usar outras palavras-chave ou navegue pelas perguntas disponíveis.
            </p>
          </div>
        ) : (
          <div className="space-y-4">
            {filteredItems.map((item, index) => {
              const open = openIndex === index;

              return (
                <div
                  key={`${item.category}-${item.question}`}
                  className="overflow-hidden rounded-3xl border border-neutral-200 bg-white shadow-sm"
                >
                  <button
                    type="button"
                    onClick={() => setOpenIndex(prev => (prev === index ? null : index))}
                    className="flex w-full items-center justify-between gap-4 px-6 py-5 text-left"
                  >
                    <div>
                      <div className="text-xs font-semibold uppercase tracking-wide text-primary-600">
                        {item.category}
                      </div>
                      <div className="mt-1 text-lg font-extrabold text-neutral-900">
                        {item.question}
                      </div>
                    </div>

                    <ChevronDown
                      className={[
                        'h-5 w-5 shrink-0 text-neutral-500 transition',
                        open ? 'rotate-180' : 'rotate-0',
                      ].join(' ')}
                    />
                  </button>

                  {open ? (
                    <div className="border-t border-neutral-100 px-6 py-5 text-sm leading-relaxed text-neutral-600">
                      {item.answer}
                    </div>
                  ) : null}
                </div>
              );
            })}
          </div>
        )}
      </div>
    </section>
  );
}
