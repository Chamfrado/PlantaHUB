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
- Status: NÃO INICIADA
- Pendente de padrão:
- Início / fim:
- Evidência: agente_responsivo/evidencias/RESP-TELA-002/
- Onde ver: http://localhost:5180/admin/produtos
- Como testar: 360/768/1366 — navegação lateral/superior do painel, troca de seção
- Como confirmar que estava quebrado:
- arquivo:linha:
- Cuidados: altera todas as telas `/admin/*` (RESP-TELA-020 a 032)

---
## RESP-TELA-003
- Módulo: Vitrine pública
- Nome: Home
- Rota: `/`
- Componente principal: `pages/public/Home/Home.tsx`
- Tipo: painel (landing)
- Perfil que abre: anônimo
- Compartilhados usados: `components/home/{Hero,CategoryShowcase,HowItWorks,WhyChoose,FinalCTA}.tsx`, `components/products/ProductCarouselSection.tsx`, `components/products/ProductCard.tsx`
- Status: NÃO INICIADA
- Pendente de padrão: **CANDIDATA A PADRÃO — tipo painel/landing**
- Início / fim:
- Evidência: agente_responsivo/evidencias/RESP-TELA-003/
- Onde ver: http://localhost:5180/
- Como testar: 360/768/1366 — carrossel de produtos, seções, CTA
- Como confirmar que estava quebrado:
- arquivo:linha:
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
- Status: NÃO INICIADA
- Pendente de padrão: **CANDIDATA A PADRÃO — tipo listagem (vitrine)**
- Início / fim:
- Evidência: agente_responsivo/evidencias/RESP-TELA-004/
- Onde ver: http://localhost:5180/produtos
- Como testar: 360/768/1366 — filtros, grade de cartões, preço e botão de cada cartão
- Como confirmar que estava quebrado:
- arquivo:linha:
- Cuidados: o commit `14d2648` já ajustou preço/alinhamento dos cartões — não desfazer

---
## RESP-TELA-005
- Módulo: Vitrine pública
- Nome: Detalhe do produto
- Rota: `/:category/:slug`
- Componente principal: `pages/public/ProductDetails/ProductDetails.tsx`
- Tipo: detalhe
- Perfil que abre: anônimo
- Compartilhados usados: `components/products/{ProductDetailsView,ProductHero,ProductDetailCard,ProductPlanSelector,ProductAccordion,ProductAccordionItem}.tsx`
- Status: NÃO INICIADA
- Pendente de padrão: **CANDIDATA A PADRÃO — tipo detalhe**
- Início / fim:
- Evidência: agente_responsivo/evidencias/RESP-TELA-005/
- Onde ver: http://localhost:5180/casas/SLUG-REAL
- Como testar: 360/768/1366 — seletor de planos, acordeão, galeria, botão de compra
- Como confirmar que estava quebrado:
- arquivo:linha:
- Cuidados: rota com parâmetro — pegar `category/slug` real pela listagem `/produtos`; sem produto no banco, `BLOQUEADA — SEM DADO`

---
## RESP-TELA-006
- Módulo: Vitrine pública
- Nome: Sobre nós
- Rota: `/sobre`
- Componente principal: `pages/public/About/AboutUs.tsx`
- Tipo: outro (institucional)
- Perfil que abre: anônimo
- Compartilhados usados: shell (RESP-TELA-001)
- Status: NÃO INICIADA
- Pendente de padrão:
- Início / fim:
- Evidência: agente_responsivo/evidencias/RESP-TELA-006/
- Onde ver: http://localhost:5180/sobre
- Como testar: 360/768/1366 — blocos de texto e imagem
- Como confirmar que estava quebrado:
- arquivo:linha:
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
- Status: NÃO INICIADA
- Pendente de padrão:
- Início / fim:
- Evidência: agente_responsivo/evidencias/RESP-TELA-007/
- Onde ver: http://localhost:5180/contato
- Como testar: 360/768/1366 — campos, labels, botão de envio (NÃO submeter)
- Como confirmar que estava quebrado:
- arquivo:linha:
- Cuidados: nunca submeter o formulário

