import { useNavigate } from 'react-router-dom';
import { useAsync } from '../../hooks/useAsync';
import { mapProductSummary } from '../../mappers/product.mapper';
import { listProducts } from '../../services/products.service';
import ProductCarouselSection from '../products/ProductCarouselSection';

type Props = {
  categorySlug: string;
  title: string;
  subtitle?: string;
  ctaLabel?: string;
  limit?: number;
};

/**
 * Vitrine de uma categoria na home.
 *
 * Substitui `ResidentialHouses` e `ChaletsSection`, que eram byte a byte idênticos exceto
 * pelos textos e pela categoria — que estavam escritos dentro do corpo de cada componente.
 * Agora a categoria é um parâmetro, e a home decide quais exibir.
 */
export default function CategoryShowcase({
  categorySlug,
  title,
  subtitle,
  ctaLabel,
  limit = 3,
}: Props) {
  const navigate = useNavigate();

  const { data, loading, error } = useAsync(`home:${categorySlug}`, () =>
    listProducts({ category: categorySlug, limit })
  );

  if (loading) {
    return (
      <section className="bg-white">
        <div className="max-w-7xl mx-auto px-6 py-16 md:py-20">
          <div className="h-8 w-64 rounded bg-neutral-100 animate-pulse" />
          <div className="mt-10 grid gap-6 lg:grid-cols-3">
            {[0, 1, 2].map(i => (
              <div key={i} className="h-96 rounded-2xl bg-neutral-100 animate-pulse" />
            ))}
          </div>
        </div>
      </section>
    );
  }

  // Uma vitrine que não carrega não pode derrubar a home inteira: ela simplesmente não
  // aparece, e o resto da página continua servindo.
  if (error || !data || data.length === 0) {
    return null;
  }

  return (
    <ProductCarouselSection
      title={title}
      subtitle={subtitle}
      products={data.map(mapProductSummary)}
      limit={limit}
      footerCtaLabel={ctaLabel ?? `Ver tudo em ${title}`}
      onFooterCtaClick={() => navigate(`/produtos?category=${encodeURIComponent(categorySlug)}`)}
      onViewDetails={p => navigate(`/${p.category}/${p.slug}`)}
      actionLabel="Ver detalhes"
    />
  );
}
