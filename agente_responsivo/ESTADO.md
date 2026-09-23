# ESTADO — AGENTE RESPONSIVO

> Primeiro arquivo a ler em qualquer sessão. Se algo aqui contradiz o Git, o Git vence.

## Identificação
- Execution ID: RESP-sistema-inteiro-2026-09-23-07-37-17
- Início original: 2026-09-23 07:37 (nunca alterar em retomadas)
- Última atualização: 2026-09-23 07:37
- Status geral: MAPEANDO

## Escopo desta execução
- Pedido original (texto do João): `/agente-responsivo` sem escopo. Perguntado; respondeu
  selecionando os quatro grupos oferecidos: **Vitrine pública + Conta do cliente +
  Painel admin + Layout global** — ou seja, todas as rotas do front.
- Prefixos de rota resolvidos:
  - Layout global: `MainLayout`, `AdminLayout` (shell, cabeçalho, menu, mini-carrinho, rodapé)
  - Vitrine pública: `/`, `/produtos`, `/:category/:slug`, `/sobre`, `/contato`, `/faq`,
    `/trabalhe-conosco`, `/legal/termos`, `/legal/privacidade`
  - Conta do cliente: `/login`, `/register`, `/configs`, `/biblioteca`,
    `/biblioteca/:productId/:planTypeCode`, `/carrinho`, `/pedidos/:orderId`,
    `/pagamento/sucesso`
  - Painel admin: `/admin/produtos`, `/admin/produtos/novo`,
    `/admin/produtos/:productId/{geral,conteudo,imagens,ofertas,arquivos}`,
    `/admin/produtos/:productId/preview`, `/admin/colecoes`, `/admin/categorias`,
    `/admin/reconciliacao`, `/admin/armazenamento`
- Total de telas no escopo: 32

## Fora do escopo desta execução
- Telas quebradas vistas fora dos prefixos (não tocadas): nenhuma — o escopo cobre todas
  as rotas declaradas em `AppRoutes.tsx` e `AdminApp.tsx`.
- Pendências para execução de layout global: não se aplica (o layout global está DENTRO
  deste escopo).

## Git
- Worktree original: C:/Users/Work/Documents/projetos/PlantaHub
- Branch origem: main
- HEAD origem inicial: b9d419b
- Worktree original no início: CLEAN
- Worktree agente: C:/Users/Work/Documents/projetos/PlantaHub-responsive-sistema-inteiro-20260923-073717
- Branch agente: agent/responsive-sistema-inteiro-20260923-073717
- Último commit do agente: fab138c — docs(responsive-agent): inicia execução
- Porta do servidor do agente: 5180 (original usa 5173; 5180 está dentro da faixa
  5173-5182 liberada no CORS da API, ver `apps/web/plantahub-web/dev.sh`)
- storageState por perfil (caminhos): **AUSENTES** — necessários para as telas privadas.
  - cliente: agente_responsivo/storage-cliente.json (a ser gerado pelo João)
  - admin:   agente_responsivo/storage-admin.json   (a ser gerado pelo João)

## Stack front (descoberto, não presumido)
- Framework: React 19.2 + TypeScript 5.9, Vite 7, React Router DOM 7.12
- Biblioteca de UI: **nenhuma** — componentes próprios em `src/components/`
  (`components/admin/ui/primitives.tsx` concentra primitivos do painel).
  Ícones: lucide-react. Dados do painel: @tanstack/react-query.
- Sistema de estilo: Tailwind CSS 4.1 via `@tailwindcss/vite`, tema em `src/index.css`
  (`@theme` com tokens de cor da marca). Sem arquivo `tailwind.config`.
- Breakpoints já usados no projeto: padrão do Tailwind 4 — `sm 640` · `md 768` ·
  `lg 1024` · `xl 1280` · `2xl 1536`. **Não criar breakpoint novo.**
- Diretório do front / comando do servidor: `apps/web/plantahub-web`;
  `./dev.sh --port 5180 --strict-port` (resolve `VITE_API_URL` sozinho).
  Backend: `apps/api/plantahub-api/serve.sh` (Spring Boot / Java 21) — **nunca alterado**.