---
## RESP-TELA-008
- Módulo: Vitrine pública
- Nome: FAQ
- Rota: `/faq`
- Componente principal: `pages/public/Faq/Faq.tsx`
- Tipo: outro (institucional)
- Perfil que abre: anônimo
- Compartilhados usados: shell (RESP-TELA-001)
- Status: NÃO INICIADA
- Pendente de padrão:
- Início / fim:
- Evidência: agente_responsivo/evidencias/RESP-TELA-008/
- Onde ver: http://localhost:5180/faq
- Como testar: 360/768/1366 — abrir e fechar itens do acordeão
- Como confirmar que estava quebrado:
- arquivo:linha:
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
- Status: NÃO INICIADA
- Pendente de padrão:
- Início / fim:
- Evidência: agente_responsivo/evidencias/RESP-TELA-009/
- Onde ver: http://localhost:5180/trabalhe-conosco
- Como testar: 360/768/1366 — campos e upload, se houver (NÃO submeter)
- Como confirmar que estava quebrado:
- arquivo:linha:
- Cuidados: nunca submeter

---
## RESP-TELA-010
- Módulo: Vitrine pública
- Nome: Termos de serviço
- Rota: `/legal/termos`
- Componente principal: `pages/legal/TermsOfServicePage.tsx`
- Tipo: outro (texto longo)
- Perfil que abre: anônimo
- Compartilhados usados: shell (RESP-TELA-001)
- Status: NÃO INICIADA
- Pendente de padrão:
- Início / fim:
- Evidência: agente_responsivo/evidencias/RESP-TELA-010/
- Onde ver: http://localhost:5180/legal/termos
- Como testar: 360/768/1366 — largura de leitura, listas, tabelas se houver
- Como confirmar que estava quebrado:
- arquivo:linha:
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
- Status: NÃO INICIADA
- Pendente de padrão:
- Início / fim:
- Evidência: agente_responsivo/evidencias/RESP-TELA-011/
- Onde ver: http://localhost:5180/legal/privacidade
- Como testar: 360/768/1366 — largura de leitura, listas
- Como confirmar que estava quebrado:
- arquivo:linha:
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
- Status: NÃO INICIADA
- Pendente de padrão: **CANDIDATA A PADRÃO — tipo formulário**
- Início / fim:
- Evidência: agente_responsivo/evidencias/RESP-TELA-012/
- Onde ver: http://localhost:5180/login
- Como testar: 360/768/1366 — campos, erro de validação, teclado mobile cobrindo o botão
- Como confirmar que estava quebrado:
- arquivo:linha:
- Cuidados: NÃO autenticar com credenciais; só inspecionar o layout

---
## RESP-TELA-013
- Módulo: Conta do cliente
- Nome: Cadastro
- Rota: `/register`
- Componente principal: `pages/public/Register/Register.tsx`
- Tipo: formulário
- Perfil que abre: anônimo
- Compartilhados usados: `components/auth/RegisterForm.tsx`
- Status: NÃO INICIADA
- Pendente de padrão:
- Início / fim:
- Evidência: agente_responsivo/evidencias/RESP-TELA-013/
- Onde ver: http://localhost:5180/register
- Como testar: 360/768/1366 — campos, validação, botão
- Como confirmar que estava quebrado:
- arquivo:linha:
- Cuidados: NUNCA submeter (criaria usuário no banco)

---
## RESP-TELA-014
- Módulo: Conta do cliente
- Nome: Preferências / conta
- Rota: `/configs`
- Componente principal: `pages/Preferences/Preferences.tsx`
- Tipo: formulário (com abas)
- Perfil que abre: cliente autenticado
- Compartilhados usados: `components/preferences/AccountTab.tsx`, `components/preferences/TransactionTab.tsx`, `app/routers/ProtectedRoute.tsx`
- Status: NÃO INICIADA
- Pendente de padrão: **CANDIDATA A PADRÃO — tipo formulário com abas**
- Início / fim:
- Evidência: agente_responsivo/evidencias/RESP-TELA-014/
- Onde ver: http://localhost:5180/configs
- Como testar: 360/768/1366 — trocar de aba, listar transações (NÃO salvar)
- Como confirmar que estava quebrado:
- arquivo:linha:
- Cuidados: exige storageState de cliente; a aba de transações provavelmente tem tabela

