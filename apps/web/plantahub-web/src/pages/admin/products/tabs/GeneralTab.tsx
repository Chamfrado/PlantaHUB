import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import MoneyInput from '../../../../components/admin/ui/MoneyInput';
import {
  FormField,
  PrimaryButton,
  SelectInput,
  TextArea,
  TextInput,
  ToggleSwitch,
} from '../../../../components/admin/ui/primitives';
import { getApiErrorMessage } from '../../../../lib/api-error';
import { listAdminCategories, updateProduct } from '../../../../services/admin/admin.service';
import type { ProductDetailResponse } from '../../../../types/api/product';

export default function GeneralTab({ product }: { product: ProductDetailResponse }) {
  const queryClient = useQueryClient();

  const categories = useQuery({
    queryKey: ['admin', 'categories'],
    queryFn: listAdminCategories,
  });

  const [name, setName] = useState(product.name);
  const [slug, setSlug] = useState(product.slug);
  const [category, setCategory] = useState(product.category);
  const [shortDescription, setShortDescription] = useState(product.shortDescription ?? '');
  const [areaM2, setAreaM2] = useState(product.areaM2 ?? 0);
  const [basePriceCents, setBasePriceCents] = useState<number | null>(
    product.basePriceCents ?? null
  );
  const [delivery, setDelivery] = useState(product.delivery ?? '');
  const [customizable, setCustomizable] = useState(product.customizable === true);

  const save = useMutation({
    mutationFn: () =>
      updateProduct(product.id, {
        name,
        slug,
        category,
        shortDescription,
        areaM2,
        basePriceCents: basePriceCents ?? 0,
        delivery,
        customizable,
      }),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['admin', 'product', product.id] });
      void queryClient.invalidateQueries({ queryKey: ['admin', 'products'] });
    },
  });

  return (
    <div className="max-w-2xl space-y-6">
      <section className="space-y-5 rounded-2xl border border-neutral-200 bg-white p-6">
        <FormField label="Nome" htmlFor="name">
          <TextInput id="name" value={name} onChange={e => setName(e.target.value)} />
        </FormField>

        <div className="grid gap-5 sm:grid-cols-2">
          <FormField label="Categoria" htmlFor="category">
            <SelectInput
              id="category"
              value={category}
              onChange={e => setCategory(e.target.value)}
            >
              {(categories.data ?? []).map(c => (
                <option key={c.slug} value={c.slug}>
                  {c.name}
                </option>
              ))}
            </SelectInput>
          </FormField>

          <FormField
            label="Slug"
            htmlFor="slug"
            hint="Parte final da URL pública. Mudar quebra links já compartilhados."
          >
            <TextInput id="slug" value={slug} onChange={e => setSlug(e.target.value)} />
          </FormField>
        </div>

        <FormField label="Descrição curta" htmlFor="short">
          <TextArea
            id="short"
            value={shortDescription}
            onChange={e => setShortDescription(e.target.value)}
          />
        </FormField>

        <div className="grid gap-5 sm:grid-cols-2">
          <FormField label="Área (m²)" htmlFor="area">
            <TextInput
              id="area"
              type="number"
              min={0}
              value={areaM2}
              onChange={e => setAreaM2(Number(e.target.value))}
            />
          </FormField>

          <FormField
            label="Preço base"
            htmlFor="price"
            hint="Exibido na vitrine. O que o cliente paga vem das ofertas por coleção."
          >
            <MoneyInput id="price" valueCents={basePriceCents} onChange={setBasePriceCents} />
          </FormField>
        </div>

        <FormField label="Entrega" htmlFor="delivery">
          <TextInput id="delivery" value={delivery} onChange={e => setDelivery(e.target.value)} />
        </FormField>

        <ToggleSwitch
          checked={customizable}
          onChange={setCustomizable}
          label="Projeto customizável"
        />
      </section>

      <div className="flex items-center justify-end gap-3">
        {save.isError ? (
          <span className="text-sm font-semibold text-red-600">
            {getApiErrorMessage(save.error, 'Não foi possível salvar.')}
          </span>
        ) : null}
        {save.isSuccess ? (
          <span className="text-sm font-semibold text-green-600">Alterações salvas.</span>
        ) : null}

        <PrimaryButton onClick={() => save.mutate()} disabled={save.isPending}>
          {save.isPending ? 'Salvando…' : 'Salvar'}
        </PrimaryButton>
      </div>
    </div>
  );
}
