# TELAS

<!-- Um bloco por tela. IDs são permanentes: nunca renumere.
     Base do front: apps/web/plantahub-web/src
     Servidor do agente: http://localhost:5180
     Ordem de execução: layout global (001-002) → candidatas a padrão (003, 004, 005,
     012, 014, 020, 022, 025) → demais telas. -->

---
## RESP-TELA-001
- Módulo: Layout global
- Nome: Shell público (cabeçalho + menu + mini-carrinho + rodapé)
- Rota: aplicado a todas as rotas públicas; medir via `/`
- Componente principal: `app/layouts/MainLayout.tsx`
- Tipo: outro (shell)
- Perfil que abre: anônimo (e logado, para o mini-carrinho)
- Compartilhados usados: `components/layout/Header.tsx`, `components/layout/Footer.tsx`, `components/cart/MiniCartDropdown.tsx`, `components/common/ScrollTop.tsx`, `components/ui/ToastProvider.tsx`
- Status: CORRIGIDA — AGUARDANDO VALIDAÇÃO VISUAL
- Pendente de padrão: **navegação mobile** — abaixo de 768 px os links Home/Produtos/Sobre/Contato somem (`Header.tsx:73` `hidden md:flex`) sem menu equivalente; hoje só logo, CTA do hero e rodapé levam a essas páginas. Abaixo de 640 px o botão "Criar conta" some (`Header.tsx:127`); a tela de login é o caminho. Criar menu hambúrguer = decisão de design, aguarda padrão/aprovação do João.
- Início / fim: 2026-09-23 08:15 / 2026-09-23 08:25
- Evidência: agente_responsivo/evidencias/RESP-TELA-001/{antes,depois}-{anonimo,logado}/ — checar-overflow exit 0 em 14 larguras (antes e depois); `carrinho-*.png` / `menu-*.png` com dropdown aberto (360/768/1366)
- Onde ver: http://localhost:5180/
- Como testar: 360/768/1366 — abrir menu, abrir mini-carrinho (logado e deslogado), rolar até o rodapé
- Como confirmar que estava quebrado: logado, clicar no carrinho em 360 px — `antes-logado/carrinho-360.png` mostra o painel começando em x=−110 (em 320 px, x=−150), texto "Seu carrinho está vazio" cortado. O script não acusa porque o dropdown só existe após o clique. Depois: x=24…336 (360) e 24…296 (320); 768/1366 idênticos ao antes.
- arquivo:linha: `components/layout/Header.tsx:92` · `components/cart/MiniCartDropdown.tsx:18,26,75`
- Cuidados: é o componente mais compartilhado do sistema; qualquer alteração aqui marca TODAS as telas públicas como REVALIDAÇÃO NECESSÁRIA. Menu do usuário já cabia (sem alteração). Rodapé: grid de 1 coluna abaixo de lg, sem vazamento. Carrinho com itens não foi visto (conta sem itens; o agente não adiciona) — conferir visualmente com itens.

---
## RESP-TELA-002
- Módulo: Layout global
- Nome: Shell do painel administrativo
- Rota: aplicado a todo `/admin/*`; medir via `/admin/produtos`
- Componente principal: `app/layouts/AdminLayout.tsx`
- Tipo: outro (shell)
- Perfil que abre: admin
- Compartilhados usados: `app/routers/AdminRoute.tsx`, `components/common/RouteFallback.tsx`, `components/ui/ToastProvider.tsx`
- Status: CORRIGIDA — AGUARDANDO VALIDAÇÃO VISUAL
- Pendente de padrão:
- Início / fim: 2026-09-23 08:27 / 2026-09-23 08:33
- Evidência: agente_responsivo/evidencias/RESP-TELA-002/{antes,depois}/ — checar-overflow exit 0 em 14 larguras antes e depois (a página não rola; o vazamento era do cabeçalho, ver D-005); `depois/360-armazenamento.png` após navegar pela faixa
- Onde ver: http://localhost:5180/admin/produtos
- Como testar: 360/768/1366 — navegação lateral/superior do painel, troca de seção
- Como confirmar que estava quebrado: `antes/360.png` — menu cortado em "Armazen…"; conteúdo do cabeçalho com 527 px em 320/360, "Ver site" em x=422–463 e **Sair em x=479–527, fora da tela e inalcançável**. Depois: cabeçalho 360/360, Sair em 288–336; links rolam dentro da faixa; clique em Armazenamento navega (320/360/768). 1024/1366 idênticos.
- arquivo:linha: `app/layouts/AdminLayout.tsx:57` (faixa de links: `min-w-0 overflow-x-auto`) · `:71` (ações: `shrink-0`)
- Cuidados: altera todas as telas `/admin/*` (RESP-TELA-020 a 032). Ponto visual para o João: ao abrir uma seção do fim da lista direto pela URL (ex.: `/admin/armazenamento`), o item ativo fica fora da faixa até rolar — corrigir exigiria JS (scrollIntoView), não feito. E-mail do usuário continua oculto abaixo de sm (já era assim; não é dado de negócio).

---
## RESP-TELA-003
- Módulo: Vitrine pública
- Nome: Home
- Rota: `/`
- Componente principal: `pages/public/Home/Home.tsx`
- Tipo: painel (landing)
- Perfil que abre: anônimo
- Compartilhados usados: `components/home/{Hero,CategoryShowcase,HowItWorks,WhyChoose,FinalCTA}.tsx`, `components/products/ProductCarouselSection.tsx`, `components/products/ProductCard.tsx`
- Status: CANDIDATA A PADRÃO — AGUARDANDO APROVAÇÃO (sem alteração de código)
- Pendente de padrão: **CANDIDATA A PADRÃO — tipo painel/landing.** Proposta = o layout atual, que já não quebra em nenhuma largura. Pergunta de design para o João: em 640–1023 px os cards de produto ficam em 1 coluna de largura total (`ProductCarouselSection.tsx:52` `grid gap-6 lg:grid-cols-3`), com imagens muito altas em 768; `md:grid-cols-2` seria a alternativa. Não aplicado — é decisão de design.
- Início / fim: 2026-09-23 08:35 / 2026-09-23 08:45
- Evidência: agente_responsivo/evidencias/RESP-TELA-003/antes/ — checar-overflow exit 0 (14 larguras); `fatia-{360,768,1366}-N.png` cobrem a página inteira (rolando o `#root`, D-008); nenhum elemento vaza da viewport em 360/768/1366
- Onde ver: http://localhost:5180/
- Como testar: 360/768/1366 — carrossel de produtos, seções, CTA
- Como confirmar que estava quebrado: não estava. Hero, benefícios, carrosséis Casas/Chalés (preço e "Ver detalhes" visíveis), Como funciona, números, CTA e rodapé íntegros nas três larguras.
- arquivo:linha: nenhum alterado
- Cuidados: carrossel é candidato clássico a overflow horizontal da página

