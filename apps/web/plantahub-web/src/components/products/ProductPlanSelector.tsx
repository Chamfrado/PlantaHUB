import { Check, CreditCard, Loader2, ShoppingCart } from 'lucide-react';
import type { PlanTypeOptionDTO } from '../../types/api/product';

type Props = {
  planTypes: PlanTypeOptionDTO[];
  selectedCodes: string[];
  onToggle: (code: string) => void;
  onBuyNow: () => void;
  onAddToCart: () => void;
  loading?: boolean;
  submitting?: 'buy' | 'cart' | null;
  error?: string | null;
};

export default function ProductPlanSelector({
  planTypes,
  selectedCodes,
  onToggle,
  onBuyNow,
  onAddToCart,
  loading = false,
  submitting = null,
  error = null,
}: Props) {
  const selectedItems = planTypes.filter(item => selectedCodes.includes(item.code));
  const totalCents = selectedItems.reduce((sum, item) => sum + item.priceCents, 0);
  const canSubmit = selectedCodes.length > 0 && !loading && !submitting;

  return (
    <section id="purchase-options" className="bg-white">
      <div className="mx-auto max-w-6xl px-6 py-14">
        <div className="grid gap-6 lg:grid-cols-[1.2fr_380px]">
          <div className="rounded-3xl border border-neutral-200 bg-white p-6 shadow-sm">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <div>
                <h2 className="text-2xl font-extrabold text-neutral-900">
                  Escolha os tipos de planta
                </h2>
                <p className="mt-1 text-sm text-neutral-600">
                  Selecione exatamente quais arquivos deseja comprar neste produto.
                </p>
              </div>

              <div className="rounded-2xl border border-orange-100 bg-orange-50 px-4 py-2 text-sm font-semibold text-primary-600">
                {selectedCodes.length} selecionado(s)
              </div>
            </div>

            {loading ? (
              <div className="mt-6 flex items-center gap-3 rounded-2xl border border-neutral-200 bg-neutral-50 p-4 text-sm text-neutral-600">
                <Loader2 className="h-4 w-4 animate-spin" />
                Carregando opções...
              </div>
            ) : (
              <div className="mt-6 grid gap-4">
                {planTypes.map(planType => {
                  const selected = selectedCodes.includes(planType.code);

                  return (
                    <button
                      key={planType.code}
                      type="button"
                      onClick={() => onToggle(planType.code)}
                      className={[
                        'w-full rounded-2xl border p-5 text-left transition',
                        selected
                          ? 'border-primary-500 bg-orange-50 shadow-sm'
                          : 'border-neutral-200 bg-white hover:border-neutral-300 hover:bg-neutral-50',
                      ].join(' ')}
                    >
                      <div className="flex items-start justify-between gap-4">
                        <div>
                          <div className="flex flex-wrap items-center gap-2">
                            <h3 className="text-lg font-extrabold text-neutral-900">
                              {planType.name}
                            </h3>

                            {planType.includedInBundle ? (
                              <span className="rounded-full border border-orange-200 bg-white px-2.5 py-1 text-[11px] font-bold text-primary-600">
                                Bundle
                              </span>
                            ) : null}
                          </div>

                          {planType.description ? (
                            <p className="mt-2 text-sm leading-relaxed text-neutral-600">
                              {planType.description}
                            </p>
                          ) : null}
                        </div>

                        <div className="flex shrink-0 flex-col items-end gap-3">
                          <div className="text-lg font-extrabold text-neutral-900">
                            {formatMoney(planType.priceCents, 'BRL')}
                          </div>

                          <div
                            className={[
                              'flex h-7 w-7 items-center justify-center rounded-full border transition',
                              selected
                                ? 'border-primary-500 bg-primary-500 text-white'
                                : 'border-neutral-300 bg-white text-transparent',
                            ].join(' ')}
                          >
                            <Check className="h-4 w-4" />
                          </div>
                        </div>
                      </div>
                    </button>
                  );
                })}
              </div>
            )}

            {error ? (
              <div className="mt-4 rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
                {error}
              </div>
            ) : null}
          </div>

          <aside className="h-fit lg:sticky lg:top-24">
            <div className="rounded-3xl border border-neutral-200 bg-neutral-50 p-6 shadow-sm">
              <h3 className="text-xl font-extrabold text-neutral-900">Resumo da compra</h3>

              <div className="mt-5 space-y-3">
                {selectedItems.length === 0 ? (
                  <p className="text-sm text-neutral-500">
                    Selecione ao menos um tipo de planta para continuar.
                  </p>
                ) : (
                  selectedItems.map(item => (
                    <div
                      key={item.code}
                      className="flex items-start justify-between gap-4 rounded-2xl border border-white bg-white p-4"
                    >
                      <div>
                        <div className="font-bold text-neutral-900">{item.name}</div>
                        <div className="mt-1 text-xs text-neutral-500">{item.code}</div>
                      </div>

                      <div className="text-sm font-bold text-neutral-900">
                        {formatMoney(item.priceCents, 'BRL')}
                      </div>
                    </div>
                  ))
                )}
              </div>

              <div className="my-5 h-px bg-neutral-200" />

              <div className="flex items-center justify-between">
                <span className="text-sm font-semibold text-neutral-600">Total</span>
                <span className="text-2xl font-extrabold text-neutral-900">
                  {formatMoney(totalCents, 'BRL')}
                </span>
              </div>

              <div className="mt-5 grid gap-3">
                <button
                  type="button"
                  onClick={onBuyNow}
                  disabled={!canSubmit}
                  className="inline-flex items-center justify-center gap-2 rounded-2xl bg-primary-500 px-5 py-3 font-semibold text-white transition hover:bg-primary-600 disabled:cursor-not-allowed disabled:opacity-50"
                >
                  {submitting === 'buy' ? (
                    <Loader2 className="h-4 w-4 animate-spin" />
                  ) : (
                    <CreditCard className="h-4 w-4" />
                  )}
                  Comprar agora
                </button>

                <button
                  type="button"
                  onClick={onAddToCart}
                  disabled={!canSubmit}
                  className="inline-flex items-center justify-center gap-2 rounded-2xl border border-neutral-300 bg-white px-5 py-3 font-semibold text-neutral-900 transition hover:bg-neutral-100 disabled:cursor-not-allowed disabled:opacity-50"
                >
                  {submitting === 'cart' ? (
                    <Loader2 className="h-4 w-4 animate-spin" />
                  ) : (
                    <ShoppingCart className="h-4 w-4" />
                  )}
                  Adicionar ao carrinho
                </button>
              </div>

              <p className="mt-4 text-xs leading-relaxed text-neutral-500">
                Após o pagamento, os arquivos comprados ficarão disponíveis na sua biblioteca.
              </p>
            </div>
          </aside>
        </div>
      </div>
    </section>
  );
}

function formatMoney(valueInCents: number, currency: 'BRL' | 'USD' | 'EUR') {
  const locale = currency === 'BRL' ? 'pt-BR' : 'en-US';

  return (valueInCents / 100).toLocaleString(locale, {
    style: 'currency',
    currency,
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  });
}
