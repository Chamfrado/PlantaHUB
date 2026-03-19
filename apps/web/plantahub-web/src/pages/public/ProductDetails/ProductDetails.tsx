import { Check, ChevronDown, Headset, ShieldCheck, Sparkles, Star } from 'lucide-react';
import { useEffect, useMemo, useState } from 'react';
import { useLocation, useNavigate, useParams } from 'react-router-dom';
import { useCart } from '../../../app/providers/useCart';
import ProductHero from '../../../components/products/ProductHero';
import ProductPlanSelector from '../../../components/products/ProductPlanSelector';
import { useToast } from '../../../components/ui/use-toast';
import { getProductByRoute } from '../../../data/productSelector';
import { getApiErrorMessage } from '../../../lib/api-error';
import { addCartItem } from '../../../services/cart.service';
import { createOrder } from '../../../services/order.service';
import { getProductPlanTypes } from '../../../services/products.service';
import { getMyProfileStatus } from '../../../services/profile.service';
import type { PlanTypeOptionDTO } from '../../../types/api/product';

export default function ProductDetails() {
  const { category = '', slug = '' } = useParams();
  const navigate = useNavigate();

  const product = useMemo(() => getProductByRoute(category, slug), [category, slug]);

  const [openFaq, setOpenFaq] = useState<number | null>(0);
  const [planTypes, setPlanTypes] = useState<PlanTypeOptionDTO[]>([]);
  const [selectedCodes, setSelectedCodes] = useState<string[]>([]);
  const [loadingPlanTypes, setLoadingPlanTypes] = useState(true);
  const [submitting, setSubmitting] = useState<'buy' | 'cart' | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);

  const { refreshCart } = useCart();

  const location = useLocation();
  const { showToast } = useToast();
  const currentPath = location.pathname + location.search;

  useEffect(() => {
    let active = true;

    async function loadPlanTypes() {
      try {
        setLoadingPlanTypes(true);
        setActionError(null);

        const response = await getProductPlanTypes(category, slug);

        if (!active) return;
        setPlanTypes(response);
      } catch (error) {
        console.error(error);
        if (!active) return;
        setActionError('Não foi possível carregar os tipos de planta deste produto.');
      } finally {
        if (active) {
          setLoadingPlanTypes(false);
        }
      }
    }

    if (category && slug) {
      void loadPlanTypes();
    }

    return () => {
      active = false;
    };
  }, [category, slug]);

  function togglePlanType(code: string) {
    setSelectedCodes(prev =>
      prev.includes(code) ? prev.filter(item => item !== code) : [...prev, code]
    );
  }

  async function validatePurchaseFlow() {
    const token = localStorage.getItem('token');

    if (!token) {
      navigate(`/login?redirect=${encodeURIComponent(currentPath)}`);
      return false;
    }

    try {
      const profileStatus = await getMyProfileStatus();

      if (!profileStatus.profileCompleted) {
        navigate(`/configs?redirect=${encodeURIComponent(currentPath)}`);
        showToast({
          variant: 'info',
          title: 'Complete seu perfil para continuar',
          description: 'Preencha os dados obrigatórios antes de finalizar sua compra.',
        });
        return false;
      }

      return true;
    } catch (error) {
      console.error(error);

      const message = getApiErrorMessage(error, 'Não foi possível validar seu perfil no momento.');

      setActionError(message);
      showToast({
        variant: 'error',
        title: 'Falha ao validar perfil',
        description: message,
      });

      return false;
    }
  }

  async function handleAddToCart() {
    if (!product) return;

    if (selectedCodes.length === 0) {
      setActionError('Selecione pelo menos um tipo de planta.');
      return;
    }

    const valid = await validatePurchaseFlow();
    if (!valid) return;

    try {
      setSubmitting('cart');
      setActionError(null);

      await addCartItem({
        productId: product.id,
        planTypeCodes: selectedCodes,
      });

      await refreshCart();

      showToast({
        variant: 'success',
        title: 'Produto adicionado ao carrinho',
        description: 'Você já pode revisar os itens e finalizar a compra.',
      });

      navigate('/carrinho');
    } catch (error) {
      const message = getApiErrorMessage(
        error,
        'Não foi possível adicionar o produto ao carrinho.'
      );

      setActionError(message);
      showToast({
        variant: 'error',
        title: 'Erro ao adicionar ao carrinho',
        description: message,
      });
    } finally {
      setSubmitting(null);
    }
  }

  async function handleBuyNow() {
    if (!product) return;

    if (selectedCodes.length === 0) {
      setActionError('Selecione pelo menos um tipo de planta.');
      return;
    }

    const valid = await validatePurchaseFlow();
    if (!valid) return;

    try {
      setSubmitting('buy');
      setActionError(null);

      const order = await createOrder({
        items: [
          {
            productId: product.id,
            quantity: 1,
            planTypeCodes: selectedCodes,
          },
        ],
      });

      showToast({
        variant: 'success',
        title: 'Pedido criado com sucesso',
        description: 'Agora é só concluir o pagamento.',
      });

      navigate(`/pedidos/${order.id}`);
    } catch (error) {
      const message = getApiErrorMessage(error, 'Não foi possível criar o pedido.');

      setActionError(message);
      showToast({
        variant: 'error',
        title: 'Erro ao criar pedido',
        description: message,
      });
    } finally {
      setSubmitting(null);
    }
  }

  if (!product) {
    return (
      <div className="min-h-[60vh] bg-white">
        <div className="mx-auto max-w-6xl px-6 py-16">
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
      <ProductHero product={product} />

      <ProductPlanSelector
        planTypes={planTypes}
        selectedCodes={selectedCodes}
        onToggle={togglePlanType}
        onBuyNow={handleBuyNow}
        onAddToCart={handleAddToCart}
        loading={loadingPlanTypes}
        submitting={submitting}
        error={actionError}
      />

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
              <div className="flex h-10 w-10 items-center justify-center rounded-xl border border-orange-100 bg-orange-50 font-bold text-primary-600">
                <Sparkles className="h-5 w-5" />
              </div>
              <h3 className="mt-4 font-extrabold text-neutral-900">{f.title}</h3>
              {f.description ? (
                <p className="mt-2 text-sm leading-relaxed text-neutral-600">{f.description}</p>
              ) : null}
            </div>
          ))}
        </div>
      </Section>

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
                <div className="flex h-9 w-9 items-center justify-center rounded-xl border border-orange-100 bg-orange-50">
                  <Check className="h-4 w-4 text-primary-600" />
                </div>
                <div className="min-w-0">
                  <div className="font-extrabold text-neutral-900">{it.title}</div>
                  {it.description ? (
                    <p className="mt-1 text-sm leading-relaxed text-neutral-600">
                      {it.description}
                    </p>
                  ) : null}
                </div>
              </div>
            </div>
          ))}
        </div>
      </Section>

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
          <div className="grid gap-4 text-center md:grid-cols-4">
            <MiniStat label="File formats" value={`${product.fileFormats?.length ?? 0}`} />
            <MiniStat label="Customizable" value={product.customizable ? '100%' : '—'} />
            <MiniStat label="Support" value="24/7" />
            <MiniStat label="Updates" value="Lifetime" />
          </div>
        </div>
      </Section>

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

              <p className="mt-3 text-sm leading-relaxed text-neutral-700">{t.quote}</p>

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

      <Section
        title={product.page.faqTitle ?? 'Frequently Asked Questions'}
        subtitle={product.page.faqIntro ?? 'Everything you need to know before buying.'}
      >
        <div className="mx-auto max-w-3xl">
          <div className="space-y-3">
            {(product.page.faq ?? []).map((f, idx) => {
              const open = openFaq === idx;

              return (
                <div key={`${f.question}-${idx}`} className="rounded-2xl border border-neutral-200">
                  <button
                    onClick={() => setOpenFaq(prev => (prev === idx ? null : idx))}
                    className="flex w-full items-center justify-between gap-4 px-5 py-4 text-left"
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
                    <div className="px-5 pb-5 text-sm leading-relaxed text-neutral-600">
                      {f.answer ?? 'Resposta em breve.'}
                    </div>
                  ) : null}
                </div>
              );
            })}
          </div>
        </div>
      </Section>

      <section className="bg-white">
        <div className="mx-auto max-w-6xl px-6 pb-20">
          <div className="rounded-3xl border border-neutral-200 bg-neutral-50 p-10 text-center">
            <h2 className="text-3xl font-extrabold text-neutral-900">
              {product.page.finalCtaTitle ?? 'Ready to build the house of your dreams?'}
            </h2>
            {product.page.finalCtaSubtitle ? (
              <p className="mt-2 text-neutral-600">{product.page.finalCtaSubtitle}</p>
            ) : null}

            <div className="mt-6 flex flex-wrap justify-center gap-3">
              <button
                type="button"
                onClick={() => {
                  document.getElementById('purchase-options')?.scrollIntoView({
                    behavior: 'smooth',
                    block: 'start',
                  });
                }}
                className="rounded-xl bg-primary-500 px-6 py-3 font-semibold text-white transition hover:bg-primary-600"
              >
                Comprar agora
              </button>

              <button className="rounded-xl border border-neutral-300 bg-white px-6 py-3 font-semibold text-neutral-900 transition hover:bg-neutral-100">
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
        <h2 className="text-center text-2xl font-extrabold text-neutral-900 md:text-3xl">
          {title}
        </h2>
        {subtitle ? (
          <p className="mx-auto mt-2 max-w-3xl text-center text-neutral-600">{subtitle}</p>
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
