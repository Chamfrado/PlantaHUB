# HISTÓRICO (append-only — nunca apagar nem reescrever)

<!-- Formato: ## AAAA-MM-DD HH:mm — EVENTO
Eventos: AGENTE_INICIADO, WORKTREE_CRIADA, AGENTE_RETOMADO, MAPEAMENTO_INICIADO,
MAPEAMENTO_FINALIZADO, TELA_INICIADA, ALTERACAO, MEDICAO, TELA_CORRIGIDA,
TELA_SEM_ALTERACAO, TELA_BLOQUEADA, REVALIDACAO_SOLICITADA, CONTEXTO_PRE_COMPACTACAO,
CONTEXTO_RECUPERADO, DIVERGENCIA, FORA_ESCOPO, COMMIT, SINCRONIZACAO_ORIGEM,
CONFLITO_DE_MERGE, PRONTO_PARA_MERGE, PAUSA_SOLICITADA, AGENTE_PAUSADO -->

## 2026-09-23 07:37 — AGENTE_INICIADO
Execution ID RESP-sistema-inteiro-2026-09-23-07-37-17. Pedido: `/agente-responsivo` sem
escopo; perguntado ao João, que escolheu os quatro grupos (Vitrine pública, Conta do
cliente, Painel admin, Layout global) — todas as rotas do front.
Origem: branch `main`, HEAD `b9d419b`, worktree original CLEAN.

## 2026-09-23 07:37 — WORKTREE_CRIADA
`git worktree add ../PlantaHub-responsive-sistema-inteiro-20260923-073717 -b agent/responsive-sistema-inteiro-20260923-073717 b9d419b`
Dependências instaladas com `npm ci` em `apps/web/plantahub-web`.
Playwright instalado FORA do repositório em `~/.resp-tools` (o `package.json` do projeto
não foi tocado).

## 2026-09-23 07:37 — MAPEAMENTO_INICIADO
Stack descoberto: React 19 + React Router 7 + Tailwind 4 (tema em `src/index.css`, sem
biblioteca de UI). Breakpoints: padrão do Tailwind 4.
Padrões importados: nenhum — `agente_responsivo/PADROES.md` não existe em `main` nem em
nenhuma branch `agent/responsive-*`. Esta é a primeira execução do agente no PlantaHub.

## 2026-09-23 07:38 — MAPEAMENTO_FINALIZADO
32 telas inventariadas em `TELAS.md` a partir de `src/app/routers/AppRoutes.tsx` e
`src/pages/admin/AdminApp.tsx`. 8 marcadas como candidatas a padrão (uma por tipo).
18 telas dependem de sessão e 4 de registro real — ver ESTADO.md → Alertas.

## 2026-09-23 07:51 — TELA_BLOQUEADA (ambiente, não tela)
Tentativa de subir a API na worktree do agente (`./serve.sh --port 8085`) falhou no boot:
Flyway reprovou a validação do banco local `plantahub`.
`Migration checksum mismatch for migration version 20` — aplicado no banco `-1271994643`,
resolvido localmente `-1508217695`. O arquivo `V20__catalog_foundation.sql` tem um único
commit (`8c6a09f`), então o banco local aplicou um rascunho anterior dele.
Sem API não há sessão, e sem sessão as 18 telas privadas ficam sem evidência.
Resolver isso é decisão do João: mexe em banco e backend, fora do escopo do agente.

## 2026-09-23 08:02 — DIVERGENCIA (banco local × repositório) — REPAIR NÃO EXECUTADO
O João autorizou `flyway repair`. A inspeção anterior ao comando mostrou que o
diagnóstico que embasou a autorização estava ERRADO, e o repair quebraria o banco.
Não foi executado. Fatos:
- A V20 aplicada no banco é `add google auth to users` (22/06/2026). Essa migration
  NÃO EXISTE no repositório. O banco tem `app_user.auth_provider` e `app_user.google_sub`.
- A V20 do repositório é `catalog_foundation`. NADA dela foi aplicado: nenhuma das 16
  colunas existe, nenhum dos 3 índices existe, e as 3 constraints que ela deveria
  derrubar continuam de pé (`uk_asset_ppt_kind_version`, `uk_ent_user_product_plan`,
  `uq_entitlement_user_product_plan`).
- O banco para na 20; o repositório vai até a V30. Faltam 11 migrations.
- `V21__apoio_collection.sql` insere em `plan_type` usando `purchasable`,
  `bundled_with_every_offer` e `sort_order` — colunas criadas pela V20. Com o repair,
  o Flyway daria a V20 por feita e a V21 falharia em "column does not exist".
Conclusão: não é rascunho editado, é COLISÃO DE NÚMERO entre duas linhas de
desenvolvimento. O banco local pertence a uma linha (Google auth) que não está neste
repositório. Resolver isso é decisão do João sobre os dados dele.

