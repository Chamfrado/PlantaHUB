import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { ArrowLeft } from 'lucide-react';
import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import RepeatableList from '../../../components/admin/forms/RepeatableList';
import { stripUid, withUid, type WithUid } from '../../../components/admin/forms/uid';
import {
  AdminPageHeader,
  DataError,
  FormField,
  PrimaryButton,
  TextArea,
  TextInput,
} from '../../../components/admin/ui/primitives';
import { getApiErrorMessage } from '../../../lib/api-error';
import {
  getSitePageForEdit,
  updateSitePage,
} from '../../../services/admin/admin.service';
import type {
  SiteFaqItem,
  SiteItem,
  SitePageResponse,
  SiteSection,
} from '../../../types/api/site';

/**
 * Uma seção em edição.
 *
 * Os itens já carregam `_uid` no estado, e não gerados na hora de renderizar: uid novo a
 * cada render faria o React remontar o campo a cada tecla e o cursor cairia fora.
 */
type EditableSection = Omit<SiteSection, 'items'> & { items: WithUid<SiteItem>[] };

/** Lista de marcadores como texto: uma linha por item, que é como se pensa a lista. */
function bulletsToText(bullets: string[]) {
  return bullets.join('\n');
}

function textToBullets(text: string) {
  return text
    .split('\n')
    .map(line => line.trim())
    .filter(Boolean);
}

export default function SitePageEditorPage() {
  const { slug = '' } = useParams();

  const page = useQuery({
    queryKey: ['admin', 'site', 'page', slug],
    queryFn: () => getSitePageForEdit(slug),
  });

  if (page.isLoading) {
    return <div className="h-96 animate-pulse rounded-2xl bg-white" />;
  }

  if (page.isError || !page.data) {
    return (
      <DataError
        message={getApiErrorMessage(page.error, 'Página não encontrada.')}
        onRetry={() => void page.refetch()}
      />
    );
  }

  // O formulário nasce com os dados já em mãos, em vez de copiá-los num efeito depois.
  // Além de ser o que a regra do lint pede, evita que uma revalidação em segundo plano
  // apague o que a pessoa estiver digitando. A `key` garante um formulário novo ao trocar
  // de página.
  return <PageForm key={page.data.slug} page={page.data} />;
}

