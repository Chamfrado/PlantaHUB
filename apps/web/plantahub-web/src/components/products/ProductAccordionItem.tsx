import { ChevronDown, Image as ImageIcon, Ruler } from 'lucide-react';
import type { ProductSummaryView } from '../../types/product-view';
import ProductDetailsCard from './ProductDetailCard';
import { formatCurrency } from '../../utils/format';

type Props = {
  product: ProductSummaryView;
  isOpen: boolean;
  onToggle: () => void;
};

export default function ProductAccordionItem({ product, isOpen, onToggle }: Props) {
  const title = product.name;
  const subtitle = product.shortDescription ?? '';

  return (
    <div className="rounded-2xl border border-neutral-200 bg-white px-6 py-5 transition duration-300 hover:-translate-y-0.5 hover:border-orange-200 hover:shadow-md">
      <button
        onClick={onToggle}
        className="w-full flex flex-wrap items-center justify-between gap-x-6 gap-y-3 text-left sm:flex-nowrap"
        aria-expanded={isOpen}
        aria-controls={`product-panel-${product.id}`}
      >
        {/* Left: Thumbnail + Info */}
        <div className="flex items-center gap-4 min-w-0">
          <Thumbnail src={product.heroImageUrl ?? undefined} alt={title} />

          <div className="min-w-0">
            <div className="font-bold text-brand-black truncate">{title}</div>

            <div className="mt-2 flex flex-wrap items-center gap-x-4 gap-y-2 text-sm text-brand-muted">
              {typeof product.areaM2 === 'number' ? (
                <span className="inline-flex items-center gap-2">
                  <Ruler className="h-4 w-4 text-primary-500" />
                  {product.areaM2} m²
                </span>
              ) : null}

              {subtitle ? <span className="truncate">{subtitle}</span> : null}
            </div>
          </div>
        </div>

        {/* Right: Price + Chevron */}
        <div className="ml-auto flex items-center gap-6 shrink-0">
          {product.basePriceCents !== null ? (
            <div className="text-right">
              <div className="text-xs text-brand-muted">A partir de</div>
              <div className="text-xl font-extrabold text-primary-500">
                {formatCurrency(product.basePriceCents)}
              </div>
            </div>
          ) : null}

          <ChevronDown
            className={[
              'h-5 w-5 text-brand-muted transition duration-300',
              isOpen ? 'rotate-180' : 'rotate-0',
            ].join(' ')}
          />
        </div>
      </button>

      {/* Expanded */}
      <div
        id={`product-panel-${product.id}`}
        className="accordion-panel"
        data-open={isOpen}
        aria-hidden={!isOpen}
      >
        <div>
          {/* Montado apenas ao abrir: o conteudo completo e buscado sob demanda, em vez
              de carregar a pagina inteira de todos os produtos da lista de uma vez. */}
          {isOpen ? (
            <div className="mt-4 animate-pop-in">
              <ProductDetailsCard category={product.category} slug={product.slug} />
            </div>
          ) : null}
        </div>
      </div>
    </div>
  );
}

function Thumbnail({ src, alt }: { src?: string; alt: string }) {
  if (!src) {
    return (
      <div className="h-16 w-24 rounded-xl bg-neutral-100 border border-neutral-200 flex items-center justify-center text-neutral-400 shrink-0">
        <ImageIcon className="h-5 w-5" />
      </div>
    );
  }

  return (
    <img
      src={src}
      alt={alt}
      className="h-16 w-24 rounded-xl object-cover border border-neutral-200 shrink-0"
      loading="lazy"
    />
  );
}