---
## RESP-TELA-004
- Módulo: Vitrine pública
- Nome: Catálogo de produtos
- Rota: `/produtos`
- Componente principal: `pages/public/Products/Products.tsx`
- Tipo: listagem
- Perfil que abre: anônimo
- Compartilhados usados: `components/products/ProductCard.tsx`, `components/products/ProductAccordion.tsx`
- Status: CANDIDATA A PADRÃO — AGUARDANDO APROVAÇÃO
- Pendente de padrão: **CANDIDATA A PADRÃO — tipo listagem (vitrine).** Proposta: abaixo de sm, cada linha do acordeão quebra em duas — miniatura + nome/área/subtítulo em cima, "A partir de"/preço + seta embaixo, alinhados à direita. sm+ inalterado. Pergunta de design: em 360 px o painel aberto tem três paddings aninhados (card `px-6` + card interno + caixa de arquivos) e o texto fica estreito; reduzir padding no mobile não foi feito.
- Início / fim: 2026-09-23 08:46 / 2026-09-23 08:55
- Evidência: agente_responsivo/evidencias/RESP-TELA-004/{antes,depois}/ — `fatias.cjs` sem vazamento em 320/360/768/1366; `depois/aberto-{360,1366}.png` com o primeiro item expandido
- Onde ver: http://localhost:5180/produtos
- Como testar: 360/768/1366 — filtros, grade de cartões, preço e botão de cada cartão
- Como confirmar que estava quebrado: `antes/fatia-360-0.png` — nas três linhas o NOME do produto some (coluna com 0 px: 264 px úteis − miniatura 96 − preço ~110 − seta e gaps), e "80 m²" quebra por cima do preço. Não é vazamento, então nenhum script acusou. Depois: nome, área, subtítulo e preço legíveis.
- arquivo:linha: `components/products/ProductAccordionItem.tsx:21` (botão: `flex-wrap gap-x-6 gap-y-3 sm:flex-nowrap`) · `:46` (bloco do preço: `ml-auto`)
- Cuidados: o commit `14d2648` já ajustou preço/alinhamento dos cartões — não desfazer. Componente usado só por `ProductAccordion` → `Products.tsx`. Subtítulo longo segue truncado com "…" (já era assim no desktop). Expandir/recolher testado em 360 e 1366.

---
## RESP-TELA-005
- Módulo: Vitrine pública
- Nome: Detalhe do produto
- Rota: `/:category/:slug`
- Componente principal: `pages/public/ProductDetails/ProductDetails.tsx`
- Tipo: detalhe
- Perfil que abre: anônimo
- Compartilhados usados: `components/products/{ProductDetailsView,ProductHero,ProductDetailCard,ProductPlanSelector,ProductAccordion,ProductAccordionItem}.tsx`
- Status: CANDIDATA A PADRÃO — AGUARDANDO APROVAÇÃO
- Pendente de padrão: **CANDIDATA A PADRÃO — tipo detalhe.** Proposta = layout atual (galeria → info → preço/CTAs → seletor de plantas → resumo, empilhados abaixo de lg) + correção objetiva da trilha do grid. Nenhuma decisão de design nova.
- Início / fim: 2026-09-23 08:57 / 2026-09-23 09:03
- Evidência: agente_responsivo/evidencias/RESP-TELA-005/{antes,depois}/ — `fatias.cjs` 320/360/768/1366
- Onde ver: http://localhost:5180/casas/confort (URL obtida clicando "Ver detalhes" na Home)
- Como testar: 360/768/1366 — seletor de planos, acordeão, galeria, botão de compra
- Como confirmar que estava quebrado: em 320 px, `antes`: `aside` de compra e card "Resumo" com 297 px ([24,321]) — encostam/passam da borda direita, margem direita some. Grid sem colunas abaixo de lg → trilha `auto` cresce até o min-content do conteúdo. Depois: [24,296], nada vaza. **Revalidação 2026-09-23 09:50:** o detector de transbordo de caixa (D-009) pegou um segundo defeito em 320 — itens do seletor com 247 px em caixa de 222; corrigido em `ProductPlanSelector.tsx:79,100,119` (ver RESP-TELA-027). Evidência: `depois2/`.
- arquivo:linha: `components/products/ProductPlanSelector.tsx:44` (`grid-cols-1`) · `:79` (`grid-cols-1`) · `:100` (`flex-wrap`) · `:119` (`ml-auto`)
- Cuidados: rota com parâmetro — pegar `category/slug` real pela listagem `/produtos`; sem produto no banco, `BLOQUEADA — SEM DADO`. `ProductPlanSelector` também é usado por `ProductDetailsView` → preview do admin (RESP-TELA-027, NÃO INICIADA). Botões Comprar/Adicionar não foram clicados (gravariam carrinho/pedido).

---
## RESP-TELA-006
- Módulo: Vitrine pública
- Nome: Sobre nós
- Rota: `/sobre`
- Componente principal: `pages/public/About/AboutUs.tsx`
- Tipo: outro (institucional)
- Perfil que abre: anônimo
- Compartilhados usados: shell (RESP-TELA-001)
- Status: SEM ALTERAÇÃO NECESSÁRIA
- Pendente de padrão:
- Início / fim: 2026-09-23 09:10 / 2026-09-23 09:30
- Evidência: agente_responsivo/evidencias/RESP-TELA-006/antes/ — `fatias.cjs` (viewport + transbordo de caixa, D-008/D-009) 320/360/768/1366; fatias 360 lidas uma a uma, 768/1366 lidas em prancha (D-010)
- Onde ver: http://localhost:5180/sobre
- Como testar: 360/768/1366 — blocos de texto e imagem
- Como confirmar que estava quebrado: não estava. Hero, manifesto, pilares, conformidade, equipe e CTA íntegros. Quadrados laranja do hero passam 16 px da caixa da imagem em TODAS as larguras (decoração intencional, igual à Home) e ficam dentro da viewport.
- arquivo:linha: nenhum alterado
- Cuidados: 

