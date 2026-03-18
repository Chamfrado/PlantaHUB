import { useMemo } from 'react';
import { useSearchParams } from 'react-router-dom';
import ProductAccordion from '../../../components/products/ProductAccordion';
import { PRODUCTS } from '../../../data/products';
import type { ProductCategory } from '../../../types/ProductData';

const VALID_CATEGORIES: ProductCategory[] = ['casas', 'chales', 'studios'];

export default function ProductsPage() {
  const [searchParams, setSearchParams] = useSearchParams();

  const categoryFromUrl = searchParams.get('category');

  const tab: ProductCategory = isValidCategory(categoryFromUrl) ? categoryFromUrl : 'casas';

  const products = useMemo(() => PRODUCTS, []);

  function handleTabChange(category: ProductCategory) {
    setSearchParams({ category });
  }

  return (
    <section className="bg-neutral-50">
      <div className="max-w-7xl mx-auto px-6 py-12">
        <h1 className="text-3xl font-extrabold text-neutral-900">Plantas Arquitetônicas</h1>

        <p className="mt-2 text-neutral-600 max-w-3xl">
          Explore nossa coleção premium de projetos completos. Cada pacote inclui documentação
          pronta para construção.
        </p>

        <div className="mt-8 border-b border-neutral-200">
          <div className="flex gap-8 text-sm font-semibold">
            <Tab label="Casas" active={tab === 'casas'} onClick={() => handleTabChange('casas')} />
            <Tab
              label="Chalés"
              active={tab === 'chales'}
              onClick={() => handleTabChange('chales')}
            />
            <Tab
              label="Studios"
              active={tab === 'studios'}
              onClick={() => handleTabChange('studios')}
            />
            <span className="text-neutral-400 py-4">Mais em breve</span>
          </div>
        </div>

        <div className="mt-8">
          <ProductAccordion products={products} category={tab} />
        </div>
      </div>
    </section>
  );
}

function isValidCategory(value: string | null): value is ProductCategory {
  return !!value && VALID_CATEGORIES.includes(value as ProductCategory);
}

function Tab({ label, active, onClick }: { label: string; active: boolean; onClick: () => void }) {
  return (
    <button
      onClick={onClick}
      className={[
        'py-4 border-b-2 transition',
        active
          ? 'border-primary-500 text-primary-600'
          : 'border-transparent text-neutral-600 hover:text-neutral-900',
      ].join(' ')}
    >
      {label}
    </button>
  );
}
