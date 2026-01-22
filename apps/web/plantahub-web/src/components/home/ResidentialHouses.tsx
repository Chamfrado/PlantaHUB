import ProductCarouselSection from '../../components/products/ProductCarouselSection';
import { PRODUCTS } from '../../data/products';
import type { Product } from '../../types/ProductData';

export default function ResidentialHouses() {
  const houses: Product[] = PRODUCTS.filter(p => p.category === 'casas');

  return (
    <ProductCarouselSection
      title="Casas Residenciais"
      subtitle="Designs modernos para uma vida confortável"
      products={houses}
      enableTiers={false} // Confort / Prime / Diamond
      limit={3}
      defaultTier="confort" // slug, não mais "Comfort"
      footerCtaLabel="Ver todas as casas"
      onFooterCtaClick={() => console.log('ir para listagem')}
      onViewDetails={p => console.log('abrir detalhes', p.id)}
      actionLabel="Ver detalhes"
    />
  );
}
