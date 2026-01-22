import { useMemo, useState } from 'react';
import ProductAccordion from '../../../components/products/ProductAccordion';
import { PRODUCTS } from '../../../data/products';
import type { ProductCategory } from '../../../types/ProductData';
export default function ProductsPage() {
  const [tab, setTab] = useState<ProductCategory>('casas');

  const products = useMemo(() => PRODUCTS, []);

  return (
    <section className="bg-neutral-50">
      <div className="max-w-7xl mx-auto px-6 py-12">
        <h1 className="text-3xl font-extrabold text-neutral-900">Plantas Arquitetônicas</h1>

        <p className="mt-2 text-neutral-600 max-w-3xl">
          Explore nossa coleção premium de projetos completos. Cada pacote inclui documentação
          pronta para construção.
        </p>

        {/* Tabs */}
        <div className="mt-8 border-b border-neutral-200">
          <div className="flex gap-8 text-sm font-semibold">
            <Tab label="Casas" active={tab === 'casas'} onClick={() => setTab('casas')} />
            <Tab label="Chalés" active={tab === 'chales'} onClick={() => setTab('chales')} />
            <Tab label="Studios" active={tab === 'studios'} onClick={() => setTab('studios')} />
            <span className="text-neutral-400 py-4">Mais em breve</span>
          </div>
        </div>

        {/* Accordion */}
        <div className="mt-8">
          <ProductAccordion products={products} category={tab} />
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
          : 'border-transparent text-neutral-600 hover:text-neutral-900',
      ].join(' ')}
    >
      {label}
    </button>
  );
}