## 2026-09-23 07:57 — AMBIENTE_DESTRAVADO
Banco `plantahub_resp` criado do zero (`./serve.sh --db plantahub_resp --create-db --port 8085`).
Flyway aplicou 29 migrations, schema em v30. O banco `plantahub` do João não foi tocado.
`DevAdminAccountRunner` criou `adm@plantahub.com` (ADMIN) no boot, como previsto.
API em http://localhost:8085 · front do agente em http://localhost:5180.

## 2026-09-23 08:05 — SESSAO_GERADA
`agente_responsivo/storage-admin.json` gravado por `gerar-sessao.cjs`; `/v1/auth/me`
confirmou papel ADMIN. Arquivo fora do Git (`.gitignore` local do agente).

## 2026-09-23 08:08 — MEDICAO — RESP-TELA-020 (antes)
`/admin/produtos` com sessão: exit 0, nenhuma das 14 larguras rola a página.
As capturas contam outra história: em 360 px o menu do painel corta em "Armazen…" e as
colunas PREÇO BASE e AÇÕES ficam fora da tela. Registrado como D-005 em ESTADO.md:
o script não substitui a leitura das capturas.

## 2026-09-23 — CONTEXTO_RECUPERADO
Sessão nova. Git: 7b78012, worktree limpa, branch do agente correta. Nenhuma tela
EM ANDAMENTO; 32 NÃO INICIADAS. Sem divergência. Padrões reimportados: nenhum aprovado
em `main` nem em outra `agent/responsive-*`. Próxima ação: subir API 8085 + front 5180
e iniciar RESP-TELA-001.

## 2026-09-23 08:14 — AMBIENTE_REINICIADO
João pediu para derrubar e subir de novo. Os processos antigos (java 46648 em 8085, vite 20520
em 5180) ainda estavam vivos e foram encerrados pelo agente. O primeiro `serve.sh` pulou para
8086 (encerrado); o `dev.sh` tinha resolvido a API para 8080 pelo `.env.local`. Resubidos:
API 8085 (`--strict-port`, banco `plantahub_resp`), front 5180 com `--api http://localhost:8085`.
Registrado como D-006.

## 2026-09-23 08:25 — TELA_CONCLUIDA — RESP-TELA-001
Mini-carrinho vazava à esquerda abaixo de `sm` (x=−110 em 360, −150 em 320). Corrigido em
`Header.tsx:92` e `MiniCartDropdown.tsx:18,26,75`. Overflow exit 0 antes e depois (14 larguras);
dropdowns medidos abertos (D-007). Pendente de padrão: navegação mobile (links do cabeçalho
somem abaixo de md sem menu equivalente). Status: CORRIGIDA — AGUARDANDO VALIDAÇÃO VISUAL.

## 2026-09-23 08:33 — TELA_CONCLUIDA — RESP-TELA-002
Cabeçalho do painel com 527 px em 320/360: Sair e Ver site inalcançáveis. Faixa de links passou a
rolar localmente (`AdminLayout.tsx:57`), ações fixas (`:71`). Overflow exit 0 antes e depois;
navegação pela faixa testada. Status: CORRIGIDA — AGUARDANDO VALIDAÇÃO VISUAL.

## 2026-09-23 08:45 — TELA_CONCLUIDA — RESP-TELA-003
Home sem defeito objetivo em 360/768/1366 (fatias da página inteira). Descoberto que o #root é o
scroller (D-008): checar-overflow não mede página nesse app; criado `fatias.cjs`. Status:
CANDIDATA A PADRÃO — AGUARDANDO APROVAÇÃO, sem alteração de código.

## 2026-09-23 08:55 — TELA_CONCLUIDA — RESP-TELA-004
`/produtos`: nome do produto invisível e área sobre o preço em 360. Linha do acordeão passa a quebrar
abaixo de sm. Sem vazamento em 320/360/768/1366; expandir testado. CANDIDATA A PADRÃO listagem vitrine.

## 2026-09-23 09:03 — TELA_CONCLUIDA — RESP-TELA-005
`/casas/confort`: aside de compra 25 px mais largo que a coluna em 320. `grid-cols-1` em
`ProductPlanSelector.tsx:44`. Sem vazamento 320-1366. CANDIDATA A PADRÃO detalhe.

## 2026-09-23 09:12 — TELAS_CONCLUIDAS — RESP-TELA-009, 012, 013
009: grid-cols-1 + break-words no e-mail (Carrer.tsx:150,188). 012: candidata formulário sem código.
013: sem alteração. Triagem de 006-008, 010-011, 014-015, 017, 019 medida, ainda sem leitura das capturas.

## 2026-09-23 09:30 — LOTE_CONCLUIDO — RESP-TELA-006/007/008/010/011/014/015/016/017/018/019
007 corrigida (ContactPage.tsx:417). 006/008/010/011/019 sem alteração. 014 candidata formulário com abas
(sem código). 015/017 BLOQUEADA — SEM DADO (só estado vazio, íntegro); 016/018 BLOQUEADA — SEM DADO.
PROB-001 e PROB-002 em FORA_ESCOPO. Detector de transbordo de caixa adicionado (D-009).
