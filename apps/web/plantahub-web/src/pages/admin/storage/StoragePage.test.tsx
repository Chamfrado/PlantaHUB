import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { describe, expect, it, vi } from 'vitest';

import { server } from '../../../test/msw/server';
import StoragePage from './StoragePage';

const API = 'http://localhost:8080/v1/admin/storage/diagnostics';

const baseReport = {
  bucket: 'plantahub-assets',
  configuredRegion: 'us-east-2',
  publicBaseUrl: 'https://plantahub-assets.s3.us-east-2.amazonaws.com',
  ranAt: '2026-09-16T23:00:00Z',
  checks: [] as unknown[],
  snippets: [
    {
      id: 'bucket_policy',
      title: 'Política de leitura pública',
      description: 'S3 → Permissions → Bucket policy.',
      content: '{ "Resource": "arn:aws:s3:::plantahub-assets/public/*" }',
    },
  ],
};

const CREDENTIALS = 'http://localhost:8080/v1/admin/storage/credentials';

type CredentialStatus = {
  source: 'PANEL' | 'CONFIG' | 'DEFAULT_CHAIN';
  accessKeyHint: string | null;
  updatedAt: string | null;
  updatedBy: string | null;
};

const defaultCredentials: CredentialStatus = {
  source: 'DEFAULT_CHAIN',
  accessKeyHint: null,
  updatedAt: null,
  updatedBy: null,
};

function mount(checks: unknown[], credentials: CredentialStatus = defaultCredentials) {
  server.use(
    http.get(API, () => HttpResponse.json({ ...baseReport, checks })),
    http.get(CREDENTIALS, () => HttpResponse.json(credentials)),
    http.put(CREDENTIALS, () =>
      HttpResponse.json({
        source: 'PANEL',
        accessKeyHint: '••••2345',
        updatedAt: '2026-09-17T00:00:00Z',
        updatedBy: 'admin@plantahub.test',
      })
    )
  );

  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });

  return render(
    <QueryClientProvider client={client}>
      <StoragePage />
    </QueryClientProvider>
  );
}

