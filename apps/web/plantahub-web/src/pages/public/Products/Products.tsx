import { useState } from 'react';
import ProductAccordion from '../../../components/products/ProductAccordion';
import type { ProductCategory, ProductDetails } from '../../../types/productDetail';

const products: ProductDetails[] = [
  {
    id: 'house-1',
    category: 'houses',
    name: 'Casa Comfort',
    shortDescription: 'Projeto arquitetônico completo com todas as plantas técnicas',
    areaM2: 180,
    startingFrom: 4500,
    includes: ['Planta baixa', 'Cortes', 'Fachadas', 'Cobertura', 'Memorial descritivo'],
    technical: ['2 pavimentos', '3 quartos', '2 banheiros', 'Garagem 2 carros'],
    files: ['BIM', 'DWG', 'PDF'],
    delivery: 'Download imediato',
    certification: 'Certificado CAU/CREA',
  },
  {
    id: 'house-2',
    category: 'houses',
    name: 'Casa Elegance',
    shortDescription: 'Design premium com acabamentos e integração smart',
    areaM2: 250,
    startingFrom: 6800,
    includes: ['Plantas técnicas', 'Detalhamento', 'Quadro de esquadrias'],
    technical: ['2 pavimentos', '4 quartos', 'Área gourmet'],
    files: ['BIM', 'DWG', 'PDF'],
    delivery: 'Download imediato',
    certification: 'Certificado CAU/CREA',
  },
  {
    id: 'chalet-1',
    category: 'chalets',
    name: 'Chalé Mountain Prime',
    shortDescription: 'Ideal para lazer e locação, com visual premium',
    areaM2: 140,
    startingFrom: 5200,
    includes: ['Planta baixa', 'Fachadas', 'Elétrico/Hidráulico (opcional)'],
    technical: ['1 pavimento', '2 quartos', 'Lareira/varanda'],
    files: ['BIM', 'DWG', 'PDF'],
    delivery: 'Download imediato',
    certification: 'Certificado CAU/CREA',
  },
];

export default function ProductsPage() {
  const [tab, setTab] = useState<ProductCategory>('houses');

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
            <Tab label="Casas" active={tab === 'houses'} onClick={() => setTab('houses')} />
            <Tab label="Chalés" active={tab === 'chalets'} onClick={() => setTab('chalets')} />
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
