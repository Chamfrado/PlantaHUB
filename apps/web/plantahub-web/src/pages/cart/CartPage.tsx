import { Loader2, ShoppingBag, Trash2 } from 'lucide-react';
import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import {
  checkoutFromCart,
  clearCart,
  getMyCart,
  removeCartItem,
} from '../../services/cart.service';
import type { CartResponse } from '../../types/api/cart';

export default function CartPage() {
  const navigate = useNavigate();

  const [cart, setCart] = useState<CartResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [busyItemId, setBusyItemId] = useState<string | null>(null);
  const [checkingOut, setCheckingOut] = useState(false);
  const [clearing, setClearing] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function loadCart() {
    try {
      setLoading(true);
      setError(null);
      const response = await getMyCart();
      setCart(response);
    } catch (err) {
      console.error(err);
      setError('Não foi possível carregar seu carrinho.');
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void loadCart();
  }, []);

  async function handleRemove(itemId: string) {
    try {
      setBusyItemId(itemId);
      await removeCartItem(itemId);
      await loadCart();
    } catch (err) {
      console.error(err);
      setError('Não foi possível remover o item.');
    } finally {
      setBusyItemId(null);
    }
  }

  async function handleClearCart() {
    try {
      setClearing(true);
      await clearCart();
      await loadCart();
    } catch (err) {
      console.error(err);
      setError('Não foi possível limpar o carrinho.');
    } finally {
      setClearing(false);
    }
  }

  async function handleCheckout() {
    try {
      setCheckingOut(true);
      setError(null);
      const order = await checkoutFromCart();
      navigate(`/pedidos/${order.id}`);
    } catch (err) {
      console.error(err);
      setError('Não foi possível finalizar o checkout.');
    } finally {
      setCheckingOut(false);
    }
  }

  if (loading) {
    return (
      <div className="mx-auto max-w-6xl px-6 py-16">
        <div className="flex items-center gap-3 text-neutral-600">
          <Loader2 className="h-5 w-5 animate-spin" />
          Carregando carrinho...
        </div>
      </div>
    );
  }

  const items = cart?.items ?? [];
  const isEmpty = items.length === 0;

  return (
    <div className="bg-white">
      <div className="mx-auto max-w-6xl px-6 py-12">
        <div className="flex flex-wrap items-center justify-between gap-4">
          <div>
            <h1 className="text-3xl font-extrabold text-neutral-900">Carrinho</h1>
            <p className="mt-2 text-neutral-600">
              Revise os itens selecionados antes de seguir para o checkout.
            </p>
          </div>

          {!isEmpty ? (
            <button
              type="button"
              onClick={handleClearCart}
              disabled={clearing}
              className="rounded-2xl border border-neutral-300 bg-white px-4 py-2 font-semibold text-neutral-900 transition hover:bg-neutral-100 disabled:opacity-50"
            >
              {clearing ? 'Limpando...' : 'Limpar carrinho'}
            </button>
          ) : null}
        </div>

        {error ? (
          <div className="mt-6 rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
            {error}
          </div>
        ) : null}

        {isEmpty ? (
          <div className="mt-8 rounded-3xl border border-neutral-200 bg-neutral-50 p-10 text-center">
            <ShoppingBag className="mx-auto h-10 w-10 text-neutral-400" />
            <h2 className="mt-4 text-2xl font-extrabold text-neutral-900">
              Seu carrinho está vazio
            </h2>
            <p className="mt-2 text-neutral-600">
              Adicione um produto para continuar com sua compra.
            </p>
            <Link
              to="/produtos"
              className="mt-6 inline-flex rounded-2xl bg-primary-500 px-5 py-3 font-semibold text-white transition hover:bg-primary-600"
            >
              Explorar produtos
            </Link>
          </div>
        ) : (
          <div className="mt-8 grid gap-6 lg:grid-cols-[1.2fr_360px]">
            <div className="space-y-4">
              {items.map(item => (
                <div
                  key={item.itemId}
                  className="rounded-3xl border border-neutral-200 bg-white p-5 shadow-sm"
                >
                  <div className="flex items-start justify-between gap-4">
                    <div className="min-w-0">
                      <h2 className="text-xl font-extrabold text-neutral-900">{item.name}</h2>
                      <p className="mt-1 text-sm text-neutral-600">{item.shortDescription}</p>

                      <div className="mt-4 flex flex-wrap gap-2">
                        {item.selections.map(selection => (
                          <span
                            key={`${item.itemId}-${selection.code}`}
                            className="rounded-full border border-orange-100 bg-orange-50 px-3 py-1 text-xs font-bold text-primary-600"
                          >
                            {selection.name}
                          </span>
                        ))}
                      </div>
                    </div>

                    <div className="text-right">
                      <div className="text-lg font-extrabold text-neutral-900">
                        {formatMoney(item.itemTotalCents, cart?.currency ?? 'BRL')}
                      </div>

                      <button
                        type="button"
                        onClick={() => handleRemove(item.itemId)}
                        disabled={busyItemId === item.itemId}
                        className="mt-4 inline-flex items-center gap-2 rounded-xl border border-neutral-300 bg-white px-3 py-2 text-sm font-semibold text-neutral-700 transition hover:bg-neutral-100 disabled:opacity-50"
                      >
                        {busyItemId === item.itemId ? (
                          <Loader2 className="h-4 w-4 animate-spin" />
                        ) : (
                          <Trash2 className="h-4 w-4" />
                        )}
                        Remover
                      </button>
                    </div>
                  </div>
                </div>
              ))}
            </div>

            <aside className="h-fit lg:sticky lg:top-24">
              <div className="rounded-3xl border border-neutral-200 bg-neutral-50 p-6 shadow-sm">
                <h3 className="text-xl font-extrabold text-neutral-900">Resumo</h3>

                <div className="mt-5 flex items-center justify-between">
                  <span className="text-sm font-semibold text-neutral-600">Itens</span>
                  <span className="font-bold text-neutral-900">{items.length}</span>
                </div>

                <div className="mt-3 flex items-center justify-between">
                  <span className="text-sm font-semibold text-neutral-600">Total</span>
                  <span className="text-2xl font-extrabold text-neutral-900">
                    {formatMoney(cart?.totalCents ?? 0, cart?.currency ?? 'BRL')}
                  </span>
                </div>

                <button
                  type="button"
                  onClick={handleCheckout}
                  disabled={checkingOut}
                  className="mt-6 inline-flex w-full items-center justify-center rounded-2xl bg-primary-500 px-5 py-3 font-semibold text-white transition hover:bg-primary-600 disabled:opacity-50"
                >
                  {checkingOut ? 'Processando...' : 'Ir para pagamento'}
                </button>
              </div>
            </aside>
          </div>
        )}
      </div>
    </div>
  );
}

function formatMoney(valueInCents: number, currency: string) {
  const resolvedCurrency =
    currency === 'USD' || currency === 'EUR' || currency === 'BRL' ? currency : 'BRL';

  return (valueInCents / 100).toLocaleString(resolvedCurrency === 'BRL' ? 'pt-BR' : 'en-US', {
    style: 'currency',
    currency: resolvedCurrency,
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  });
}
