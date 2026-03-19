export type CreateOrderItemRequest = {
  productId: string;
  quantity: number;
  planTypeCodes: string[];
};

export type CreateOrderRequest = {
  items: CreateOrderItemRequest[];
};

export type SelectionDTO = {
  planTypeCode: string;
  priceCents: number;
};

export type OrderItemDTO = {
  id: string;
  productId: string;
  quantity: number;
  totalCents: number;
  selections: SelectionDTO[];
};

export type OrderResponseDTO = {
  id: string;
  status: string;
  totalCents: number;
  currency: string;
  createdAt: string;
  paidAt?: string | null;
  items: OrderItemDTO[];
};