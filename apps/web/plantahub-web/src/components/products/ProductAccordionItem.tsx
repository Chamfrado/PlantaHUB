import { ChevronDown, Ruler } from 'lucide-react';
import ProductDetailsCard from '../../components/products/ProductDetailCard';
import type { ProductDetails } from '../../types/productDetail';

type Props = {
  product: ProductDetails;
  isOpen: boolean;
  onToggle: () => void;
};

export default function ProductAccordionItem({ product, isOpen, onToggle }: Props) {
  return (
    <div className="rounded-2xl border border-neutral-200 bg-white px-6 py-5">
      <button
        onClick={onToggle}
        className="w-full flex items-center justify-between gap-6 text-left"
      >
        <div className="min-w-0">
          <div className="font-bold text-neutral-900 truncate">{product.name}</div>

          <div className="mt-2 flex flex-wrap items-center gap-x-4 gap-y-2 text-sm text-neutral-600">
            <span className="inline-flex items-center gap-2">
              <Ruler className="h-4 w-4 text-primary-500" />
              {product.areaM2} m²
            </span>
            <span className="truncate">{product.shortDescription}</span>
          </div>
        </div>

        <div className="flex items-center gap-6 shrink-0">
          <div className="text-right">
            <div className="text-xs text-neutral-500">A partir de</div>
            <div className="text-xl font-extrabold text-primary-500">
              {formatBRL(product.startingFrom)}
            </div>
          </div>

          <ChevronDown
            className={[
              'h-5 w-5 text-neutral-500 transition',
              isOpen ? 'rotate-180' : 'rotate-0',
            ].join(' ')}
          />
        </div>
      </button>

      {isOpen ? <ProductDetailsCard product={product} /> : null}
    </div>
  );
}

function formatBRL(value: number) {
  return value.toLocaleString('pt-BR', {
    style: 'currency',
    currency: 'BRL',
    maximumFractionDigits: 0,
  });
}
