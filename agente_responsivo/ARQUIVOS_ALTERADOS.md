# ARQUIVOS ALTERADOS

## apps/web/plantahub-web/src/components/layout/Header.tsx
- Primeira / última alteração: 2026-09-23 (RESP-TELA-001)
- Telas relacionadas: todas as rotas de `MainLayout` (RESP-TELA-001, 003-019)
- Motivo: mini-carrinho abria com a borda esquerda fora da viewport abaixo de `sm`
- O que mudou: linha 92 — wrapper do carrinho `relative` → `static sm:relative`; abaixo de
  `sm` o dropdown ancora no grupo de ações (mesma borda direita do menu do usuário)
- Lógica, API ou regra de negócio alterada: NÃO
- Revalidação necessária: nenhuma tela pública processada ainda (todas NÃO INICIADA)

## apps/web/plantahub-web/src/components/cart/MiniCartDropdown.tsx
- Primeira / última alteração: 2026-09-23 (RESP-TELA-001)
- Telas relacionadas: usado só em `Header.tsx` (mesmas telas acima)
- Motivo: largura fixa `w-80` (320 px) maior que o espaço útil em 320 px
- O que mudou: linhas 18, 26, 75 — `max-w-[calc(100vw-3rem)]` junto do `w-80`
- Lógica, API ou regra de negócio alterada: NÃO
- Revalidação necessária: idem

## apps/web/plantahub-web/src/app/layouts/AdminLayout.tsx
- Primeira / última alteração: 2026-09-23 (RESP-TELA-002)
- Telas relacionadas: todo `/admin/*` (RESP-TELA-002, 020-032)
- Motivo: abaixo de lg, "Ver site" e "Sair" ficavam fora da tela (cabeçalho com 527 px em 360)
- O que mudou: linha 57 — faixa de links `min-w-0 overflow-x-auto`; linha 71 — ações `shrink-0`
- Lógica, API ou regra de negócio alterada: NÃO
- Revalidação necessária: nenhuma tela do painel processada ainda

## apps/web/plantahub-web/src/components/products/ProductAccordionItem.tsx
- Primeira / última alteração: 2026-09-23 (RESP-TELA-004)
- Telas relacionadas: RESP-TELA-004 (`/produtos`) — único uso
- Motivo: abaixo de sm o nome do produto ficava com 0 px e a área se sobrepunha ao preço
- O que mudou: linha 21 — botão `flex-wrap gap-x-6 gap-y-3 sm:flex-nowrap`; linha 46 — `ml-auto` no bloco do preço
- Lógica, API ou regra de negócio alterada: NÃO
- Revalidação necessária: nenhuma

## apps/web/plantahub-web/src/components/products/ProductPlanSelector.tsx
- Primeira / última alteração: 2026-09-23 (RESP-TELA-005)
- Telas relacionadas: RESP-TELA-005 (detalhe público), RESP-TELA-027 (preview admin)
- Motivo: grid sem colunas abaixo de lg; trilha auto crescia até 297 px em 320
- O que mudou: linha 44 — `grid-cols-1`
- Lógica, API ou regra de negócio alterada: NÃO
- Revalidação necessária: RESP-TELA-027 ainda NÃO INICIADA (medir normalmente)