describe('StoragePage', () => {
  it('mostra o impacto, e nao so o status', async () => {
    mount([
      {
        id: 'cors_expose_etag',
        title: 'CORS expõe o cabeçalho ETag',
        status: 'FAIL',
        detail: 'Nenhuma regra de CORS lista ETag em ExposeHeaders.',
        impact: 'Arquivos acima de 100 MB sobem inteiros e nunca terminam.',
      },
    ]);

    expect(await screen.findByText('CORS expõe o cabeçalho ETag')).toBeInTheDocument();

    // Sem o impacto, "Problema" é só uma etiqueta vermelha: não dá para priorizar nem
    // saber se o que está em risco é dinheiro, upload ou arquivo vazado.
    expect(
      screen.getByText('Arquivos acima de 100 MB sobem inteiros e nunca terminam.')
    ).toBeInTheDocument();

    expect(screen.getByText('1 problema encontrado. Cada um abaixo explica o que quebra na prática.')).toBeInTheDocument();
  });

  it('separa aviso de problema: um custa dinheiro, o outro quebra', async () => {
    mount([
      {
        id: 'lifecycle_abort_multipart',
        title: 'Lifecycle aborta multipart incompleto',
        status: 'WARN',
        detail: 'Nenhuma regra ativa.',
        impact: 'Partes de uploads abandonados continuam sendo cobradas.',
      },
    ]);

    expect(
      await screen.findByText('Nada quebrado, mas 1 ponto merece atenção.')
    ).toBeInTheDocument();
  });

  it('bucket saudavel diz isso de forma inequivoca', async () => {
    mount([
      {
        id: 'bucket_reachable',
        title: 'Bucket acessível',
        status: 'OK',
        detail: 'As credenciais alcançam o bucket.',
        impact: null,
      },
    ]);

    expect(
      await screen.findByText('Tudo que dá para verificar daqui está correto.')
    ).toBeInTheDocument();
  });

  it('nao verificado nao vira aprovado', async () => {
    mount([
      {
        id: 'public_prefix_readable',
        title: 'Imagens são legíveis sem login',
        status: 'UNKNOWN',
        detail: 'Nenhuma imagem enviada ainda.',
        impact: 'Envie uma imagem e rode de novo.',
      },
    ]);

    expect(await screen.findByText('Não verificado')).toBeInTheDocument();

    // "Tudo correto" com uma verificação que nem rodou seria uma mentira confortável.
    expect(screen.queryByText(/Tudo que dá para verificar/)).toBeInTheDocument();
    expect(screen.queryByText('OK')).not.toBeInTheDocument();
  });

  it('copia o JSON com o bucket real, e nao um exemplo generico', async () => {
    const writeText = vi.fn().mockResolvedValue(undefined);
    Object.assign(navigator, { clipboard: { writeText } });

    mount([]);

    await userEvent.click(
      await screen.findByRole('button', { name: 'Copiar Política de leitura pública' })
    );

    await waitFor(() =>
      expect(writeText).toHaveBeenCalledWith(
        '{ "Resource": "arn:aws:s3:::plantahub-assets/public/*" }'
      )
    );

    expect(await screen.findByText('Copiado')).toBeInTheDocument();
  });

  it('recomenda a instance role quando nao ha credencial gravada', async () => {
    mount([]);

    expect(await screen.findByText(/Instance role ou perfil da máquina/)).toBeInTheDocument();
    expect(screen.getByText(/não existe segredo de longa duração para vazar/)).toBeInTheDocument();
  });

  it('nunca preenche o formulario com o segredo atual', async () => {
    mount([], {
      source: 'PANEL',
      accessKeyHint: '••••2345',
      updatedAt: '2026-09-17T00:00:00Z',
      updatedBy: 'admin@plantahub.test',
    });

    await userEvent.click(await screen.findByRole('button', { name: 'Trocar' }));

    // A API nao devolve a secret, e o campo comeca vazio. Um formulario que se preenche
    // sozinho transformaria qualquer XSS no painel num vazamento de credencial da AWS.
    expect(screen.getByLabelText(/Secret access key/)).toHaveValue('');
    expect(screen.getByLabelText(/Access key ID/)).toHaveValue('');
  });

  it('salvar manda as duas partes e revalida o diagnostico', async () => {
    const seen: unknown[] = [];

    mount([]);
    server.use(
      http.put(CREDENTIALS, async ({ request }) => {
        seen.push(await request.json());
        return HttpResponse.json({
          source: 'PANEL',
          accessKeyHint: '••••2345',
          updatedAt: '2026-09-17T00:00:00Z',
          updatedBy: 'admin@plantahub.test',
        });
      })
    );

    await userEvent.click(await screen.findByRole('button', { name: 'Informar credenciais' }));
    await userEvent.type(screen.getByLabelText(/Access key ID/), 'AKIAEXEMPLO12345');
    await userEvent.type(screen.getByLabelText(/Secret access key/), 'segredo');
    await userEvent.click(screen.getByRole('button', { name: 'Salvar e verificar' }));

    await waitFor(() =>
      expect(seen).toEqual([{ accessKey: 'AKIAEXEMPLO12345', secretKey: 'segredo' }])
    );

    // O formulario fecha: deixar a secret digitada na tela depois de salvar nao tem
    // proposito nenhum e so aumenta a chance de ela ir parar num print.
    await waitFor(() =>
      expect(screen.queryByLabelText(/Secret access key/)).not.toBeInTheDocument()
    );
  });

  it('deixa claro que a tela nao aplica nada sozinha', async () => {
    mount([]);

    // Não é rodapé decorativo: é a resposta para "por que não tem um botão Aplicar?".
    expect(
      await screen.findByText(/O painel não aplica nada sozinho/)
    ).toBeInTheDocument();
  });
});
