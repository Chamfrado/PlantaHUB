# Production checklist

## Backend environment

Run the API with the `prod` Spring profile and configure:

```env
SPRING_PROFILES_ACTIVE=prod
SPRING_DATASOURCE_URL=jdbc:postgresql://...
SPRING_DATASOURCE_USERNAME=...
SPRING_DATASOURCE_PASSWORD=...
JWT_SECRET=use-a-long-random-secret-at-least-64-characters
APP_WEB_BASE_URL=https://plantahub.com.br
APP_CORS_ALLOWED_ORIGINS=https://plantahub.com.br,https://www.plantahub.com.br
AUTH_COOKIE_SECURE=true
APP_S3_BUCKET=plantahub-assets
APP_S3_REGION=us-east-2
# Deixe as duas vazias para usar a role da instancia (recomendado). Se preenchidas,
# a aplicacao usa credenciais estaticas. Antes elas eram lidas direto do ambiente sem
# aparecer em nenhum application.yml, o que tornava a configuracao invisivel.
APP_S3_ACCESS_KEY=
APP_S3_SECRET_KEY=
# E-mails promovidos a ADMIN no boot, separados por virgula. Só promove, nunca rebaixa.
APP_ADMIN_BOOTSTRAP_EMAILS=dono@plantahub.com.br
# Enquanto true, um entitlement sem assets pinados cai na listagem por prefixo antiga.
# Só desligue depois que o backfill reportar entitlementsWithNoAssets vazio.
APP_DOWNLOADS_LEGACY_FALLBACK=true
INFINITEPAY_HANDLE=...
INFINITEPAY_REDIRECT_URL=https://plantahub.com.br/pagamento/sucesso
INFINITEPAY_WEBHOOK_URL=https://api.plantahub.com.br/v1/webhooks/infinitepay
INFINITEPAY_API_BASE_URL=https://api.checkout.infinitepay.io
```

## Frontend environment

Build the web app with:

```env
VITE_API_URL=https://api.plantahub.com.br
```

## Security expectations

- Serve both frontend and API over HTTPS.
- Keep `AUTH_COOKIE_SECURE=true` in production so session cookies are sent only over HTTPS.
- Keep CORS restricted to `https://plantahub.com.br` and `https://www.plantahub.com.br`.
- Keep Springdoc/Swagger disabled in the `prod` profile.
- Do not store JWTs in `localStorage`; the app now uses an `HttpOnly` `access_token` cookie.
- Rotate `JWT_SECRET` if it was ever exposed or reused from development.

## Pré-requisitos de infraestrutura na AWS

A aplicação não consegue configurar nada disto sozinha.

### 1. CORS do bucket

Necessário para o upload direto do navegador para a S3 (painel Super Admin).

```json
[{
  "AllowedOrigins": ["https://plantahub.com.br", "https://www.plantahub.com.br", "http://localhost:5173"],
  "AllowedMethods": ["PUT", "POST", "GET", "HEAD"],
  "AllowedHeaders": ["*"],
  "ExposeHeaders": ["ETag"],
  "MaxAgeSeconds": 3000
}]
```

`ExposeHeaders: ["ETag"]` não é opcional. Sem ele o navegador não consegue ler o ETag de
cada parte e **completar um upload multipart se torna impossível** — é a causa número um
de "multipart falha em silêncio no browser".

### 2. Regra de lifecycle

`AbortIncompleteMultipartUpload` após 7 dias, no bucket inteiro. Partes órfãs de uploads
interrompidos continuam sendo cobradas e não aparecem na listagem normal.

### 3. Permissões IAM

Além de `s3:GetObject` e `s3:ListBucket`, a aplicação passa a precisar de:

```
s3:PutObject
s3:AbortMultipartUpload
s3:ListBucketMultipartUploads
s3:ListMultipartUploadParts
```

### 4. Política pública

Se algum dia o bucket ganhar uma política `public-read` para imagens de produto, ela deve
ser restrita ao prefixo `public/`. Os ZIPs de download ficam sob `private/bundles/` e não
podem ser expostos.

## API administrativa

Toda rota sob `/v1/admin/**` exige `ROLE_ADMIN`, garantido em duas camadas: a regra de URL
no `SecurityConfig` (que falha fechado para qualquer endpoint que alguém esqueça de anotar)
e `@PreAuthorize` em cada controller (que sobrevive a uma mudança de path).

