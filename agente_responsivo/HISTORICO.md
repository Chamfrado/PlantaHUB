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
