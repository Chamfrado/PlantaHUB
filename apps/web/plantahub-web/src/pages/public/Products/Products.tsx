import { useMemo } from 'react';
import { useSearchParams } from 'react-router-dom';
import ProductAccordion from '../../../components/products/ProductAccordion';
import { useAsync } from '../../../hooks/useAsync';
import { getApiErrorMessage } from '../../../lib/api-error';
import { mapProductSummary } from '../../../mappers/product.mapper';
import { listCategories } from '../../../services/categories.service';
import { listProducts } from '../../../services/products.service';

export default function ProductsPage() {
  const [searchParams, setSearchParams] = useSearchParams();

  // Busca tudo uma vez e filtra em memória. São poucas dezenas de produtos: uma requisição
  // só, troca de aba instantânea, e a lista de categorias exibidas pode sair da resposta
  // caso o endpoint de categorias ainda não exista no ambiente.
  const products = useAsync('products:all', () => listProducts());
  const categories = useAsync('categories', () => listCategories());

  const items = useMemo(
    () => (products.data ?? []).map(mapProductSummary),
    [products.data]
  );

  const tabs = useMemo(() => {
    if (categories.data && categories.data.length > 0) {
      return categories.data.map(c => ({ slug: c.slug, label: c.name }));
    }

    // Reserva: deriva das categorias que os produtos realmente têm.
    const seen = new Map<string, string>();
    items.forEach(p => seen.set(p.category, p.categoryName));
    return Array.from(seen, ([slug, label]) => ({ slug, label }));
  }, [categories.data, items]);

  const categoryFromUrl = searchParams.get('category');

  // Sem categoria válida na URL, cai na primeira que existe — e não numa string fixa.
  const activeCategory =
    categoryFromUrl && tabs.some(t => t.slug === categoryFromUrl)
      ? categoryFromUrl
      : (tabs[0]?.slug ?? '');

  const visible = useMemo(
    () => items.filter(p => p.category === activeCategory),
    [items, activeCategory]
  );

  return (
    <section className="bg-brand-light">
      <div className="max-w-7xl mx-auto px-6 py-12">
        <h1 className="text-3xl font-extrabold text-brand-black">Plantas Arquitetônicas</h1>

        <p className="mt-2 text-brand-muted max-w-3xl">
          Explore nossa coleção premium de projetos completos. Cada pacote inclui documentação
          pronta para construção.
        </p>

        {tabs.length > 0 && (
          <div className="mt-8 border-b border-neutral-200">
            <div className="flex flex-wrap gap-8 text-sm font-semibold">
              {tabs.map(tab => (
                <Tab
                  key={tab.slug}
                  label={tab.label}
                  active={tab.slug === activeCategory}
                  onClick={() => setSearchParams({ category: tab.slug })}
                />
              ))}
            </div>
          </div>
        )}

        <div className="mt-8">
          {products.loading ? (
            <ProductListSkeleton />
          ) : products.error ? (
            <ErrorState message={getApiErrorMessage(products.error, 'Não foi possível carregar os produtos.')} onRetry={products.reload} />
          ) : visible.length === 0 ? (
            <EmptyState />
          ) : (
            <ProductAccordion products={visible} />
          )}
        </div>
      </div>
    </section>
  );
}

function Tab({ label, active, onClick }: { label: string; active: boolean; onClick: () => void }) {
  return (
    <button
      onClick={onClick}
      className={[
        'py-4 border-b-2 transition',
        active
          ? 'border-primary-500 text-primary-600'
          : 'border-transparent text-brand-muted hover:text-brand-black',
      ].join(' ')}
    >
      {label}
    </button>
  );
}

/** Placeholders com a altura do conteúdo real, para a página não pular quando carregar. */
function ProductListSkeleton() {
  return (
    <div className="space-y-4" aria-busy="true" aria-label="Carregando produtos">
      {[0, 1, 2].map(i => (
        <div key={i} className="h-28 rounded-2xl border border-neutral-200 bg-white animate-pulse" />
      ))}
    </div>
  );
}

function ErrorState({ message, onRetry }: { message: string; onRetry: () => void }) {
  return (
    <div className="rounded-2xl border border-neutral-200 bg-white p-8 text-center">
      <p className="text-brand-black font-semibold">{message}</p>
      <button
        onClick={onRetry}
        className="mt-4 rounded-xl bg-primary-500 px-5 py-2.5 font-semibold text-white transition hover:bg-primary-600"
      >
        Tentar novamente
      </button>
    </div>
  );
}

function EmptyState() {
  return (
    <div className="rounded-2xl border border-neutral-200 bg-white p-8 text-center text-brand-muted">
      Nenhum produto publicado nesta categoria ainda.
    </div>
  );
}
