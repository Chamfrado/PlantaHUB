import { BadgeCheck, Download, FileText } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { useAsync } from '../../hooks/useAsync';
import { mapProductDetail } from '../../mappers/product.mapper';
import { getProduct } from '../../services/products.service';

type Props = {
  category: string;
  slug: string;
};

/**
 * Painel expandido de um item da lista.
 *
 * Busca o produto completo sob demanda: montar isto só quando o acordeão abre evita
 * carregar o conteúdo editorial de todos os produtos da lista de uma vez.
 */
export default function ProductDetailsCard({ category, slug }: Props) {
  const navigate = useNavigate();

  const { data, loading, error } = useAsync(`product:${category}/${slug}`, () =>
    getProduct(category, slug).then(mapProductDetail)
  );

  if (loading) {
    return (
      <div className="mt-4 h-40 rounded-2xl border border-neutral-200 bg-white animate-pulse" />
    );
  }

  if (error || !data) {
    return (
      <div className="mt-4 rounded-2xl border border-neutral-200 bg-white p-6 text-sm text-brand-muted">
        Não foi possível carregar os detalhes deste projeto.
      </div>
    );
  }

  const product = data;

  return (
    <div className="mt-4 rounded-2xl border border-neutral-200 bg-white p-6">
      <div className="grid gap-6 md:grid-cols-3">
        {/* Cada coluna some quando não tem conteúdo: um produto recém-criado não pode
            renderizar títulos de seção com nada embaixo. */}
        {product.includes.items.length > 0 ? (
          <div>
            <h4 className="font-bold text-brand-black">
              {product.includes.title ?? 'O que inclui'}
            </h4>

            <ul className="mt-3 space-y-3 text-sm text-brand-muted">
              {product.includes.items.map((item, idx) => (
                <li key={idx} className="flex gap-2">
                  <span className="text-primary-500">•</span>
                  <div>
                    <div className="font-semibold text-brand-black">{item.title}</div>
                    {item.description ? (
                      <div className="text-brand-muted">{item.description}</div>
                    ) : null}
                  </div>
                </li>
              ))}
            </ul>
          </div>
        ) : null}

        {product.whyChoose.items.length > 0 ? (
          <div>
            <h4 className="font-bold text-brand-black">
              {product.whyChoose.title ?? 'Diferenciais do projeto'}
            </h4>

            <ul className="mt-3 space-y-3 text-sm text-brand-muted">
              {product.whyChoose.items.map((f, idx) => (
                <li key={idx} className="flex gap-2">
                  <span className="text-primary-500">•</span>
                  <div>
                    <div className="font-semibold text-brand-black">{f.title}</div>
                    {f.description ? <div className="text-brand-muted">{f.description}</div> : null}
                  </div>
                </li>
              ))}
            </ul>
          </div>
        ) : null}

        <div className="rounded-xl bg-brand-light p-4 border border-neutral-100">
          {product.fileFormats.length > 0 ? (
            <>
              <div className="text-sm font-semibold text-brand-black">Arquivos disponíveis</div>
              <div className="mt-3 flex flex-wrap gap-2">
                {product.fileFormats.map(f => (
                  <span
                    key={f}
                    className="inline-flex items-center gap-2 rounded-full bg-white border border-neutral-200 px-3 py-1 text-xs font-semibold text-brand-muted"
                  >
                    <FileText className="h-4 w-4 text-primary-500" />
                    {f}
                  </span>
                ))}
              </div>
            </>
          ) : null}

          <div className="mt-4 space-y-2 text-sm text-brand-muted">
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

          <button
            className="mt-5 w-full rounded-xl bg-primary-500 text-white font-semibold py-2.5 hover:bg-primary-600 transition"
            onClick={() => navigate(`/${product.category}/${product.slug}`)}
          >
            Ver página do produto
          </button>
        </div>
      </div>
    </div>
  );
}
