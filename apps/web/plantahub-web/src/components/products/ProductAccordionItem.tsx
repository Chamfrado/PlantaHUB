import { ChevronDown, Image as ImageIcon, Ruler } from 'lucide-react';
import type { Product } from '../../types/ProductData';
import ProductDetailsCard from './ProductDetailCard';
type Props = {
  product: Product;
  isOpen: boolean;
  onToggle: () => void;
};

export default function ProductAccordionItem({ product, isOpen, onToggle }: Props) {
  const title = product.page?.headline ?? product.name;
  const subtitle = product.page?.subheadline ?? product.shortDescription ?? '';

  return (
    <div className="rounded-2xl border border-neutral-200 bg-white px-6 py-5">
      <button
        onClick={onToggle}
        className="w-full flex items-center justify-between gap-6 text-left"
        aria-expanded={isOpen}
        aria-controls={`product-panel-${product.id}`}
      >
        {/* Left: Thumbnail + Info */}
        <div className="flex items-center gap-4 min-w-0">
          <Thumbnail src={product.heroImageUrl} alt={title} />

          <div className="min-w-0">
            <div className="font-bold text-neutral-900 truncate">{title}</div>

            <div className="mt-2 flex flex-wrap items-center gap-x-4 gap-y-2 text-sm text-neutral-600">
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
        <div className="flex items-center gap-6 shrink-0">
          {product.price ? (
            <div className="text-right">
              <div className="text-xs text-neutral-500">
                {product.price.isStartingFrom ? 'A partir de' : 'Preço'}
              </div>
              <div className="text-xl font-extrabold text-primary-500">
                {formatMoney(product.price.amount, product.price.currency)}
              </div>
            </div>
          ) : null}

          <ChevronDown
            className={[
              'h-5 w-5 text-neutral-500 transition',
              isOpen ? 'rotate-180' : 'rotate-0',
            ].join(' ')}
          />
        </div>
      </button>

      {/* Expanded */}
      {isOpen ? (
        <div id={`product-panel-${product.id}`} className="mt-4">
          <ProductDetailsCard product={product} />
        </div>
      ) : null}
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
