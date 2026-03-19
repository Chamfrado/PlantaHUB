export type CartPlanTypeResponse = {
  id: string;
  code: string;
  name: string;
  description: string;
  priceCents: number;
};

export type CartItemResponse = {
  itemId: string;
  productId: string;
  slug: string;
  name: string;
  category: string;
  shortDescription: string;
  heroImageUrl: string;
  selections: CartPlanTypeResponse[];
  itemTotalCents: number;
};

export type CartResponse = {
  cartId: string;
  status: string;
  items: CartItemResponse[];
  totalCents: number;
  currency: string;
};

export type AddCartItemRequest = {
  productId: string;
  planTypeCodes: string[];
};

export type ReplaceCartItemSelectionsRequest = {
  planTypeCodes: string[];
};