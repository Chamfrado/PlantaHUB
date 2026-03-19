import { CheckCircle2, CreditCard, Loader2 } from 'lucide-react';
import { useEffect, useMemo, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useToast } from '../../components/ui/use-toast';
import { getApiErrorMessage } from '../../lib/api-error';
import { getMyOrders, payMock } from '../../services/order.service';
import type { OrderResponseDTO } from '../../types/api/order';

export default function OrderDetailsPage() {
  const { orderId = '' } = useParams();

  const [orders, setOrders] = useState<OrderResponseDTO[]>([]);
  const [loading, setLoading] = useState(true);
  const [paying, setPaying] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const { showToast } = useToast();

  async function loadOrders() {
    try {
      setLoading(true);
      setError(null);
      const response = await getMyOrders();
      setOrders(response);
    } catch (err) {
      console.error(err);
      setError('Não foi possível carregar o pedido.');
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void loadOrders();
  }, []);

  const order = useMemo(() => orders.find(item => item.id === orderId) ?? null, [orders, orderId]);

  async function handlePayMock() {
    if (!order) return;

    try {
      setPaying(true);
      setError(null);
      const updated = await payMock(order.id);
      setOrders(prev => prev.map(item => (item.id === updated.id ? updated : item)));
      showToast({
        variant: 'success',
        title: 'Pagamento confirmado',
        description: 'Seu pedido foi pago com sucesso.',
      });
    } catch (error) {
      const message = getApiErrorMessage(error, 'Não foi possível concluir o pagamento mock.');

      setError(message);
      showToast({
        variant: 'error',
        title: 'Erro no pagamento',
        description: message,
      });
    } finally {
      setPaying(false);
    }
  }

  if (loading) {
    return (
      <div className="mx-auto max-w-5xl px-6 py-16">
        <div className="flex items-center gap-3 text-neutral-600">
          <Loader2 className="h-5 w-5 animate-spin" />
          Carregando pedido...
        </div>
      </div>
    );
  }

  if (!order) {
    return (
      <div className="mx-auto max-w-5xl px-6 py-16">
        <div className="rounded-3xl border border-neutral-200 bg-white p-10 text-center">
          <h1 className="text-2xl font-extrabold text-neutral-900">Pedido não encontrado</h1>
          <p className="mt-2 text-neutral-600">
            Verifique se o pedido existe ou se já foi removido.
          </p>
        </div>
      </div>
    );
  }

  const isPaid = order.status?.toUpperCase() === 'PAID';

  return (
    <div className="bg-white">
      <div className="mx-auto max-w-5xl px-6 py-12">
        <div className="rounded-3xl border border-neutral-200 bg-white p-8 shadow-sm">
          <div className="flex flex-wrap items-start justify-between gap-4">
            <div>
              <h1 className="text-3xl font-extrabold text-neutral-900">Pedido #{order.id}</h1>
              <p className="mt-2 text-neutral-600">
                Criado em {new Date(order.createdAt).toLocaleString('pt-BR')}
              </p>
            </div>

            <span
              className={[
                'rounded-full px-4 py-2 text-sm font-bold',
                isPaid
                  ? 'bg-emerald-50 text-emerald-700 border border-emerald-100'
                  : 'bg-orange-50 text-primary-700 border border-orange-100',
              ].join(' ')}
            >
              {order.status}
            </span>
          </div>

          {error ? (
            <div className="mt-6 rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
              {error}
            </div>
          ) : null}

          <div className="mt-8 space-y-4">
            {order.items.map(item => (
              <div
                key={item.id}
                className="rounded-2xl border border-neutral-200 bg-neutral-50 p-5"
              >
                <div className="flex items-center justify-between gap-4">
                  <div>
                    <div className="text-sm font-semibold text-neutral-500">
                      Produto ID: {item.productId}
                    </div>

                    <div className="mt-3 flex flex-wrap gap-2">
                      {item.selections.map(selection => (
                        <span
                          key={`${item.id}-${selection.planTypeCode}`}
                          className="rounded-full border border-orange-100 bg-white px-3 py-1 text-xs font-bold text-primary-600"
                        >
                          {selection.planTypeCode}
                        </span>
                      ))}
                    </div>
                  </div>

                  <div className="text-lg font-extrabold text-neutral-900">
                    {formatMoney(item.totalCents, order.currency)}
                  </div>
                </div>
              </div>
            ))}
          </div>

          <div className="mt-8 flex flex-wrap items-center justify-between gap-4 rounded-2xl border border-neutral-200 bg-neutral-50 p-5">
            <div>
              <div className="text-sm font-semibold text-neutral-500">Total do pedido</div>
              <div className="text-3xl font-extrabold text-neutral-900">
                {formatMoney(order.totalCents, order.currency)}
              </div>
            </div>

            {isPaid ? (
              <div className="inline-flex items-center gap-2 rounded-2xl bg-emerald-50 px-4 py-3 font-semibold text-emerald-700">
                <CheckCircle2 className="h-5 w-5" />
                Pagamento confirmado
              </div>
            ) : (
              <button
                type="button"
                onClick={handlePayMock}
                disabled={paying}
                className="inline-flex items-center gap-2 rounded-2xl bg-primary-500 px-5 py-3 font-semibold text-white transition hover:bg-primary-600 disabled:opacity-50"
              >
                {paying ? (
                  <Loader2 className="h-4 w-4 animate-spin" />
                ) : (
                  <CreditCard className="h-4 w-4" />
                )}
                Pagar com mock
              </button>
            )}
          </div>

          {isPaid ? (
            <div className="mt-6">
              <Link
                to="/biblioteca"
                className="inline-flex rounded-2xl border border-neutral-300 bg-white px-5 py-3 font-semibold text-neutral-900 transition hover:bg-neutral-100"
              >
                Ir para minha biblioteca
              </Link>
            </div>
          ) : null}
        </div>
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
