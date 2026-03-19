import { http } from '../lib/http';
import type {
    AddCartItemRequest,
    CartResponse,
    ReplaceCartItemSelectionsRequest,
} from '../types/api/cart';
import type { OrderResponseDTO } from '../types/api/order';

export async function getMyCart() {
  return http<CartResponse>('/v1/me/cart');
}

export async function addCartItem(payload: AddCartItemRequest) {
  return http<CartResponse>('/v1/me/cart/items', {
    method: 'POST',
    body: payload,
  });
}

export async function replaceCartItemSelections(
  itemId: string,
  payload: ReplaceCartItemSelectionsRequest,
) {
  return http<CartResponse>(`/v1/me/cart/items/${itemId}`, {
    method: 'PUT',
    body: payload,
  });
}

export async function removeCartItem(itemId: string) {
  return http<void>(`/v1/me/cart/items/${itemId}`, {
    method: 'DELETE',
  });
}

export async function clearCart() {
  return http<void>('/v1/me/cart', {
    method: 'DELETE',
  });
}

export async function checkoutFromCart() {
  return http<OrderResponseDTO>('/v1/me/checkout/from-cart', {
    method: 'POST',
  });
}