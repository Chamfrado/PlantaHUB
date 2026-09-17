import type { ProductSummaryView } from '../../types/product-view';
import { formatCurrency } from '../../utils/format';

type Props = {
  product: ProductSummaryView;
  onViewDetails?: (product: ProductSummaryView) => void;
  actionLabel?: string;
};

export default function ProductCard({
  product,
  onViewDetails,
  actionLabel = 'Ver detalhes',
}: Props) {
  const title = product.name;
  const subtitle = product.shortDescription ?? '';
  const img = product.heroImageUrl;

  return (
    // `h-full flex flex-col` com o rodape em `mt-auto`: sem isso cada cartao tinha a
    // altura do proprio conteudo, e uma etiqueta a mais ou uma descricao mais longa
    // deixava um quadro maior que o vizinho na mesma linha da grade.
    <article className="group flex h-full flex-col bg-white rounded-2xl border border-neutral-200 shadow-sm overflow-hidden transition duration-300 hover:-translate-y-1 hover:border-orange-200 hover:shadow-lg">
      {/* Image */}
      <div className="relative h-56 w-full shrink-0">
        {img ? (
          <img
            src={img}
            alt={title}
            className="h-full w-full object-cover transition duration-500 group-hover:scale-105"
            loading="lazy"
          />
        ) : (
          <div className="h-full w-full bg-neutral-100 border-b border-neutral-200" />
        )}

        {typeof product.areaM2 === 'number' && (
          <div className="absolute top-4 right-4">
            <span className="px-3 py-1 rounded-full bg-white/95 text-sm font-semibold text-brand-black border border-neutral-200 shadow-sm">
              {product.areaM2} m²
            </span>
          </div>
        )}
      </div>

      {/* Content */}
      <div className="flex flex-1 flex-col p-6">
        {/* tags */}
        {product.tags.length > 0 ? (
          <div className="flex flex-wrap gap-2">
            {product.tags.map((t, idx) => (
              <span
                key={t}
                className={[
                  'px-3 py-1 rounded-full text-xs font-semibold',
                  // A primeira etiqueta ganha destaque. A regra anterior pintava de laranja
                  // quando o texto fosse "confort", "prime" ou "diamond" — nomes de produto
                  // escritos no codigo, que deixam de existir com o catalogo administravel.
                  idx === 0 ? 'bg-orange-50 text-primary-600' : 'bg-green-50 text-brand-green',
                ].join(' ')}
              >
                {t}
              </span>
            ))}
          </div>
        ) : null}

        <h3 className="mt-4 text-lg font-extrabold text-brand-black">{title}</h3>

        <p className="mt-2 text-sm text-brand-muted leading-relaxed min-h-11">{subtitle}</p>

        {/* formats */}
        {product.fileFormats.length > 0 ? (
          <div className="mt-4 flex items-center gap-4 text-xs font-semibold text-brand-muted">
            {product.fileFormats.map(f => (
              <div key={f} className="inline-flex items-center gap-2">
                <FormatIcon />
                <span>{f}</span>
              </div>
            ))}
          </div>
        ) : null}

        {/* price + button */}
        <div className="mt-auto flex items-end justify-between gap-4 pt-6">
          <div>
            {product.basePriceCents !== null ? (
              <>
                {/* "A partir de" porque o valor e a oferta mais barata do produto: cada
                    colecao tem seu preco, e o cliente monta a compra. Mostrar o numero
                    sozinho sugeriria que este e o preco de levar tudo. */}
                <div className="text-xs font-semibold text-brand-muted">A partir de</div>
                <div className="text-xl font-extrabold text-primary-500">
                  {formatCurrency(product.basePriceCents)}
                </div>
              </>
            ) : (
              <div className="text-sm font-semibold text-brand-muted">Preço sob consulta</div>
            )}
          </div>

          <button
            onClick={() => onViewDetails?.(product)}
            className="px-5 py-2.5 rounded-xl bg-primary-500 text-white font-semibold cursor-pointer transition duration-200 hover:-translate-y-0.5 hover:bg-primary-600 hover:shadow-md"
          >
            {actionLabel}
          </button>
        </div>
      </div>
    </article>
  );
}

function FormatIcon() {
  return (
    <span className="inline-flex h-5 w-5 items-center justify-center rounded-md bg-orange-50 text-primary-600 border border-orange-100">
      ▦
    </span>
  );
}
