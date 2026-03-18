import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import ProductCarouselSection from '../../components/products/ProductCarouselSection';
import { mapProductSummaryToProduct } from '../../mappers/product.mapper';
import { listProducts } from '../../services/products.service';
import type { Product } from '../../types/ProductData';

export default function ResidentialHouses() {
  const [houses, setHouses] = useState<Product[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const navigate = useNavigate();

  function handleViewProduct(category: string, slug: string) {
    navigate(`/${category}/${slug}`);
  }

  function handleViewAllHouses() {
    navigate(`/produtos?category=casas`);
  }

  useEffect(() => {
    async function loadHouses() {
      try {
        setLoading(true);
        setError(null);

        const response = await listProducts({
          category: 'casas',
          limit: 3,
        });

        setHouses(response.map(mapProductSummaryToProduct));
      } catch (err) {
        console.error(err);
        setError('Não foi possível carregar as casas.');
      } finally {
        setLoading(false);
      }
    }

    loadHouses();
  }, []);

  if (loading) {
    return (
      <section className="bg-white">
        <div className="max-w-7xl mx-auto px-6 py-16 md:py-20">
          <p className="text-neutral-500">Carregando casas...</p>
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
      title="Casas Residenciais"
      subtitle="Designs modernos para uma vida confortável"
      products={houses}
      enableTiers={false}
      limit={3}
      defaultTier="confort"
      footerCtaLabel="Ver todas as casas"
      onFooterCtaClick={() => handleViewAllHouses()}
      onViewDetails={p => handleViewProduct(p.category, p.slug)}
      actionLabel="Ver detalhes"
    />
  );
}
