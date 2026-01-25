// src/pages/ProductPage.tsx
import { Check, ChevronDown, Headset, ShieldCheck, Sparkles, Star } from 'lucide-react';
import { useMemo, useState } from 'react';
import { useParams } from 'react-router-dom';
import ProductHero from '../../../components/products/ProductHero';
import { getProductByRoute } from '../../../data/productSelector';

export default function ProductDetails() {
  const { category = '', slug = '' } = useParams();

  const product = useMemo(() => getProductByRoute(category, slug), [category, slug]);
  const [openFaq, setOpenFaq] = useState<number | null>(0);

  if (!product) {
    return (
      <div className="min-h-[60vh] bg-white">
        <div className="max-w-6xl mx-auto px-6 py-16">
          <h1 className="text-2xl font-extrabold text-neutral-900">Produto não encontrado</h1>
          <p className="mt-2 text-neutral-600">
            Verifique a URL. Ex.: <span className="font-mono">/casas/confort</span>
          </p>
        </div>
      </div>
    );
  }

  return (
    <div className="bg-white">
      {/* HERO */}
      <ProductHero product={product} />

      {/* WHY CHOOSE */}
      <Section
        title={product.page.whyChooseTitle ?? 'Why choose this product?'}
        subtitle={
          product.page.whyChooseIntro ??
          'Benefits designed to maximize practical value with professional-quality documentation.'
        }
      >
        <div className="grid gap-6 md:grid-cols-3">
          {(product.page.whyChooseFeatures ?? []).slice(0, 3).map((f, idx) => (
            <div
              key={`${f.title}-${idx}`}
              className="rounded-2xl border border-neutral-200 bg-white p-6 shadow-sm"
            >
              <div className="h-10 w-10 rounded-xl bg-orange-50 border border-orange-100 flex items-center justify-center text-primary-600 font-bold">
                <Sparkles className="h-5 w-5" />
              </div>
              <h3 className="mt-4 font-extrabold text-neutral-900">{f.title}</h3>
              {f.description ? (
                <p className="mt-2 text-sm text-neutral-600 leading-relaxed">{f.description}</p>
              ) : null}
            </div>
          ))}
        </div>
      </Section>

      {/* INCLUDED */}
      <Section
        title={product.page.includesTitle ?? "What's included in your purchase?"}
        subtitle={
          product.page.includesIntro ??
          'A complete professional package with the technical documentation required.'
        }
      >
        <div className="grid gap-6 md:grid-cols-3">
          {(product.page.includedItems ?? []).map((it, idx) => (
            <div
              key={`${it.title}-${idx}`}
              className="rounded-2xl border border-neutral-200 bg-white p-5 shadow-sm"
            >
              <div className="flex items-start gap-3">
                <div className="h-9 w-9 rounded-xl bg-orange-50 border border-orange-100 flex items-center justify-center">
                  <Check className="h-4 w-4 text-primary-600" />
                </div>
                <div className="min-w-0">
                  <div className="font-extrabold text-neutral-900">{it.title}</div>
                  {it.description ? (
                    <p className="mt-1 text-sm text-neutral-600 leading-relaxed">
                      {it.description}
                    </p>
                  ) : null}
                </div>
              </div>
            </div>
          ))}
        </div>
      </Section>

      {/* KEY FACTS */}
      <Section
        title={product.page.keyFactsTitle ?? 'Key facts'}
        subtitle={product.page.keyFactsIntro ?? 'Numbers that showcase the comprehensive value.'}
      >
        <div className="grid gap-6 md:grid-cols-2">
          {(product.page.keyFacts ?? []).slice(0, 2).map((k, idx) => (
            <div
              key={`${k.label}-${idx}`}
              className="rounded-2xl border border-orange-100 bg-orange-50 p-10 text-center"
            >
              <div className="text-5xl font-extrabold text-primary-600">{k.value}</div>
              <div className="mt-2 font-bold text-neutral-900">{k.label}</div>
            </div>
          ))}
        </div>

        <div className="mt-6 rounded-2xl border border-neutral-200 bg-white p-6">
          <div className="grid gap-4 md:grid-cols-4 text-center">
            <MiniStat label="File formats" value={`${product.fileFormats?.length ?? 0}`} />
            <MiniStat label="Customizable" value={product.customizable ? '100%' : '—'} />
            <MiniStat label="Support" value="24/7" />
            <MiniStat label="Updates" value="Lifetime" />
          </div>
        </div>
      </Section>

      {/* TESTIMONIALS */}
      <Section
        title={product.page.testimonialsTitle ?? 'Hear from our happy clients'}
        subtitle={product.page.testimonialsIntro ?? 'What people say about our plans.'}
      >
        <div className="grid gap-6 md:grid-cols-3">
          {(product.page.testimonials ?? []).slice(0, 3).map((t, idx) => (
            <div
              key={`${t.authorName}-${idx}`}
              className="rounded-2xl border border-neutral-200 bg-white p-6 shadow-sm"
            >
              <div className="flex gap-1 text-primary-500">
                <Star className="h-4 w-4 fill-current" />
                <Star className="h-4 w-4 fill-current" />
                <Star className="h-4 w-4 fill-current" />
                <Star className="h-4 w-4 fill-current" />
                <Star className="h-4 w-4 fill-current" />
              </div>

              <p className="mt-3 text-sm text-neutral-700 leading-relaxed">{t.quote}</p>

              <div className="mt-4 flex items-center gap-3">
                <div className="h-9 w-9 rounded-full bg-neutral-200" />
                <div>
                  <div className="text-sm font-extrabold text-neutral-900">{t.authorName}</div>
                  {t.authorTitle ? (
                    <div className="text-xs text-neutral-500">{t.authorTitle}</div>
                  ) : null}
                </div>
              </div>
            </div>
          ))}
        </div>
      </Section>

      {/* FAQ */}
      <Section
        title={product.page.faqTitle ?? 'Frequently Asked Questions'}
        subtitle={product.page.faqIntro ?? 'Everything you need to know before buying.'}
      >
        <div className="max-w-3xl mx-auto">
          <div className="space-y-3">
            {(product.page.faq ?? []).map((f, idx) => {
              const open = openFaq === idx;
              return (
                <div key={`${f.question}-${idx}`} className="rounded-2xl border border-neutral-200">
                  <button
                    onClick={() => setOpenFaq(prev => (prev === idx ? null : idx))}
                    className="w-full flex items-center justify-between gap-4 px-5 py-4 text-left"
                  >
                    <span className="font-bold text-neutral-900">{f.question}</span>
                    <ChevronDown
                      className={[
                        'h-5 w-5 text-neutral-500 transition',
                        open ? 'rotate-180' : 'rotate-0',
                      ].join(' ')}
                    />
                  </button>

                  {open ? (
                    <div className="px-5 pb-5 text-sm text-neutral-600 leading-relaxed">
                      {f.answer ??
                        'Add the answer text later (your model allows optional answers).'}
                    </div>
                  ) : null}
                </div>
              );
            })}
          </div>
        </div>
      </Section>

      {/* FINAL CTA */}
      <section className="bg-white">
        <div className="max-w-6xl mx-auto px-6 pb-20">
          <div className="rounded-3xl border border-neutral-200 bg-neutral-50 p-10 text-center">
            <h2 className="text-3xl font-extrabold text-neutral-900">
              {product.page.finalCtaTitle ?? 'Ready to build the house of your dreams?'}
            </h2>
            {product.page.finalCtaSubtitle ? (
              <p className="mt-2 text-neutral-600">{product.page.finalCtaSubtitle}</p>
            ) : null}

            <div className="mt-6 flex flex-wrap justify-center gap-3">
              <button className="rounded-xl bg-primary-500 px-6 py-3 text-white font-semibold hover:bg-primary-600 transition">
                Download Now{' '}
                {product.price
                  ? `— ${formatMoney(product.price.amount, product.price.currency)}`
                  : ''}
              </button>
              <button className="rounded-xl border border-neutral-300 bg-white px-6 py-3 font-semibold text-neutral-900 hover:bg-neutral-100 transition">
                Contact Support
              </button>
            </div>

            <div className="mt-6 flex flex-wrap justify-center gap-6 text-xs font-semibold text-neutral-600">
              <span className="inline-flex items-center gap-2">
                <ShieldCheck className="h-4 w-4 text-primary-500" /> Secure SSL Payment
              </span>
              <span className="inline-flex items-center gap-2">
                <Check className="h-4 w-4 text-primary-500" /> Money-Back Guarantee
              </span>
              <span className="inline-flex items-center gap-2">
                <Headset className="h-4 w-4 text-primary-500" /> Professional Support Team
              </span>
            </div>
          </div>
        </div>
      </section>
    </div>
  );
}

/* ---------- small components ---------- */

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
      <div className="max-w-6xl mx-auto px-6 py-16">
        <h2 className="text-2xl md:text-3xl font-extrabold text-neutral-900 text-center">
          {title}
        </h2>
        {subtitle ? (
          <p className="mt-2 text-neutral-600 text-center max-w-3xl mx-auto">{subtitle}</p>
        ) : null}

        <div className="mt-10">{children}</div>
      </div>
    </section>
  );
}

function MiniStat({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <div className="text-lg font-extrabold text-neutral-900">{value}</div>
      <div className="mt-1 text-xs font-semibold text-neutral-500">{label}</div>
    </div>
  );
}

function formatMoney(value: number, currency: 'BRL' | 'USD' | 'EUR') {
  const locale = currency === 'BRL' ? 'pt-BR' : 'en-US';
  return value.toLocaleString(locale, {
    style: 'currency',
    currency,
    maximumFractionDigits: 0,
  });
}
