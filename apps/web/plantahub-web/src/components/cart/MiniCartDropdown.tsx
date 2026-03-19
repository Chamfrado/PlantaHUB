import { X } from 'lucide-react';
import { Link } from 'react-router-dom';
import { useCart } from '../../app/providers/useCart';
import { removeCartItem } from '../../services/cart.service';
import { formatCurrency } from '../../utils/format';
import { useToast } from '../ui/use-toast';

type Props = {
  onClose: () => void;
};

export default function MiniCartDropdown({ onClose }: Props) {
  const { cart, loadingCart, refreshCart, setCart } = useCart();
  const { showToast } = useToast();

  if (loadingCart) {
    return (
      <div className="w-80 rounded-2xl border bg-white p-4 shadow-xl">
        <p className="text-sm text-neutral-500">Carregando carrinho...</p>
      </div>
    );
  }

  if (!cart || cart.items.length === 0) {
    return (
      <div className="w-80 rounded-2xl border bg-white p-6 shadow-xl text-center">
        <p className="font-semibold text-neutral-800">Seu carrinho está vazio</p>
        <Link
          to="/"
          onClick={onClose}
          className="mt-4 inline-block text-sm text-primary-600 hover:underline"
        >
          Ver produtos
        </Link>
      </div>
    );
  }

  async function handleRemove(itemId: string) {
    if (!cart) return;

    // 🔥 remove instantaneamente da UI
    const previousCart = cart;

    setCart(prev =>
      prev
        ? {
            ...prev,
            items: prev.items.filter(i => i.itemId !== itemId),
          }
        : prev
    );

    try {
      await removeCartItem(itemId);

      showToast({
        variant: 'success',
        title: 'Item removido',
      });
    } catch (error) {
      console.error(error);

      setCart(previousCart);

      showToast({
        variant: 'error',
        title: 'Erro ao remover item',
      });
    } finally {
      await refreshCart();
    }
  }
  return (
    <div className="w-80 rounded-2xl border bg-white shadow-xl">
      {/* HEADER */}
      <div className="flex items-center justify-between border-b px-4 py-3">
        <span className="font-bold text-neutral-800">Seu carrinho</span>
        <button onClick={onClose}>
          <X className="h-4 w-4 text-neutral-500" />
        </button>
      </div>

      {/* ITEMS */}
      <div className="max-h-64 overflow-y-auto px-4 py-3 space-y-3">
        {cart.items.map(item => (
          <div key={item.itemId} className="flex gap-3 items-start">
            <img
              src={item.heroImageUrl}
              alt={item.name}
              className="h-12 w-12 rounded-lg object-cover"
            />

            <div className="flex-1 min-w-0">
              <p className="text-sm font-semibold text-neutral-800 truncate">{item.name}</p>

              <p className="text-xs text-neutral-500">
                {item.selections.map(s => s.name).join(', ')}
              </p>

              <p className="mt-1 text-sm font-bold text-neutral-900">
                {formatCurrency(item.itemTotalCents)}
              </p>
            </div>
            <button
              onClick={() => handleRemove(item.itemId)}
              className="rounded-lg p-1 text-neutral-400 hover:bg-neutral-100 hover:text-red-500 transition"
            >
              <X className="h-4 w-4" />
            </button>
          </div>
        ))}
      </div>

      {/* FOOTER */}
      <div className="border-t px-4 py-3 space-y-3">
        <div className="flex justify-between text-sm font-semibold text-neutral-800">
          <span>Total</span>
          <span>{formatCurrency(cart.totalCents)}</span>
        </div>

        <div className="flex gap-2">
          <Link
            to="/carrinho"
            onClick={onClose}
            className="flex-1 rounded-xl border px-3 py-2 text-center text-sm font-semibold hover:bg-neutral-50"
          >
            Ver carrinho
          </Link>

          <Link
            to="/checkout"
            onClick={onClose}
            className="flex-1 rounded-xl bg-primary-500 px-3 py-2 text-center text-sm font-bold text-white hover:bg-primary-600"
          >
            Finalizar
          </Link>
        </div>
      </div>
    </div>
  );
}