---
## RESP-TELA-015
- Módulo: Conta do cliente
- Nome: Biblioteca (itens comprados)
- Rota: `/biblioteca`
- Componente principal: `pages/Library/Library.tsx`
- Tipo: listagem
- Perfil que abre: cliente autenticado
- Compartilhados usados: `components/library/LibraryProductCard.tsx`, `app/routers/ProtectedRoute.tsx`
- Status: NÃO INICIADA
- Pendente de padrão:
- Início / fim:
- Evidência: agente_responsivo/evidencias/RESP-TELA-015/
- Onde ver: http://localhost:5180/biblioteca
- Como testar: 360/768/1366 — grade de cartões, ação de download
- Como confirmar que estava quebrado:
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
- Status: NÃO INICIADA
- Pendente de padrão:
- Início / fim:
- Evidência: agente_responsivo/evidencias/RESP-TELA-016/
- Onde ver: http://localhost:5180/biblioteca/PRODUCT-ID/PLAN-CODE
- Como testar: 360/768/1366 — lista de arquivos, botões de download
- Como confirmar que estava quebrado:
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
- Status: NÃO INICIADA
- Pendente de padrão:
- Início / fim:
- Evidência: agente_responsivo/evidencias/RESP-TELA-017/
- Onde ver: http://localhost:5180/carrinho
- Como testar: 360/768/1366 — itens, quantidades, total, botão de finalizar (NÃO finalizar)
- Como confirmar que estava quebrado:
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
- Status: NÃO INICIADA
- Pendente de padrão:
- Início / fim:
- Evidência: agente_responsivo/evidencias/RESP-TELA-018/
- Onde ver: http://localhost:5180/pedidos/ORDER-ID
- Como testar: 360/768/1366 — itens do pedido, status, valores
- Como confirmar que estava quebrado:
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
- Status: NÃO INICIADA
- Pendente de padrão:
- Início / fim:
- Evidência: agente_responsivo/evidencias/RESP-TELA-019/
- Onde ver: http://localhost:5180/pagamento/sucesso
- Como testar: 360/768/1366 — mensagem e ações seguintes
- Como confirmar que estava quebrado:
- arquivo:linha:
- Cuidados: exige sessão; pode depender de query string do provedor de pagamento

---
## RESP-TELA-020
- Módulo: Painel admin
- Nome: Lista de produtos
- Rota: `/admin/produtos`
- Componente principal: `pages/admin/products/ProductListPage.tsx`
- Tipo: listagem
- Perfil que abre: admin
- Compartilhados usados: `app/layouts/AdminLayout.tsx`, `components/admin/ui/primitives.tsx`
- Status: NÃO INICIADA
- Pendente de padrão: **CANDIDATA A PADRÃO — tipo listagem administrativa (tabela)**
- Início / fim:
- Evidência: agente_responsivo/evidencias/RESP-TELA-020/antes/ (medição de 2026-09-23 08:08)
- Onde ver: http://localhost:5180/admin/produtos
- Como testar: 360/768/1366 — busca, filtro, paginação, cada ação da linha
- Como confirmar que estava quebrado: o script devolve exit 0 nas 14 larguras (a PÁGINA
  não rola), mas `antes/360.png` mostra o menu do painel cortado em "Armazen…" e as
  colunas PREÇO BASE e AÇÕES fora da tela — as ações Despublicar e Arquivar ficam
  inalcançáveis sem scroll dentro do contêiner. `antes/1366.png` mostra a tela íntegra,
  com os 6 produtos e o rótulo adm@plantahub.com, o que prova que a sessão vale.
- arquivo:linha:
- Cuidados: tabela larga — scroll horizontal no contêiner, nunca na página; nenhuma coluna ou ação pode sumir no mobile sem equivalente visível

---
## RESP-TELA-021
- Módulo: Painel admin
- Nome: Novo produto
- Rota: `/admin/produtos/novo`
- Componente principal: `pages/admin/products/ProductEditorPage.tsx` (mode=create)
- Tipo: formulário
- Perfil que abre: admin
- Compartilhados usados: `components/admin/ui/primitives.tsx`, `components/admin/ui/MoneyInput.tsx`
- Status: NÃO INICIADA
- Pendente de padrão:
- Início / fim:
- Evidência: agente_responsivo/evidencias/RESP-TELA-021/
- Onde ver: http://localhost:5180/admin/produtos/novo
- Como testar: 360/768/1366 — campos, MoneyInput, botão salvar (NÃO salvar)
- Como confirmar que estava quebrado:
- arquivo:linha:
- Cuidados: NUNCA salvar

---
## RESP-TELA-022
- Módulo: Painel admin
- Nome: Editor de produto — aba Geral
- Rota: `/admin/produtos/:productId/geral`
- Componente principal: `pages/admin/products/tabs/GeneralTab.tsx`
- Tipo: formulário (com abas)
- Perfil que abre: admin
- Compartilhados usados: `ProductEditorPage.tsx` (barra de abas), `components/admin/ui/primitives.tsx`, `MoneyInput.tsx`
- Status: NÃO INICIADA
- Pendente de padrão: **CANDIDATA A PADRÃO — tipo editor com abas (barra de 5 abas no mobile)**
- Início / fim:
- Evidência: agente_responsivo/evidencias/RESP-TELA-022/
- Onde ver: http://localhost:5180/admin/produtos/PRODUCT-ID/geral
- Como testar: 360/768/1366 — barra de abas com 5 itens, campos, botão salvar (NÃO salvar)
- Como confirmar que estava quebrado:
- arquivo:linha:
- Cuidados: a barra de abas é compartilhada pelas telas 022-026; corrigi-la marca as quatro seguintes como REVALIDAÇÃO NECESSÁRIA

