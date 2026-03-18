import { http } from '../lib/http';
import type { OrderResponseDTO } from '../types/api/order';

export async function getMyOrders() {
  return http<OrderResponseDTO[]>('/v1/me/orders');
}