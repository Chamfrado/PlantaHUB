import type { Product } from "../types/ProductData";
import { PRODUCTS } from "./products";


export function getProductByRoute(category: string, slug: string): Product | undefined {
  // category in URL is expected to match ProductCategory: "casas" | "chales" | "studios"
  return PRODUCTS.find((p) => p.category === category && p.slug === slug);
}
