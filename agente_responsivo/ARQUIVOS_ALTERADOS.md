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
- O que mudou: linha 44 — `grid-cols-1`; linha 79 — `grid-cols-1`; linha 100 — `flex-wrap`; linha 119 — `ml-auto` (revalidação 005 + 027)
- Lógica, API ou regra de negócio alterada: NÃO
- Revalidação necessária: RESP-TELA-027 ainda NÃO INICIADA (medir normalmente)

## apps/web/plantahub-web/src/pages/public/Carrer/Carrer.tsx
- Primeira / última alteração: 2026-09-23 (RESP-TELA-009)
- Telas relacionadas: RESP-TELA-009 — único uso
- Motivo: cards com 326 px em 320 (grid sem colunas abaixo de lg); e-mail sem quebra
- O que mudou: linha 150 — `grid-cols-1`; linha 188 — `break-words`
- Lógica, API ou regra de negócio alterada: NÃO
- Revalidação necessária: nenhuma

## apps/web/plantahub-web/src/pages/public/Contact/ContactPage.tsx
- Primeira / última alteração: 2026-09-23 (RESP-TELA-007)
- Telas relacionadas: RESP-TELA-007 — único uso (`SocialLink` é local do arquivo)
- Motivo: rótulos "Instagram"/"WhatsApp" passavam da borda do botão em 320
- O que mudou: linha 417 — `gap-2 px-3 sm:gap-3 sm:px-4`
- Lógica, API ou regra de negócio alterada: NÃO
- Revalidação necessária: nenhuma

## apps/web/plantahub-web/src/pages/admin/products/ProductEditorPage.tsx
- Primeira / última alteração: 2026-09-23
- Telas relacionadas: RESP-TELA-022 a 026 (barra de abas compartilhada)
- Motivo: barra de 5 abas com 407 px em 312; últimas abas inalcançáveis
- O que mudou: linha 169 — `overflow-x-auto`
- Lógica, API ou regra de negócio alterada: NÃO
- Revalidação necessária: nenhuma pendente (medido após a alteração)

## apps/web/plantahub-web/src/pages/admin/products/ProductListPage.tsx
- Primeira / última alteração: 2026-09-23
- Telas relacionadas: RESP-TELA-020
- Motivo: filtros de status com 342 px em 312; "Arquivados" inalcançável
- O que mudou: linha 71 — `max-w-full overflow-x-auto`
- Lógica, API ou regra de negócio alterada: NÃO
- Revalidação necessária: nenhuma pendente (medido após a alteração)

## apps/web/plantahub-web/src/components/admin/forms/RepeatableList.tsx
- Primeira / última alteração: 2026-09-23
- Telas relacionadas: RESP-TELA-023 (único uso: ContentTab)
- Motivo: título do item empurrava os botões em 320
- O que mudou: linha 68 — `min-w-0 break-words`
- Lógica, API ou regra de negócio alterada: NÃO
- Revalidação necessária: nenhuma pendente (medido após a alteração)

## apps/web/plantahub-web/src/pages/admin/storage/StoragePage.tsx
- Primeira / última alteração: 2026-09-23
- Telas relacionadas: RESP-TELA-031
- Motivo: mensagem de diagnóstico sem espaço cortada
- O que mudou: linha 337 — `break-words`
- Lógica, API ou regra de negócio alterada: NÃO
- Revalidação necessária: nenhuma pendente (medido após a alteração)
