import { http } from '../lib/http';
import type {
  CheckoutFromCartResponseDTO,
  CreateOrderRequest,
  OrderResponseDTO,
} from '../types/api/order';

export async function getMyOrders() {
  return http<OrderResponseDTO[]>('/v1/me/orders');
}

export async function createOrder(payload: CreateOrderRequest) {
  return http<OrderResponseDTO>('/v1/me/orders', {
    method: 'POST',
    body: payload,
  });
}

export async function checkoutDirect(payload: CreateOrderRequest) {
  return http<CheckoutFromCartResponseDTO>('/v1/me/checkout/direct', {
    method: 'POST',
    body: payload,
  });
}

export async function getOrderPaymentLink(orderId: string) {
  return http<CheckoutFromCartResponseDTO>(`/v1/me/orders/${orderId}/payment-link`, {
    method: 'POST',
  });
}

export async function payMock(orderId: string) {
  return http<OrderResponseDTO>(`/v1/me/orders/${orderId}/pay-mock`, {
    method: 'POST',
  });
}
