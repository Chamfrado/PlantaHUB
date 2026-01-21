import type { Product } from '../../types/product';

type Props = {
  product: Product;
  onViewDetails?: (product: Product) => void;
  actionLabel?: string;
};

export default function ProductCard({
  product,
  onViewDetails,
  actionLabel = 'Ver detalhes',
}: Props) {
  return (
    <article className="bg-white rounded-2xl border border-neutral-200 shadow-sm overflow-hidden">
      {/* Image */}
      <div className="relative h-56 w-full">
        <img
          src={product.imageUrl}
          alt={product.title}
          className="h-full w-full object-cover"
          loading="lazy"
        />

        {typeof product.areaM2 === 'number' && (
          <div className="absolute top-4 right-4">
            <span className="px-3 py-1 rounded-full bg-white/95 text-sm font-semibold text-neutral-900 border border-neutral-200 shadow-sm">
              {product.areaM2} m²
            </span>
          </div>
        )}
      </div>

      {/* Content */}
      <div className="p-6">
        {/* tags */}
        {product.tags?.length ? (
          <div className="flex flex-wrap gap-2">
            {product.tags.map(t => (
              <span
                key={t}
                className={[
                  'px-3 py-1 rounded-full text-xs font-semibold',
                  t === 'Comfort' || t === 'Prime' || t === 'Diamond'
                    ? 'bg-orange-50 text-primary-600'
                    : 'bg-neutral-100 text-neutral-700',
                ].join(' ')}
              >
                {t}
              </span>
            ))}
          </div>
        ) : null}

        <h3 className="mt-4 text-lg font-extrabold text-neutral-900">{product.title}</h3>

        <p className="mt-2 text-sm text-neutral-600 leading-relaxed min-h-11">{product.subtitle}</p>

        {/* formats */}
        {product.formats?.length ? (
          <div className="mt-4 flex items-center gap-4 text-xs font-semibold text-neutral-700">
            {product.formats.map(f => (
              <div key={f} className="inline-flex items-center gap-2">
                <FormatIcon />
                <span>{f}</span>
              </div>
            ))}
          </div>
        ) : null}

        {/* price + button */}
        <div className="mt-6 flex items-center justify-between gap-4">
          <div className="text-xl font-extrabold text-neutral-900">{formatBRL(product.price)}</div>

          <button
            onClick={() => onViewDetails?.(product)}
            className="px-5 py-2.5 rounded-xl bg-primary-500 text-white font-semibold cursor-pointer hover:bg-primary-600 transition"
          >
            {actionLabel}
          </button>
        </div>
      </div>
    </article>
  );
}

function formatBRL(value: number) {
  return value.toLocaleString('pt-BR', {
    style: 'currency',
    currency: 'BRL',
    maximumFractionDigits: 0,
  });
}

function FormatIcon() {
  return (
    <span className="inline-flex h-5 w-5 items-center justify-center rounded-md bg-orange-50 text-primary-600 border border-orange-100">
      ▦
    </span>
  );
}
