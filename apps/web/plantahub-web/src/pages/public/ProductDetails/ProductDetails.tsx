import { useEffect, useMemo, useState } from 'react';
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom';
import { useCart } from '../../../app/providers/useCart';
import ProductDetailsView from '../../../components/products/ProductDetailsView';
import { useToast } from '../../../components/ui/use-toast';
import { useAuth } from '../../../contexts/AuthContext';
import { useAsync } from '../../../hooks/useAsync';
import { getApiErrorMessage } from '../../../lib/api-error';
import { mapProductDetail } from '../../../mappers/product.mapper';
import { addCartItem } from '../../../services/cart.service';
import { getMyLibrary } from '../../../services/library.service';
import { checkoutDirect } from '../../../services/order.service';
import { getProduct, getProductPlanTypes } from '../../../services/products.service';
import { getMyProfileStatus } from '../../../services/profile.service';

type HttpError = Error & { status?: number };

export default function ProductDetails() {
  const { category = '', slug = '' } = useParams();
  const navigate = useNavigate();
  const location = useLocation();

  const { refreshCart } = useCart();
  const { isAuthenticated } = useAuth();
  const { showToast } = useToast();

  const currentPath = location.pathname + location.search;

  const productRequest = useAsync(`product:${category}/${slug}`, () =>
    getProduct(category, slug).then(mapProductDetail)
  );

  // Buscado em paralelo de propósito: só depende da URL, não do produto carregado.
  const planTypesRequest = useAsync(`plan-types:${category}/${slug}`, () =>
    getProductPlanTypes(category, slug)
  );

  const product = productRequest.data;
  // Memoizado: sem isto o `?? []` cria um array novo a cada render e o efeito de
  // pré-seleção passa a rodar em todos eles.
  const planTypes = useMemo(() => planTypesRequest.data ?? [], [planTypesRequest.data]);

  const [selectedCodes, setSelectedCodes] = useState<string[]>([]);
  const [submitting, setSubmitting] = useState<'buy' | 'cart' | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [ownedPlanTypeCodes, setOwnedPlanTypeCodes] = useState<string[]>([]);
  const [loadingOwnedPlanTypes, setLoadingOwnedPlanTypes] = useState(false);

  const productId = product?.id;

  useEffect(() => {
    let active = true;

    async function loadOwnedPlanTypes() {
      if (!isAuthenticated || !productId) {
        if (active) setOwnedPlanTypeCodes([]);
        return;
      }

      try {
        setLoadingOwnedPlanTypes(true);

        const library = await getMyLibrary();
        if (!active) return;

        const libraryItem = library.find(item => item.productId === productId);
        setOwnedPlanTypeCodes(
          libraryItem ? libraryItem.planTypes.map(item => item.code.toUpperCase()) : []
        );
      } catch (error) {
        console.error(error);
        if (active) setOwnedPlanTypeCodes([]);
      } finally {
        if (active) setLoadingOwnedPlanTypes(false);
      }
    }

    void loadOwnedPlanTypes();

    return () => {
      active = false;
    };
  }, [isAuthenticated, productId]);

  // Pré-seleção via ?planTypes=ARCH,HYD
  useEffect(() => {
    if (!planTypes.length) return;

    const preselected = new URLSearchParams(location.search)
      .get('planTypes')
      ?.split(',')
      .map(item => item.trim().toUpperCase())
      .filter(Boolean);

    if (!preselected?.length) return;

    const validCodes = planTypes
      .map(item => item.code.toUpperCase())
      .filter(code => preselected.includes(code) && !ownedPlanTypeCodes.includes(code));

    if (!validCodes.length) return;

    setSelectedCodes(prev => {
      const current = prev.map(code => code.toUpperCase());
      const same =
        current.length === validCodes.length && current.every(code => validCodes.includes(code));
      return same ? prev : validCodes;
    });
  }, [planTypes, location.search, ownedPlanTypeCodes]);

  function togglePlanType(code: string) {
    const normalizedCode = code.toUpperCase();

    if (ownedPlanTypeCodes.includes(normalizedCode)) {
      showToast({
        variant: 'info',
        title: 'Item já adquirido',
        description: 'Esse tipo de planta já está disponível na sua biblioteca.',
      });
      return;
    }

    setSelectedCodes(prev =>
      prev.includes(normalizedCode)
        ? prev.filter(item => item !== normalizedCode)
        : [...prev, normalizedCode]
    );
  }

  async function validatePurchaseFlow() {
    if (!isAuthenticated) {
      navigate(`/login?redirect=${encodeURIComponent(currentPath)}`);
      return false;
    }

    try {
      const profileStatus = await getMyProfileStatus();

      if (!profileStatus.profileCompleted) {
        navigate(`/configs?redirect=${encodeURIComponent(currentPath)}`);
        showToast({
          variant: 'info',
          title: 'Complete seu perfil para continuar',
          description: 'Preencha os dados obrigatórios antes de finalizar sua compra.',
        });
        return false;
      }

      return true;
    } catch (error) {
      console.error(error);

      const message = getApiErrorMessage(error, 'Não foi possível validar seu perfil no momento.');

      setActionError(message);
      showToast({ variant: 'error', title: 'Falha ao validar perfil', description: message });
      return false;
    }
  }

  function purchasableCodes() {
    return selectedCodes.filter(code => !ownedPlanTypeCodes.includes(code.toUpperCase()));
  }

  async function handleAddToCart() {
    if (!product) return;

    const codes = purchasableCodes();

    if (codes.length === 0) {
      setActionError('Selecione pelo menos um tipo de planta disponível para compra.');
      return;
    }

    if (!(await validatePurchaseFlow())) return;

    try {
      setSubmitting('cart');
      setActionError(null);

      // O id vem da resposta da API. Antes vinha de um arquivo TypeScript local, o que
      // exigia que aquele literal fosse idêntico ao id do banco para a compra funcionar.
      await addCartItem({ productId: product.id, planTypeCodes: codes });
      await refreshCart();

      showToast({
        variant: 'success',
        title: 'Produto adicionado ao carrinho',
        description: 'Você já pode revisar os itens e finalizar a compra.',
      });

      navigate('/carrinho');
    } catch (error) {
      const message = getApiErrorMessage(error, 'Não foi possível adicionar o produto ao carrinho.');

      setActionError(message);
      showToast({ variant: 'error', title: 'Erro ao adicionar ao carrinho', description: message });
    } finally {
      setSubmitting(null);
    }
  }

  async function handleBuyNow() {
    if (!product) return;

    const codes = purchasableCodes();

    if (codes.length === 0) {
      setActionError('Selecione pelo menos um tipo de planta disponível para compra.');
      return;
    }

    if (!(await validatePurchaseFlow())) return;

    try {
      setSubmitting('buy');
      setActionError(null);

      const response = await checkoutDirect({
        items: [{ productId: product.id, quantity: 1, planTypeCodes: codes }],
      });

      if (!response.paymentUrl) {
        throw new Error('Link de pagamento não retornado.');
      }

      showToast({
        variant: 'success',
        title: 'Checkout iniciado',
        description: 'Você será redirecionado para o pagamento.',
      });

      window.location.href = response.paymentUrl;
    } catch (error) {
      const message = getApiErrorMessage(error, 'Não foi possível iniciar o checkout.');

      setActionError(message);
      showToast({ variant: 'error', title: 'Erro no checkout', description: message });
    } finally {
      setSubmitting(null);
    }
  }

  if (productRequest.loading) {
    return <ProductDetailsSkeleton />;
  }

  // 404 e falha de rede são situações diferentes: uma é "esse produto não existe", a outra
  // é "tente de novo". Tratá-las igual manda o visitante embora sem motivo.
  if (productRequest.error || !product) {
    const status = (productRequest.error as HttpError | null)?.status;

    return status === 404 ? <NotFound /> : <LoadFailed onRetry={productRequest.reload} />;
  }

  return (
    <ProductDetailsView
      product={product}
      planTypes={planTypes}
      selectedCodes={selectedCodes}
      ownedCodes={ownedPlanTypeCodes}
      loadingOwnedCodes={loadingOwnedPlanTypes}
      loadingPlanTypes={planTypesRequest.loading}
      submitting={submitting}
      error={actionError}
      onToggle={togglePlanType}
      onBuyNow={handleBuyNow}
      onAddToCart={handleAddToCart}
    />
  );
}