---
## RESP-TELA-023
- Módulo: Painel admin
- Nome: Editor de produto — aba Conteúdo
- Rota: `/admin/produtos/:productId/conteudo`
- Componente principal: `pages/admin/products/tabs/ContentTab.tsx`
- Tipo: formulário
- Perfil que abre: admin
- Compartilhados usados: `components/admin/forms/RepeatableList.tsx`, `primitives.tsx`
- Status: NÃO INICIADA
- Pendente de padrão:
- Início / fim:
- Evidência: agente_responsivo/evidencias/RESP-TELA-023/
- Onde ver: http://localhost:5180/admin/produtos/PRODUCT-ID/conteudo
- Como testar: 360/768/1366 — adicionar/remover item da lista repetível (sem salvar)
- Como confirmar que estava quebrado:
- arquivo:linha:
- Cuidados: `RepeatableList` é compartilhado dentro do painel

---
## RESP-TELA-024
- Módulo: Painel admin
- Nome: Editor de produto — aba Imagens
- Rota: `/admin/produtos/:productId/imagens`
- Componente principal: `pages/admin/products/tabs/MediaTab.tsx`
- Tipo: outro (galeria + upload)
- Perfil que abre: admin
- Compartilhados usados: `components/admin/upload/UploadDropzone.tsx`, `UploadQueue.tsx`
- Status: NÃO INICIADA
- Pendente de padrão:
- Início / fim:
- Evidência: agente_responsivo/evidencias/RESP-TELA-024/
- Onde ver: http://localhost:5180/admin/produtos/PRODUCT-ID/imagens
- Como testar: 360/768/1366 — grade de imagens, área de upload (NÃO enviar arquivo)
- Como confirmar que estava quebrado:
- arquivo:linha:
- Cuidados: nunca enviar arquivo (grava no storage)

---
## RESP-TELA-025
- Módulo: Painel admin
- Nome: Editor de produto — aba Ofertas
- Rota: `/admin/produtos/:productId/ofertas`
- Componente principal: `pages/admin/products/tabs/OffersTab.tsx`
- Tipo: grade de lançamento (preços por plano)
- Perfil que abre: admin
- Compartilhados usados: `components/admin/ui/MoneyInput.tsx`, `primitives.tsx`
- Status: NÃO INICIADA
- Pendente de padrão: **CANDIDATA A PADRÃO — tipo grade de lançamento**
- Início / fim:
- Evidência: agente_responsivo/evidencias/RESP-TELA-025/
- Onde ver: http://localhost:5180/admin/produtos/PRODUCT-ID/ofertas
- Como testar: 360/768/1366 — leitura em linha dos preços por plano (NÃO salvar)
- Como confirmar que estava quebrado:
- arquivo:linha:
- Cuidados: grade de lançamento NÃO vira cartão — scroll horizontal localizado com primeira coluna fixa

---
## RESP-TELA-026
- Módulo: Painel admin
- Nome: Editor de produto — aba Arquivos
- Rota: `/admin/produtos/:productId/arquivos`
- Componente principal: `pages/admin/products/tabs/AssetsTab.tsx`
- Tipo: listagem
- Perfil que abre: admin
- Compartilhados usados: `components/admin/upload/UploadDropzone.tsx`, `UploadQueue.tsx`, `FolderMappingTable.tsx`
- Status: NÃO INICIADA
- Pendente de padrão:
- Início / fim:
- Evidência: agente_responsivo/evidencias/RESP-TELA-026/
- Onde ver: http://localhost:5180/admin/produtos/PRODUCT-ID/arquivos
- Como testar: 360/768/1366 — tabela de mapeamento de pastas, fila de upload (NÃO enviar)
- Como confirmar que estava quebrado:
- arquivo:linha:
- Cuidados: `FolderMappingTable` é tabela larga; nunca enviar arquivo

