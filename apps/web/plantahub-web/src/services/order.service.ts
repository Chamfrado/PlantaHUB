import { http } from '../lib/http';
import type { CreateOrderRequest, OrderResponseDTO } from '../types/api/order';

export async function getMyOrders() {
  return http<OrderResponseDTO[]>('/v1/me/orders');
}

export async function createOrder(payload: CreateOrderRequest) {
  return http<OrderResponseDTO>('/v1/me/orders', {
    method: 'POST',
    body: payload,
  });
}

export async function payMock(orderId: string) {
  return http<OrderResponseDTO>(`/v1/me/orders/${orderId}/pay-mock`, {
    method: 'POST',
  });
}