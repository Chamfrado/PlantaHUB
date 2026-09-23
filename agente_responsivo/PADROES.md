# PADRÕES VISUAIS APROVADOS

> Decisões do João. Valem para o sistema todo e para todas as execuções.
> O agente só registra aqui o que o João aprovou — nunca a própria candidata.

## Aprovados

### Painel / landing
- Tela de referência: RESP-TELA-003 — `/`
- Arquivo principal: `Home.tsx + components/home/*`
- Branch de origem da aprovação: agent/responsive-sistema-inteiro-20260923-073717
- Aprovado em: 2026-09-23 10:10 (João: "tá ok")
- Obrigatório copiar à risca: layout atual; cards de produto em 1 coluna até lg; breakpoints só sm/md/lg do Tailwind (D-001)
- Proibido: esconder dado/ação sem equivalente visível; breakpoint novo; media query em px

### Listagem (vitrine)
- Tela de referência: RESP-TELA-004 — `/produtos`
- Arquivo principal: `components/products/ProductAccordionItem.tsx`
- Branch de origem da aprovação: agent/responsive-sistema-inteiro-20260923-073717
- Aprovado em: 2026-09-23 10:10 (João: "tá ok")
- Obrigatório copiar à risca: abaixo de sm a linha do acordeão quebra em 2 (info em cima; preço + seta embaixo, à direita); breakpoints só sm/md/lg do Tailwind (D-001)
- Proibido: esconder dado/ação sem equivalente visível; breakpoint novo; media query em px

### Detalhe
- Tela de referência: RESP-TELA-005 — `/casas/confort`
- Arquivo principal: `components/products/ProductPlanSelector.tsx`
- Branch de origem da aprovação: agent/responsive-sistema-inteiro-20260923-073717
- Aprovado em: 2026-09-23 10:10 (João: "tá ok")
- Obrigatório copiar à risca: grades com grid-cols-1 explícito abaixo de lg; item com flex-wrap e preço ml-auto; breakpoints só sm/md/lg do Tailwind (D-001)
- Proibido: esconder dado/ação sem equivalente visível; breakpoint novo; media query em px

### Formulário
- Tela de referência: RESP-TELA-012 — `/login`
- Arquivo principal: `pages/public/Login/Login.tsx`
- Branch de origem da aprovação: agent/responsive-sistema-inteiro-20260923-073717
- Aprovado em: 2026-09-23 10:10 (João: "tá ok")
- Obrigatório copiar à risca: layout atual: rótulo acima do campo, campo e botão em largura total no mobile; breakpoints só sm/md/lg do Tailwind (D-001)
- Proibido: esconder dado/ação sem equivalente visível; breakpoint novo; media query em px

### Formulário com abas
- Tela de referência: RESP-TELA-014 — `/configs`
- Arquivo principal: `pages/Preferences/Preferences.tsx`
- Branch de origem da aprovação: agent/responsive-sistema-inteiro-20260923-073717
- Aprovado em: 2026-09-23 10:10 (João: "tá ok")
- Obrigatório copiar à risca: layout atual: abas viram lista vertical de cartões abaixo de lg; campos 1 col → 2 col em md; breakpoints só sm/md/lg do Tailwind (D-001)
- Proibido: esconder dado/ação sem equivalente visível; breakpoint novo; media query em px

### Listagem administrativa (tabela)
- Tela de referência: RESP-TELA-020 — `/admin/produtos`
- Arquivo principal: `pages/admin/products/ProductListPage.tsx`
- Branch de origem da aprovação: agent/responsive-sistema-inteiro-20260923-073717
- Aprovado em: 2026-09-23 10:10 (João: "tá ok")
- Obrigatório copiar à risca: tabela com scroll horizontal localizado (overflow-x-auto no contêiner), sem virar cartão; faixas de filtro rolam; breakpoints só sm/md/lg do Tailwind (D-001)
- Proibido: esconder dado/ação sem equivalente visível; breakpoint novo; media query em px

### Editor com abas
- Tela de referência: RESP-TELA-022 — `/admin/produtos/:id/geral`
- Arquivo principal: `pages/admin/products/ProductEditorPage.tsx`
- Branch de origem da aprovação: agent/responsive-sistema-inteiro-20260923-073717
- Aprovado em: 2026-09-23 10:10 (João: "tá ok")
- Obrigatório copiar à risca: barra de abas sublinhada com overflow-x-auto; breakpoints só sm/md/lg do Tailwind (D-001)
- Proibido: esconder dado/ação sem equivalente visível; breakpoint novo; media query em px

### Grade de lançamento
- Tela de referência: RESP-TELA-025 — `/admin/produtos/:id/ofertas`
- Arquivo principal: `pages/admin/products/tabs/OffersTab.tsx`
- Branch de origem da aprovação: agent/responsive-sistema-inteiro-20260923-073717
- Aprovado em: 2026-09-23 10:10 (João: "tá ok")
- Obrigatório copiar à risca: layout atual: um cartão por plano abaixo de md, linha única a partir de md; breakpoints só sm/md/lg do Tailwind (D-001)
- Proibido: esconder dado/ação sem equivalente visível; breakpoint novo; media query em px

<!-- Uma entrada por tipo de tela. Exemplo:

### Listagem
- Tela de referência: RESP-TELA-NNN — <rota>
- Arquivo principal:
- Branch de origem da aprovação:
- Aprovado em:
- Obrigatório copiar à risca:
- Proibido:
-->

## Candidatas aguardando aprovação

<!-- Tipo — RESP-TELA-NNN — rota — branch — desde -->

## Reprovações

<!-- Data — tipo — tela — motivo dado pelo João — o que foi corrigido -->

<!-- 2026-09-23: importação feita na criação desta execução.
     Nenhum padrão aprovado encontrado em `main` nem em branch `agent/responsive-*`.
     Esta é a primeira execução do agente responsivo no PlantaHub. -->

## Candidatas aguardando aprovação

- nenhuma (as 8 desta execução foram aprovadas em 2026-09-23 10:10)

## Perguntas de design que a aprovação NÃO resolveu (não implementadas)
- Menu mobile no cabeçalho público (links Home/Produtos/Sobre/Contato somem abaixo de md) — RESP-TELA-001
- Cards de produto em 2 colunas em md na Home — RESP-TELA-003
- Paddings aninhados do acordeão aberto em 360 — RESP-TELA-004
- Ordem formulário × cards de marketing no login/cadastro — RESP-TELA-012
- Tabelas do admin como cartões no mobile — RESP-TELA-020
- Aba ativa fora da vista ao abrir direto uma aba do fim — RESP-TELA-002/022
