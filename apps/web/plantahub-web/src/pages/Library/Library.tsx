import { BookOpen } from 'lucide-react';
import { useEffect, useState } from 'react';
import LibraryProductCard from '../../components/library/LibraryProductCard';
import { getMyLibrary } from '../../services/library.service';
import { getProductPlanTypes } from '../../services/products.service';
import type { LibraryProductDTO } from '../../types/api/library';
import type { PlanTypeOptionDTO } from '../../types/api/product';

type LibraryProductViewModel = LibraryProductDTO & {
  availablePlanTypes: PlanTypeOptionDTO[];
};

export default function LibraryPage() {
  const [items, setItems] = useState<LibraryProductViewModel[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    async function loadLibrary() {
      try {
        setLoading(true);
        setError(null);

        const library = await getMyLibrary();

        const enriched = await Promise.all(
          library.map(async product => {
            try {
              const allPlanTypes = await getProductPlanTypes(product.category, product.slug);

              return {
                ...product,
                availablePlanTypes: allPlanTypes,
              };
            } catch (err) {
              console.error(`Erro ao carregar plan types de ${product.slug}`, err);

              return {
                ...product,
                availablePlanTypes: [],
              };
            }
          })
        );

        setItems(enriched);
      } catch (err) {
        console.error(err);
        setError('Não foi possível carregar sua livraria.');
      } finally {
        setLoading(false);
      }
    }

    loadLibrary();
  }, []);

  return (
    <section className="min-h-[calc(100vh-64px)] bg-neutral-50">
      <div className="max-w-7xl mx-auto px-6 py-10 md:py-14">
        <div className="max-w-3xl">
          <span className="inline-flex items-center rounded-full bg-orange-50 px-4 py-2 text-sm font-medium text-primary-600">
            Minha livraria
          </span>

          <h1 className="mt-5 text-3xl md:text-4xl font-extrabold tracking-tight text-neutral-900">
            Seus projetos adquiridos
          </h1>

          <p className="mt-3 text-base md:text-lg text-neutral-600 leading-relaxed">
            Acesse os produtos já comprados, visualize os tipos de planta disponíveis na sua conta e
            veja o que ainda pode ser adquirido para cada projeto.
          </p>
        </div>

        {loading ? (
          <div className="mt-10 rounded-2xl border border-neutral-200 bg-white p-8 shadow-sm">
            <p className="text-sm text-neutral-500">Carregando sua livraria...</p>
          </div>
        ) : error ? (
          <div className="mt-10 rounded-2xl border border-red-200 bg-white p-8 shadow-sm">
            <p className="text-sm font-medium text-red-600">{error}</p>
          </div>
        ) : !items.length ? (
          <div className="mt-10 rounded-2xl border border-neutral-200 bg-white p-10 shadow-sm text-center">
            <div className="mx-auto inline-flex h-14 w-14 items-center justify-center rounded-2xl bg-orange-50 text-primary-600">
              <BookOpen className="h-6 w-6" />
            </div>

            <h2 className="mt-4 text-xl font-extrabold text-neutral-900">
              Sua livraria ainda está vazia
            </h2>

            <p className="mt-2 text-sm text-neutral-600">
              Quando você adquirir projetos, eles aparecerão aqui com os tipos de planta liberados
              para download.
            </p>
          </div>
        ) : (
          <div className="mt-10 space-y-6">
            {items.map(product => (
              <LibraryProductCard key={product.productId} product={product} />
            ))}
          </div>
        )}
      </div>
    </section>
  );
}
