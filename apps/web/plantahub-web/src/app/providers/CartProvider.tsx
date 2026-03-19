import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react';
import { useAuth } from '../../contexts/AuthContext';
import { CartContext } from '../../contexts/CartContext';
import { getMyCart } from '../../services/cart.service';
import type { CartResponse } from '../../types/api/cart';

export function CartProvider({ children }: { children: ReactNode }) {
  const { isAuthenticated } = useAuth();

  const [cart, setCart] = useState<CartResponse | null>(null);
  const [loadingCart, setLoadingCart] = useState(false);

  const refreshCart = useCallback(async () => {
    if (!isAuthenticated) {
      setCart(null);
      return;
    }

    try {
      setLoadingCart(true);
      const response = await getMyCart();
      setCart(response);
    } catch (error) {
      console.error('Failed to refresh cart', error);
      setCart(null);
    } finally {
      setLoadingCart(false);
    }
  }, [isAuthenticated]);

  useEffect(() => {
    void refreshCart();
  }, [refreshCart]);

  const cartCount = useMemo(() => {
    return cart?.items?.length ?? 0;
  }, [cart]);

  const value = useMemo(
    () => ({
      cart,
      cartCount,
      loadingCart,
      refreshCart,
      setCart,
    }),
    [cart, cartCount, loadingCart, refreshCart]
  );

  return <CartContext.Provider value={value}>{children}</CartContext.Provider>;
}
