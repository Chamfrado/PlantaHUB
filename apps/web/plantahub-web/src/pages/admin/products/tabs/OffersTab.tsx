import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import MoneyInput from '../../../../components/admin/ui/MoneyInput';
import {
  DataError,
  EmptyState,
  PrimaryButton,
  SelectInput,
  ToggleSwitch,
} from '../../../../components/admin/ui/primitives';
import { getApiErrorMessage } from '../../../../lib/api-error';
import {
  listCollections,
  listOffers,
  upsertOffer,
} from '../../../../services/admin/admin.service';
import type { AdminOffer } from '../../../../types/api/admin';

export default function OffersTab({ productId }: { productId: string }) {
  const queryClient = useQueryClient();
  const [newCode, setNewCode] = useState('');

  const offers = useQuery({
    queryKey: ['admin', 'offers', productId],
    queryFn: () => listOffers(productId),
  });

  const collections = useQuery({
    queryKey: ['admin', 'collections'],
    queryFn: listCollections,
  });

  const save = useMutation({
    mutationFn: (input: { code: string; priceCents?: number; available?: boolean }) =>
      upsertOffer(productId, input.code, {
        priceCents: input.priceCents,
        available: input.available,
      }),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['admin', 'offers', productId] });
    },
  });

  const existingCodes = new Set((offers.data ?? []).map(o => o.collectionCode));
  const available = (collections.data ?? []).filter(c => !existingCodes.has(c.code));

  return (
    <div className="max-w-3xl space-y-6">
      <section className="rounded-2xl border border-neutral-200 bg-white p-6">
        <h2 className="text-base font-extrabold text-neutral-900">Adicionar coleção</h2>
        <p className="mt-1 text-xs text-neutral-500">
          Uma coleção marcada como acompanhante só serve para ancorar arquivos: ela nunca
          aparece como opção de compra.
        </p>

        <div className="mt-4 flex flex-wrap gap-3">
          <SelectInput value={newCode} onChange={e => setNewCode(e.target.value)}>
            <option value="">Selecione…</option>
            {available.map(c => (
              <option key={c.code} value={c.code}>
                {c.name} ({c.code}){c.bundledWithEveryOffer ? ' · acompanhante' : ''}
              </option>
            ))}
          </SelectInput>

          <PrimaryButton
            disabled={!newCode || save.isPending}
            onClick={() => {
              save.mutate({ code: newCode, priceCents: 0, available: false });
              setNewCode('');
            }}
          >
            Adicionar
          </PrimaryButton>
        </div>
      </section>

      {offers.isLoading ? (
        <div className="h-48 animate-pulse rounded-2xl bg-white" />
      ) : offers.isError ? (
        <DataError
          message={getApiErrorMessage(offers.error, 'Não foi possível carregar as ofertas.')}
          onRetry={() => void offers.refetch()}
        />
      ) : (offers.data ?? []).length === 0 ? (
        <EmptyState message="Nenhuma coleção associada. Um produto precisa de ao menos uma oferta com preço para ser publicado." />
      ) : (
        <div className="space-y-3">
          {(offers.data ?? []).map(offer => (
            <OfferRow
              key={offer.id}
              offer={offer}
              busy={save.isPending}
              onSave={(priceCents, isAvailable) =>
                save.mutate({
                  code: offer.collectionCode,
                  priceCents: priceCents ?? 0,
                  available: isAvailable,
                })
              }
            />
          ))}
        </div>
      )}
    </div>
  );
}

function OfferRow({
  offer,
  busy,
  onSave,
}: {
  offer: AdminOffer;
  busy: boolean;
  onSave: (priceCents: number | null, available: boolean) => void;
}) {
  const [priceCents, setPriceCents] = useState<number | null>(offer.priceCents);
  const [available, setAvailable] = useState(offer.available);

  const dirty = priceCents !== offer.priceCents || available !== offer.available;

  return (
    <div className="rounded-2xl border border-neutral-200 bg-white p-5">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <div className="font-bold text-neutral-900">{offer.collectionName}</div>
          <div className="text-xs text-neutral-400">
            {offer.collectionCode}
            {offer.bundledWithEveryOffer ? ' · acompanha toda oferta' : ''}
            {!offer.purchasableCollection ? ' · não comprável' : ''}
          </div>
        </div>

        <div className="flex flex-wrap items-center gap-4">
          <div className="w-40">
            {/* Sem rotulo, quem usa leitor de tela nao tem como saber a que colecao este
                preco pertence — sao varias linhas identicas na mesma tela. */}
            <MoneyInput
              valueCents={priceCents}
              onChange={setPriceCents}
              disabled={!offer.purchasableCollection}
              ariaLabel={`Preço de ${offer.collectionName}`}
            />
          </div>

          <ToggleSwitch
            checked={available}
            onChange={setAvailable}
            label="À venda"
            disabled={!offer.purchasableCollection}
          />

          <PrimaryButton
            onClick={() => onSave(priceCents, available)}
            disabled={!dirty || busy}
            aria-label={`Salvar oferta de ${offer.collectionName}`}
          >
            Salvar
          </PrimaryButton>
        </div>
      </div>
    </div>
  );
}