---
## RESP-TELA-007
- Módulo: Vitrine pública
- Nome: Contato
- Rota: `/contato`
- Componente principal: `pages/public/Contact/ContactPage.tsx`
- Tipo: formulário
- Perfil que abre: anônimo
- Compartilhados usados: shell (RESP-TELA-001)
- Status: CORRIGIDA — AGUARDANDO VALIDAÇÃO VISUAL
- Pendente de padrão:
- Início / fim: 2026-09-23 09:14 / 2026-09-23 09:20
- Evidência: agente_responsivo/evidencias/RESP-TELA-007/{antes,depois}/ — `fatias.cjs` 320/360/768/1366
- Onde ver: http://localhost:5180/contato
- Como testar: 360/768/1366 — campos, labels, botão de envio (NÃO submeter)
- Como confirmar que estava quebrado: em 320 px, botões sociais em 2 colunas com conteúdo de 107 px em 103 px — "Instagram" e "WhatsApp" encostam/passam da borda do botão (`antes/fatia-320-0.png`). Depois: sem transbordo em nenhuma largura.
- arquivo:linha: `pages/public/Contact/ContactPage.tsx:417` (`SocialLink`: `gap-2 px-3 sm:gap-3 sm:px-4`)
- Cuidados: nunca submeter o formulário Formulário NÃO submetido. Quadrados decorativos do hero passam 16 px da caixa (intencional). Telefone "(xx) xxxxx-xxxx" é conteúdo provisório → FORA_ESCOPO PROB-001.

---
## RESP-TELA-008
- Módulo: Vitrine pública
- Nome: FAQ
- Rota: `/faq`
- Componente principal: `pages/public/Faq/Faq.tsx`
- Tipo: outro (institucional)
- Perfil que abre: anônimo
- Compartilhados usados: shell (RESP-TELA-001)
- Status: SEM ALTERAÇÃO NECESSÁRIA
- Pendente de padrão:
- Início / fim: 2026-09-23 09:10 / 2026-09-23 09:30
- Evidência: agente_responsivo/evidencias/RESP-TELA-008/antes/ — `fatias.cjs` (viewport + transbordo de caixa, D-008/D-009) 320/360/768/1366; fatias 360 lidas uma a uma, 768/1366 lidas em prancha (D-010)
- Onde ver: http://localhost:5180/faq
- Como testar: 360/768/1366 — abrir e fechar itens do acordeão
- Como confirmar que estava quebrado: não estava. Busca e acordeão íntegros; primeiro item aberto por padrão lido nas 3 larguras.
- arquivo:linha: nenhum alterado
- Cuidados: 

---
## RESP-TELA-009
- Módulo: Vitrine pública
- Nome: Trabalhe conosco
- Rota: `/trabalhe-conosco`
- Componente principal: `pages/public/Carrer/Carrer.tsx`
- Tipo: formulário
- Perfil que abre: anônimo
- Compartilhados usados: shell (RESP-TELA-001)
- Status: CORRIGIDA — AGUARDANDO VALIDAÇÃO VISUAL
- Pendente de padrão:
- Início / fim: 2026-09-23 09:06 / 2026-09-23 09:12
- Evidência: agente_responsivo/evidencias/RESP-TELA-009/{antes,depois}/ — `fatias.cjs` 320/360/768/1366
- Onde ver: http://localhost:5180/trabalhe-conosco
- Como testar: 360/768/1366 — campos e upload, se houver (NÃO submeter)
- Como confirmar que estava quebrado: em 320 px os cards "O que esperamos receber" e "Canal de contato" tinham 326 px ([24,350]), passando 30 px da borda — o e-mail `parcerias@plantahub.com.br` ficava cortado pelo #root. Com só o `grid-cols-1`, o e-mail passou a estourar a própria caixa; com `break-words` ele quebra dentro dela (78–242 em caixa 57–263).
- arquivo:linha: `pages/public/Carrer/Carrer.tsx:150` (`grid-cols-1`) · `:188` (`break-words` no e-mail)
- Cuidados: nunca submeter Página não tem formulário (só links mailto) — nenhum envio possível.

---
## RESP-TELA-010
- Módulo: Vitrine pública
- Nome: Termos de serviço
- Rota: `/legal/termos`
- Componente principal: `pages/legal/TermsOfServicePage.tsx`
- Tipo: outro (texto longo)
- Perfil que abre: anônimo
- Compartilhados usados: shell (RESP-TELA-001)
- Status: SEM ALTERAÇÃO NECESSÁRIA
- Pendente de padrão:
- Início / fim: 2026-09-23 09:10 / 2026-09-23 09:30
- Evidência: agente_responsivo/evidencias/RESP-TELA-010/antes/ — `fatias.cjs` (viewport + transbordo de caixa, D-008/D-009) 320/360/768/1366; fatias 360 lidas uma a uma, 768/1366 lidas em prancha (D-010)
- Onde ver: http://localhost:5180/legal/termos
- Como testar: 360/768/1366 — largura de leitura, listas, tabelas se houver
- Como confirmar que estava quebrado: não estava. Sumário "Nesta página" empilha acima do texto abaixo de lg; texto longo sem transbordo.
- arquivo:linha: nenhum alterado
- Cuidados: 

---
## RESP-TELA-011
- Módulo: Vitrine pública
- Nome: Política de privacidade
- Rota: `/legal/privacidade`
- Componente principal: `pages/legal/PrivacyPolicyPage.tsx`
- Tipo: outro (texto longo)
- Perfil que abre: anônimo
- Compartilhados usados: shell (RESP-TELA-001)
- Status: SEM ALTERAÇÃO NECESSÁRIA
- Pendente de padrão:
- Início / fim: 2026-09-23 09:10 / 2026-09-23 09:30
- Evidência: agente_responsivo/evidencias/RESP-TELA-011/antes/ — `fatias.cjs` (viewport + transbordo de caixa, D-008/D-009) 320/360/768/1366; fatias 360 lidas uma a uma, 768/1366 lidas em prancha (D-010)
- Onde ver: http://localhost:5180/legal/privacidade
- Como testar: 360/768/1366 — largura de leitura, listas
- Como confirmar que estava quebrado: não estava. Mesmo modelo de RESP-TELA-010; íntegro.
- arquivo:linha: nenhum alterado
- Cuidados: 

