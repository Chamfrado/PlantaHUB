import { CalendarDays, Download, ShoppingCart } from 'lucide-react';
import { useMemo } from 'react';
import { Link } from 'react-router-dom';
import type { LibraryProductDTO } from '../../types/api/library';
import type { PlanTypeOptionDTO } from '../../types/api/product';

type Props = {
  product: LibraryProductDTO & {
    availablePlanTypes: PlanTypeOptionDTO[];
  };
};

export default function LibraryProductCard({ product }: Props) {
  const purchasedCodes = useMemo(
    () => new Set(product.planTypes.map(plan => plan.code)),
    [product.planTypes]
  );

  const missingPlanTypes = useMemo(
    () => product.availablePlanTypes.filter(plan => !purchasedCodes.has(plan.code)),
    [product.availablePlanTypes, purchasedCodes]
  );

  return (
    <article className="overflow-hidden rounded-3xl border border-neutral-200 bg-white shadow-sm">
      <div className="grid gap-0 lg:grid-cols-[320px_minmax(0,1fr)]">
        <div className="h-full min-h-[240px] bg-neutral-100">
          {product.heroImageUrl ? (
            <img
              src={product.heroImageUrl}
              alt={product.name}
              className="h-full w-full object-cover"
              loading="lazy"
            />
          ) : (
            <div className="flex h-full min-h-[240px] items-center justify-center text-sm text-neutral-400">
              Sem imagem
            </div>
          )}
        </div>

        <div className="p-6 md:p-8">
          <div className="flex flex-col gap-4 md:flex-row md:items-start md:justify-between">
            <div>
              <div className="inline-flex rounded-full bg-orange-50 px-3 py-1 text-xs font-semibold text-primary-600">
                {product.category}
              </div>

              <h2 className="mt-4 text-2xl font-extrabold text-neutral-900">{product.name}</h2>

              <div className="mt-3 flex flex-wrap items-center gap-4 text-sm text-neutral-500">
                <span>{product.areaM2} m²</span>

                <span className="inline-flex items-center gap-2">
                  <CalendarDays className="h-4 w-4" />
                  Comprado em {formatDateTime(product.purchasedAt)}
                </span>
              </div>
            </div>

            <Link
              to={`/produtos/${product.category}/${product.slug}`}
              className="inline-flex items-center justify-center rounded-xl border border-neutral-300 px-4 py-2 text-sm font-semibold text-neutral-700 transition hover:bg-neutral-50"
            >
              Ver produto
            </Link>
          </div>

          <div className="mt-8 grid gap-6 xl:grid-cols-2">
            <section className="rounded-2xl border border-green-200 bg-green-50/40 p-5">
              <div className="flex items-center gap-2">
                <Download className="h-4 w-4 text-green-700" />
                <h3 className="text-sm font-extrabold text-green-800">Tipos adquiridos</h3>
              </div>

              <div className="mt-4 space-y-3">
                {product.planTypes.map(plan => (
                  <div key={plan.code} className="rounded-2xl border border-green-200 bg-white p-4">
                    <div className="flex items-center justify-between gap-3">
                      <div>
                        <div className="text-sm font-bold text-neutral-900">{plan.name}</div>
                        <div className="mt-1 text-xs text-neutral-500">
                          {plan.assets.length} arquivo(s) disponível(is)
                        </div>
                      </div>

                      <Link
                        to={`/my-library/${product.productId}/${plan.code}`}
                        className="inline-flex items-center rounded-xl bg-primary-500 px-4 py-2 text-sm font-semibold text-white transition hover:bg-primary-600"
                      >
                        Acessar
                      </Link>
                    </div>
                  </div>
                ))}
              </div>
            </section>

            <section className="rounded-2xl border border-orange-200 bg-orange-50/40 p-5">
              <div className="flex items-center gap-2">
                <ShoppingCart className="h-4 w-4 text-orange-700" />
                <h3 className="text-sm font-extrabold text-orange-800">
                  Disponíveis para adquirir
                </h3>
              </div>

              <div className="mt-4 space-y-3">
                {!missingPlanTypes.length ? (
                  <div className="rounded-2xl border border-orange-200 bg-white p-4 text-sm text-neutral-600">
                    Você já possui todos os tipos de planta disponíveis para este produto.
                  </div>
                ) : (
                  missingPlanTypes.map(plan => (
                    <div
                      key={plan.code}
                      className="rounded-2xl border border-orange-200 bg-white p-4"
                    >
                      <div className="flex items-start justify-between gap-4">
                        <div>
                          <div className="text-sm font-bold text-neutral-900">{plan.name}</div>
                          <p className="mt-1 text-xs leading-relaxed text-neutral-500">
                            {plan.description}
                          </p>
                          <div className="mt-3 text-sm font-extrabold text-neutral-900">
                            {formatMoneyFromCents(plan.priceCents)}
                          </div>
                        </div>

                        <Link
                          to={`/produtos/${product.category}/${product.slug}`}
                          className="inline-flex items-center rounded-xl border border-primary-200 bg-primary-50 px-4 py-2 text-sm font-semibold text-primary-700 transition hover:bg-primary-100"
                        >
                          Comprar
                        </Link>
                      </div>
                    </div>
                  ))
                )}
              </div>
            </section>
          </div>
        </div>
      </div>
    </article>
  );
}

function formatDateTime(value: string) {
  return new Date(value).toLocaleDateString('pt-BR');
}

function formatMoneyFromCents(value: number) {
  return (value / 100).toLocaleString('pt-BR', {
    style: 'currency',
    currency: 'BRL',
  });
}
