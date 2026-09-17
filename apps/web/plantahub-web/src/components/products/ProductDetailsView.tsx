import { Check, ChevronDown, Headset, ShieldCheck, Sparkles } from 'lucide-react';
import { useState } from 'react';
import { Link } from 'react-router-dom';
import type { PlanTypeOptionDTO } from '../../types/api/product';
import type { ProductDetailView } from '../../types/product-view';
import ProductHero from './ProductHero';
import ProductPlanSelector from './ProductPlanSelector';

type Props = {
  product: ProductDetailView;

  planTypes: PlanTypeOptionDTO[];
  selectedCodes: string[];
  ownedCodes: string[];
  loadingOwnedCodes: boolean;
  loadingPlanTypes: boolean;
  submitting: 'buy' | 'cart' | null;
  error: string | null;

  onToggle: (code: string) => void;
  onBuyNow: () => void;
  onAddToCart: () => void;

  /** Na pré-visualização do painel, o bloco de compra é exibido mas não age. */
  readOnly?: boolean;
};

/**
 * Todo o corpo visual da página de produto, sem nenhuma busca de dados.
 *
 * É esta separação que torna a pré-visualização do painel honesta: o admin vê exatamente
 * este componente, com os mesmos DTOs da rota pública. Uma tela de preview própria poderia
 * divergir da página real sem ninguém perceber.
 *
 * **Toda seção some quando não tem conteúdo.** O admin cria produtos incompletos o tempo
 * todo — um rascunho com só nome e slug precisa renderizar sem cabeçalhos órfãos.
 */
