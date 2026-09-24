import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import AvatarInput from '../../../../components/admin/forms/AvatarInput';
import RepeatableList from '../../../../components/admin/forms/RepeatableList';
import { stripUid, withUid, type WithUid } from '../../../../components/admin/forms/uid';
import {
  FormField,
  PrimaryButton,
  TextArea,
  TextInput,
} from '../../../../components/admin/ui/primitives';
import { getApiErrorMessage } from '../../../../lib/api-error';
import { replaceProductContent } from '../../../../services/admin/admin.service';
import type {
  ProductFaqItemDTO,
  ProductFeatureDTO,
  ProductIncludedItemDTO,
  ProductKeyFactDTO,
  ProductPageContentDTO,
  ProductTestimonialDTO,
} from '../../../../types/api/product';

type Props = {
  productId: string;
  content: ProductPageContentDTO;
};

/**
 * Editor do texto da página pública.
 *
 * Salva o documento inteiro de uma vez — é assim que o conteúdo é persistido (uma coluna
 * `jsonb`) e é assim que o admin pensa nele: uma página, não trinta campos independentes.
 */
export default function ContentTab({ productId, content }: Props) {
  const queryClient = useQueryClient();

  const [headline, setHeadline] = useState(content.headline ?? '');
  const [subheadline, setSubheadline] = useState(content.subheadline ?? '');
  const [description, setDescription] = useState(content.description ?? '');

  const [whyChooseTitle, setWhyChooseTitle] = useState(content.whyChooseTitle ?? '');
  const [whyChooseIntro, setWhyChooseIntro] = useState(content.whyChooseIntro ?? '');
  const [features, setFeatures] = useState<WithUid<ProductFeatureDTO>[]>(
    withUid(content.whyChooseFeatures ?? [])
  );

  const [includesTitle, setIncludesTitle] = useState(content.includesTitle ?? '');
  const [includedItems, setIncludedItems] = useState<WithUid<ProductIncludedItemDTO>[]>(
    withUid(content.includedItems ?? [])
  );

  const [keyFactsTitle, setKeyFactsTitle] = useState(content.keyFactsTitle ?? '');
  const [keyFacts, setKeyFacts] = useState<WithUid<ProductKeyFactDTO>[]>(
    withUid(content.keyFacts ?? [])
  );

  const [testimonialsTitle, setTestimonialsTitle] = useState(content.testimonialsTitle ?? '');
  const [testimonials, setTestimonials] = useState<WithUid<ProductTestimonialDTO>[]>(
    withUid(content.testimonials ?? [])
  );

  const [faqTitle, setFaqTitle] = useState(content.faqTitle ?? '');
  const [faq, setFaq] = useState<WithUid<ProductFaqItemDTO>[]>(withUid(content.faq ?? []));

  const [finalCtaTitle, setFinalCtaTitle] = useState(content.finalCtaTitle ?? '');
  const [finalCtaSubtitle, setFinalCtaSubtitle] = useState(content.finalCtaSubtitle ?? '');

  const [tags, setTags] = useState((content.tags ?? []).join(', '));

  const save = useMutation({
    mutationFn: () =>
      replaceProductContent(productId, {
        headline: headline || null,
        subheadline: subheadline || null,
        description: description || null,
        whyChooseTitle: whyChooseTitle || null,
        whyChooseIntro: whyChooseIntro || null,
        whyChooseFeatures: stripUid(features),
        includesTitle: includesTitle || null,
        includedItems: stripUid(includedItems),
        keyFactsTitle: keyFactsTitle || null,
        keyFacts: stripUid(keyFacts),
        testimonialsTitle: testimonialsTitle || null,
        testimonials: stripUid(testimonials),
        faqTitle: faqTitle || null,
        faq: stripUid(faq),
        finalCtaTitle: finalCtaTitle || null,
        finalCtaSubtitle: finalCtaSubtitle || null,
        tags: tags
          .split(',')
          .map(tag => tag.trim())
          .filter(Boolean),
      }),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['admin', 'product', productId] });
    },
  });

  return (
    <div className="space-y-8">
      <SectionCard
        title="Cabeçalho"
        hint="O título aparece como manchete da página. Sem ele, o nome do produto é usado."
      >
        <FormField label="Título">
          <TextInput value={headline} onChange={e => setHeadline(e.target.value)} />
        </FormField>
        <FormField label="Subtítulo">
          <TextInput value={subheadline} onChange={e => setSubheadline(e.target.value)} />
        </FormField>
        <FormField label="Descrição">
          <TextArea value={description} onChange={e => setDescription(e.target.value)} />
        </FormField>
      </SectionCard>

      <SectionCard
        title="Diferenciais"
        hint="Seção vazia não aparece na página pública."
      >
        <FormField label="Título da seção">
          <TextInput value={whyChooseTitle} onChange={e => setWhyChooseTitle(e.target.value)} />
        </FormField>
        <FormField label="Introdução">
          <TextInput value={whyChooseIntro} onChange={e => setWhyChooseIntro(e.target.value)} />
        </FormField>
        <RepeatableList
          label="Diferenciais"
          value={features}
          onChange={setFeatures}
          newItem={() => ({ title: '', description: '' })}
          itemTitle={item => item.title}
          max={12}
          renderItem={(item, patch) => (
            <>
              <TextInput
                placeholder="Título"
                value={item.title}
                onChange={e => patch({ title: e.target.value })}
              />
              <TextArea
                placeholder="Descrição"
                value={item.description ?? ''}
                onChange={e => patch({ description: e.target.value })}
              />
            </>
          )}
        />
      </SectionCard>

      <SectionCard title="O que está incluso">
        <FormField label="Título da seção">
          <TextInput value={includesTitle} onChange={e => setIncludesTitle(e.target.value)} />
        </FormField>
        <RepeatableList
          label="Itens inclusos"
          value={includedItems}
          onChange={setIncludedItems}
          newItem={() => ({ title: '', description: '' })}
          itemTitle={item => item.title}
          renderItem={(item, patch) => (
            <>
              <TextInput
                placeholder="Título"
                value={item.title}
                onChange={e => patch({ title: e.target.value })}
              />
              <TextArea
                placeholder="Descrição"
                value={item.description ?? ''}
                onChange={e => patch({ description: e.target.value })}
              />
            </>
          )}
        />
      </SectionCard>

      <SectionCard title="Fatos em destaque">
        <FormField label="Título da seção">
          <TextInput value={keyFactsTitle} onChange={e => setKeyFactsTitle(e.target.value)} />
        </FormField>
        <RepeatableList
          label="Fatos"
          value={keyFacts}
          onChange={setKeyFacts}
          newItem={() => ({ value: '', label: '' })}
          itemTitle={item => `${item.value} ${item.label}`.trim()}
          max={12}
          renderItem={(item, patch) => (
            <div className="grid gap-3 sm:grid-cols-[120px_1fr]">
              <TextInput
                placeholder="80"
                value={item.value}
                onChange={e => patch({ value: e.target.value })}
              />
              <TextInput
                placeholder="metros quadrados"
                value={item.label}
                onChange={e => patch({ label: e.target.value })}
              />
            </div>
          )}
        />
      </SectionCard>

      <SectionCard title="Depoimentos">
        <FormField label="Título da seção">
          <TextInput
            value={testimonialsTitle}
            onChange={e => setTestimonialsTitle(e.target.value)}
          />
        </FormField>
        <RepeatableList
          label="Depoimentos"
          value={testimonials}
          onChange={setTestimonials}
          newItem={(): ProductTestimonialDTO => ({ quote: '', authorName: '', authorTitle: '' })}
          itemTitle={item => item.authorName}
          renderItem={(item, patch) => (
            <>
              <AvatarInput
                productId={productId}
                value={item.avatarUrl}
                alt={item.authorName}
                onChange={avatarUrl => patch({ avatarUrl })}
              />
              <TextArea
                placeholder="Depoimento"
                value={item.quote}
                onChange={e => patch({ quote: e.target.value })}
              />
              <div className="grid gap-3 sm:grid-cols-2">
                <TextInput
                  placeholder="Nome"
                  value={item.authorName}
                  onChange={e => patch({ authorName: e.target.value })}
                />
                <TextInput
                  placeholder="Cargo (opcional)"
                  value={item.authorTitle ?? ''}
                  onChange={e => patch({ authorTitle: e.target.value })}
                />
              </div>
            </>
          )}
        />
      </SectionCard>

      <SectionCard title="Perguntas frequentes">
        <FormField label="Título da seção">
          <TextInput value={faqTitle} onChange={e => setFaqTitle(e.target.value)} />
        </FormField>
        <RepeatableList
          label="Perguntas"
          value={faq}
          onChange={setFaq}
          newItem={() => ({ question: '', answer: '' })}
          itemTitle={item => item.question}
          renderItem={(item, patch) => (
            <>
              <TextInput
                placeholder="Pergunta"
                value={item.question}
                onChange={e => patch({ question: e.target.value })}
              />
              <TextArea
                placeholder="Resposta"
                value={item.answer ?? ''}
                onChange={e => patch({ answer: e.target.value })}
              />
            </>
          )}
        />
      </SectionCard>

      <SectionCard title="Chamada final e etiquetas">
        <FormField label="Título">
          <TextInput value={finalCtaTitle} onChange={e => setFinalCtaTitle(e.target.value)} />
        </FormField>
        <FormField label="Subtítulo">
          <TextInput
            value={finalCtaSubtitle}
            onChange={e => setFinalCtaSubtitle(e.target.value)}
          />
        </FormField>
        <FormField label="Etiquetas" hint="Separadas por vírgula. Aparecem no card e no topo da página.">
          <TextInput value={tags} onChange={e => setTags(e.target.value)} />
        </FormField>
      </SectionCard>

      <div className="sticky bottom-0 flex items-center justify-end gap-3 border-t border-neutral-200 bg-white/95 py-4 backdrop-blur">
        {save.isError ? (
          <span className="text-sm font-semibold text-red-600">
            {getApiErrorMessage(save.error, 'Não foi possível salvar.')}
          </span>
        ) : null}
        {save.isSuccess ? (
          <span className="text-sm font-semibold text-green-600">Conteúdo salvo.</span>
        ) : null}

        <PrimaryButton onClick={() => save.mutate()} disabled={save.isPending}>
          {save.isPending ? 'Salvando…' : 'Salvar conteúdo'}
        </PrimaryButton>
      </div>
    </div>
  );
}

function SectionCard({
  title,
  hint,
  children,
}: {
  title: string;
  hint?: string;
  children: React.ReactNode;
}) {
  return (
    <section className="rounded-2xl border border-neutral-200 bg-white p-6">
      <h2 className="text-base font-extrabold text-neutral-900">{title}</h2>
      {hint ? <p className="mt-1 text-xs text-neutral-500">{hint}</p> : null}
      <div className="mt-5 space-y-5">{children}</div>
    </section>
  );
}
