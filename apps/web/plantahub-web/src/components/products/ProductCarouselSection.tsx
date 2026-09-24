import { useMemo } from 'react';
import ProductCard from '../../components/products/ProductCard';
import type { ProductSummaryView } from '../../types/product-view';

type Props = {
  title: string;
  subtitle?: string;

  products: ProductSummaryView[];

  /** Quantos cards mostrar. */
  limit?: number;

  footerCtaLabel?: string;
  onFooterCtaClick?: () => void;

  onViewDetails?: (product: ProductSummaryView) => void;
  actionLabel?: string;
};

/**
 * Vitrine horizontal de produtos.
 *
 * As abas "Confort / Prime / Diamond" foram removidas. Elas filtravam por
 * `p.slug === tier`, o que assume que o slug do produto *é* o nome da linha — verdade
 * apenas para os seis produtos originais. Um produto criado no painel com slug
 * `casa-familia-120` sumiria da vitrine sem aviso. Já estavam desligadas nos dois usos, e
 * a diferenciação agora vive nas coleções ofertadas, não numa linha de produto.
 */
export default function ProductCarouselSection({
  title,
  subtitle,
  products,
  limit = 3,
  footerCtaLabel = 'Ver tudo',
  onFooterCtaClick,
  onViewDetails,
  actionLabel = 'Ver detalhes',
}: Props) {
  const visibleProducts = useMemo(() => products.slice(0, limit), [products, limit]);

  return (
    <section className="bg-white">
      <div className="max-w-7xl mx-auto px-6 py-16 md:py-20">
        <div className="flex flex-col md:flex-row md:items-start md:justify-between gap-6">
          <div>
            <h2 className="text-3xl font-extrabold text-brand-black">{title}</h2>
            {subtitle ? <p className="mt-2 text-brand-muted">{subtitle}</p> : null}
          </div>
        </div>

        <div className="mt-10 grid gap-6 lg:grid-cols-3">
          {visibleProducts.map(product => (
            <div key={product.id} className="h-full animate-pop-in">
              <ProductCard
                product={product}
                onViewDetails={onViewDetails}
                actionLabel={actionLabel}
              />
            </div>
          ))}
        </div>

        <div className="mt-10 flex justify-center">
          <button
            onClick={onFooterCtaClick}
            className="px-6 py-3 rounded-xl border border-neutral-300 bg-white font-semibold cursor-pointer text-brand-black transition duration-200 hover:-translate-y-0.5 hover:bg-neutral-100 hover:shadow-md inline-flex items-center gap-2"
          >
            {footerCtaLabel} <span className="text-lg">→</span>
          </button>
        </div>
      </div>
    </section>
  );
}