function PageForm({ page }: { page: SitePageResponse }) {
  const queryClient = useQueryClient();
  const slug = page.slug;

  const [title, setTitle] = useState(page.title);
  const [headline, setHeadline] = useState(page.content.headline ?? '');
  const [subheadline, setSubheadline] = useState(page.content.subheadline ?? '');
  const [intro, setIntro] = useState(page.content.intro ?? '');
  const [ctaTitle, setCtaTitle] = useState(page.content.ctaTitle ?? '');
  const [ctaSubtitle, setCtaSubtitle] = useState(page.content.ctaSubtitle ?? '');
  const [saved, setSaved] = useState(false);

  const [sections, setSections] = useState<WithUid<EditableSection>[]>(() =>
    withUid(page.content.sections.map(section => ({ ...section, items: withUid(section.items) })))
  );

  const [faq, setFaq] = useState<WithUid<SiteFaqItem>[]>(() => withUid(page.content.faq));

  const save = useMutation({
    mutationFn: () =>
      updateSitePage(slug, {
        title,
        content: {
          headline: headline || null,
          subheadline: subheadline || null,
          intro: intro || null,
          sections: stripUid(sections).map(section => ({
            ...section,
            items: stripUid(section.items),
          })),
          faq: stripUid(faq),
          ctaTitle: ctaTitle || null,
          ctaSubtitle: ctaSubtitle || null,
          ctaHref: page.content.ctaHref,
          ctaLabel: page.content.ctaLabel,
        },
      }),
    onSuccess: () => {
      setSaved(true);
      window.setTimeout(() => setSaved(false), 3000);
      void queryClient.invalidateQueries({ queryKey: ['admin', 'site'] });
    },
  });

  const isFaqPage = faq.length > 0 || slug === 'faq';

  return (
    <div className="max-w-3xl space-y-6">
      <Link
        to="/admin/paginas"
        className="inline-flex items-center gap-1.5 text-sm font-semibold text-neutral-500 transition hover:text-neutral-900"
      >
        <ArrowLeft className="h-4 w-4" /> Páginas do site
      </Link>

      <AdminPageHeader
        title={page.title}
        description="O texto aqui é o que o visitante lê. Publicar não exige deploy."
        actions={
          <PrimaryButton onClick={() => save.mutate()} disabled={save.isPending}>
            {save.isPending ? 'Salvando…' : 'Salvar'}
          </PrimaryButton>
        }
      />

      {save.error ? (
        <DataError
          message={getApiErrorMessage(save.error, 'Não foi possível salvar a página.')}
        />
      ) : null}

      {saved ? (
        <p className="rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm font-semibold text-emerald-800">
          Página salva. O site já mostra o texto novo.
        </p>
      ) : null}

      <section className="space-y-4 rounded-2xl border border-neutral-200 bg-white p-6">
        <h2 className="text-base font-extrabold text-neutral-900">Abertura</h2>

        <FormField label="Nome no painel" htmlFor="site-title">
          <TextInput id="site-title" value={title} onChange={e => setTitle(e.target.value)} />
        </FormField>

        <FormField label="Título" htmlFor="site-headline">
          <TextInput id="site-headline" value={headline} onChange={e => setHeadline(e.target.value)} />
        </FormField>

        <FormField label="Chapéu" htmlFor="site-subheadline" hint="Texto curto acima do título.">
          <TextInput
            id="site-subheadline"
            value={subheadline}
            onChange={e => setSubheadline(e.target.value)}
          />
        </FormField>

        <FormField label="Parágrafo de abertura" htmlFor="site-intro">
          <TextArea id="site-intro" rows={3} value={intro} onChange={e => setIntro(e.target.value)} />
        </FormField>
      </section>

      <section className="rounded-2xl border border-neutral-200 bg-white p-6">
        <RepeatableList<EditableSection>
          label="Seções"
          value={sections}
          onChange={setSections}
          max={40}
          newItem={() => ({ key: null, title: '', body: '', items: [] })}
          itemTitle={(section, index) => section.title || `Seção ${index + 1}`}
          emptyHint="Sem seções — a página fica só com a abertura."
          renderItem={(section, patch) => (
            <div className="space-y-3">
              <FormField label="Título da seção">
                <TextInput
                  value={section.title ?? ''}
                  onChange={e => patch({ title: e.target.value })}
                />
              </FormField>

              <FormField label="Texto">
                <TextArea
                  rows={4}
                  value={section.body ?? ''}
                  onChange={e => patch({ body: e.target.value })}
                />
              </FormField>

              <SectionItems items={section.items} onChange={items => patch({ items })} />

              {section.key ? (
                <p className="text-xs text-neutral-400">
                  Bloco <code>{section.key}</code> — a página tem desenho próprio para ele.
                </p>
              ) : null}
            </div>
          )}
        />
      </section>

      {isFaqPage ? (
        <section className="rounded-2xl border border-neutral-200 bg-white p-6">
          <RepeatableList<SiteFaqItem>
            label="Perguntas frequentes"
            value={faq}
            onChange={setFaq}
            max={60}
            newItem={() => ({ category: '', question: '', answer: '' })}
            itemTitle={(item, index) => item.question || `Pergunta ${index + 1}`}
            emptyHint="Sem perguntas — a página aparece vazia."
            renderItem={(item, patch) => (
              <div className="space-y-3">
                <FormField label="Categoria" hint="Agrupa a pergunta na busca da página.">
                  <TextInput
                    value={item.category ?? ''}
                    onChange={e => patch({ category: e.target.value })}
                  />
                </FormField>

                <FormField label="Pergunta">
                  <TextInput value={item.question} onChange={e => patch({ question: e.target.value })} />
                </FormField>

                <FormField label="Resposta">
                  <TextArea
                    rows={3}
                    value={item.answer}
                    onChange={e => patch({ answer: e.target.value })}
                  />
                </FormField>
              </div>
            )}
          />
        </section>
      ) : null}

      <section className="space-y-4 rounded-2xl border border-neutral-200 bg-white p-6">
        <h2 className="text-base font-extrabold text-neutral-900">Chamada final</h2>

        <FormField label="Título da chamada" htmlFor="site-cta-title">
          <TextInput id="site-cta-title" value={ctaTitle} onChange={e => setCtaTitle(e.target.value)} />
        </FormField>

        <FormField label="Subtítulo da chamada" htmlFor="site-cta-subtitle">
          <TextInput
            id="site-cta-subtitle"
            value={ctaSubtitle}
            onChange={e => setCtaSubtitle(e.target.value)}
          />
        </FormField>
      </section>
    </div>
  );
}

/**
 * Os itens de uma seção.
 *
 * Título vazio é marcador simples; preenchido, vira cartão com descrição — que é a mesma
 * distinção que a página usa para decidir como desenhar.
 */
function SectionItems({
  items,
  onChange,
}: {
  items: WithUid<SiteItem>[];
  onChange: (next: WithUid<SiteItem>[]) => void;
}) {
  return (
    <RepeatableList<SiteItem>
      label="Itens"
      value={items}
      onChange={onChange}
      max={30}
      newItem={() => ({ title: '', text: '', bullets: [] })}
      itemTitle={(item, index) => item.title || item.text?.slice(0, 40) || `Item ${index + 1}`}
      emptyHint="Sem itens — a seção mostra só o texto."
      renderItem={(item, patch) => (
        <div className="space-y-3">
          <FormField label="Título" hint="Deixe vazio para virar um marcador simples.">
            <TextInput value={item.title ?? ''} onChange={e => patch({ title: e.target.value })} />
          </FormField>

          <FormField label="Texto">
            <TextArea rows={2} value={item.text ?? ''} onChange={e => patch({ text: e.target.value })} />
          </FormField>

          <FormField label="Marcadores" hint="Um por linha.">
            <TextArea
              rows={3}
              value={bulletsToText(item.bullets)}
              onChange={e => patch({ bullets: textToBullets(e.target.value) })}
            />
          </FormField>
        </div>
      )}
    />
  );
}