| Recurso | Rotas |
|---|---|
| Produtos | `GET/POST /products` · `GET/PUT/DELETE /products/{id}` · `PUT /{id}/content` · `POST /{id}/publish\|unpublish\|archive` · `GET /{id}/publish-check` · `GET /{id}/preview` |
| Coleções | `GET/POST /collections` · `GET/PUT/DELETE /collections/{id}` · `POST /{id}/activate\|deactivate` |
| Ofertas | `GET /products/{id}/offers` · `PUT/DELETE /offers/{codigo}` · `POST /offers/reorder` |
| Arquivos | `GET /products/{id}/assets` · `PATCH /assets/{id}` · `POST /assets/{id}/move` · `POST /assets/reorder` · `DELETE /assets/{id}?force=` · `POST /assets/{id}/restore` |
| Mídia | `GET /products/{id}/media` · `PATCH /media/{id}` · `POST /media/reorder` · `DELETE /media/{id}` |
| Categorias | `GET/POST /categories` · `PUT/DELETE /categories/{slug}` |
| Usuários | `GET /users` · `PATCH /users/{id}/role` |
| Operação | reconciliação e backfill (seção abaixo) |

### Regras que o painel impõe

- **Publicar** exige capa, pelo menos uma oferta comprável com preço maior que zero, e
  `content.headline`. O erro `409` lista **todos** os problemas de uma vez.
- **O código da coleção é imutável.** Ele está gravado dentro das chaves S3 legadas e é o
  que o mapeamento pasta→coleção usa; renomeá-lo orfanaria, em silêncio, todo arquivo
  legado daquela pasta.
- **Arquivar não libera o slug.** Mudar o slug de um produto arquivado quebraria URLs e SEO
  já existentes; o erro identifica qual produto arquivado o ocupa.
- **Apagar é bloqueado onde destruiria dados**: produto com vendas, coleção em uso, oferta
  com arquivos ou vendas. O caminho é arquivar ou desativar.
- **Mover um arquivo nunca toca a chave no bucket** — só o vínculo. Mover objetos
  invalidaria URLs assinadas já emitidas.
- **Apagar arquivo já vendido exige `?force=true`**, e mesmo assim a exclusão é lógica:
  quem já pagou continua baixando.
- **O último administrador não pode ser rebaixado**, e ninguém rebaixa a si mesmo — senão o
  painel ficaria inacessível.

### Pré-visualização

`GET /products/{id}/preview` usa o **mesmo montador e os mesmos DTOs** da página pública. A
única diferença é que o preview não filtra por status. Um DTO separado permitiria que as
duas telas divergissem, que é exatamente o que uma pré-visualização não pode fazer.

## Upload pelo painel

O navegador envia os arquivos **direto para a S3**; a API só assina a URL e registra o
resultado. Passar centenas de megabytes por dentro da aplicação consumiria memória e tempo
de requisição sem nenhum ganho.

| Passo | O que acontece |
|---|---|
| `POST /v1/admin/uploads/presign` | O **servidor** escolhe a chave, grava um `pending_upload` e devolve a URL assinada. |
| `PUT` na URL assinada | O navegador envia os bytes direto ao bucket. |
| `POST /{uploadId}/confirm` | A API consulta o bucket (`HeadObject`) e cria a linha com o tamanho e o tipo **reais**. |

O que o cliente declara sobre o arquivo serve apenas para escolher entre PUT único e
multipart. Tamanho e tipo gravados vêm sempre do bucket — confiar no cliente permitiria
registrar metadados inventados, ou registrar um arquivo que nem existe.

Acima de 100 MB o envio vira multipart automaticamente: um PUT único é irretomável, e numa
conexão instável perder 90% de um arquivo grande significa recomeçar do zero.

### Se o upload falhar em silêncio, é o CORS

O erro mais provável é o multipart nunca fechar. Sem `ExposeHeaders: ["ETag"]` na
configuração de CORS do bucket, o navegador **não consegue ler** o ETag de cada parte, e
sem os ETags a S3 não junta o arquivo. O painel detecta essa situação e mostra a mensagem
apontando o CORS, em vez de falhar sem explicação.

### Uploads abandonados

Conexão cai, aba fecha. O varredor (`app.uploads.janitor`) roda de hora em hora, marca os
registros vencidos e **aborta** os multipart — partes órfãs continuam sendo cobradas e não
aparecem na listagem normal do bucket. Ele nunca apaga objeto: a regra de lifecycle do
bucket é que recolhe um PUT que foi gravado e nunca confirmado.

## Migração dos arquivos existentes (reconciliação)

O bucket já contém os arquivos dos produtos. A reconciliação cria as linhas em
`digital_asset` apontando para as **chaves literais** que já existem — nenhum objeto é
movido, renomeado ou apagado.

Todos os endpoints exigem `ROLE_ADMIN` e vêm com `dryRun=true` por padrão.

### Ordem de execução

```bash
# 1. Ensaio. Não grava nada; devolve o runId.
curl -X POST "$API/v1/admin/reconciliation/runs?dryRun=true" --cookie "$ADMIN_COOKIE"

# 2. Leia os achados. Trate cada um antes de seguir.
curl "$API/v1/admin/reconciliation/runs/$RUN_ID"
curl "$API/v1/admin/reconciliation/runs/$RUN_ID/findings?type=UNKNOWN_COLLECTION"
```