## Regras do projeto
- Documentos lidos: `README.md` (raiz), `apps/web/plantahub-web/dev.sh` (cabeçalho),
  `vite.config.ts`, `src/index.css`. Não existe `CLAUDE.md` nem `AGENTS.md` no repositório.
- Testes: PERMITIDOS rodar, **PROIBIDO criar ou alterar** (regra da skill).
  Suítes existentes: `npm test` (vitest), `npm run e2e` (playwright), `npm run lint`,
  `npm run build` (tsc -b + vite build).
- Pode rodar: build · lint · typecheck · testes (somente execução, sem escrita)
- Branch/convenção de merge: origem `main`. O merge é decisão do João — o agente para
  em PRONTO PARA MERGE.

## Progresso
- Total: 32 · Aguardando validação visual: 0 · Sem alteração: 0 · Bloqueadas: 0 ·
  Revalidação: 0 · Pendentes: 32

## Tela atual
- ID / nome / rota: nenhuma ainda
- Iniciada em: —
- Feito até agora: worktree criada, dependências instaladas (`npm ci`), Playwright
  instalado fora do repositório (`~/.resp-tools`), inventário de 32 telas escrito em
  `TELAS.md`, padrões importados (nenhum existia).
- Falta: subir o servidor na porta 5180, obter storageState dos perfis, iniciar por
  RESP-TELA-001.
- **Próxima ação exata:** na worktree do agente, rodar
  `cd apps/web/plantahub-web && ./dev.sh --port 5180 --strict-port`; medir
  RESP-TELA-001 (shell público, via `/`) com
  `node agente_responsivo/checar-overflow.cjs --url http://localhost:5180/ --out agente_responsivo/evidencias/RESP-TELA-001/antes`.

## Trabalho em curso não commitado
- Arquivos: nenhum código do app alterado. Apenas `agente_responsivo/` (novo).
- O que já mudou: nada em `apps/` nem `docs/`.
- Build passa: NÃO TESTADO (nada do app foi tocado)

## Decisões (valem para as próximas telas)
- **D-001 (2026-09-23):** usar exclusivamente os breakpoints padrão do Tailwind 4 já
  presentes no projeto (`sm/md/lg/xl/2xl`). Nenhuma media query solta em px, nenhum
  breakpoint novo.
- **D-002 (2026-09-23):** o projeto não usa biblioteca de UI. Correções são feitas com
  classes utilitárias Tailwind nos próprios componentes, sem introduzir dependência.
- **D-003 (2026-09-23):** porta do agente fixada em 5180 para ficar dentro da faixa de
  CORS da API (5173-5182); fora dela o painel não carrega dado nenhum e as capturas
  sairiam vazias — o que seria evidência falsa.
- **D-004 (2026-09-23):** não existe `PADROES.md` aprovado em nenhuma branch. Esta é a
  primeira execução, então cada tipo de tela recebe **uma** candidata a padrão.

## Componentes compartilhados alterados
- (nenhum)

## Alertas
- **A-001 — BLOQUEIO PARCIAL PREVISTO:** 18 das 32 telas exigem sessão
  (`/configs`, `/biblioteca*`, `/carrinho`, `/pedidos/:id`, `/pagamento/sucesso` e todo
  `/admin/*`). Sem `storageState` de cliente e de admin, o script cai na tela de login e
  elas ficam `BLOQUEADA — SEM SESSÃO DO PERFIL`. O agente **não** faz login por conta
  própria. O João precisa gerar os dois arquivos uma vez.
- **A-002:** 4 telas dependem de registro real no ambiente
  (`/:category/:slug`, `/biblioteca/:productId/:planTypeCode`, `/pedidos/:orderId`,
  `/admin/produtos/:productId/*`). O id sai da própria listagem na interface; se o banco
  de desenvolvimento estiver vazio, ficam `BLOQUEADA — SEM DADO`. O agente não cria
  registro para testar.
- **A-003:** a API (Spring Boot) precisa estar no ar para as telas com dado. Subir com
  `apps/api/plantahub-api/serve.sh` — somente leitura, nenhuma escrita no banco.
