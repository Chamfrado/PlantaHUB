import { useMemo, useState } from 'react';
import type { Product, ProductCategory } from '../../types/ProductData';
import ProductAccordionItem from './ProductAccordionItem';

type Props = {
  products: Product[];
  category: ProductCategory;
};

export default function ProductAccordion({ products, category }: Props) {
  const [openId, setOpenId] = useState<string | null>(null);

  const list = useMemo(() => products.filter(p => p.category === category), [products, category]);

  return (
    <div className="space-y-4">
      {list.map(p => (
        <ProductAccordionItem
          key={`${p.category}-${p.id}`}
          product={p}
          isOpen={openId === p.id}
          onToggle={() => setOpenId(prev => (prev === p.id ? null : p.id))}
        />
      ))}
    </div>
  );
}
