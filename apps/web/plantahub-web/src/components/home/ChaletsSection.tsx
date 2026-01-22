import { PRODUCTS } from '../../data/products';
import type { Product } from '../../types/ProductData';
import ProductCarouselSection from '../products/ProductCarouselSection';

export default function ChaletsSection() {
  const chalets: Product[] = PRODUCTS.filter(p => p.category === 'chales');

  return (
    <ProductCarouselSection
      title="Chalés e Casas de Montanha"
      subtitle="Perfeitos para natureza e lazer"
      products={chalets}
      enableTiers={false} // agora funciona via slug: confort/prime/diamond
      limit={3}
      defaultTier="prime" // slug, não mais "Prime"
      footerCtaLabel="Ver todos os chalés"
      onFooterCtaClick={() => console.log('ir para listagem de chalés')}
      onViewDetails={p => console.log('abrir detalhes do chalé', p.id)}
      actionLabel="Ver detalhes"
    />
  );
}