---
## RESP-TELA-012
- Módulo: Conta do cliente
- Nome: Login
- Rota: `/login`
- Componente principal: `pages/public/Login/Login.tsx`
- Tipo: formulário
- Perfil que abre: anônimo
- Compartilhados usados: `components/auth/LoginForm.tsx`
- Status: CANDIDATA A PADRÃO — AGUARDANDO APROVAÇÃO (sem alteração de código)
- Pendente de padrão: **CANDIDATA A PADRÃO — tipo formulário.** Proposta = layout atual (cartão de formulário com rótulo acima do campo, largura total no mobile, botão largo). Perguntas de design: (1) no mobile o formulário vem DEPOIS do título e dos 2 cards de marketing (começa em y≈680 em 360×800, quase fora da primeira dobra); (2) em 768 os cards ficam alinhados à esquerda e o formulário centralizado.
- Início / fim: 2026-09-23 09:05 / 2026-09-23 09:08
- Evidência: agente_responsivo/evidencias/RESP-TELA-012/antes/ — `fatias.cjs` sem vazamento 320/360/768/1366
- Onde ver: http://localhost:5180/login
- Como testar: 360/768/1366 — campos, erro de validação, teclado mobile cobrindo o botão
- Como confirmar que estava quebrado: não estava. Campos, "Esqueci minha senha", botão e link de cadastro visíveis e alinhados nas três larguras.
- arquivo:linha: nenhum alterado
- Cuidados: NÃO autenticar com credenciais; só inspecionar o layout. Estado de erro de validação NÃO foi visto (exigiria submeter) — pendente de validação humana.

---
## RESP-TELA-013
- Módulo: Conta do cliente
- Nome: Cadastro
- Rota: `/register`
- Componente principal: `pages/public/Register/Register.tsx`
- Tipo: formulário
- Perfil que abre: anônimo
- Compartilhados usados: `components/auth/RegisterForm.tsx`
- Status: SEM ALTERAÇÃO NECESSÁRIA
- Pendente de padrão:
- Início / fim: 2026-09-23 09:05 / 2026-09-23 09:08
- Evidência: agente_responsivo/evidencias/RESP-TELA-013/antes/ — `fatias.cjs` sem vazamento 320/360/768/1366
- Onde ver: http://localhost:5180/register
- Como testar: 360/768/1366 — campos, validação, botão
- Como confirmar que estava quebrado: não estava. Mesmo layout do login (RESP-TELA-012), 4 campos e botão íntegros.
- arquivo:linha: nenhum alterado
- Cuidados: NUNCA submeter (criaria usuário no banco). Segue a candidata de formulário RESP-TELA-012: se o João mudar a ordem formulário/marketing lá, esta volta para a fila. Validação não vista (não submetido).

---
## RESP-TELA-014
- Módulo: Conta do cliente
- Nome: Preferências / conta
- Rota: `/configs`
- Componente principal: `pages/Preferences/Preferences.tsx`
- Tipo: formulário (com abas)
- Perfil que abre: cliente autenticado
- Compartilhados usados: `components/preferences/AccountTab.tsx`, `components/preferences/TransactionTab.tsx`, `app/routers/ProtectedRoute.tsx`
- Status: CANDIDATA A PADRÃO — AGUARDANDO APROVAÇÃO (sem alteração de código)
- Pendente de padrão: **CANDIDATA A PADRÃO — tipo formulário com abas.** Proposta = layout atual: abaixo de lg as abas viram lista vertical de cartões acima do conteúdo; campos em 1 coluna no mobile e 2 a partir de md. Sem decisão nova.
- Início / fim: 2026-09-23 09:10 / 2026-09-23 09:30
- Evidência: agente_responsivo/evidencias/RESP-TELA-014/antes/ — `fatias.cjs` (viewport + transbordo de caixa, D-008/D-009) 320/360/768/1366; fatias 360 lidas uma a uma, 768/1366 lidas em prancha (D-010); `transacoes-{320,360,768,1366}.png` com a aba Transações aberta
- Onde ver: http://localhost:5180/configs
- Como testar: 360/768/1366 — trocar de aba, listar transações (NÃO salvar)
- Como confirmar que estava quebrado: não estava. Abas Conta e Transações, campos, Salvar, Segurança e Zona de perigo íntegros. Em 768 o botão "Excluir conta" quebra em 2 linhas (legível).
- arquivo:linha: nenhum alterado
- Cuidados: exige storageState de cliente; a aba de transações provavelmente tem tabela Nada salvo/excluído. Transações: conta sem histórico — só o estado vazio foi visto. Texto técnico interno na seção Segurança → FORA_ESCOPO PROB-002.

---
## RESP-TELA-015
- Módulo: Conta do cliente
- Nome: Biblioteca (itens comprados)
- Rota: `/biblioteca`
- Componente principal: `pages/Library/Library.tsx`
- Tipo: listagem
- Perfil que abre: cliente autenticado
- Compartilhados usados: `components/library/LibraryProductCard.tsx`, `app/routers/ProtectedRoute.tsx`
- Status: BLOQUEADA — SEM DADO
- Pendente de padrão:
- Início / fim: 2026-09-23 09:10 / —
- Evidência: agente_responsivo/evidencias/RESP-TELA-015/
- Onde ver: http://localhost:5180/biblioteca
- Como testar: 360/768/1366 — grade de cartões, ação de download
- Como confirmar que estava quebrado: conta `adm@plantahub.com` sem compras: só o estado vazio existe, e está íntegro nas 4 larguras (`evidencias/RESP-TELA-015/antes`). A grade de `LibraryProductCard` não pôde ser vista; o agente não cria compra.
- arquivo:linha:
- Cuidados: exige storageState de cliente com compra feita

