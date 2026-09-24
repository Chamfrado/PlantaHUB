import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { describe, expect, it } from 'vitest';

import { server } from '../../test/msw/server';
import { ToastProvider } from '../ui/ToastProvider';
import ForgotPasswordForm from './ForgotPasswordForm';

const API = 'http://localhost:8080';

function renderForm() {
  return render(
    <ToastProvider>
      <MemoryRouter initialEntries={['/forgot-password']}>
        <Routes>
          <Route path="/forgot-password" element={<ForgotPasswordForm />} />
          <Route path="/login" element={<p>tela de login</p>} />
        </Routes>
      </MemoryRouter>
    </ToastProvider>
  );
}

describe('ForgotPasswordForm', () => {
  it('percorre as três etapas pelo SMS e volta para o login', async () => {
    const user = userEvent.setup();
    const bodies: Record<string, unknown> = {};

    server.use(
      http.post(`${API}/v1/auth/password-reset/request`, async ({ request }) => {
        bodies.request = await request.json();
        return new HttpResponse(null, { status: 202 });
      }),
      http.post(`${API}/v1/auth/password-reset/verify`, async ({ request }) => {
        bodies.verify = await request.json();
        return HttpResponse.json({ resetToken: 'tok-123' });
      }),
      http.post(`${API}/v1/auth/password-reset/confirm`, async ({ request }) => {
        bodies.confirm = await request.json();
        return new HttpResponse(null, { status: 204 });
      })
    );

    renderForm();

    await user.type(screen.getByLabelText('E-mail'), 'maria@exemplo.com');
    await user.click(await screen.findByText('SMS'));
    await user.click(screen.getByRole('button', { name: 'Enviar código' }));

    expect(await screen.findByText(/enviamos um código de 6 dígitos por SMS/)).toBeInTheDocument();
    expect(bodies.request).toEqual({ email: 'maria@exemplo.com', channel: 'SMS' });
    expect(screen.getByRole('button', { name: /Reenviar código em \d+s/ })).toBeDisabled();

    // Letras sao descartadas: o campo so aceita os 6 digitos.
    await user.type(screen.getByLabelText('Código'), '12a3456');
    await user.click(screen.getByRole('button', { name: 'Validar código' }));

    expect(await screen.findByLabelText('Nova senha')).toBeInTheDocument();
    expect(bodies.verify).toEqual({ email: 'maria@exemplo.com', code: '123456' });

    await user.type(screen.getByLabelText('Nova senha'), 'senha-nova-1');
    await user.type(screen.getByLabelText('Confirmar nova senha'), 'senha-nova-1');
    await user.click(screen.getByRole('button', { name: 'Salvar nova senha' }));

    expect(await screen.findByText('tela de login')).toBeInTheDocument();
    expect(bodies.confirm).toEqual({ resetToken: 'tok-123', newPassword: 'senha-nova-1' });
  });

  it('sem provedor de SMS, oferece só o e-mail', async () => {
    server.use(
      http.get(`${API}/v1/auth/password-reset/channels`, () =>
        HttpResponse.json({ email: true, sms: false })
      )
    );

    renderForm();

    await waitFor(() => expect(screen.queryByText('Receber o código por')).not.toBeInTheDocument());
    expect(screen.getByRole('button', { name: 'Enviar código' })).toBeInTheDocument();
  });

  it('mostra a mensagem certa para código errado', async () => {
    const user = userEvent.setup();
    server.use(
      http.post(`${API}/v1/auth/password-reset/verify`, () =>
        HttpResponse.json({ error: 'invalid_or_expired_code' }, { status: 400 })
      )
    );

    renderForm();

    await user.type(screen.getByLabelText('E-mail'), 'maria@exemplo.com');
    await user.click(screen.getByRole('button', { name: 'Enviar código' }));
    await user.type(await screen.findByLabelText('Código'), '000000');
    await user.click(screen.getByRole('button', { name: 'Validar código' }));

    expect(await screen.findByText(/Código inválido ou expirado/)).toBeInTheDocument();
  });

  it('avisa quando o limite de pedidos foi atingido', async () => {
    const user = userEvent.setup();
    server.use(
      http.post(`${API}/v1/auth/password-reset/request`, () =>
        HttpResponse.json({ error: 'too_many_requests' }, { status: 429 })
      )
    );

    renderForm();

    await user.type(screen.getByLabelText('E-mail'), 'maria@exemplo.com');
    await user.click(screen.getByRole('button', { name: 'Enviar código' }));

    expect(await screen.findByText(/Muitas tentativas seguidas/)).toBeInTheDocument();
    expect(screen.queryByLabelText('Código')).not.toBeInTheDocument();
  });

  it('recusa senhas diferentes sem chamar a API', async () => {
    const user = userEvent.setup();
    let confirmed = false;
    server.use(
      http.post(`${API}/v1/auth/password-reset/confirm`, () => {
        confirmed = true;
        return new HttpResponse(null, { status: 204 });
      })
    );

    renderForm();

    await user.type(screen.getByLabelText('E-mail'), 'maria@exemplo.com');
    await user.click(screen.getByRole('button', { name: 'Enviar código' }));
    await user.type(await screen.findByLabelText('Código'), '123456');
    await user.click(screen.getByRole('button', { name: 'Validar código' }));

    await user.type(await screen.findByLabelText('Nova senha'), 'senha-nova-1');
    await user.type(screen.getByLabelText('Confirmar nova senha'), 'outra-senha-2');
    await user.click(screen.getByRole('button', { name: 'Salvar nova senha' }));

    expect(await screen.findByText('As senhas não coincidem.')).toBeInTheDocument();
    expect(confirmed).toBe(false);
  });

  it('token expirado volta para o início', async () => {
    const user = userEvent.setup();
    server.use(
      http.post(`${API}/v1/auth/password-reset/confirm`, () =>
        HttpResponse.json({ error: 'invalid_or_expired_token' }, { status: 400 })
      )
    );

    renderForm();

    await user.type(screen.getByLabelText('E-mail'), 'maria@exemplo.com');
    await user.click(screen.getByRole('button', { name: 'Enviar código' }));
    await user.type(await screen.findByLabelText('Código'), '123456');
    await user.click(screen.getByRole('button', { name: 'Validar código' }));
    await user.type(await screen.findByLabelText('Nova senha'), 'senha-nova-1');
    await user.type(screen.getByLabelText('Confirmar nova senha'), 'senha-nova-1');
    await user.click(screen.getByRole('button', { name: 'Salvar nova senha' }));

    expect(await screen.findByText(/O prazo para definir a nova senha acabou/)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Enviar código' })).toBeInTheDocument();
  });
});
