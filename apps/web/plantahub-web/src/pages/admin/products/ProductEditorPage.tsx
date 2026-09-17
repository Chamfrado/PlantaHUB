import { useMutation, useQuery } from '@tanstack/react-query';
import { ArrowLeft, Eye } from 'lucide-react';
import { useState } from 'react';
import { Link, NavLink, Navigate, Route, Routes, useNavigate, useParams } from 'react-router-dom';
import {
  AdminPageHeader,
  DataError,
  FormField,
  PrimaryButton,
  SelectInput,
  StatusBadge,
  TextInput,
} from '../../../components/admin/ui/primitives';
import { getApiErrorMessage } from '../../../lib/api-error';
import {
  createProduct,
  getAdminProduct,
  listAdminCategories,
} from '../../../services/admin/admin.service';
import AssetsTab from './tabs/AssetsTab';
import ContentTab from './tabs/ContentTab';
import GeneralTab from './tabs/GeneralTab';
import MediaTab from './tabs/MediaTab';
import OffersTab from './tabs/OffersTab';

type Props = { mode: 'create' | 'edit' };

export default function ProductEditorPage({ mode }: Props) {
  return mode === 'create' ? <CreateProduct /> : <EditProduct />;
}

/**
 * Criação enxuta de propósito: nome, categoria e pronto.
 *
 * O produto nasce em rascunho e tudo o mais é preenchido nas abas. Pedir a página inteira
 * antes de deixar criar qualquer coisa é o tipo de formulário que ninguém termina.
 */
function CreateProduct() {
  const navigate = useNavigate();

  const categories = useQuery({ queryKey: ['admin', 'categories'], queryFn: listAdminCategories });

  const [name, setName] = useState('');
  const [category, setCategory] = useState('');

  const firstCategory = categories.data?.[0]?.slug ?? '';

  // O select exibe a primeira categoria quando o estado ainda esta vazio; enviar o estado
  // cru mandaria categoria em branco para quem nao encostasse no dropdown.
  const selectedCategory = category || firstCategory;

  const create = useMutation({
    mutationFn: () => createProduct({ name, category: selectedCategory }),
    onSuccess: product => navigate(`/admin/produtos/${encodeURIComponent(product.id)}`),
  });

  return (
    <div className="max-w-xl space-y-6">
      <AdminPageHeader
        title="Novo produto"
        description="O produto nasce como rascunho e não aparece no site até ser publicado."
      />

      <section className="space-y-5 rounded-2xl border border-neutral-200 bg-white p-6">
        <FormField label="Nome" htmlFor="name">
          <TextInput
            id="name"
            value={name}
            onChange={e => setName(e.target.value)}
            placeholder="Casa Confort"
          />
        </FormField>

        <FormField label="Categoria" htmlFor="category">
          <SelectInput
            id="category"
            value={selectedCategory}
            onChange={e => setCategory(e.target.value)}
          >
            {(categories.data ?? []).map(c => (
              <option key={c.slug} value={c.slug}>
                {c.name}
              </option>
            ))}
          </SelectInput>
        </FormField>
      </section>

      {create.isError ? (
        <DataError message={getApiErrorMessage(create.error, 'Não foi possível criar o produto.')} />
      ) : null}

      <div className="flex justify-end gap-3">
        <Link
          to="/admin/produtos"
          className="rounded-xl border border-neutral-300 bg-white px-4 py-2.5 text-sm font-semibold text-neutral-700"
        >
          Cancelar
        </Link>
        <PrimaryButton
          onClick={() => create.mutate()}
          disabled={!name || !selectedCategory || create.isPending}
        >
          {create.isPending ? 'Criando…' : 'Criar rascunho'}
        </PrimaryButton>
      </div>
    </div>
  );
}

const TABS = [
  { path: 'geral', label: 'Geral' },
  { path: 'conteudo', label: 'Conteúdo' },
  { path: 'imagens', label: 'Imagens' },
  { path: 'ofertas', label: 'Ofertas' },
  { path: 'arquivos', label: 'Arquivos' },
];

function EditProduct() {
  const { productId = '' } = useParams();

  const product = useQuery({
    queryKey: ['admin', 'product', productId],
    queryFn: () => getAdminProduct(productId),
  });

  if (product.isLoading) {
    return <div className="h-96 animate-pulse rounded-2xl bg-white" />;
  }

  if (product.isError || !product.data) {
    return (
      <DataError
        message={getApiErrorMessage(product.error, 'Produto não encontrado.')}
        onRetry={() => void product.refetch()}
      />
    );
  }

  const data = product.data;

  return (
    <div className="space-y-6">
      <div>
        <Link
          to="/admin/produtos"
          className="inline-flex items-center gap-1.5 text-sm font-semibold text-neutral-500 transition hover:text-neutral-900"
        >
          <ArrowLeft className="h-4 w-4" /> Produtos
        </Link>
      </div>

      <AdminPageHeader
        title={data.name}
        description={`${data.category}/${data.slug}`}
        actions={
          <>
            <StatusBadge status={data.status} />
            <Link
              to={`/admin/produtos/${encodeURIComponent(productId)}/preview`}
              className="inline-flex items-center gap-2 rounded-xl border border-neutral-300 bg-white px-4 py-2.5 text-sm font-semibold text-neutral-700 transition hover:bg-neutral-50"
            >
              <Eye className="h-4 w-4" /> Pré-visualizar
            </Link>
          </>
        }
      />

      <div className="flex gap-1 border-b border-neutral-200">
        {TABS.map(tab => (
          <NavLink
            key={tab.path}
            to={`/admin/produtos/${encodeURIComponent(productId)}/${tab.path}`}
            className={({ isActive }) =>
              [
                'border-b-2 px-4 py-3 text-sm font-semibold transition',
                isActive
                  ? 'border-primary-500 text-primary-600'
                  : 'border-transparent text-neutral-500 hover:text-neutral-900',
              ].join(' ')
            }
          >
            {tab.label}
          </NavLink>
        ))}
      </div>

      <Routes>
        <Route index element={<Navigate to="geral" replace />} />
        <Route path="geral" element={<GeneralTab product={data} />} />
        <Route
          path="conteudo"
          element={
            // `key` força um formulário novo quando o produto muda: sem isso, o estado
            // local das abas sobreviveria à navegação entre produtos.
            <ContentTab key={data.id} productId={data.id} content={data.content ?? {}} />
          }
        />
        <Route path="imagens" element={<MediaTab productId={data.id} />} />
        <Route path="ofertas" element={<OffersTab productId={data.id} />} />
        <Route path="arquivos" element={<AssetsTab productId={data.id} />} />
      </Routes>
    </div>
  );
}