---
## RESP-TELA-016
- Módulo: Conta do cliente
- Nome: Detalhe do item da biblioteca
- Rota: `/biblioteca/:productId/:planTypeCode`
- Componente principal: `pages/Library/LibraryItemDetailsPage.tsx`
- Tipo: detalhe
- Perfil que abre: cliente autenticado
- Compartilhados usados: `app/routers/ProtectedRoute.tsx`
- Status: BLOQUEADA — SEM DADO
- Pendente de padrão:
- Início / fim: 2026-09-23 09:10 / —
- Evidência: agente_responsivo/evidencias/RESP-TELA-016/
- Onde ver: http://localhost:5180/biblioteca/PRODUCT-ID/PLAN-CODE
- Como testar: 360/768/1366 — lista de arquivos, botões de download
- Como confirmar que estava quebrado: sem item na biblioteca, não há productId/planTypeCode real para abrir a rota.
- arquivo:linha:
- Cuidados: exige sessão + item real; ids saem de `/biblioteca`

---
## RESP-TELA-017
- Módulo: Conta do cliente
- Nome: Carrinho
- Rota: `/carrinho`
- Componente principal: `pages/cart/CartPage.tsx`
- Tipo: listagem (com totais)
- Perfil que abre: cliente autenticado
- Compartilhados usados: `app/providers/CartProvider.tsx`, `components/cart/MiniCartDropdown.tsx`
- Status: BLOQUEADA — SEM DADO
- Pendente de padrão:
- Início / fim: 2026-09-23 09:10 / —
- Evidência: agente_responsivo/evidencias/RESP-TELA-017/
- Onde ver: http://localhost:5180/carrinho
- Como testar: 360/768/1366 — itens, quantidades, total, botão de finalizar (NÃO finalizar)
- Como confirmar que estava quebrado: carrinho vazio: estado vazio íntegro nas 4 larguras (`evidencias/RESP-TELA-017/antes`). Itens/totais não vistos; o agente não adiciona ao carrinho (gravaria).
- arquivo:linha:
- Cuidados: nunca concluir a compra; o total não pode ficar truncado em nenhuma largura

---
## RESP-TELA-018
- Módulo: Conta do cliente
- Nome: Detalhe do pedido
- Rota: `/pedidos/:orderId`
- Componente principal: `pages/Orders/OrderDetailsPage.tsx`
- Tipo: detalhe
- Perfil que abre: cliente autenticado
- Compartilhados usados: `app/routers/ProtectedRoute.tsx`
- Status: BLOQUEADA — SEM DADO
- Pendente de padrão:
- Início / fim: 2026-09-23 09:10 / —
- Evidência: agente_responsivo/evidencias/RESP-TELA-018/
- Onde ver: http://localhost:5180/pedidos/ORDER-ID
- Como testar: 360/768/1366 — itens do pedido, status, valores
- Como confirmar que estava quebrado: sem pedido na conta (aba Transações vazia), não há orderId real.
- arquivo:linha:
- Cuidados: exige sessão + pedido real (ids na aba de transações de `/configs`)

---
## RESP-TELA-019
- Módulo: Conta do cliente
- Nome: Pagamento concluído
- Rota: `/pagamento/sucesso`
- Componente principal: `pages/Payment/PaymentSuccessPage.tsx`
- Tipo: outro (confirmação)
- Perfil que abre: cliente autenticado
- Compartilhados usados: `app/routers/ProtectedRoute.tsx`
- Status: SEM ALTERAÇÃO NECESSÁRIA
- Pendente de padrão:
- Início / fim: 2026-09-23 09:10 / 2026-09-23 09:30
- Evidência: agente_responsivo/evidencias/RESP-TELA-019/antes/ — `fatias.cjs` (viewport + transbordo de caixa, D-008/D-009) 320/360/768/1366; fatias 360 lidas uma a uma, 768/1366 lidas em prancha (D-010)
- Onde ver: http://localhost:5180/pagamento/sucesso
- Como testar: 360/768/1366 — mensagem e ações seguintes
- Como confirmar que estava quebrado: não estava. Cartão de confirmação e dois botões íntegros.
- arquivo:linha: nenhum alterado
- Cuidados: exige sessão; pode depender de query string do provedor de pagamento Aberto sem query string do provedor; com parâmetros reais o conteúdo pode variar — conferir.

---
## RESP-TELA-020
- Módulo: Painel admin
- Nome: Lista de produtos
- Rota: `/admin/produtos`
- Componente principal: `pages/admin/products/ProductListPage.tsx`
- Tipo: listagem
- Perfil que abre: admin
- Compartilhados usados: `app/layouts/AdminLayout.tsx`, `components/admin/ui/primitives.tsx`
- Status: CANDIDATA A PADRÃO — AGUARDANDO APROVAÇÃO
- Pendente de padrão: **CANDIDATA A PADRÃO — tipo listagem administrativa (tabela).** Proposta: manter a tabela com scroll horizontal LOCALIZADO no contêiner (`overflow-x-auto`, já usado em todas as tabelas do painel: produtos, coleções, categorias, arquivos); filtros de status rolam na própria faixa. Pergunta de design: em 360 só PRODUTO/CATEGORIA/STATUS aparecem sem rolar — Despublicar/Arquivar exigem rolar a tabela. Virar cartões no mobile NÃO foi feito (decisão de design).
- Início / fim: 2026-09-23 09:35 / 2026-09-23 10:00
- Evidência: agente_responsivo/evidencias/RESP-TELA-020/{antes,depois}/ — `fatias.cjs` com storage admin, 320/360/768/1366; lidas em prancha (D-010)
- Onde ver: http://localhost:5180/admin/produtos
- Como testar: 360/768/1366 — busca, filtro, paginação, cada ação da linha
- Como confirmar que estava quebrado: `antes/fatia-360-0.png` — grupo de filtros Todos/Rascunhos/Publicados/Arquivados com 342 px em 312 disponíveis: "Arquivados" cortado e o botão passava da borda ([271,361]), inalcançável. A tabela já rolava localmente (720 px em 310, ações alcançáveis por rolagem). Depois: filtros rolam na faixa, nada vaza.
  não rola), mas `antes/360.png` mostra o menu do painel cortado em "Armazen…" e as
  colunas PREÇO BASE e AÇÕES fora da tela — as ações Despublicar e Arquivar ficam
  inalcançáveis sem scroll dentro do contêiner. `antes/1366.png` mostra a tela íntegra,
  com os 6 produtos e o rótulo adm@plantahub.com, o que prova que a sessão vale.
