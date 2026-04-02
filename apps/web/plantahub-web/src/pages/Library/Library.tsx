import { BookOpen, Download, Loader2 } from 'lucide-react';
import { useEffect, useMemo, useState } from 'react';
import LibraryProductCard from '../../components/library/LibraryProductCard';
import { useToast } from '../../components/ui/use-toast';
import { getApiErrorMessage } from '../../lib/api-error';
import { createDownloadBundle } from '../../services/download.service';
import { getMyLibrary } from '../../services/library.service';
import { getProductPlanTypes } from '../../services/products.service';
import type { LibraryProductDTO } from '../../types/api/library';
import type { PlanTypeOptionDTO } from '../../types/api/product';

type LibraryProductViewModel = LibraryProductDTO & {
  availablePlanTypes: PlanTypeOptionDTO[];
};

export default function LibraryPage() {
  const { showToast } = useToast();

  const [items, setItems] = useState<LibraryProductViewModel[]>([]);
  const [selectedKeys, setSelectedKeys] = useState<string[]>([]);
  const [loading, setLoading] = useState(true);
  const [downloadingBundle, setDownloadingBundle] = useState(false);
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
            } catch (error) {
              console.error(`Erro ao carregar plan types de ${product.slug}`, error);

              return {
                ...product,
                availablePlanTypes: [],
              };
            }
          })
        );

        setItems(enriched);
      } catch (error) {
        console.error(error);
        setError('Não foi possível carregar sua biblioteca.');
      } finally {
        setLoading(false);
      }
    }

    void loadLibrary();
  }, []);

  function toggleBundleSelection(productId: string, planTypeCode: string) {
    const normalizedKey = `${productId}:${planTypeCode.toUpperCase()}`;

    setSelectedKeys(prev =>
      prev.includes(normalizedKey)
        ? prev.filter(item => item !== normalizedKey)
        : [...prev, normalizedKey]
    );
  }

  const selectedCount = selectedKeys.length;

  const bundlePayload = useMemo(() => {
    const grouped = new Map<string, Set<string>>();

    selectedKeys.forEach(key => {
      const [productId, planTypeCode] = key.split(':');

      if (!productId || !planTypeCode) return;

      if (!grouped.has(productId)) {
        grouped.set(productId, new Set());
      }

      grouped.get(productId)?.add(planTypeCode.toUpperCase());
    });

    return {
      items: Array.from(grouped.entries()).map(([productId, codes]) => ({
        productId,
        planTypeCodes: Array.from(codes),
      })),
    };
  }, [selectedKeys]);

  async function handleDownloadBundle() {
    if (!bundlePayload.items.length) {
      showToast({
        variant: 'info',
        title: 'Selecione ao menos uma planta',
        description: 'Marque um ou mais itens adquiridos para gerar o ZIP.',
      });
      return;
    }

    try {
      setDownloadingBundle(true);

      const response = await createDownloadBundle(bundlePayload);

      window.open(response.url, '_blank', 'noopener,noreferrer');

      showToast({
        variant: 'success',
        title: 'Bundle gerado com sucesso',
        description: 'O download do ZIP foi iniciado.',
      });
    } catch (error) {
      console.error(error);
      showToast({
        variant: 'error',
        title: 'Erro ao gerar bundle',
        description: getApiErrorMessage(error, 'Não foi possível gerar o ZIP de download.'),
      });
    } finally {
      setDownloadingBundle(false);
    }
  }

  return (
    <section className="min-h-[calc(100vh-64px)] bg-neutral-50">
      <div className="mx-auto max-w-7xl px-6 py-10 md:py-14">
        <div className="flex flex-col gap-6 lg:flex-row lg:items-end lg:justify-between">
          <div className="max-w-3xl">
            <span className="inline-flex items-center rounded-full bg-orange-50 px-4 py-2 text-sm font-medium text-primary-600">
              Minha biblioteca
            </span>

            <h1 className="mt-5 text-3xl font-extrabold tracking-tight text-neutral-900 md:text-4xl">
              Seus projetos adquiridos
            </h1>

            <p className="mt-3 text-base leading-relaxed text-neutral-600 md:text-lg">
              Acesse os produtos já comprados, visualize os tipos de planta disponíveis na sua conta
              e veja o que ainda pode ser adquirido para cada projeto.
            </p>
          </div>

          {!loading && items.length ? (
            <div className="rounded-3xl border border-neutral-200 bg-white p-5 shadow-sm">
              <div className="text-sm font-semibold text-neutral-500">
                Selecionados para bundle ZIP
              </div>
              <div className="mt-1 text-3xl font-extrabold text-neutral-900">{selectedCount}</div>

              <button
                type="button"
                onClick={handleDownloadBundle}
                disabled={downloadingBundle}
                className="mt-4 inline-flex w-full items-center justify-center gap-2 rounded-2xl bg-primary-500 px-5 py-3 font-semibold text-white transition hover:bg-primary-600 disabled:opacity-50"
              >
                {downloadingBundle ? (
                  <Loader2 className="h-4 w-4 animate-spin" />
                ) : (
                  <Download className="h-4 w-4" />
                )}
                Baixar ZIP
              </button>
            </div>
          ) : null}
        </div>

        {loading ? (
          <div className="mt-10 rounded-2xl border border-neutral-200 bg-white p-8 shadow-sm">
            <p className="text-sm text-neutral-500">Carregando sua biblioteca...</p>
          </div>
        ) : error ? (
          <div className="mt-10 rounded-2xl border border-red-200 bg-white p-8 shadow-sm">
            <p className="text-sm font-medium text-red-600">{error}</p>
          </div>
        ) : !items.length ? (
          <div className="mt-10 rounded-2xl border border-neutral-200 bg-white p-10 text-center shadow-sm">
            <div className="mx-auto inline-flex h-14 w-14 items-center justify-center rounded-2xl bg-orange-50 text-primary-600">
              <BookOpen className="h-6 w-6" />
            </div>

            <h2 className="mt-4 text-xl font-extrabold text-neutral-900">
              Sua biblioteca ainda está vazia
            </h2>

            <p className="mt-2 text-sm text-neutral-600">
              Quando você adquirir projetos, eles aparecerão aqui com os tipos de planta liberados
              para download.
            </p>
          </div>
        ) : (
          <div className="mt-10 space-y-6">
            {items.map(product => (
              <LibraryProductCard
                key={product.productId}
                product={product}
                selectedCodes={selectedKeys}
                onToggleBundleSelection={toggleBundleSelection}
              />
            ))}
          </div>
        )}
      </div>
    </section>
  );
}
