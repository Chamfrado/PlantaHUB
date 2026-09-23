#!/usr/bin/env node
/*
 * gerar-sessao.cjs — produz um `storageState` logado para o checar-overflow.cjs usar.
 *
 * Por que existe: as telas privadas do PlantaHub (todo `/admin/*`, `/configs`,
 * `/biblioteca`, `/carrinho`, `/pedidos/:id`) só renderizam com sessão. Sem isto o
 * medidor de overflow cai na tela de login e a captura viraria evidência falsa.
 *
 * A sessão do PlantaHub é um cookie HttpOnly emitido pela API (`lib/http.ts` manda
 * `credentials: 'include'`). Cookie ignora porta: o cookie de `localhost:8085` vale
 * para o front em `localhost:5180`. Por isso dá para autenticar direto contra a API,
 * sem abrir o navegador na tela de login.
 *
 * Uso:
 *   node agente_responsivo/gerar-sessao.cjs \
 *     --api http://localhost:8085 \
 *     --email adm@plantahub.com --senha 123456 \
 *     --out agente_responsivo/storage-admin.json
 *
 * Saída: o arquivo JSON do `--out` e uma linha dizendo o papel da conta.
 * Código de saída: 0 = sessão gravada; 1 = credencial recusada; 2 = erro de uso/rede.
 *
 * Só autentica. Não cadastra, não altera dado nenhum.
 */
const os = require('os');
const path = require('path');

function carregarPlaywright() {
  const base = process.env.RESP_TOOLS || path.join(os.homedir(), '.resp-tools');
  try {
    return require(require.resolve('playwright', {
      paths: [path.join(base, 'node_modules'), base],
    }));
  } catch (_) {
    try {
      return require('playwright');
    } catch (e) {
      console.error(`Playwright não encontrado em ${base}.`);
      console.error('Instale com: npm i --prefix ~/.resp-tools playwright');
      process.exit(2);
    }
  }
}

const { request } = carregarPlaywright();

function arg(nome) {
  const i = process.argv.indexOf(`--${nome}`);
  return i > -1 ? process.argv[i + 1] : undefined;
}

(async () => {
  const api = arg('api');
  const email = arg('email');
  const senha = arg('senha');
  const out = arg('out');

  if (!api || !email || !senha || !out) {
    console.error('Uso: --api <url> --email <e-mail> --senha <senha> --out <arquivo.json>');
    process.exit(2);
  }

  const ctx = await request.newContext({ baseURL: api });

  let login;
  try {
    login = await ctx.post('/v1/auth/login', { data: { email, password: senha } });
  } catch (e) {
    console.error(`Não foi possível falar com a API em ${api}: ${e.message}`);
    console.error('Suba com: apps/api/plantahub-api/serve.sh');
    await ctx.dispose();
    process.exit(2);
  }

  if (!login.ok()) {
    console.error(`Login recusado (HTTP ${login.status()}) para ${email}.`);
    console.error(await login.text().catch(() => ''));
    await ctx.dispose();
    process.exit(1);
  }

  // Confirma que o cookie realmente autentica, em vez de confiar no 200 do login: um
  // storageState que parece bom e não abre as telas custa uma rodada inteira de medição.
  const me = await ctx.get('/v1/auth/me');
  if (!me.ok()) {
    console.error(`Login passou mas /v1/auth/me devolveu HTTP ${me.status()} — sessão não persistiu.`);
    await ctx.dispose();
    process.exit(1);
  }

  const perfil = await me.json().catch(() => ({}));

  await ctx.storageState({ path: out });
  await ctx.dispose();

  const papel = (perfil.role || 'desconhecido').toString().replace(/^ROLE_/, '');
  console.log(`Sessão gravada em ${out}`);
  console.log(`  conta: ${perfil.email || email}`);
  console.log(`  papel: ${papel}`);
})();
