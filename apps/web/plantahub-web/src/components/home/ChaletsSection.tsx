import { useEffect, useState } from 'react';
import { mapProductSummaryToProduct } from '../../mappers/product.mapper';
import { listProducts } from '../../services/products.service';
import type { Product } from '../../types/ProductData';
import ProductCarouselSection from '../products/ProductCarouselSection';

export default function ChaletsSection() {
  const [chalets, setChalets] = useState<Product[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    async function loadChalets() {
      try {
        setLoading(true);
        setError(null);

        const response = await listProducts({
          category: 'chales',
          limit: 3,
        });

        setChalets(response.map(mapProductSummaryToProduct));
      } catch (err) {
        console.error(err);
        setError('Não foi possível carregar os chalés.');
      } finally {
        setLoading(false);
      }
    }

    loadChalets();
  }, []);

  if (loading) {
    return (
      <section className="bg-white">
        <div className="max-w-7xl mx-auto px-6 py-16 md:py-20">
          <p className="text-neutral-500">Carregando chalés...</p>
        </div>
      </section>
    );
  }

  if (error) {
    return (
      <section className="bg-white">
        <div className="max-w-7xl mx-auto px-6 py-16 md:py-20">
          <p className="text-red-500">{error}</p>
        </div>
      </section>
    );
  }

  return (
    <ProductCarouselSection
      title="Chalés e Casas de Montanha"
      subtitle="Perfeitos para natureza e lazer"
      products={chalets}
      enableTiers={false}
      limit={3}
      defaultTier="prime"
      footerCtaLabel="Ver todos os chalés"
      onFooterCtaClick={() => console.log('ir para listagem de chalés')}
      onViewDetails={p => console.log('abrir detalhes do chalé', p.id)}
      actionLabel="Ver detalhes"
    />
  );
}