- arquivo:linha: `pages/admin/products/ProductListPage.tsx:71` (`max-w-full overflow-x-auto` no grupo de filtros)
- Cuidados: tabela larga — scroll horizontal no contêiner, nunca na página; nenhuma coluna ou ação pode sumir no mobile sem equivalente visível Busca/filtro não gravam; Despublicar/Arquivar NÃO clicados.

---
## RESP-TELA-021
- Módulo: Painel admin
- Nome: Novo produto
- Rota: `/admin/produtos/novo`
- Componente principal: `pages/admin/products/ProductEditorPage.tsx` (mode=create)
- Tipo: formulário
- Perfil que abre: admin
- Compartilhados usados: `components/admin/ui/primitives.tsx`, `components/admin/ui/MoneyInput.tsx`
- Status: SEM ALTERAÇÃO NECESSÁRIA
- Pendente de padrão:
- Início / fim: 2026-09-23 09:35 / 2026-09-23 10:00
- Evidência: agente_responsivo/evidencias/RESP-TELA-021/antes/ — `fatias.cjs` com storage admin, 320/360/768/1366; lidas em prancha (D-010)
- Onde ver: http://localhost:5180/admin/produtos/novo
- Como testar: 360/768/1366 — campos, MoneyInput, botão salvar (NÃO salvar)
- Como confirmar que estava quebrado: não estava. Nome, Categoria, Cancelar e Criar rascunho íntegros.
- arquivo:linha: nenhum alterado
- Cuidados: NUNCA salvar Criar rascunho NÃO clicado.

---
## RESP-TELA-022
- Módulo: Painel admin
- Nome: Editor de produto — aba Geral
- Rota: `/admin/produtos/:productId/geral`
- Componente principal: `pages/admin/products/tabs/GeneralTab.tsx`
- Tipo: formulário (com abas)
- Perfil que abre: admin
- Compartilhados usados: `ProductEditorPage.tsx` (barra de abas), `components/admin/ui/primitives.tsx`, `MoneyInput.tsx`
- Status: CANDIDATA A PADRÃO — AGUARDANDO APROVAÇÃO
- Pendente de padrão: **CANDIDATA A PADRÃO — tipo editor com abas.** Proposta: barra de abas sublinhada com rolagem horizontal abaixo de md; campos em 1 coluna no mobile e 2 a partir de md (já era assim). Pergunta: a aba ativa pode ficar fora da vista ao abrir direto uma aba do fim (ex.: /arquivos).
- Início / fim: 2026-09-23 09:35 / 2026-09-23 10:00
- Evidência: agente_responsivo/evidencias/RESP-TELA-022/{antes,depois}/ — `fatias.cjs` com storage admin, 320/360/768/1366; lidas em prancha (D-010)
- Onde ver: http://localhost:5180/admin/produtos/casa-confort-80m2/geral
- Como testar: 360/768/1366 — barra de abas com 5 itens, campos, botão salvar (NÃO salvar)
- Como confirmar que estava quebrado: em 320/360 a barra de 5 abas do editor tinha 407 px: "Ofertas" cortada e "Arquivos" fora da tela, sem rolagem (`antes/fatia-360-0.png`). Depois: a faixa rola localmente, as 5 abas alcançáveis.
- arquivo:linha: `pages/admin/products/ProductEditorPage.tsx:169` (`overflow-x-auto` na faixa de abas — compartilhada 022-026)
- Cuidados: a barra de abas é compartilhada pelas telas 022-026; corrigi-la marca as quatro seguintes como REVALIDAÇÃO NECESSÁRIA Salvar NÃO clicado. Telas 023-026 herdam a correção da barra.

---
## RESP-TELA-023
- Módulo: Painel admin
- Nome: Editor de produto — aba Conteúdo
- Rota: `/admin/produtos/:productId/conteudo`
- Componente principal: `pages/admin/products/tabs/ContentTab.tsx`
- Tipo: formulário
- Perfil que abre: admin
- Compartilhados usados: `components/admin/forms/RepeatableList.tsx`, `primitives.tsx`
- Status: CORRIGIDA — AGUARDANDO VALIDAÇÃO VISUAL
- Pendente de padrão:
- Início / fim: 2026-09-23 09:35 / 2026-09-23 10:00
- Evidência: agente_responsivo/evidencias/RESP-TELA-023/{antes,depois}/ — `fatias.cjs` com storage admin, 320/360/768/1366; lidas em prancha (D-010)
- Onde ver: http://localhost:5180/admin/produtos/casa-confort-80m2/conteudo
- Como testar: 360/768/1366 — adicionar/remover item da lista repetível (sem salvar)
- Como confirmar que estava quebrado: em 320/360 a barra de 5 abas do editor tinha 407 px: "Ofertas" cortada e "Arquivos" fora da tela, sem rolagem (`antes/fatia-360-0.png`). Depois: a faixa rola localmente, as 5 abas alcançáveis. Além disso, em 320 os títulos dos itens da lista repetível (maiúsculas com tracking) passavam 7–10 px da linha e empurravam os botões mover/remover. Depois: título quebra, botões no lugar.
- arquivo:linha: `pages/admin/products/ProductEditorPage.tsx:169` (`overflow-x-auto` na faixa de abas — compartilhada 022-026) · `components/admin/forms/RepeatableList.tsx:68` (`min-w-0 break-words` no título do item)
- Cuidados: `RepeatableList` é compartilhado dentro do painel Nenhum item adicionado/removido; Salvar NÃO clicado. `RepeatableList.test.tsx` passa (67/67 da suíte).

