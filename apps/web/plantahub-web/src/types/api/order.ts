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
  /** Snapshot do momento da compra. */
  planTypeName?: string | null;
  priceCents: number;
};

export type OrderItemDTO = {
  id: string;
  productId: string;
  /**
   * Snapshots do momento da compra: renomear um produto no painel não pode reescrever o
   * histórico de quem já comprou.
   */
  productName?: string | null;
  productCategory?: string | null;
  productSlug?: string | null;
  productImageUrl?: string | null;
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
  paymentUrl?: string | null;
  items: OrderItemDTO[];
};

export type CheckoutFromCartResponseDTO = {
  order: OrderResponseDTO;
  paymentUrl: string;
};
