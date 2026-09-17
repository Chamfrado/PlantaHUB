import { useQuery } from '@tanstack/react-query';
import { ArrowLeft } from 'lucide-react';
import { Link, useParams } from 'react-router-dom';
import ProductDetailsView from '../../../components/products/ProductDetailsView';
import { DataError } from '../../../components/admin/ui/primitives';
import { getApiErrorMessage } from '../../../lib/api-error';
import { mapProductDetail } from '../../../mappers/product.mapper';
import { previewProduct } from '../../../services/admin/admin.service';

/**
 * Pré-visualização.
 *
 * Renderiza o **mesmo** `ProductDetailsView` da página pública, alimentado pelos mesmos
 * DTOs — o endpoint de preview só difere por não filtrar por status. Uma tela de preview
 * própria poderia divergir da página real sem ninguém perceber, que é justamente o que uma
 * pré-visualização não pode fazer.
 */
export default function ProductPreviewPage() {
  const { productId = '' } = useParams();

  const preview = useQuery({
    queryKey: ['admin', 'preview', productId],
    queryFn: () => previewProduct(productId),
  });

  if (preview.isLoading) {
    return <div className="h-96 animate-pulse rounded-2xl bg-white" />;
  }

  if (preview.isError || !preview.data) {
    return (
      <DataError
        message={getApiErrorMessage(preview.error, 'Não foi possível carregar a pré-visualização.')}
        onRetry={() => void preview.refetch()}
      />
    );
  }

  const product = mapProductDetail(preview.data.product);

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <Link
          to={`/admin/produtos/${encodeURIComponent(productId)}`}
          className="inline-flex items-center gap-1.5 text-sm font-semibold text-neutral-500 transition hover:text-neutral-900"
        >
          <ArrowLeft className="h-4 w-4" /> Voltar ao editor
        </Link>
      </div>

      <div className="rounded-xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm font-semibold text-amber-800">
        Pré-visualização — {product.status === 'PUBLISHED' ? 'publicado' : 'não visível ao público'}
      </div>

      <div className="overflow-hidden rounded-2xl border border-neutral-200 bg-white">
        <ProductDetailsView
          product={product}
          planTypes={preview.data.planTypes}
          selectedCodes={[]}
          ownedCodes={[]}
          loadingOwnedCodes={false}
          loadingPlanTypes={false}
          submitting={null}
          error={null}
          onToggle={() => {}}
          onBuyNow={() => {}}
          onAddToCart={() => {}}
          readOnly
        />
      </div>
    </div>
  );
}