| Achado | O que fazer |
|---|---|
| `UNKNOWN_COLLECTION` | Uma pasta do bucket não tem coleção correspondente. **Crie a coleção com esse código** e rode o ensaio de novo. |
| `UNKNOWN_PRODUCT` | A chave aponta para um id de produto que não existe. Investigue antes de seguir. |
| `MEDIA_CANDIDATE` | Imagem solta sob o produto (capa). Informativo — ela nunca vira arquivo entregue ao cliente. |
| `ORPHAN_DB` | Linha no banco sem objeto no bucket. Nada é apagado; investigue se o arquivo foi removido por engano. |
| `DUPLICATE_FILENAME` | Nomes iguais na mesma coleção. Legítimo se estiverem em subpastas diferentes. |
| `MISSING_PRODUCT_PLAN_TYPE` | O vínculo produto‑coleção será criado, sempre com `is_available = false`. |

```bash
# 3. Execução real, depois que o ensaio estiver limpo.
curl -X POST "$API/v1/admin/reconciliation/runs?dryRun=false" --cookie "$ADMIN_COOKIE"

# 4. Ensaio do backfill de pinagem.
curl -X POST "$API/v1/admin/entitlements/backfill-pins?dryRun=true" --cookie "$ADMIN_COOKIE"
```

### O portão do cutover

O campo `entitlementsWithNoAssets` da resposta do backfill é o sinal que importa. Ele lista
compras pagas que ficariam **sem nenhum arquivo** para baixar.

```bash
# 5. Backfill real — só depois que entitlementsWithNoAssets estiver vazio.
curl -X POST "$API/v1/admin/entitlements/backfill-pins?dryRun=false" --cookie "$ADMIN_COOKIE"
```

Enquanto essa lista não estiver vazia, **não desligue** `APP_DOWNLOADS_LEGACY_FALLBACK`.
Com ela ligada, um direito sem arquivos pinados continua caindo no comportamento antigo
(listagem por prefixo no S3), então ninguém perde acesso. Desligá-la antes da hora tiraria
o download de compradores reais.

A reconciliação pode ser repetida à vontade: dez execuções produzem um único estado.

### Desligando a compatibilidade

```bash
# 6. Antes de desligar: capture a biblioteca e um bundle de um comprador real.
#    Depois de desligar, compare byte a byte.
APP_DOWNLOADS_LEGACY_FALLBACK=false
```

Com a flag desligada, o banco passa a ser a única fonte da verdade: um arquivo que existe
no bucket mas não tem linha em `digital_asset` simplesmente não é entregue.

Depois de confirmado em produção, o ramo de compatibilidade pode ser removido do código —
são três métodos, todos marcados com `legacy` no nome, e o teste
`ApoioLiteralConfinedTest` aponta exatamente onde eles estão.

### Nota sobre o cache de ZIPs

Os bundles passaram de `bundles/` para `private/bundles/`, e a versão do cache foi para
`v2` porque o layout do ZIP mudou (subpastas agora são preservadas). Os ZIPs antigos sob
`bundles/` nunca mais serão servidos — são apenas cache e podem ser apagados quando for
conveniente. Manter os bundles sob `private/` garante que uma eventual política
`public-read` para imagens de produto nunca os alcance.

## Rodando os testes

Os testes que precisam de banco usam Postgres de verdade — as migrations dependem de
`jsonb`, `gen_random_uuid()`, índices parciais e regex em CHECK, que o H2 não reproduz.
Há duas formas de fornecer esse banco:

```bash
cd apps/api/plantahub-api

# 1. Testcontainers (padrão, usado pela CI). Exige Docker.
./mvnw verify

# 2. Postgres local, para quem não tem Docker.
#    Aponte para um banco DESCARTÁVEL: o Flyway vai migrá-lo e os testes escrevem nele.
createdb plantahub_test_run
TEST_POSTGRES_URL=jdbc:postgresql://localhost:5432/plantahub_test_run TEST_POSTGRES_USER=postgres TEST_POSTGRES_PASSWORD=postgres   ./mvnw test

# 3. Só o que não precisa de banco nenhum.
./mvnw test -DexcludedGroups=db
```

Os testes com banco estão marcados com `@Tag("db")`. A CI não passa `-DexcludedGroups`,
então lá eles nunca são pulados em silêncio.

O teste mais importante é `FlywayMigrationTest`: ele aplica todas as migrations num banco
limpo e em seguida roda o Hibernate com `ddl-auto=validate`. Qualquer divergência entre
uma `@Entity` e o schema real derruba o build — sem depender de ninguém lembrar de conferir.
