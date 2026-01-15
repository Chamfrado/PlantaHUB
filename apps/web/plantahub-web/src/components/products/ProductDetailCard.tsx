import { BadgeCheck, Download, FileText } from 'lucide-react';
import type { ProductDetails } from '../../types/productDetail';

export default function ProductDetailsCard({ product }: { product: ProductDetails }) {
  return (
    <div className="mt-4 rounded-2xl border border-neutral-200 bg-white p-6">
      <div className="grid gap-6 md:grid-cols-3">
        {/* Coluna 1 */}
        <div>
          <h4 className="font-bold text-neutral-900">O que inclui</h4>
          <ul className="mt-3 space-y-2 text-sm text-neutral-600">
            {product.includes.map(x => (
              <li key={x} className="flex gap-2">
                <span className="text-primary-500">•</span>
                <span>{x}</span>
              </li>
            ))}
          </ul>
        </div>

        {/* Coluna 2 */}
        <div>
          <h4 className="font-bold text-neutral-900">Detalhes técnicos</h4>
          <ul className="mt-3 space-y-2 text-sm text-neutral-600">
            {product.technical.map(x => (
              <li key={x} className="flex gap-2">
                <span className="text-primary-500">•</span>
                <span>{x}</span>
              </li>
            ))}
          </ul>
        </div>

        {/* Coluna 3 */}
        <div className="rounded-xl bg-neutral-50 p-4 border border-neutral-100">
          <div className="text-sm font-semibold text-neutral-900">Arquivos disponíveis</div>

          <div className="mt-3 flex flex-wrap gap-2">
            {product.files.map(f => (
              <span
                key={f}
                className="inline-flex items-center gap-2 rounded-full bg-white border border-neutral-200 px-3 py-1 text-xs font-semibold text-neutral-700"
              >
                <FileText className="h-4 w-4 text-primary-500" />
                {f}
              </span>
            ))}
          </div>

          <div className="mt-4 space-y-2 text-sm text-neutral-700">
            <div className="inline-flex items-center gap-2">
              <Download className="h-4 w-4 text-primary-500" />
              <span>{product.delivery}</span>
            </div>

            {product.certification ? (
              <div className="inline-flex items-center gap-2">
                <BadgeCheck className="h-4 w-4 text-primary-500" />
                <span>{product.certification}</span>
              </div>
            ) : null}
          </div>

          <button className="mt-5 w-full rounded-xl bg-primary-500 text-white font-semibold py-2.5 hover:bg-primary-600 transition">
            Ver página do produto
          </button>
        </div>
      </div>
    </div>
  );
}
