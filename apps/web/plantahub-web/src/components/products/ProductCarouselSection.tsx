import { useMemo, useState } from 'react';
import ProductCard from '../../components/products/ProductCard';
import type { Product, Tier } from '../../types/product';

type Props = {
  title: string;
  subtitle?: string;

  products: Product[];

  /** Se quiser tabs (Comfort/Prime/Diamond), passe true */
  enableTiers?: boolean;
  defaultTier?: Tier;

  /** Quantos cards mostrar (ex: 3 igual no print) */
  limit?: number;

  /** CTA de rodapé */
  footerCtaLabel?: string;
  onFooterCtaClick?: () => void;

  /** Clique no card */
  onViewDetails?: (product: Product) => void;
  actionLabel?: string;
};

export default function ProductCarouselSection({
  title,
  subtitle,
  products,
  enableTiers = true,
  defaultTier = 'Comfort',
  limit = 3,
  footerCtaLabel = 'Ver tudo',
  onFooterCtaClick,
  onViewDetails,
  actionLabel = 'Ver detalhes',
}: Props) {
  const [tier, setTier] = useState<Tier>(defaultTier);

  const visibleProducts = useMemo(() => {
    const list = enableTiers ? products.filter(p => (p.tier ?? 'Comfort') === tier) : products;

    return list.slice(0, limit);
  }, [products, enableTiers, tier, limit]);

  return (
    <section className="bg-neutral-50">
      <div className="max-w-7xl mx-auto px-6 py-16 md:py-20">
        {/* Header row */}
        <div className="flex flex-col md:flex-row md:items-start md:justify-between gap-6">
          <div>
            <h2 className="text-3xl font-extrabold text-neutral-900">{title}</h2>
            {subtitle ? <p className="mt-2 text-neutral-600">{subtitle}</p> : null}
          </div>

          {enableTiers ? <TierTabs value={tier} onChange={setTier} /> : null}
        </div>

        {/* Cards */}
        <div className="mt-10 grid gap-6 lg:grid-cols-3">
          {visibleProducts.map(product => (
            <ProductCard
              key={product.id}
              product={product}
              onViewDetails={onViewDetails}
              actionLabel={actionLabel}
            />
          ))}
        </div>

        {/* Bottom CTA */}
        <div className="mt-10 flex justify-center">
          <button
            onClick={onFooterCtaClick}
            className="px-6 py-3 rounded-xl border border-neutral-300 bg-white font-semibold text-neutral-900 hover:bg-neutral-100 transition inline-flex items-center gap-2"
          >
            {footerCtaLabel} <span className="text-lg">→</span>
          </button>
        </div>
      </div>
    </section>
  );
}

function TierTabs({ value, onChange }: { value: Tier; onChange: (v: Tier) => void }) {
  const tabs: Tier[] = ['Comfort', 'Prime', 'Diamond'];

  return (
    <div className="inline-flex rounded-xl bg-white border border-neutral-200 p-1 shadow-sm">
      {tabs.map(t => {
        const active = t === value;
        return (
          <button
            key={t}
            onClick={() => onChange(t)}
            className={[
              'px-4 py-2 rounded-lg text-sm font-semibold transition',
              active ? 'bg-primary-500 text-white' : 'text-neutral-700 hover:bg-neutral-100',
            ].join(' ')}
          >
            {t}
          </button>
        );
      })}
    </div>
  );
}
