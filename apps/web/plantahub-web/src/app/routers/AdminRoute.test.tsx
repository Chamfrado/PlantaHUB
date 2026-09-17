import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';

import AdminRoute from './AdminRoute';

const authState = {
  isAuthenticated: false,
  isAdmin: false,
  isLoading: false,
};

vi.mock('../../contexts/AuthContext', () => ({
  useAuth: () => authState,
}));

function renderGuard() {
  return render(
    <MemoryRouter initialEntries={['/admin/produtos']}>
      <Routes>
        <Route
          path="/admin/*"
          element={
            <AdminRoute>
              <div>painel</div>
            </AdminRoute>
          }
        />
        <Route path="/login" element={<div>tela de login</div>} />
      </Routes>
    </MemoryRouter>
  );
}

describe('AdminRoute', () => {
  it('espera enquanto a sessão resolve', () => {
    Object.assign(authState, { isLoading: true, isAuthenticated: false, isAdmin: false });

    renderGuard();

    // Espera visível, e não tela branca: a rota do painel ainda vai baixar um chunk.
    expect(screen.getByRole('status')).toBeInTheDocument();
    expect(screen.queryByText('painel')).not.toBeInTheDocument();
  });

  it('manda visitante anônimo para o login', () => {
    Object.assign(authState, { isLoading: false, isAuthenticated: false, isAdmin: false });

    renderGuard();

    expect(screen.getByText('tela de login')).toBeInTheDocument();
  });

  it('mostra 403 para usuário comum, em vez de redirecionar em silêncio', () => {
    Object.assign(authState, { isLoading: false, isAuthenticated: true, isAdmin: false });

    renderGuard();

    // Redirecionar sem explicação tornaria "por que não consigo entrar?" indepurável.
    expect(screen.getByText('Acesso restrito')).toBeInTheDocument();
    expect(screen.queryByText('painel')).not.toBeInTheDocument();
  });

  it('deixa o administrador passar', () => {
    Object.assign(authState, { isLoading: false, isAuthenticated: true, isAdmin: true });

    renderGuard();

    expect(screen.getByText('painel')).toBeInTheDocument();
  });
});