function ProductDetailsSkeleton() {
  return (
    <div className="bg-white" aria-busy="true" aria-label="Carregando produto">
      <div className="mx-auto max-w-7xl px-6 py-12">
        <div className="h-4 w-48 rounded bg-neutral-100 animate-pulse" />
        <div className="mt-6 h-10 w-2/3 rounded bg-neutral-100 animate-pulse" />
        <div className="mt-10 grid gap-6 lg:grid-cols-2">
          <div className="h-80 rounded-2xl bg-neutral-100 animate-pulse" />
          <div className="h-80 rounded-2xl bg-neutral-100 animate-pulse" />
        </div>
      </div>
    </div>
  );
}

function NotFound() {
  return (
    <div className="min-h-[60vh] bg-white">
      <div className="mx-auto max-w-6xl px-6 py-16">
        <h1 className="text-2xl font-extrabold text-brand-black">Produto não encontrado</h1>
        <p className="mt-2 text-brand-muted">
          Este projeto não existe ou não está mais disponível.
        </p>
        <Link
          to="/produtos"
          className="mt-6 inline-flex rounded-xl bg-primary-500 px-5 py-2.5 font-semibold text-white transition hover:bg-primary-600"
        >
          Ver todos os projetos
        </Link>
      </div>
    </div>
  );
}

function LoadFailed({ onRetry }: { onRetry: () => void }) {
  return (
    <div className="min-h-[60vh] bg-white">
      <div className="mx-auto max-w-6xl px-6 py-16">
        <h1 className="text-2xl font-extrabold text-brand-black">
          Não foi possível carregar este projeto
        </h1>
        <p className="mt-2 text-brand-muted">Verifique sua conexão e tente novamente.</p>
        <button
          onClick={onRetry}
          className="mt-6 rounded-xl bg-primary-500 px-5 py-2.5 font-semibold text-white transition hover:bg-primary-600"
        >
          Tentar novamente
        </button>
      </div>
    </div>
  );
}
