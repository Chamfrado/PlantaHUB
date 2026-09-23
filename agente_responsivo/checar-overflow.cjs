#!/usr/bin/env node
/*
 * checar-overflow.cjs — mede overflow horizontal de uma rota em várias larguras.
 *
 * Instalação UMA vez, FORA do repositório (não mexe no package.json do projeto).
 * Funciona igual no Git Bash do Windows, macOS e Linux:
 *   npm i --prefix ~/.resp-tools playwright
 *   npx --prefix ~/.resp-tools playwright install chromium
 * (Outra pasta: defina a variável RESP_TOOLS com o caminho dela.)
 *
 * Uso:
 *   node checar-overflow.cjs \
 *     --url "http://localhost:<porta>/<rota>" \
 *     --out agente_responsivo/evidencias/RESP-TELA-001/depois \
 *     [--storage caminho/storageState.json] [--espera "<seletor css>"]
 *
 * Saída: <out>/resultado.json e capturas <out>/<largura>.png (360, 768, 1366).
 * Código de saída: 0 = sem overflow; 1 = overflow encontrado; 2 = erro/sessão.
 * Só LÊ a página: não clica, não digita, não envia formulário.
 */
const fs = require('fs');
const os = require('os');
const path = require('path');

function carregarPlaywright() {
  const base = process.env.RESP_TOOLS || path.join(os.homedir(), '.resp-tools');
  try {
    return require(require.resolve('playwright', { paths: [path.join(base, 'node_modules'), base] }));
  } catch (_) {
    try {
      return require('playwright');
    } catch (e) {
      console.error(`Playwright não encontrado em ${base}. Rode a instalação do topo deste script.`);
      process.exit(2);
    }
  }
}
const { chromium } = carregarPlaywright();

const LARGURAS = [320, 360, 375, 390, 414, 480, 600, 768, 820, 1024, 1280, 1366, 1440, 1920];
const CAPTURAS = new Set([360, 768, 1366]);

function arg(nome) {
  const i = process.argv.indexOf(`--${nome}`);
  return i > -1 ? process.argv[i + 1] : undefined;
}

(async () => {
  const url = arg('url');
  const out = arg('out');
  const storage = arg('storage');
  const espera = arg('espera');
  if (!url || !out) {
    console.error('Uso: --url <url> --out <pasta> [--storage <json>] [--espera <seletor>]');
    process.exit(2);
  }
  fs.mkdirSync(out, { recursive: true });

  let browser;
  try {
    browser = await chromium.launch();
  } catch (e) {
    console.error('Não foi possível abrir o Chromium. Rode o passo de instalação no topo do script.\n' + e.message);
    process.exit(2);
  }
  const resultado = { url, data: new Date().toISOString(), larguras: [] };
  let houveOverflow = false;

  try {
    for (const largura of LARGURAS) {
      const altura = largura < 768 ? 800 : 900;
      const context = await browser.newContext({
        viewport: { width: largura, height: altura },
        isMobile: largura < 768,
        hasTouch: largura < 1024,
        ...(storage ? { storageState: storage } : {}),
      });
      const page = await context.newPage();
      await page.goto(url, { waitUntil: 'networkidle', timeout: 45000 });
      if (espera) await page.waitForSelector(espera, { timeout: 20000 });
      await page.waitForTimeout(500);

      const finalUrl = page.url();
      if (/\/login(\b|\/|\?|$)/i.test(new URL(finalUrl).pathname)) {
        resultado.erro = `Redirecionado para login (${finalUrl}). Sessão ausente ou expirada.`;
        await context.close();
        fs.writeFileSync(path.join(out, 'resultado.json'), JSON.stringify(resultado, null, 2));
        console.error(resultado.erro);
        await browser.close();
        process.exit(2);
      }

      const medida = await page.evaluate((larguraAlvo) => {
        // Mede contra a largura configurada, não contra innerWidth: sem <meta viewport>
        // o celular renderiza em ~980px e esconderia o problema.
        const vw = larguraAlvo;
        const metaViewport = !!document.querySelector('meta[name="viewport"]');
        const doc = document.documentElement;
        const scrollW = Math.max(doc.scrollWidth, document.body ? document.body.scrollWidth : 0);

        // Um elemento que vaza dentro de um ancestral com overflow-x auto/scroll/hidden
        // é scroll localizado (permitido), não overflow da página.
        const contido = (el) => {
          for (let p = el.parentElement; p && p !== document.body; p = p.parentElement) {
            const ox = getComputedStyle(p).overflowX;
            if (ox === 'auto' || ox === 'scroll' || ox === 'hidden' || ox === 'clip') return true;
          }
          return false;
        };
        const descrever = (el) => {
          const id = el.id ? `#${el.id}` : '';
          const cls = typeof el.className === 'string' && el.className.trim()
            ? '.' + el.className.trim().split(/\s+/).slice(0, 3).join('.') : '';
          return `${el.tagName.toLowerCase()}${id}${cls}`;
        };

        const vazando = [];
        for (const el of document.body.querySelectorAll('*')) {
          const r = el.getBoundingClientRect();
          if (r.width === 0 || r.height === 0) continue;
          const st = getComputedStyle(el);
          if (st.visibility === 'hidden' || st.display === 'none') continue;
          if ((r.right > vw + 1 || r.left < -1) && !contido(el)) {
            vazando.push({ el: descrever(el), left: Math.round(r.left), right: Math.round(r.right), width: Math.round(r.width) });
          }
        }
        return { vw, metaViewport, scrollW, overflowPagina: scrollW > vw + 1, vazando: vazando.slice(0, 25) };
      }, largura);

      if (medida.overflowPagina || medida.vazando.length || !medida.metaViewport) houveOverflow = true;
      if (CAPTURAS.has(largura)) {
        await page.screenshot({ path: path.join(out, `${largura}.png`), fullPage: true });
      }
      resultado.larguras.push({ largura, ...medida });
      await context.close();
    }
  } catch (e) {
    resultado.erro = String(e && e.message ? e.message : e);
    fs.writeFileSync(path.join(out, 'resultado.json'), JSON.stringify(resultado, null, 2));
    console.error(resultado.erro);
    await browser.close();
    process.exit(2);
  }

  await browser.close();
  resultado.overflow = houveOverflow;
  fs.writeFileSync(path.join(out, 'resultado.json'), JSON.stringify(resultado, null, 2));
  for (const l of resultado.larguras) {
    const flag = !l.metaViewport ? 'SEM-META-VIEWPORT' : (l.overflowPagina || l.vazando.length ? 'OVERFLOW' : 'ok');
    console.log(`${String(l.largura).padStart(4)}px  ${flag}  scrollWidth=${l.scrollW}  vazando=${l.vazando.length}`);
  }
  process.exit(houveOverflow ? 1 : 0);
})();