export default function ProductDetailsView({
  product,
  planTypes,
  selectedCodes,
  ownedCodes,
  loadingOwnedCodes,
  loadingPlanTypes,
  submitting,
  error,
  onToggle,
  onBuyNow,
  onAddToCart,
  readOnly = false,
}: Props) {
  const [openFaq, setOpenFaq] = useState<number | null>(0);

  const showKeyFacts = product.keyFacts.items.length > 0 || product.fileFormats.length > 0;

  return (
    <div className="bg-white">
      <ProductHero product={product} />

      <ProductPlanSelector
        productId={product.id}
        planTypes={planTypes}
        selectedCodes={selectedCodes}
        ownedCodes={ownedCodes}
        loadingOwnedCodes={loadingOwnedCodes}
        onToggle={readOnly ? () => {} : onToggle}
        onBuyNow={readOnly ? () => {} : onBuyNow}
        onAddToCart={readOnly ? () => {} : onAddToCart}
        loading={loadingPlanTypes}
        submitting={submitting}
        error={error}
      />

      {product.whyChoose.items.length > 0 ? (
        <Section title={product.whyChoose.title ?? 'Por que escolher este projeto?'}
                 subtitle={product.whyChoose.intro ?? undefined}>
          {/* Sem corte em 3: um quarto diferencial cadastrado pelo admin sumiria em
              silêncio. A grade quebra a linha sozinha. */}
          <div className="grid gap-6 md:grid-cols-3">
            {product.whyChoose.items.map((f, idx) => (
              <div key={`${f.title}-${idx}`}
                   className="rounded-2xl border border-neutral-200 bg-white p-6 shadow-sm">
                <div className="flex h-10 w-10 items-center justify-center rounded-xl border border-orange-100 bg-orange-50 font-bold text-primary-600">
                  <Sparkles className="h-5 w-5" />
                </div>
                <h3 className="mt-4 font-extrabold text-brand-black">{f.title}</h3>
                {f.description ? (
                  <p className="mt-2 text-sm leading-relaxed text-brand-muted">{f.description}</p>
                ) : null}
              </div>
            ))}
          </div>
        </Section>
      ) : null}

      {product.includes.items.length > 0 ? (
        <Section title={product.includes.title ?? 'O que está incluso na sua compra'}
                 subtitle={product.includes.intro ?? undefined}>
          <div className="grid gap-6 md:grid-cols-3">
            {product.includes.items.map((it, idx) => (
              <div key={`${it.title}-${idx}`}
                   className="rounded-2xl border border-neutral-200 bg-white p-5 shadow-sm">
                <div className="flex items-start gap-3">
                  <div className="flex h-9 w-9 items-center justify-center rounded-xl border border-orange-100 bg-orange-50">
                    <Check className="h-4 w-4 text-primary-600" />
                  </div>
                  <div className="min-w-0">
                    <div className="font-extrabold text-brand-black">{it.title}</div>
                    {it.description ? (
                      <p className="mt-1 text-sm leading-relaxed text-brand-muted">{it.description}</p>
                    ) : null}
                  </div>
                </div>
              </div>
            ))}
          </div>
        </Section>
      ) : null}

      {showKeyFacts ? (
        <Section title={product.keyFacts.title ?? 'Fatos sobre este projeto'}
                 subtitle={product.keyFacts.intro ?? undefined}>
          {product.keyFacts.items.length > 0 ? (
            <div className="grid gap-6 md:grid-cols-2">
              {product.keyFacts.items.map((k, idx) => (
                <div key={`${k.label}-${idx}`}
                     className="rounded-2xl border border-orange-100 bg-orange-50 p-10 text-center">
                  <div className="text-5xl font-extrabold text-primary-600">{k.value}</div>
                  <div className="mt-2 font-bold text-brand-black">{k.label}</div>
                </div>
              ))}
            </div>
          ) : null}

          {/* Só dados do produto. "Suporte 24/7" e "Atualizações vitalícias" estavam aqui
              como se fossem características do projeto — são promessas do site, e nenhuma
              delas vinha de lugar nenhum. */}
          {product.fileFormats.length > 0 || product.customizable ? (
            <div className="mt-6 rounded-2xl border border-neutral-200 bg-white p-6">
              <div className="grid gap-4 text-center md:grid-cols-2">
                {product.fileFormats.length > 0 ? (
                  <MiniStat label="Formatos de arquivo" value={product.fileFormats.join(' · ')} />
                ) : null}
                {product.customizable ? (
                  <MiniStat label="Customização" value="Disponível" />
                ) : null}
              </div>
            </div>
          ) : null}
        </Section>
      ) : null}

      {product.testimonials.items.length > 0 ? (
        <Section title={product.testimonials.title ?? 'O que dizem nossos clientes'}
                 subtitle={product.testimonials.intro ?? undefined}>
          <div className="grid gap-6 md:grid-cols-3">
            {product.testimonials.items.map((t, idx) => (
              <div key={`${t.authorName}-${idx}`}
                   className="rounded-2xl border border-neutral-200 bg-white p-6 shadow-sm">
                {/* As cinco estrelas fixas saíram: não existe nota nenhuma por trás delas. */}
                <p className="text-sm leading-relaxed text-brand-muted">{t.quote}</p>

                <div className="mt-4 flex items-center gap-3">
                  {t.avatarUrl ? (
                    <img src={t.avatarUrl} alt={t.authorName}
                         className="h-9 w-9 rounded-full object-cover" loading="lazy" />
                  ) : (
                    <div className="h-9 w-9 rounded-full bg-neutral-200" />
                  )}
                  <div>
                    <div className="text-sm font-extrabold text-brand-black">{t.authorName}</div>
                    {t.authorTitle ? (
                      <div className="text-xs text-brand-muted">{t.authorTitle}</div>
                    ) : null}
                  </div>
                </div>
              </div>
            ))}
          </div>
        </Section>
      ) : null}

      {product.faq.items.length > 0 ? (
        <Section title={product.faq.title ?? 'Perguntas frequentes'}
                 subtitle={product.faq.intro ?? undefined}>
          <div className="mx-auto max-w-3xl">
            <div className="space-y-3">
              {product.faq.items.map((f, idx) => {
                const open = openFaq === idx;

                return (
                  <div key={`${f.question}-${idx}`} className="rounded-2xl border border-neutral-200">
                    <button
                      onClick={() => setOpenFaq(prev => (prev === idx ? null : idx))}
                      className="flex w-full items-center justify-between gap-4 px-5 py-4 text-left"
                    >
                      <span className="font-bold text-brand-black">{f.question}</span>
                      <ChevronDown className={[
                        'h-5 w-5 text-brand-muted transition',
                        open ? 'rotate-180' : 'rotate-0',
                      ].join(' ')} />
                    </button>

                    {open && f.answer ? (
                      <div className="px-5 pb-5 text-sm leading-relaxed text-brand-muted">
                        {f.answer}
                      </div>
                    ) : null}
                  </div>
                );
              })}
            </div>
          </div>
        </Section>
      ) : null}

      {/* O CTA final renderiza sempre: é o ponto de conversão da página. */}
      <section className="bg-white">
        <div className="mx-auto max-w-6xl px-6 pb-20">
          <div className="rounded-3xl border border-neutral-200 bg-brand-light p-10 text-center">
            <h2 className="text-3xl font-extrabold text-brand-black">
              {product.finalCta.title ?? 'Pronto para construir a casa dos seus sonhos?'}
            </h2>
            {product.finalCta.subtitle ? (
              <p className="mt-2 text-brand-muted">{product.finalCta.subtitle}</p>
            ) : null}

            <div className="mt-6 flex flex-wrap justify-center gap-3">
              <button
                type="button"
                onClick={() =>
                  document.getElementById('purchase-options')?.scrollIntoView({
                    behavior: 'smooth',
                    block: 'start',
                  })
                }
                className="rounded-xl bg-primary-500 px-6 py-3 font-semibold text-white transition hover:bg-primary-600"
              >
                Comprar agora
              </button>

              {/* Era um botão sem onClick: clicar não fazia nada. */}
              <Link
                to="/contato"
                className="rounded-xl border border-neutral-300 bg-white px-6 py-3 font-semibold text-brand-black transition hover:bg-neutral-100"
              >
                Falar com o suporte
              </Link>
            </div>

            <div className="mt-6 flex flex-wrap justify-center gap-6 text-xs font-semibold text-brand-muted">
              <span className="inline-flex items-center gap-2">
                <ShieldCheck className="h-4 w-4 text-primary-500" /> Pagamento seguro
              </span>
              <span className="inline-flex items-center gap-2">
                <Check className="h-4 w-4 text-primary-500" /> Download imediato
              </span>
              <span className="inline-flex items-center gap-2">
                <Headset className="h-4 w-4 text-primary-500" /> Suporte especializado
              </span>
            </div>
          </div>
        </div>
      </section>
    </div>
  );
}

function Section({
  title,
  subtitle,
  children,
}: {
  title: string;
  subtitle?: string;
  children: React.ReactNode;
}) {
  return (
    <section className="bg-white">
      <div className="mx-auto max-w-6xl px-6 py-16">
        <h2 className="text-center text-2xl font-extrabold text-brand-black md:text-3xl">
          {title}
        </h2>
        {subtitle ? (
          <p className="mx-auto mt-2 max-w-3xl text-center text-brand-muted">{subtitle}</p>
        ) : null}
        <div className="mt-10">{children}</div>
      </div>
    </section>
  );
}

function MiniStat({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <div className="text-lg font-extrabold text-brand-black">{value}</div>
      <div className="mt-1 text-xs font-semibold text-brand-muted">{label}</div>
    </div>
  );
}
