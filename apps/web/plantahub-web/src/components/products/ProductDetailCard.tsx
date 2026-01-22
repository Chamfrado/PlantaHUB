import { BadgeCheck, Download, FileText } from 'lucide-react';
import type { Product } from '../../types/ProductData';

type Props = {
  product: Product;
};

export default function ProductDetailsCard({ product }: Props) {
  const page = product.page;

  return (
    <div className="mt-4 rounded-2xl border border-neutral-200 bg-white p-6">
      <div className="grid gap-6 md:grid-cols-3">
        {/* Coluna 1 — O que inclui */}
        <div>
          <h4 className="font-bold text-neutral-900">{page.includesTitle ?? 'O que inclui'}</h4>

          <ul className="mt-3 space-y-3 text-sm text-neutral-600">
            {page.includedItems?.map((item, idx) => (
              <li key={idx} className="flex gap-2">
                <span className="text-primary-500">•</span>
                <div>
                  <div className="font-semibold text-neutral-800">{item.title}</div>
                  {item.description ? (
                    <div className="text-neutral-600">{item.description}</div>
                  ) : null}
                </div>
              </li>
            ))}
          </ul>
        </div>

        {/* Coluna 2 — Detalhes / Diferenciais */}
        <div>
          <h4 className="font-bold text-neutral-900">
            {page.whyChooseTitle ?? 'Diferenciais do projeto'}
          </h4>

          <ul className="mt-3 space-y-3 text-sm text-neutral-600">
            {page.whyChooseFeatures?.map((f, idx) => (
              <li key={idx} className="flex gap-2">
                <span className="text-primary-500">•</span>
                <div>
                  <div className="font-semibold text-neutral-800">{f.title}</div>
                  {f.description ? <div className="text-neutral-600">{f.description}</div> : null}
                </div>
              </li>
            ))}
          </ul>
        </div>

        {/* Coluna 3 — Arquivos / Entrega / CTA */}
        <div className="rounded-xl bg-neutral-50 p-4 border border-neutral-100">
          <div className="text-sm font-semibold text-neutral-900">Arquivos disponíveis</div>

          {product.fileFormats?.length ? (
            <div className="mt-3 flex flex-wrap gap-2">
              {product.fileFormats.map(f => (
                <span
                  key={f}
                  className="inline-flex items-center gap-2 rounded-full bg-white border border-neutral-200 px-3 py-1 text-xs font-semibold text-neutral-700"
                >
                  <FileText className="h-4 w-4 text-primary-500" />
                  {f}
                </span>
              ))}
            </div>
          ) : null}

          <div className="mt-4 space-y-2 text-sm text-neutral-700">
            {product.delivery ? (
              <div className="inline-flex items-center gap-2">
                <Download className="h-4 w-4 text-primary-500" />
                <span>{product.delivery}</span>
              </div>
            ) : null}

            {product.customizable ? (
              <div className="inline-flex items-center gap-2">
                <BadgeCheck className="h-4 w-4 text-primary-500" />
                <span>Projeto customizável</span>
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