---
## RESP-TELA-027
- Módulo: Painel admin
- Nome: Preview do produto
- Rota: `/admin/produtos/:productId/preview`
- Componente principal: `pages/admin/products/ProductPreviewPage.tsx`
- Tipo: detalhe
- Perfil que abre: admin
- Compartilhados usados: componentes de `components/products/`
- Status: NÃO INICIADA
- Pendente de padrão:
- Início / fim:
- Evidência: agente_responsivo/evidencias/RESP-TELA-027/
- Onde ver: http://localhost:5180/admin/produtos/PRODUCT-ID/preview
- Como testar: 360/768/1366 — comparar com RESP-TELA-005
- Como confirmar que estava quebrado:
- arquivo:linha:
- Cuidados: reaproveita componentes públicos — alterações aqui afetam a vitrine

---
## RESP-TELA-028
- Módulo: Painel admin
- Nome: Coleções
- Rota: `/admin/colecoes`
- Componente principal: `pages/admin/collections/CollectionListPage.tsx`
- Tipo: listagem
- Perfil que abre: admin
- Compartilhados usados: `AdminLayout.tsx`, `primitives.tsx`
- Status: NÃO INICIADA
- Pendente de padrão:
- Início / fim:
- Evidência: agente_responsivo/evidencias/RESP-TELA-028/
- Onde ver: http://localhost:5180/admin/colecoes
- Como testar: 360/768/1366 — tabela, ações, modal de criação (NÃO salvar)
- Como confirmar que estava quebrado:
- arquivo:linha:
- Cuidados:

---
## RESP-TELA-029
- Módulo: Painel admin
- Nome: Categorias
- Rota: `/admin/categorias`
- Componente principal: `pages/admin/categories/CategoryListPage.tsx`
- Tipo: listagem
- Perfil que abre: admin
- Compartilhados usados: `AdminLayout.tsx`, `primitives.tsx`
- Status: NÃO INICIADA
- Pendente de padrão:
- Início / fim:
- Evidência: agente_responsivo/evidencias/RESP-TELA-029/
- Onde ver: http://localhost:5180/admin/categorias
- Como testar: 360/768/1366 — tabela, ações, modal (NÃO salvar)
- Como confirmar que estava quebrado:
- arquivo:linha:
- Cuidados:

---
## RESP-TELA-030
- Módulo: Painel admin
- Nome: Reconciliação
- Rota: `/admin/reconciliacao`
- Componente principal: `pages/admin/reports/ReconciliationPage.tsx`
- Tipo: painel (relatório)
- Perfil que abre: admin
- Compartilhados usados: `AdminLayout.tsx`, `primitives.tsx`
- Status: NÃO INICIADA
- Pendente de padrão:
- Início / fim:
- Evidência: agente_responsivo/evidencias/RESP-TELA-030/
- Onde ver: http://localhost:5180/admin/reconciliacao
- Como testar: 360/768/1366 — filtros de período, indicadores, tabela de divergências
- Como confirmar que estava quebrado:
- arquivo:linha:
- Cuidados: números não podem truncar em nenhuma largura

---
## RESP-TELA-031
- Módulo: Painel admin
- Nome: Armazenamento
- Rota: `/admin/armazenamento`
- Componente principal: `pages/admin/storage/StoragePage.tsx`
- Tipo: painel
- Perfil que abre: admin
- Compartilhados usados: `AdminLayout.tsx`, `primitives.tsx`
- Status: NÃO INICIADA
- Pendente de padrão:
- Início / fim:
- Evidência: agente_responsivo/evidencias/RESP-TELA-031/
- Onde ver: http://localhost:5180/admin/armazenamento
- Como testar: 360/768/1366 — indicadores de uso, listagem de arquivos
- Como confirmar que estava quebrado:
- arquivo:linha:
- Cuidados: tela recém-criada (commit `c76c78e`) — não desfazer o que foi feito lá

---
## RESP-TELA-032
- Módulo: Painel admin / Layout global
- Nome: Estados de exceção do painel (404 do painel, acesso negado, fallback de rota)
- Rota: `/admin/rota-inexistente` · acesso sem perfil admin
- Componente principal: `pages/admin/AdminNotFound.tsx`, `pages/admin/Forbidden.tsx`, `components/common/RouteFallback.tsx`
- Tipo: outro
- Perfil que abre: admin (404) · não-admin (acesso negado)
- Compartilhados usados: `app/routers/AdminRoute.tsx`
- Status: NÃO INICIADA
- Pendente de padrão:
- Início / fim:
- Evidência: agente_responsivo/evidencias/RESP-TELA-032/
- Onde ver: http://localhost:5180/admin/rota-que-nao-existe
- Como testar: 360/768/1366 — mensagem e botão de volta
- Como confirmar que estava quebrado:
- arquivo:linha:
- Cuidados: o acesso negado exige sessão de cliente comum (não admin)