---
## RESP-TELA-024
- Módulo: Painel admin
- Nome: Editor de produto — aba Imagens
- Rota: `/admin/produtos/:productId/imagens`
- Componente principal: `pages/admin/products/tabs/MediaTab.tsx`
- Tipo: outro (galeria + upload)
- Perfil que abre: admin
- Compartilhados usados: `components/admin/upload/UploadDropzone.tsx`, `UploadQueue.tsx`
- Status: CORRIGIDA — AGUARDANDO VALIDAÇÃO VISUAL
- Pendente de padrão:
- Início / fim: 2026-09-23 09:35 / 2026-09-23 10:00
- Evidência: agente_responsivo/evidencias/RESP-TELA-024/{antes,depois}/ — `fatias.cjs` com storage admin, 320/360/768/1366; lidas em prancha (D-010)
- Onde ver: http://localhost:5180/admin/produtos/casa-confort-80m2/imagens
- Como testar: 360/768/1366 — grade de imagens, área de upload (NÃO enviar arquivo)
- Como confirmar que estava quebrado: em 320/360 a barra de 5 abas do editor tinha 407 px: "Ofertas" cortada e "Arquivos" fora da tela, sem rolagem (`antes/fatia-360-0.png`). Depois: a faixa rola localmente, as 5 abas alcançáveis.
- arquivo:linha: `pages/admin/products/ProductEditorPage.tsx:169` (`overflow-x-auto` na faixa de abas — compartilhada 022-026)
- Cuidados: nunca enviar arquivo (grava no storage) Nenhuma imagem enviada nem removida.

---
## RESP-TELA-025
- Módulo: Painel admin
- Nome: Editor de produto — aba Ofertas
- Rota: `/admin/produtos/:productId/ofertas`
- Componente principal: `pages/admin/products/tabs/OffersTab.tsx`
- Tipo: grade de lançamento (preços por plano)
- Perfil que abre: admin
- Compartilhados usados: `components/admin/ui/MoneyInput.tsx`, `primitives.tsx`
- Status: CANDIDATA A PADRÃO — AGUARDANDO APROVAÇÃO
- Pendente de padrão: **CANDIDATA A PADRÃO — tipo grade de lançamento.** Proposta = layout atual: abaixo de md cada plano vira um cartão (nome/código, preço, "À venda", Salvar); a partir de md vira linha única. Não é tabela, então não há primeira coluna fixa — nenhum dado some. Sem decisão nova além da barra de abas (022).
- Início / fim: 2026-09-23 09:35 / 2026-09-23 10:00
- Evidência: agente_responsivo/evidencias/RESP-TELA-025/{antes,depois}/ — `fatias.cjs` com storage admin, 320/360/768/1366; lidas em prancha (D-010)
- Onde ver: http://localhost:5180/admin/produtos/casa-confort-80m2/ofertas
- Como testar: 360/768/1366 — leitura em linha dos preços por plano (NÃO salvar)
- Como confirmar que estava quebrado: em 320/360 a barra de 5 abas do editor tinha 407 px: "Ofertas" cortada e "Arquivos" fora da tela, sem rolagem (`antes/fatia-360-0.png`). Depois: a faixa rola localmente, as 5 abas alcançáveis.
- arquivo:linha: `pages/admin/products/ProductEditorPage.tsx:169` (`overflow-x-auto` na faixa de abas — compartilhada 022-026)
- Cuidados: grade de lançamento NÃO vira cartão — scroll horizontal localizado com primeira coluna fixa Nenhum preço salvo.

---
## RESP-TELA-026
- Módulo: Painel admin
- Nome: Editor de produto — aba Arquivos
- Rota: `/admin/produtos/:productId/arquivos`
- Componente principal: `pages/admin/products/tabs/AssetsTab.tsx`
- Tipo: listagem
- Perfil que abre: admin
- Compartilhados usados: `components/admin/upload/UploadDropzone.tsx`, `UploadQueue.tsx`, `FolderMappingTable.tsx`
- Status: CORRIGIDA — AGUARDANDO VALIDAÇÃO VISUAL
- Pendente de padrão:
- Início / fim: 2026-09-23 09:35 / 2026-09-23 10:00
- Evidência: agente_responsivo/evidencias/RESP-TELA-026/{antes,depois}/ — `fatias.cjs` com storage admin, 320/360/768/1366; lidas em prancha (D-010)
- Onde ver: http://localhost:5180/admin/produtos/casa-confort-80m2/arquivos
- Como testar: 360/768/1366 — tabela de mapeamento de pastas, fila de upload (NÃO enviar)
- Como confirmar que estava quebrado: em 320/360 a barra de 5 abas do editor tinha 407 px: "Ofertas" cortada e "Arquivos" fora da tela, sem rolagem (`antes/fatia-360-0.png`). Depois: a faixa rola localmente, as 5 abas alcançáveis. A tabela de arquivos (820 px) já rolava localmente.
- arquivo:linha: `pages/admin/products/ProductEditorPage.tsx:169` (`overflow-x-auto` na faixa de abas — compartilhada 022-026)
- Cuidados: `FolderMappingTable` é tabela larga; nunca enviar arquivo Nenhum arquivo enviado/removido.

---
## RESP-TELA-027
- Módulo: Painel admin
- Nome: Preview do produto
- Rota: `/admin/produtos/:productId/preview`
- Componente principal: `pages/admin/products/ProductPreviewPage.tsx`
- Tipo: detalhe
- Perfil que abre: admin
- Compartilhados usados: componentes de `components/products/`
- Status: CORRIGIDA — AGUARDANDO VALIDAÇÃO VISUAL
- Pendente de padrão:
- Início / fim: 2026-09-23 09:35 / 2026-09-23 10:00
- Evidência: agente_responsivo/evidencias/RESP-TELA-027/{antes,depois}/ — `fatias.cjs` com storage admin, 320/360/768/1366; lidas em prancha (D-010)
- Onde ver: http://localhost:5180/admin/produtos/casa-confort-80m2/preview
- Como testar: 360/768/1366 — comparar com RESP-TELA-005
- Como confirmar que estava quebrado: em 320/360 os itens do seletor de plantas tinham min-content de 247 px em caixa de 172/212 (preço grudado/fora do item). Mesmo defeito existia na vitrine (RESP-TELA-005) em 320. Depois: preço desce para a 2ª linha, alinhado à direita.
- arquivo:linha: `components/products/ProductPlanSelector.tsx:79` (`grid-cols-1`) · `:100` (`flex-wrap`) · `:119` (`ml-auto`)
- Cuidados: reaproveita componentes públicos — alterações aqui afetam a vitrine Mesmo componente da vitrine — RESP-TELA-005 revalidada junto.

