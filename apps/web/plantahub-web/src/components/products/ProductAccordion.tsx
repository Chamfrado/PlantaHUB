import { useState } from 'react';
import type { ProductSummaryView } from '../../types/product-view';
import ProductAccordionItem from './ProductAccordionItem';

type Props = {
  /** Já filtrados pela página: a aba ativa é estado de lá, não daqui. */
  products: ProductSummaryView[];
};

export default function ProductAccordion({ products }: Props) {
  const [openId, setOpenId] = useState<string | null>(null);

  return (
    <div className="space-y-4">
      {products.map(p => (
        <ProductAccordionItem
          key={p.id}
          product={p}
          isOpen={openId === p.id}
          onToggle={() => setOpenId(prev => (prev === p.id ? null : p.id))}
        />
      ))}
    </div>
  );
}
