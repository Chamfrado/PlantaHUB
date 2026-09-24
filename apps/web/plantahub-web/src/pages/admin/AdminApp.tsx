import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useState } from 'react';
import { Navigate, Route, Routes } from 'react-router-dom';
import AdminLayout from '../../app/layouts/AdminLayout';
import AdminNotFound from './AdminNotFound';
import CategoryListPage from './categories/CategoryListPage';
import CollectionListPage from './collections/CollectionListPage';
import ProductEditorPage from './products/ProductEditorPage';
import ProductListPage from './products/ProductListPage';
import ProductPreviewPage from './products/ProductPreviewPage';
import ReconciliationPage from './reports/ReconciliationPage';
import StoragePage from './storage/StoragePage';

/**
 * Raiz do painel.
 *
 * O `QueryClientProvider` fica **aqui dentro**, e não na raiz do app: o painel é onde
 * invalidar cache depois de salvar custa caro, e montar o provider dentro do limite lazy
 * faz o site público não pagar nenhum byte por ele.
 */
export default function AdminApp() {
  const [queryClient] = useState(
    () =>
      new QueryClient({
        defaultOptions: {
          queries: {
            // O catálogo muda quando o próprio administrador muda, então revalidar a cada
            // foco de janela só gera requisição sem informação nova.
            refetchOnWindowFocus: false,
            staleTime: 30_000,
            retry: 1,
          },
        },
      })
  );

  return (
    <QueryClientProvider client={queryClient}>
      <Routes>
        <Route element={<AdminLayout />}>
          <Route index element={<Navigate to="/admin/produtos" replace />} />

          <Route path="produtos" element={<ProductListPage />} />
          <Route path="produtos/novo" element={<ProductEditorPage mode="create" />} />
          {/* As abas são rotas aninhadas: um link direto para a aba sobrevive ao reload e
              um relato de erro pode citar exatamente onde estava. */}
          <Route path="produtos/:productId/*" element={<ProductEditorPage mode="edit" />} />
          <Route path="produtos/:productId/preview" element={<ProductPreviewPage />} />

          <Route path="colecoes" element={<CollectionListPage />} />
          <Route path="categorias" element={<CategoryListPage />} />
          <Route path="reconciliacao" element={<ReconciliationPage />} />
          <Route path="armazenamento" element={<StoragePage />} />

          <Route path="*" element={<AdminNotFound />} />
        </Route>
      </Routes>
    </QueryClientProvider>
  );
}
