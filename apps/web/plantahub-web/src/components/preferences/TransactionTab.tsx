import { CreditCard } from 'lucide-react';
import { useEffect, useMemo, useState } from 'react';
import { getMyOrders } from '../../services/order.service';
import type { OrderResponseDTO } from '../../types/api/order';

type TransactionRow = {
  id: string;
  orderDate: string;
  paymentConfirmedAt: string | null;
  product: string;
  totalCents: number;
  status: string;
};

export default function TransactionsTab() {
  const [orders, setOrders] = useState<OrderResponseDTO[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    async function loadOrders() {
      try {
        setLoading(true);
        setError(null);

        const response = await getMyOrders();
        setOrders(response);
      } catch (err) {
        console.error(err);
        setError('Não foi possível carregar o histórico de transações.');
      } finally {
        setLoading(false);
      }
    }

    loadOrders();
  }, []);

  const rows = useMemo<TransactionRow[]>(() => {
    return orders.map(order => ({
      id: order.id,
      orderDate: order.createdAt,
      paymentConfirmedAt: order.paidAt ?? null,
      product: buildProductLabel(order),
      totalCents: order.totalCents,
      status: order.status,
    }));
  }, [orders]);

  if (loading) {
    return <CardSkeleton text="Carregando transações..." />;
  }

  return (
    <SectionCard
      icon={<CreditCard className="h-5 w-5" />}
      title="Histórico de transações"
      description="Acompanhe seus pedidos e a confirmação dos pagamentos."
    >
      {error ? <p className="mb-4 text-sm font-medium text-red-600">{error}</p> : null}

      {!rows.length ? (
        <div className="rounded-2xl border border-neutral-200 bg-neutral-50 p-6 text-sm text-neutral-600">
          Você ainda não possui transações registradas.
        </div>
      ) : (
        <div className="overflow-x-auto rounded-2xl border border-neutral-200">
          <table className="min-w-full border-separate border-spacing-0">
            <thead>
              <tr>
                <Th>Pedido</Th>
                <Th>Data do pedido</Th>
                <Th>Pagamento confirmado</Th>
                <Th>Produto</Th>
                <Th>Status</Th>
                <Th align="right">Valor total</Th>
              </tr>
            </thead>

            <tbody>
              {rows.map(row => (
                <tr key={row.id} className="bg-white">
                  <Td>{shortOrderId(row.id)}</Td>
                  <Td>{formatDateTime(row.orderDate)}</Td>
                  <Td>{row.paymentConfirmedAt ? formatDateTime(row.paymentConfirmedAt) : '—'}</Td>
                  <Td>{row.product}</Td>
                  <Td>
                    <StatusBadge status={row.status} />
                  </Td>
                  <Td align="right">{formatMoneyFromCents(row.totalCents)}</Td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </SectionCard>
  );
}

function buildProductLabel(order: OrderResponseDTO) {
  if (!order.items?.length) return 'Pedido sem itens';

  if (order.items.length === 1) {
    return order.items[0].productId;
  }

  return `${order.items[0].productId} + ${order.items.length - 1} item(ns)`;
}

function shortOrderId(id: string) {
  return id.slice(0, 8).toUpperCase();
}

function StatusBadge({ status }: { status: string }) {
  const normalized = status.toLowerCase();

  const className =
    normalized === 'paid'
      ? 'bg-green-50 text-green-700 border-green-200'
      : normalized === 'pending'
        ? 'bg-yellow-50 text-yellow-700 border-yellow-200'
        : normalized === 'cancelled'
          ? 'bg-red-50 text-red-700 border-red-200'
          : 'bg-neutral-100 text-neutral-700 border-neutral-200';

  return (
    <span
      className={`inline-flex rounded-full border px-2.5 py-1 text-xs font-semibold ${className}`}
    >
      {status}
    </span>
  );
}

function SectionCard({
  icon,
  title,
  description,
  children,
}: {
  icon: React.ReactNode;
  title: string;
  description: string;
  children: React.ReactNode;
}) {
  return (
    <div className="rounded-2xl border border-neutral-200 bg-white p-6 md:p-8 shadow-sm">
      <div className="flex items-start gap-4">
        <div className="inline-flex h-11 w-11 items-center justify-center rounded-2xl bg-orange-50 text-primary-600">
          {icon}
        </div>

        <div>
          <h2 className="text-xl font-extrabold text-neutral-900">{title}</h2>
          <p className="mt-1 text-sm text-neutral-600">{description}</p>
        </div>
      </div>

      <div className="mt-8">{children}</div>
    </div>
  );
}

function Th({ children, align = 'left' }: { children: React.ReactNode; align?: 'left' | 'right' }) {
  return (
    <th
      className={[
        'border-b border-neutral-200 bg-neutral-50 px-4 py-3 text-xs font-bold uppercase tracking-wide text-neutral-500',
        align === 'right' ? 'text-right' : 'text-left',
      ].join(' ')}
    >
      {children}
    </th>
  );
}

function Td({ children, align = 'left' }: { children: React.ReactNode; align?: 'left' | 'right' }) {
  return (
    <td
      className={[
        'border-b border-neutral-200 px-4 py-4 text-sm text-neutral-700',
        align === 'right' ? 'text-right' : 'text-left',
      ].join(' ')}
    >
      {children}
    </td>
  );
}

function formatMoneyFromCents(value: number) {
  return (value / 100).toLocaleString('pt-BR', {
    style: 'currency',
    currency: 'BRL',
  });
}

function formatDateTime(value: string) {
  return new Date(value).toLocaleString('pt-BR');
}

function CardSkeleton({ text }: { text: string }) {
  return (
    <div className="rounded-2xl border border-neutral-200 bg-white p-8 shadow-sm">
      <p className="text-sm text-neutral-500">{text}</p>
    </div>
  );
}
