import { createContext } from 'react';
import type { CartResponse } from '../types/api/cart';

export type CartContextValue = {
  cart: CartResponse | null;
  cartCount: number;
  loadingCart: boolean;
  refreshCart: () => Promise<void>;
  setCart: React.Dispatch<React.SetStateAction<CartResponse | null>>;
};

export const CartContext = createContext<CartContextValue | undefined>(undefined);