---
## RESP-TELA-028
- Módulo: Painel admin
- Nome: Coleções
- Rota: `/admin/colecoes`
- Componente principal: `pages/admin/collections/CollectionListPage.tsx`
- Tipo: listagem
- Perfil que abre: admin
- Compartilhados usados: `AdminLayout.tsx`, `primitives.tsx`
- Status: SEM ALTERAÇÃO NECESSÁRIA
- Pendente de padrão:
- Início / fim: 2026-09-23 09:35 / 2026-09-23 10:00
- Evidência: agente_responsivo/evidencias/RESP-TELA-028/antes/ — `fatias.cjs` com storage admin, 320/360/768/1366; lidas em prancha (D-010)
- Onde ver: http://localhost:5180/admin/colecoes
- Como testar: 360/768/1366 — tabela, ações, modal de criação (NÃO salvar)
- Como confirmar que estava quebrado: não estava. Tabela (680 px) rola localmente; Nova coleção visível.
- arquivo:linha: nenhum alterado
- Cuidados: Modal de criação não aberto/salvo.

---
## RESP-TELA-029
- Módulo: Painel admin
- Nome: Categorias
- Rota: `/admin/categorias`
- Componente principal: `pages/admin/categories/CategoryListPage.tsx`
- Tipo: listagem
- Perfil que abre: admin
- Compartilhados usados: `AdminLayout.tsx`, `primitives.tsx`
- Status: SEM ALTERAÇÃO NECESSÁRIA
- Pendente de padrão:
- Início / fim: 2026-09-23 09:35 / 2026-09-23 10:00
- Evidência: agente_responsivo/evidencias/RESP-TELA-029/antes/ — `fatias.cjs` com storage admin, 320/360/768/1366; lidas em prancha (D-010)
- Onde ver: http://localhost:5180/admin/categorias
- Como testar: 360/768/1366 — tabela, ações, modal (NÃO salvar)
- Como confirmar que estava quebrado: não estava. Tabela (560 px) rola localmente; Nova categoria visível.
- arquivo:linha: nenhum alterado
- Cuidados: Modal não salvo.

---
## RESP-TELA-030
- Módulo: Painel admin
- Nome: Reconciliação
- Rota: `/admin/reconciliacao`
- Componente principal: `pages/admin/reports/ReconciliationPage.tsx`
- Tipo: painel (relatório)
- Perfil que abre: admin
- Compartilhados usados: `AdminLayout.tsx`, `primitives.tsx`
- Status: SEM ALTERAÇÃO NECESSÁRIA
- Pendente de padrão:
- Início / fim: 2026-09-23 09:35 / 2026-09-23 10:00
- Evidência: agente_responsivo/evidencias/RESP-TELA-030/antes/ — `fatias.cjs` com storage admin, 320/360/768/1366; lidas em prancha (D-010)
- Onde ver: http://localhost:5180/admin/reconciliacao
- Como testar: 360/768/1366 — filtros de período, indicadores, tabela de divergências
- Como confirmar que estava quebrado: não estava. Ensaiar/Executar, aviso e estado vazio íntegros.
- arquivo:linha: nenhum alterado
- Cuidados: números não podem truncar em nenhuma largura Ensaiar/Executar NÃO clicados. Tabela de divergências não vista (nenhuma varredura executada — sem dado).

---
## RESP-TELA-031
- Módulo: Painel admin
- Nome: Armazenamento
- Rota: `/admin/armazenamento`
- Componente principal: `pages/admin/storage/StoragePage.tsx`
- Tipo: painel
- Perfil que abre: admin
- Compartilhados usados: `AdminLayout.tsx`, `primitives.tsx`
- Status: CORRIGIDA — AGUARDANDO VALIDAÇÃO VISUAL
- Pendente de padrão:
- Início / fim: 2026-09-23 09:35 / 2026-09-23 10:00
- Evidência: agente_responsivo/evidencias/RESP-TELA-031/{antes,depois}/ — `fatias.cjs` com storage admin, 320/360/768/1366; lidas em prancha (D-010)
- Onde ver: http://localhost:5180/admin/armazenamento
- Como testar: 360/768/1366 — indicadores de uso, listagem de arquivos
- Como confirmar que estava quebrado: no ambiente do agente o diagnóstico do bucket falha por falta de credencial AWS e a mensagem traz um token longo sem espaço ("AwsCredentialsProviderChain(credentialsProviders=[..."): 294 px em caixa de 230 (320) / 270 (360), cortado. Depois: quebra dentro do card. Blocos JSON (`<pre>`) já rolam localmente.
- arquivo:linha: `pages/admin/storage/StoragePage.tsx:337` (`break-words` no detalhe da verificação)
- Cuidados: tela recém-criada (commit `c76c78e`) — não desfazer o que foi feito lá Verificar agora / Informar credenciais / Copiar NÃO acionados para gravar. Com bucket configurado as mensagens são curtas — o defeito aparece só em erro.

---
## RESP-TELA-032
- Módulo: Painel admin / Layout global
- Nome: Estados de exceção do painel (404 do painel, acesso negado, fallback de rota)
- Rota: `/admin/rota-inexistente` · acesso sem perfil admin
- Componente principal: `pages/admin/AdminNotFound.tsx`, `pages/admin/Forbidden.tsx`, `components/common/RouteFallback.tsx`
- Tipo: outro
- Perfil que abre: admin (404) · não-admin (acesso negado)
- Compartilhados usados: `app/routers/AdminRoute.tsx`
- Status: SEM ALTERAÇÃO NECESSÁRIA
- Pendente de padrão:
- Início / fim: 2026-09-23 09:35 / 2026-09-23 10:00
- Evidência: agente_responsivo/evidencias/RESP-TELA-032/antes/ — `fatias.cjs` com storage admin, 320/360/768/1366; lidas em prancha (D-010)
- Onde ver: http://localhost:5180/admin/rota-que-nao-existe
- Como testar: 360/768/1366 — mensagem e botão de volta
- Como confirmar que estava quebrado: não estava. 404 do painel íntegro. Acesso negado (não-admin) NÃO visto — sem sessão de cliente comum; pendente de validação humana.
- arquivo:linha: nenhum alterado
- Cuidados: o acesso negado exige sessão de cliente comum (não admin)
