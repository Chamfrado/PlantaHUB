import ProductCarouselSection from '../../components/products/ProductCarouselSection';
import type { Product } from '../../types/product';

const houses: Product[] = [
  {
    id: 'h1',
    tier: 'Comfort',
    title: 'Casa Familiar Moderna',
    subtitle: 'Projeto completo com toda a documentação técnica e recursos sustentáveis.',
    areaM2: 180,
    tags: ['Comfort', '3 Quartos'],
    price: 2499,
    imageUrl:
      'https://images.unsplash.com/photo-1568605114967-8130f3a36994?auto=format&fit=crop&w=1600&q=80',
    formats: ['BIM', 'DWG', 'PDF'],
  },
  {
    id: 'h2',
    tier: 'Comfort',
    title: 'Vila Contemporânea',
    subtitle: 'Design de dois pavimentos com ambientes integrados e soluções eficientes.',
    areaM2: 220,
    tags: ['Comfort', '4 Quartos'],
    price: 3299,
    imageUrl:
      'https://images.unsplash.com/photo-1600585154340-be6161a56a0c?auto=format&fit=crop&w=1600&q=80',
    formats: ['BIM', 'DWG', 'PDF'],
  },
  {
    id: 'h3',
    tier: 'Comfort',
    title: 'Casa Moderna Compacta',
    subtitle: 'Projeto eficiente, ideal para famílias pequenas e primeira construção.',
    areaM2: 150,
    tags: ['Comfort', '2 Quartos'],
    price: 1899,
    imageUrl:
      'https://images.unsplash.com/photo-1605276374104-dee2a0ed3cd6?auto=format&fit=crop&w=1600&q=80',
    formats: ['BIM', 'DWG', 'PDF'],
  },
];

export default function ResidentialHouses() {
  return (
    <ProductCarouselSection
      title="Casas Residenciais"
      subtitle="Designs modernos para uma vida confortável"
      products={houses}
      enableTiers={false}
      footerCtaLabel="Ver todas as casas"
      onFooterCtaClick={() => console.log('ir para listagem')}
      onViewDetails={p => console.log('abrir detalhes', p.id)}
    />
  );
}
