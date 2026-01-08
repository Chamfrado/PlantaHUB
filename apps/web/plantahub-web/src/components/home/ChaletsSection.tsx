import type { Product } from '../../types/product';
import ProductCarouselSection from '../products/ProductCarouselSection';

const chalets: Product[] = [
  {
    id: 'c1',
    tier: 'Prime',
    title: 'Chalé Alpino Luxuoso',
    subtitle: 'Design premium com vistas panorâmicas e acabamentos de alto padrão.',
    areaM2: 280,
    tags: ['Prime', '4 Quartos'],
    price: 4799,
    imageUrl:
      'https://images.unsplash.com/photo-1542314831-068cd1dbfeeb?auto=format&fit=crop&w=1600&q=80',
    formats: ['BIM', 'DWG', 'PDF'],
  },
  {
    id: 'c2',
    tier: 'Prime',
    title: 'Chalé Moderno envidraçado',
    subtitle: 'Projeto contemporâneo com grandes aberturas e ambientes integrados.',
    areaM2: 320,
    tags: ['Prime', '5 Quartos'],
    price: 5499,
    imageUrl:
      'https://images.unsplash.com/photo-1528909514045-2fa4ac7a08ba?auto=format&fit=crop&w=1600&q=80',
    formats: ['BIM', 'DWG', 'PDF'],
  },
  {
    id: 'c3',
    tier: 'Prime',
    title: 'Refúgio Rústico na Montanha',
    subtitle: 'Charme tradicional com conforto moderno e ótima funcionalidade.',
    areaM2: 240,
    tags: ['Prime', '3 Quartos'],
    price: 3999,
    imageUrl:
      'https://images.unsplash.com/photo-1505692952047-1a78307da8f2?auto=format&fit=crop&w=1600&q=80',
    formats: ['BIM', 'DWG', 'PDF'],
  },

  // exemplos extras pra funcionar nas outras tabs (se quiser)
  {
    id: 'c4',
    tier: 'Comfort',
    title: 'Chalé Compacto de Madeira',
    subtitle: 'Ideal para lazer e locação, com custo reduzido e boa implantação.',
    areaM2: 140,
    tags: ['Comfort', '2 Quartos'],
    price: 2199,
    imageUrl:
      'https://images.unsplash.com/photo-1441974231531-c6227db76b6e?auto=format&fit=crop&w=1600&q=80',
    formats: ['BIM', 'DWG', 'PDF'],
  },
  {
    id: 'c5',
    tier: 'Diamond',
    title: 'Mountain Lodge Diamond',
    subtitle: 'Alto padrão, grandes vãos e áreas sociais pensadas para receber.',
    areaM2: 420,
    tags: ['Diamond', '6 Quartos'],
    price: 9999,
    imageUrl:
      'https://images.unsplash.com/photo-1455587734955-081b22074882?auto=format&fit=crop&w=1600&q=80',
    formats: ['BIM', 'DWG', 'PDF'],
  },
];

export default function ChaletsSection() {
  return (
    <ProductCarouselSection
      title="Chalés e Casas de Montanha"
      subtitle="Perfeitos para natureza e lazer"
      products={chalets}
      enableTiers={false}
      defaultTier="Prime"
      footerCtaLabel="Ver todos os chalés"
      onFooterCtaClick={() => console.log('ir para listagem de chalés')}
      onViewDetails={p => console.log('abrir detalhes do chalé', p.id)}
      actionLabel="Ver detalhes"
    />
  );
}
