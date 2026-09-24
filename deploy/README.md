# PlantaHUB — Implantação em uma VM Ubuntu 24.04 nova

Roteiro para reconstruir a produção do zero em uma VM vazia e deixar os próximos pushes
na branch `prod` implantando sozinhos, **sem build na VM**.

```
GitHub Actions (push em prod)                          VM Ubuntu 24.04
┌─────────────────────────────────────┐                ┌──────────────────────────────────────────┐
│ backend:  mvnw clean package (+testes)│── app.jar ──▶ │ /opt/plantahub/incoming/<release>/        │
│ frontend: npm ci, lint, test, build   │── dist/  ──▶ │   sudo plantahub-release activate <id>    │
│ deploy:   rsync + ssh deploy@VM       │               │   ├ pg_dump                               │
│           verificação pela Internet   │               │   ├ backend: symlink → restart → /health  │
└─────────────────────────────────────┘                │   ├ frontend: symlink → index + JS + MIME │
                                                       │   └ falhou? volta a versão anterior        │
Internet ─▶ 80/443 ─▶ Nginx ─┬─ www.plantahub.com.br → /var/www/plantahub/frontend (SPA)            │
                             ├─ plantahub.com.br     → 301 https://www.plantahub.com.br             │
                             └─ api.plantahub.com.br → 127.0.0.1:8080 (Spring Boot, perfil prod)    │
                                                       │ PostgreSQL 16 em localhost:5432 (fechado)│
                                                       └──────────────────────────────────────────┘
```

Índice: [Valores detectados](#valores-detectados-automaticamente-no-projeto) ·
[Diferenças da VM anterior](#diferenças-em-relação-à-vm-anterior) ·
[Arquivos](#o-que-há-nesta-pasta) · Fases [0](#fase-0--pré-requisitos) ·
[1](#fase-1--auditoria--valores-detectados) · [2](#fase-2--bootstrap) ·
[3](#fase-3--postgresql) · [4](#fase-4--secrets--env) · [5](#fase-5--backend) ·
[6](#fase-6--nginx--api) · [7](#fase-7--dns) · [8](#fase-8--ssl-da-api) ·
[9](#fase-9--frontend) · [10](#fase-10--ssl-do-frontend) · [11](#fase-11--testes) ·
[12](#fase-12--github-actions) · [13](#fase-13--primeiro-auto-deploy) ·
[14](#fase-14--rollback) · [15](#fase-15--operação-e-manutenção) ·
[Checklist](#checklist-da-vm-nova)

---

## Valores detectados automaticamente no projeto

Levantados do código em 2026-09-23 (branch `main`, commit `9bb61c2`). Onde está escrito
**NÃO IDENTIFICADO**, o valor não existe no repositório.

| Item | Valor real | Onde está definido |
|---|---|---|
| Frontend path | `apps/web/plantahub-web` | monorepo `apps/` |
| Backend path | `apps/api/plantahub-api` | monorepo `apps/` (`apps/api/out/` é um resto versionado do IntelliJ, não é usado) |
| Java | 21 | `pom.xml` → `<java.version>21` |
| Spring Boot | **4.0.2** (o `README.md` da raiz ainda diz 3.4.2 — desatualizado) | `pom.xml` → parent |
| Node | **22** no CI (antes 20). Nenhum `.nvmrc`/`engines` no projeto; `jsdom@30` e `@testing-library/jest-dom@7` exigem ≥ 22 | `package-lock.json` |
| Package manager | npm (`package-lock.json`) | — |
| Build backend | `./mvnw -B clean package` (roda os testes; exige Docker p/ Testcontainers) | `.github/workflows/deploy-prod.yml` |
| Build frontend | `npm ci && npm run build` (`tsc -b && vite build`) | `package.json` |
| Backend artifact | `target/plantahub-api-0.0.1-SNAPSHOT.jar` (~81 MB) → renomeado `app.jar` | `pom.xml` (artifactId + version) |
| Frontend artifact | `dist/` → `index.html`, `assets/*.js|css` (hash), `brand/`, `photos/`, `products/`, `vite.svg` | Vite (sem `outDir`/`base` customizados) |
| Backend port | `8080` | `application-prod.yml` → `server.port` |
| Spring profile | `prod` — **não é ativado sozinho**: precisa de `SPRING_PROFILES_ACTIVE=prod` (fixado no unit systemd) | `application-prod.yml`; `@Profile("!prod")` em `DevOrderController` e `DevAdminAccountRunner` |
| Health endpoint | `GET` e `HEAD /health` → `200 {"status":"ok"}`, público, sem autenticação. Não consulta o banco — mas o boot falha se Flyway/banco falharem, então 200 após restart = app de pé | `HealthController.java`, `SecurityConfig.java` |
| Actuator | NÃO IDENTIFICADO (dependência ausente; não existe `/actuator/health`) | — |
| Database name esperado | `plantahub` | `application.yml` (default da URL) |
| Flyway location | `classpath:db/migration` (padrão) → `src/main/resources/db/migration`, V1…V30 (sem V12) | `spring-boot-starter-flyway` |
| API env var frontend | `VITE_API_URL` (build-time) = `https://api.plantahub.com.br` | `src/lib/http.ts`, workflow |
| S3 bucket env | `APP_S3_BUCKET` (obrigatória no prod) — valor em uso: `plantahub-assets` | `application-prod.yml`, migrations V17/V25 |
| S3 region env | `APP_S3_REGION` (obrigatória no prod) — valor em uso: `us-east-2` | idem |
| S3 credenciais | `APP_S3_ACCESS_KEY`/`APP_S3_SECRET_KEY` (opcionais) → painel admin (tabela `app_secret`, cifrada) → cadeia padrão AWS | `DynamicS3CredentialsProvider.java` |
| S3 URLs | imagens: `https://<bucket>.s3.<região>.amazonaws.com/<key>` ou `APP_S3_PUBLIC_BASE_URL`; arquivos pagos: **presigned GET** (15 min); uploads admin: **presigned PUT/multipart** direto do navegador | `PublicMediaUrls`, `S3ObjectStorageAdapter`, `UploadService` |
| JWT env | `JWT_SECRET` (obrigatória, ≥ 32 bytes); issuer e duração fixos no prod (`plantahub`, 240 min) | `application-prod.yml`, `JwtService` |
| Sessão | cookie `access_token` HttpOnly, `Secure` (`AUTH_COOKIE_SECURE`), `SameSite=Lax`, host-only em `api.` | `AuthCookieService` |
| CORS config | `APP_CORS_ALLOWED_ORIGINS` (default prod: `https://plantahub.com.br,https://www.plantahub.com.br`), com credentials | `CorsConfig.java` |
| Proxy headers | `server.forward-headers-strategy=framework` (usa `X-Forwarded-*`) | `application-prod.yml` |
| Pagamentos | InfinitePay: `INFINITEPAY_HANDLE`, `_REDIRECT_URL`, `_WEBHOOK_URL` obrigatórias; webhook público `POST /v1/webhooks/infinitepay` | `application-prod.yml`, `SecurityConfig` |
| GitHub workflow | `.github/workflows/deploy-prod.yml` (deploy) e `ci.yml` (testes em `main`/PRs) | — |
| Production branch | `prod` (hoje 36 commits atrás de `main`) | `deploy-prod.yml` |
| Segredo extra não documentado | `APP_SECRETS_KEY` → `app.secrets.key` (chave das credenciais AWS salvas pelo painel; vazio = derivada do `JWT_SECRET`) | `SecretCipher.java` |

### Diferenças em relação à VM anterior

| VM anterior | Agora | Por quê |
|---|---|---|
| `/opt/plantahub/backend/app.jar` arquivo comum | **mesmo caminho**, agora symlink → `current/app.jar` → `releases/<id>/app.jar` | rollback instantâneo; o systemd continua apontando para o mesmo caminho |
| `/opt/plantahub/backend/.env` | **igual** (`root:plantahub 640`) | — |
| `/var/www/plantahub/frontend/` diretório | **mesmo caminho**, agora symlink → `releases/<id>/` | troca atômica: nunca há um instante com `index.html` novo e assets faltando |
| serviço `plantahub-backend` | **igual**, rodando como usuário `plantahub` sem login | antes o workflow exigia root (`systemctl` sem sudo) |
| `sites-available/plantahub-api` e `plantahub-frontend` | **mesmos nomes**; conteúdo versionado em `deploy/nginx/` | — |
| deploy como root via SSH | usuário `deploy` + sudo **só** para `/usr/local/sbin/plantahub-release` | sem root permanente |
| `rsync --delete` direto no diretório servido | upload para `incoming/`, validação, troca de symlink | a janela de upload parcial causava 404/MIME `text/html` |
| sem backup/rollback/health check | `pg_dump` + health check + rollback automático | — |
| `ssh-keyscan` a cada deploy | secret `VM_KNOWN_HOSTS` (keyscan só como fallback com aviso) | evita confiar em qualquer chave apresentada |
| Node 20 | Node 22 | exigência das devDependencies de teste |
| certbot `--nginx` (presumido) | `certbot certonly --webroot` + configs versionadas | o certbot não reescreve os arquivos do Nginx |

## O que há nesta pasta

```
deploy/
├── README.md                          este roteiro
├── env/
│   ├── backend.env.example            → /opt/plantahub/backend/.env (runtime, segredos)
│   └── deploy.conf.example            → /etc/plantahub/deploy.conf (IP, domínios; sem segredos)
├── nginx/
│   ├── plantahub-api.http.conf        API, fase provisória (HTTP)
│   ├── plantahub-api.conf             API, definitiva (HTTPS)
│   ├── plantahub-frontend.http.conf   site, fase provisória (HTTP)
│   ├── plantahub-frontend.conf        site, definitiva (HTTPS + redirects)
│   ├── plantahub-default.conf         Host desconhecido → 444
│   ├── conf.d/plantahub-global.conf   server_tokens off, gzip
│   └── snippets/                      acme, ssl, proxy, spa, security-headers
├── systemd/plantahub-backend.service
├── sudoers/plantahub-deploy
└── scripts/
    ├── lib/common.sh
    ├── 01-bootstrap-server.sh         pacotes, JDK 21, usuários, UFW, swap
    ├── 02-setup-postgresql.sh         role, banco, schema public, validação TCP
    ├── 03-setup-directories.sh        diretórios, .env, systemd, sudoers, plantahub-release
    ├── 04-install-backend.sh          instala um app.jar manualmente
    ├── 05-install-frontend.sh         instala um dist/ manualmente
    ├── 06-setup-nginx.sh              instala os sites (HTTP ou HTTPS, conforme o certificado)
    ├── 07-setup-ssl.sh                valida DNS/portas → certbot → HTTPS
    ├── plantahub-release.sh           → /usr/local/sbin/plantahub-release (activate/rollback/status)
    ├── verify-deployment.sh           verificação completa (somente leitura)
    └── rollback.sh                    rollback manual
```

Todos os scripts são idempotentes: rodar de novo corrige desvios sem destruir nada
(o `.env` existente nunca é sobrescrito).

### Layout na VM

```
/etc/plantahub/deploy.conf                 root:root       644
/opt/plantahub/backend/.env                root:plantahub  640
/opt/plantahub/backend/app.jar           → current/app.jar
/opt/plantahub/backend/current           → releases/<id>
/opt/plantahub/backend/previous          → releases/<id>
/opt/plantahub/backend/releases/<id>/app.jar   root:root 644 (o app não consegue alterar o próprio jar)
/opt/plantahub/incoming/                   deploy:deploy   750   (upload do CI)
/opt/plantahub/staging/                    root:root       700   (o pacote é movido para cá e validado aqui)
/var/www/plantahub/frontend              → releases/<id>          (root do Nginx)
/var/www/plantahub/previous              → releases/<id>
/var/www/plantahub/releases/<id>/          root:root, dirs 755, arquivos 644
/var/www/letsencrypt/                      webroot do ACME
/var/backups/plantahub/db/*.dump           root 600  (pg_dump antes de cada deploy; mantém 10)
```

---

## FASE 0 — Pré-requisitos

**OBJETIVO** Ter em mãos tudo que a VM não consegue descobrir sozinha.

**PRÉ-REQUISITOS / o que você precisa**

- VM Ubuntu Server 24.04 64 bits, acesso SSH com um usuário com `sudo` (ex.: `ubuntu`).
- O **IP público novo** da VM.
- Acesso ao painel DNS de `plantahub.com.br` e ao **firewall do provedor**.
- Um e-mail para o Let's Encrypt.
- Valores de produção: handle InfinitePay, `JWT_SECRET` (novo ou o antigo — ver fase 4),
  credenciais AWS (ou uma instance role / credenciais salvas no painel).
- Se houver dados reais na VM antiga: acesso a ela para gerar um dump (fase 3, caminho B).
- Recomendado: ≥ 2 GB de RAM e ≥ 20 GB de disco (o ZIP de download de um pacote BIM é
  montado em `/tmp` antes de ir para a S3).

**COMANDOS** — na sua máquina (não na VM):

```bash
# 1. Chave SSH exclusiva do GitHub Actions (sem senha). Não reutilize a sua chave pessoal.
ssh-keygen -t ed25519 -N "" -C "github-actions-plantahub" -f ./plantahub_deploy
#    plantahub_deploy      → secret VM_SSH_KEY  (PRIVADA — nunca no git)
#    plantahub_deploy.pub  → DEPLOY_PUBKEY na fase 2

# 2. Copiar a pasta deploy/ para a VM (a branch prod ainda não tem estes arquivos).
scp -r deploy ubuntu@<NOVO_IP>:~/plantahub-deploy
```

**RESULTADO ESPERADO** `~/plantahub-deploy/scripts/01-bootstrap-server.sh` existe na VM.

**VALIDAÇÃO** Na VM: `head -1 ~/plantahub-deploy/scripts/01-bootstrap-server.sh | od -c | head -2`
não deve mostrar `\r`.

**ROLLBACK** Nada foi alterado ainda.

**ERROS COMUNS**
- `$'\r': command not found` → os arquivos saíram do Windows com CRLF. O `.gitattributes`
  força LF em `deploy/**`; se copiou de outra fonte:
  `find ~/plantahub-deploy -type f -exec sed -i 's/\r$//' {} +`.
- Firewall do provedor bloqueando a 22: resolva no painel da nuvem antes de tudo.

---

## FASE 1 — Auditoria / valores detectados

**OBJETIVO** Conferir que os valores de [Valores detectados](#valores-detectados-automaticamente-no-projeto)
continuam verdadeiros na revisão que vai para `prod`.

**PRÉ-REQUISITOS** Checkout da branch que será implantada.

**COMANDOS**

```bash
grep -n 'port:\|profiles\|forward-headers' apps/api/plantahub-api/src/main/resources/application-prod.yml
grep -rn '"/health"' apps/api/plantahub-api/src/main/java
ls apps/api/plantahub-api/src/main/resources/db/migration | sort -V | tail -3
grep -n VITE_API_URL .github/workflows/deploy-prod.yml
```

**RESULTADO ESPERADO** porta 8080, `/health`, última migration V30, `VITE_API_URL=https://api.plantahub.com.br`.

**VALIDAÇÃO** Se algo mudou (porta, health), ajuste `BACKEND_PORT`/`HEALTH_PATH` em
`deploy/env/deploy.conf.example` **e** o `proxy_pass` em `deploy/nginx/snippets/plantahub-proxy.conf`.

**ROLLBACK** —

**ERROS COMUNS** Implantar `prod` sem antes trazer `main`: `prod` não tem `deploy/` nem
as migrations V20–V30.

---

## FASE 2 — Bootstrap

**OBJETIVO** Pacotes de execução (JDK 21, PostgreSQL, Nginx, Certbot), usuários
`plantahub` e `deploy`, UFW, swap, limite de logs. Sem Node e sem Maven.

**PRÉ-REQUISITOS** Fase 0. Saber a porta real do SSH (padrão 22).

**COMANDOS** — na VM:

```bash
sudo PUBLIC_IP=<NOVO_IP> \
     LETSENCRYPT_EMAIL=<seu-email> \
     DEPLOY_PUBKEY="$(cat <<'EOF'
<conteúdo de plantahub_deploy.pub>
EOF
)" \
     bash ~/plantahub-deploy/scripts/01-bootstrap-server.sh
```

Se o SSH não estiver na 22, edite `SSH_PORT` em `/etc/plantahub/deploy.conf` (o script
recusa ativar o UFW se a porta configurada não for a do `sshd`, para não trancar você fora).

**RESULTADO ESPERADO** Terminar com `Bootstrap concluído`, mostrando `java -version` e
`javac -version` 21, e `ufw status` com só SSH/80/443.

**VALIDAÇÃO**

```bash
java -version && javac -version          # os dois: 21
sudo ufw status verbose                  # 22, 80, 443 ALLOW; nada de 5432/8080
id plantahub && id deploy
cat /etc/plantahub/deploy.conf           # PUBLIC_IP e LETSENCRYPT_EMAIL preenchidos
# De OUTRA máquina (valida a chave do CI e o firewall do provedor na 22):
ssh -i ./plantahub_deploy deploy@<NOVO_IP> 'whoami'
```

**ROLLBACK** `sudo ufw disable` desfaz o firewall; pacotes podem ficar.

**ERROS COMUNS**
- `javac ausente` → instalou só JRE. O script usa `openjdk-21-jdk-headless` (tem `javac`).
- Perdeu o SSH depois do UFW → porta errada em `SSH_PORT`; entre pelo console web do
  provedor e rode `sudo ufw allow <porta>/tcp`.
- **Firewall do provedor**: o UFW não o substitui. Libere 22/80/443 lá também.

---

## FASE 3 — PostgreSQL

**OBJETIVO** Usuário `plantahub` (não superusuário) dono do banco `plantahub` e do schema
`public`, acessível só por `127.0.0.1`.

**PRÉ-REQUISITOS** Fase 2.

**COMANDOS**

```bash
# Gera uma senha forte e a mostra UMA vez (copie para a fase 4):
sudo GENERATE_PASSWORD=1 bash ~/plantahub-deploy/scripts/02-setup-postgresql.sh
# ou informe a sua (20+ caracteres [A-Za-z0-9_-]):
sudo bash ~/plantahub-deploy/scripts/02-setup-postgresql.sh
```

**Caminho A — banco novo (sem dados antigos):** nada mais. O Flyway cria tudo (V1…V30,
incluindo os 6 produtos, categorias, coleções e conteúdo) no primeiro boot.

**Caminho B — trazer os dados da VM antiga** (usuários, pedidos, direitos de download).
Faça **antes** do primeiro start do backend:

```bash
# Na VM ANTIGA:
sudo -u postgres pg_dump -Fc plantahub > plantahub-$(date +%F).dump
# Copie o arquivo para a VM nova (/tmp) e, na VM NOVA:
sudo -u postgres pg_restore --no-owner --role=plantahub -d plantahub /tmp/plantahub-*.dump
sudo bash ~/plantahub-deploy/scripts/02-setup-postgresql.sh   # ENTER vazio: corrige donos e privilégios
sudo -u postgres psql -d plantahub -tAc 'select max(version::int) from flyway_schema_history'
```

Erros `already exists` para `schema public` e `extension pgcrypto` no `pg_restore` são
esperados (o `02` já os criou). Qualquer outro erro: pare e investigue. A última consulta
mostra em que versão a VM antiga estava (a `prod` atual para em V19); o primeiro boot
aplica só as que faltam.

No caminho B, reutilize o `JWT_SECRET` antigo **ou** ponha o antigo em `APP_SECRETS_KEY`
(fase 4); senão as credenciais AWS salvas pelo painel ficam ilegíveis (salve-as de novo).

**RESULTADO ESPERADO** `CREATE em public: true` e `criar/remover tabela: ok`.

**VALIDAÇÃO**

```bash
# Como administrador (socket Unix + peer: usuário Linux postgres):
sudo -u postgres psql -c '\l plantahub'
# Como a aplicação (TCP + senha — é assim que o Spring conecta):
psql -h 127.0.0.1 -U plantahub -d plantahub -c 'select current_user'
sudo ss -tlnp | grep 5432                 # só 127.0.0.1 / ::1
```

**ROLLBACK** `sudo -u postgres psql -c 'DROP DATABASE plantahub' -c 'DROP ROLE plantahub'` e rode de novo.

**ERROS COMUNS**
- `Peer authentication failed for user "plantahub"` → rodou `psql -U plantahub` sem `-h`.
  Sem `-h` o psql usa socket Unix com autenticação *peer*. Use `-h 127.0.0.1`.
- `permission denied for schema public` → o schema não pertence ao `plantahub` (ex.:
  restore feito como `postgres`). Rode o `02` de novo: ele reatribui a posse.
- Não libere a 5432 no UFW nem no provedor, e não mude `listen_addresses`.

---

## FASE 4 — Secrets / .env

**OBJETIVO** Criar a estrutura da VM e preencher as variáveis de runtime.

**PRÉ-REQUISITOS** Fases 2 e 3 (senha do banco em mãos).

**COMANDOS**

```bash
sudo bash ~/plantahub-deploy/scripts/03-setup-directories.sh
openssl rand -base64 64 | tr -d '\n'; echo      # um JWT_SECRET novo, se for o caso
sudo nano /opt/plantahub/backend/.env
```

### Variáveis

Todas do backend são **runtime** e vivem só em `/opt/plantahub/backend/.env`.
A única do frontend é **build-time** e vive no workflow. Nenhuma vai para o git.

| Variável | Obrig. | Secret | Onde | Valor de produção |
|---|---|---|---|---|
| `SPRING_PROFILES_ACTIVE` | sim | não | backend runtime (também no unit) | `prod` |
| `SERVER_ADDRESS` | não | não | backend runtime (também no unit) | `127.0.0.1` |
| `SPRING_DATASOURCE_URL` | sim | não | backend runtime | `jdbc:postgresql://127.0.0.1:5432/plantahub` |
| `SPRING_DATASOURCE_USERNAME` | sim | não | backend runtime | `plantahub` |
| `SPRING_DATASOURCE_PASSWORD` | sim | **sim** | backend runtime | senha da fase 3 |
| `JWT_SECRET` | sim | **sim** | backend runtime | ≥ 32 bytes aleatórios |
| `APP_SECRETS_KEY` | não | **sim** | backend runtime | vazio (deriva do JWT) ou chave antiga |
| `AUTH_COOKIE_SECURE` | não | não | backend runtime | `true` |
| `APP_CORS_ALLOWED_ORIGINS` | não | não | backend runtime | `https://plantahub.com.br,https://www.plantahub.com.br` |
| `APP_WEB_BASE_URL` | não | não | backend runtime | `https://www.plantahub.com.br` (hoje não usada pelo código) |
| `APP_ADMIN_BOOTSTRAP_EMAILS` | não | não | backend runtime | e-mail(s) do dono |
| `APP_S3_BUCKET` | sim | não | backend runtime | `plantahub-assets` |
| `APP_S3_REGION` | sim | não | backend runtime | `us-east-2` |
| `APP_S3_ACCESS_KEY` / `APP_S3_SECRET_KEY` | não | **sim** | backend runtime | vazias se usar painel/role |
| `APP_S3_PUBLIC_BASE_URL` | não | não | backend runtime | vazio (ou CloudFront) |
| `APP_UPLOADS_MAX_SIZE_BYTES` | não | não | backend runtime | `2147483648` |
| `APP_UPLOADS_JANITOR_ENABLED` | não | não | backend runtime | `true` |
| `APP_DOWNLOADS_LEGACY_FALLBACK` | não | não | backend runtime | `true` (ver `docs/production.md`) |
| `ORDERS_PENDING_EXPIRATION_HOURS` / `_SCAN_MS` | não | não | backend runtime | `48` / `3600000` |
| `INFINITEPAY_HANDLE` | sim | não* | backend runtime | handle da conta |
| `INFINITEPAY_REDIRECT_URL` | sim | não | backend runtime | `https://www.plantahub.com.br/pagamento/sucesso` |
| `INFINITEPAY_WEBHOOK_URL` | sim | não | backend runtime | `https://api.plantahub.com.br/v1/webhooks/infinitepay` |
| `INFINITEPAY_API_BASE_URL` | não | não | backend runtime | `https://api.checkout.infinitepay.io` |
| `APP_PASSWORD_RESET_SECRET` | sim | **sim** | backend runtime | ≥ 32 caracteres aleatórios (HMAC dos códigos de recuperação de senha) |
| `APP_MAIL_ENABLED` | não | não | backend runtime | `true` (default no prod) |
| `APP_MAIL_FROM` | não | não | backend runtime | `PlantaHub <nao-responda@plantahub.com.br>` |
| `SPRING_MAIL_HOST` / `_PORT` | sim, com e-mail ligado | não | backend runtime | host SMTP / `587` |
| `SPRING_MAIL_USERNAME` / `_PASSWORD` | não | senha **sim** | backend runtime | credenciais SMTP |
| `APP_SMS_ENABLED` | não | não | backend runtime | `false` até ter conta Twilio |
| `TWILIO_ACCOUNT_SID` / `TWILIO_AUTH_TOKEN` | sim, com SMS ligado | token **sim** | backend runtime | console do Twilio |
| `TWILIO_FROM_NUMBER` ou `TWILIO_MESSAGING_SERVICE_SID` | um dos dois, com SMS ligado | não | backend runtime | número remetente (E.164) ou `MG...` |
| `JAVA_OPTS` | não | não | backend runtime | default no unit: `MaxRAMPercentage=45` |
| `VITE_API_URL` | sim | não | **frontend build-time** (workflow) | `https://api.plantahub.com.br` |

\* O handle não autentica nada sozinho, mas não há motivo para publicá-lo.

Formato do arquivo (é um `EnvironmentFile` do systemd): `VAR=valor`, sem `export`, e
**sem comentário no fim da linha** (vira parte do valor).

**RESULTADO ESPERADO** `.env` sem nenhum `TROQUE_*`, dono `root:plantahub`, modo `640`.

**VALIDAÇÃO**

```bash
sudo stat -c '%U:%G %a' /opt/plantahub/backend/.env         # root:plantahub 640
sudo grep -c TROQUE /opt/plantahub/backend/.env              # 0
sudo -n -u deploy sudo -n -l                                 # só plantahub-release
systemctl cat plantahub-backend | head -30
```

O `plantahub-release` também recusa ativar um backend se faltar variável obrigatória,
se o `JWT_SECRET` tiver < 32 caracteres ou se o `.env` tiver CRLF.

**ROLLBACK** O `.env` é seu; os scripts nunca o sobrescrevem.

**ERROS COMUNS** Copiar o `.env` do Windows (CRLF) → valores com `\r` no fim; o `03`
converte. `WeakKeyException` no boot → `JWT_SECRET` curto.

---

## FASE 5 — Backend

**OBJETIVO** Primeira subida do Spring Boot, com o Flyway criando/atualizando o schema.

**PRÉ-REQUISITOS** Fases 3 e 4. Um `app.jar` compilado **fora** da VM.

**COMANDOS** — escolha um jeito de obter o jar:

```bash
# Opção A — compilar na SUA máquina (Java 21 + Docker para os testes):
cd apps/api/plantahub-api && ./mvnw -B clean package
scp -P 22 target/plantahub-api-0.0.1-SNAPSHOT.jar ubuntu@<NOVO_IP>:/tmp/app.jar

# Opção B — baixar de uma execução do workflow (Actions → execução → Artifacts →
# backend-jar), descompactar e copiar o app.jar para /tmp/app.jar na VM.
```

Na VM:

```bash
sudo bash ~/plantahub-deploy/scripts/04-install-backend.sh /tmp/app.jar
```

Ou pule esta fase e deixe a [fase 13](#fase-13--primeiro-auto-deploy) instalar tudo.

**RESULTADO ESPERADO** `health check OK: {"status":"ok"}` e `backend manual-… no ar`.

**VALIDAÇÃO**

```bash
curl -s http://127.0.0.1:8080/health                    # {"status":"ok"}
sudo ss -tlnp | grep 8080                                # só 127.0.0.1:8080
journalctl -u plantahub-backend -n 50 --no-pager | grep -E 'profile is active|Successfully applied|Started'
sudo -u postgres psql -d plantahub -tAc 'select max(version::int) from flyway_schema_history'   # 30
```

**ROLLBACK** `sudo bash ~/plantahub-deploy/scripts/rollback.sh backend` (se já houver uma versão anterior).

**ERROS COMUNS**
- `No compiler is provided in this environment` → isso acontece em build com JRE; aqui
  não há build na VM. Se aparecer, alguém rodou Maven na VM.
- `permission denied for schema public` → fase 3.
- `Detected resolved migration not applied` / checksum mismatch → alguém **alterou uma
  migration já aplicada**. Nunca edite migrations antigas: crie `V31__...sql`.
- O perfil não é `prod` → `SPRING_PROFILES_ACTIVE` ausente; o unit já fixa `prod`.

---

## FASE 6 — Nginx / API

**OBJETIVO** Publicar a API na porta 80 (provisório) e preparar o webroot do ACME.

**PRÉ-REQUISITOS** Fase 5 (para o teste da API).

**COMANDOS**

```bash
sudo bash ~/plantahub-deploy/scripts/06-setup-nginx.sh
```

**RESULTADO ESPERADO** `nginx -t` OK; `API via Nginx (porta 80, Host api.plantahub.com.br): 200`.

**VALIDAÇÃO**

```bash
curl -s -H 'Host: api.plantahub.com.br' http://127.0.0.1/health          # {"status":"ok"}
curl -s -o /dev/null -w '%{http_code}\n' http://127.0.0.1/                 # 000 (444: Host desconhecido)
ls -l /etc/nginx/sites-enabled/
```

**ROLLBACK** `sudo rm /etc/nginx/sites-enabled/plantahub-* && sudo ln -s /etc/nginx/sites-available/default /etc/nginx/sites-enabled/ && sudo systemctl reload nginx`

**ERROS COMUNS** `duplicate gzip_types` → o `06` detecta e remove a linha duplicada.

---

## FASE 7 — DNS

**OBJETIVO** Os três nomes apontando para o IP **novo**, e nada mais.

**PRÉ-REQUISITOS** IP novo; acesso ao painel DNS.

**COMANDOS** — no painel DNS da zona `plantahub.com.br`:

| Nome (campo "host") | Tipo | Valor | TTL |
|---|---|---|---|
| `@` | A | `<NOVO_IP>` | 300 |
| `www` | A | `<NOVO_IP>` | 300 |
| `api` | A | `<NOVO_IP>` | 300 |

- **Apague** os registros A antigos desses nomes (não deixe dois A para o mesmo nome).
- **Apague** qualquer AAAA desses nomes, a não ser que aponte para o IPv6 desta VM (o Let's
  Encrypt prefere IPv6; um AAAA velho derruba a emissão).
- Se preferir `www` como CNAME para `plantahub.com.br`, apague o A de `www` — não pode
  haver CNAME e A no mesmo nome.
- No campo nome, digite só `www`/`api`/`@`. Digitar `plantahub.com.br` cria o registro
  errado `plantahub.com.br.plantahub.com.br` (já aconteceu). Apague-o se existir.
- Registros de e-mail (MX, TXT/SPF/DKIM) não mudam.

**RESULTADO ESPERADO** Os três nomes resolvem para `<NOVO_IP>` em qualquer resolvedor.

**VALIDAÇÃO** — da sua máquina ou da VM:

```bash
for n in plantahub.com.br www.plantahub.com.br api.plantahub.com.br; do
  for r in 1.1.1.1 8.8.8.8; do printf '%-24s @%-8s A=%s AAAA=%s\n' $n $r \
    "$(dig +short A $n @$r | tr '\n' ' ')" "$(dig +short AAAA $n @$r | tr '\n' ' ')"; done; done
dig +short plantahub.com.br.plantahub.com.br        # deve ser vazio
```

O `07-setup-ssl.sh` refaz essas checagens (inclusive nos nameservers autoritativos) e
não chama o Certbot se algo divergir.

**ROLLBACK** Volte os A para o IP antigo (a VM antiga continua funcionando se ainda existir).

**ERROS COMUNS** Propagação: com TTL alto no registro antigo, espere o TTL antigo expirar.

---

## FASE 8 — SSL da API

**OBJETIVO** Certificado de `api.plantahub.com.br` e a config HTTPS definitiva da API.

**PRÉ-REQUISITOS** Fase 7 resolvendo; 80 e 443 liberadas no **UFW e no provedor**.

**COMANDOS**

```bash
sudo bash ~/plantahub-deploy/scripts/07-setup-ssl.sh api
```

O script: (1) valida o DNS em 1.1.1.1, 8.8.8.8 e nos autoritativos, e AAAA; (2) publica
um arquivo de teste no webroot e pede que você o busque **de fora** (é o único teste
que prova o firewall do provedor); (3) roda `certbot --dry-run` e só então a emissão
real; (4) instala `plantahub-api.conf`, valida `nginx -t` (e desfaz se falhar);
(5) roda `certbot renew --dry-run`.

**RESULTADO ESPERADO** `https://api.plantahub.com.br/ → ...` e `certbot renew --dry-run aprovado`.

**VALIDAÇÃO**

```bash
curl -s https://api.plantahub.com.br/health                       # {"status":"ok"}
curl -sI http://api.plantahub.com.br/health | head -1             # 301
sudo certbot certificates
```

**ROLLBACK** `sudo install -m 644 ~/plantahub-deploy/nginx/plantahub-api.http.conf /etc/nginx/sites-available/plantahub-api && sudo systemctl reload nginx`

**ERROS COMUNS**
- `Timeout during connect (likely firewall problem)` → 80 fechada no **provedor**.
- `unauthorized` / IP errado nos logs do certbot → DNS ainda no IP antigo ou AAAA velho.
- `too many failed authorizations` → é por isso que o script faz `--dry-run` antes.

---

## FASE 9 — Frontend

**OBJETIVO** Publicar o `dist/` com permissões corretas.

**PRÉ-REQUISITOS** Fase 6. Um build feito **fora** da VM com `VITE_API_URL=https://api.plantahub.com.br`.

**COMANDOS**

```bash
# Na sua máquina:
cd apps/web/plantahub-web
npm ci && VITE_API_URL=https://api.plantahub.com.br npm run build
tar -C dist -czf frontend-dist.tar.gz .
scp frontend-dist.tar.gz ubuntu@<NOVO_IP>:/tmp/
# (ou baixe o artifact "frontend-dist" de uma execução do workflow — é um .zip)

# Na VM:
sudo bash ~/plantahub-deploy/scripts/05-install-frontend.sh /tmp/frontend-dist.tar.gz
```

**RESULTADO ESPERADO** `frontend verificado via Nginx: ... (200 html), /assets/index-*.js (200 javascript), asset inexistente 404`.

**VALIDAÇÃO**

```bash
curl -s -o /dev/null -w '%{http_code} %{content_type}\n' -H 'Host: www.plantahub.com.br' http://127.0.0.1/
JS=$(curl -s -H 'Host: www.plantahub.com.br' http://127.0.0.1/ | grep -o '/assets/[^"]*\.js' | head -1)
curl -s -o /dev/null -w '%{http_code} %{content_type}\n' -H 'Host: www.plantahub.com.br' "http://127.0.0.1$JS"  # 200 application/javascript
sudo -u www-data test -r /var/www/plantahub/frontend$JS && echo legivel
namei -m /var/www/plantahub/frontend$JS          # todos os diretórios com r-x para "others"
```

**ROLLBACK** `sudo bash ~/plantahub-deploy/scripts/rollback.sh frontend`

**ERROS COMUNS** — os três da VM anterior, e por que não se repetem:
- **Página branca + `Expected a JavaScript module script ... MIME type "text/html"`** →
  o fallback da SPA devolvia `index.html` para um `.js` inexistente. Agora `/assets/`
  (e qualquer arquivo com extensão estática) usa `try_files $uri =404`.
- **404 em `/assets/*.js`** → `rsync --delete` no diretório servido deixava o `index.html`
  novo apontar para assets ainda não enviados (ou já apagados). Agora a troca é atômica
  por symlink, e os assets da versão anterior são mantidos na nova para abas abertas.
- **`Permission denied` em `/var/www/plantahub/frontend/assets/`** → o script de release
  aplica `root:root`, diretórios `755`, arquivos `644`, e `755` em `/var/www` e
  `/var/www/plantahub` (travessia).

---

## FASE 10 — SSL do frontend

**OBJETIVO** Certificado de `plantahub.com.br` + `www` e os redirects definitivos.

**PRÉ-REQUISITOS** Fases 7 e 9.

**COMANDOS**

```bash
sudo bash ~/plantahub-deploy/scripts/07-setup-ssl.sh frontend
```

**RESULTADO ESPERADO**
`https://plantahub.com.br/ → 301 https://www.plantahub.com.br/` e `https://www.plantahub.com.br/ → 200`.

**VALIDAÇÃO**

```bash
curl -sI http://plantahub.com.br/sobre     | grep -iE '^HTTP|^location'   # 301 https://www.plantahub.com.br/sobre
curl -sI https://plantahub.com.br/         | grep -iE '^HTTP|^location'   # 301 https://www.plantahub.com.br/
curl -sI https://www.plantahub.com.br/     | head -1                      # 200
```

**ROLLBACK** `sudo install -m 644 ~/plantahub-deploy/nginx/plantahub-frontend.http.conf /etc/nginx/sites-available/plantahub-frontend && sudo systemctl reload nginx`

**ERROS COMUNS** Os mesmos da fase 8. Com HTTPS no ar, o login passa a funcionar
(o cookie é `Secure`).

---

## FASE 11 — Testes

**OBJETIVO** Verificar o ambiente inteiro de uma vez.

**PRÉ-REQUISITOS** Fases 2–10.

**COMANDOS**

```bash
sudo bash ~/plantahub-deploy/scripts/verify-deployment.sh
# de qualquer máquina, só a parte pública:
bash deploy/scripts/verify-deployment.sh --public-only
```

Verifica: disco, memória, JDK, UFW (22/80/443 abertas; 5432/8080 não), PostgreSQL só em
loopback, Flyway (sem falhas, banco na mesma versão que a última migration do jar),
`.env` (dono, modo, placeholders), serviço ativo e habilitado, processo com perfil `prod`
rodando como `plantahub`, 8080 só em 127.0.0.1, health local, Nginx (`-t`, sites em
HTTPS), permissões e travessia do frontend, e pela Internet: health HTTPS, redirect
http→https, preflight CORS, index `text/html`, asset JS `200` + MIME JavaScript,
asset inexistente `404`, rota SPA, três redirects para `https://www.`, validade dos
três certificados.

**RESULTADO ESPERADO** `Resultado: N ok · 1 avisos · 0 falhas` (o aviso fixo lembra de
conferir o firewall do provedor).

**Teste funcional manual** (não automatizável sem credenciais): cadastrar conta, logar,
ver catálogo com imagens da S3, adicionar ao carrinho, e no painel admin
`Armazenamento → diagnóstico` (valida CORS/lifecycle/permissões do bucket).

**ROLLBACK** —

**ERROS COMUNS** Preflight CORS sem `Access-Control-Allow-Origin` → `APP_CORS_ALLOWED_ORIGINS`.

---

## FASE 12 — GitHub Actions

**OBJETIVO** Permitir que o workflow entre na VM como `deploy`.

**PRÉ-REQUISITOS** Fase 2 (chave pública em `~deploy/.ssh/authorized_keys`).

**COMANDOS** — GitHub → Settings → Secrets and variables → Actions → **Secrets**:

| Secret | Obrigatório | Valor |
|---|---|---|
| `VM_HOST` | sim | `<NOVO_IP>` (ou `api.plantahub.com.br` depois do DNS) |
| `VM_USER` | sim | `deploy` |
| `VM_PORT` | não (padrão 22) | porta SSH |
| `VM_SSH_KEY` | sim | conteúdo **inteiro** de `plantahub_deploy` (chave privada) |
| `VM_KNOWN_HOSTS` | recomendado | saída de `ssh-keyscan -p 22 <NOVO_IP>` |

Para `VM_KNOWN_HOSTS`, confira a impressão digital antes de confiar:
`ssh-keyscan -p 22 <NOVO_IP> | ssh-keygen -lf -` na sua máquina deve bater com
`ssh-keygen -lf /etc/ssh/ssh_host_ed25519_key.pub` rodado **na VM**.

**Variables** (opcionais, não secretas): `VITE_API_URL`, `PUBLIC_API_URL`, `PUBLIC_WEB_URL`
— os defaults do workflow já são os domínios de produção.

**Não** cadastre no GitHub: senha do banco, `JWT_SECRET`, credenciais AWS, InfinitePay.
Esses são **runtime** e ficam só no `.env` da VM. O workflow não precisa deles: não roda
a aplicação e os testes usam valores próprios (`application-test.yml`).

O job `deploy` usa o environment `production` (criado automaticamente). Em Settings →
Environments você pode exigir aprovação manual e restringir à branch `prod`.

**RESULTADO ESPERADO** Secrets cadastrados.

**VALIDAÇÃO** Da sua máquina: `ssh -i ./plantahub_deploy deploy@<NOVO_IP> 'sudo -n /usr/local/sbin/plantahub-release status'`.

**ROLLBACK** Apague os secrets; remova a linha de `~deploy/.ssh/authorized_keys`.

**ERROS COMUNS** Chave colada sem a linha final `-----END OPENSSH PRIVATE KEY-----`.
`Host key verification failed` → `VM_KNOWN_HOSTS` de outro IP (VM antiga).

---

## FASE 13 — Primeiro auto-deploy

**OBJETIVO** Levar `main` para `prod` e ver o pipeline implantar sozinho.

**PRÉ-REQUISITOS** Fases 2–4 e 12 no mínimo (o deploy instala backend e frontend
sozinho; para a verificação pela Internet passar, também 6–10).

**COMANDOS**

```bash
git checkout prod && git pull
git merge --no-ff main          # traz deploy/, workflow novo e migrations V20–V30
git push origin prod            # dispara "Deploy PlantaHUB Production"
```

Antes de DNS/SSL existirem, rode pelo botão *Run workflow* com
`skip_public_checks = true`.

**RESULTADO ESPERADO** Jobs `backend`, `frontend` e `deploy` verdes; no log de *Ativar
release*: `pg_dump`, `health check OK`, `frontend verificado via Nginx`, `ativada com sucesso`;
em *Verificar pela Internet*: `Deploy <id> verificado pela Internet.`

**VALIDAÇÃO** `sudo /usr/local/sbin/plantahub-release status` na VM mostra a release nova
em `current`; `cat /opt/plantahub/backend/current/RELEASE` mostra o commit.

**ROLLBACK** O próprio `plantahub-release` volta a versão anterior quando o health check
ou a verificação do frontend falham. Manual: fase 14.

**ERROS COMUNS**
- `sudo: a password is required` → sudoers não instalado (rode `03`) ou `VM_USER` ≠ `deploy`.
- `variáveis obrigatórias vazias` → `.env` incompleto; nada foi trocado.
- Na **primeira** execução, as migrations V20–V30 serão aplicadas (se o banco veio da VM
  antiga, que parou em V19). O `pg_dump` pré-deploy fica em `/var/backups/plantahub/db/`.

---

## FASE 14 — Rollback

**OBJETIVO** Voltar à versão anterior.

**COMANDOS**

```bash
sudo bash ~/plantahub-deploy/scripts/rollback.sh backend     # ou frontend, ou all
sudo bash ~/plantahub-deploy/scripts/rollback.sh status
# pelo usuário deploy (ex.: da sua máquina):
ssh deploy@<IP> 'sudo -n /usr/local/sbin/plantahub-release rollback all'
```

O rollback troca `current` ↔ `previous`; rodar de novo volta para onde estava. Releases
mais antigas continuam em `releases/` (5 por padrão) — para ir a uma delas:
`sudo ln -sfn releases/<id> /opt/plantahub/backend/current && sudo systemctl restart plantahub-backend`.

**O banco não volta sozinho.** As migrations do Flyway só andam para frente. Um jar antigo
funciona com um schema mais novo enquanto a mudança for aditiva (o Flyway ignora versões
futuras; o Hibernate `validate` só confere o que o jar conhece). Se a release removeu ou
renomeou colunas, restaure o dump tirado antes do deploy:

```bash
sudo systemctl stop plantahub-backend
ls -lt /var/backups/plantahub/db/                        # escolha o dump "antes-de-<release>"
sudo -u postgres psql -c 'DROP DATABASE plantahub'
sudo bash ~/plantahub-deploy/scripts/02-setup-postgresql.sh    # recria vazio (ENTER mantém a senha)
sudo -u postgres pg_restore --no-owner --role=plantahub -d plantahub /var/backups/plantahub/db/<arquivo>.dump
sudo bash ~/plantahub-deploy/scripts/02-setup-postgresql.sh    # corrige donos
sudo bash ~/plantahub-deploy/scripts/rollback.sh backend       # jar compatível com o dump
```

Restaurar um dump **descarta** pedidos/cadastros feitos depois dele.

**ERROS COMUNS** `não há release anterior` → primeira instalação; reinstale um jar com `04`.

---

## FASE 15 — Operação e manutenção

| Tarefa | Comando |
|---|---|
| Logs do backend | `journalctl -u plantahub-backend -f` |
| Logs do Nginx | `sudo tail -f /var/log/nginx/plantahub-{api,frontend}.{access,error}.log` |
| Reiniciar backend | `sudo systemctl restart plantahub-backend` |
| Watchdog (auto-restart) | ativo por padrão: `/health` a cada minuto; ~3 min sem resposta → restart (máx. 1 a cada 10 min, nunca durante deploy). Logs: `journalctl -u plantahub-watchdog` |
| Pausar o watchdog (manutenção) | `sudo touch /etc/plantahub/watchdog.disabled` · reativar: `sudo rm /etc/plantahub/watchdog.disabled` |
| Parar o backend de propósito | pause o watchdog **antes** de `systemctl stop`, senão ele religa em ~3 min |
| Estado das releases | `sudo /usr/local/sbin/plantahub-release status` |
| Verificação completa | `sudo bash ~/plantahub-deploy/scripts/verify-deployment.sh` |
| Mudar uma variável | `sudo nano /opt/plantahub/backend/.env && sudo systemctl restart plantahub-backend` |
| Tornar alguém admin | incluir o e-mail em `APP_ADMIN_BOOTSTRAP_EMAILS` e reiniciar (a pessoa precisa já ter conta) |
| Certificados | renovação automática por `certbot.timer`; teste: `sudo certbot renew --dry-run` |
| Backup manual | `sudo -u postgres pg_dump -Fc plantahub > plantahub-$(date +%F).dump` |
| Atualizar o sistema | `sudo apt update && sudo apt upgrade` (reinicie o backend se o JDK mudar) |
| Espaço em disco | `df -h /` · `sudo du -sh /var/backups/plantahub /opt/plantahub /var/log` |

**Atualizar a própria ferramenta de deploy.** O CI não altera nada fora de
`releases/`. Depois de mudar arquivos em `deploy/`, copie a pasta de novo para a VM e
rode o script correspondente (`03` para `plantahub-release`/unit/sudoers, `06` para Nginx).

**Migrations.** Nunca edite uma migration já aplicada (o checksum muda e o boot falha em
todas as bases). Correções são sempre uma nova `V<n+1>__descricao.sql`. O teste
`MigrationNamingTest` impede nomes fora do padrão e a reintrodução da V12.

**Backups fora da VM.** Os dumps ficam na própria VM: se ela for perdida, eles também.
Copie-os periodicamente para fora (ex.: um bucket S3 separado) — isto **não** está
automatizado.

---

## Checklist da VM nova

```
Fase 0
[ ] VM Ubuntu 24.04 criada; IP público anotado: ______________
[ ] Firewall do PROVEDOR liberou 22
[ ] Firewall do PROVEDOR liberou 80
[ ] Firewall do PROVEDOR liberou 443
[ ] Firewall do provedor NÃO libera 5432 nem 8080
[ ] Chave plantahub_deploy gerada; pasta deploy/ copiada para a VM
Fases 2–5
[ ] 01-bootstrap: java/javac 21; UFW só 22/80/443; usuários plantahub e deploy
[ ] 02-postgresql: "CREATE em public: true"; senha guardada
[ ] (se houver dados) dump da VM antiga restaurado ANTES do primeiro boot
[ ] 03-directories + .env preenchido (sem TROQUE_*), JWT_SECRET decidido (novo ou antigo)
[ ] backend no ar: curl 127.0.0.1:8080/health → {"status":"ok"}
Fases 6–10
[ ] 06-nginx: nginx -t OK
[ ] DNS: @, www e api → NOVO IP; A antigos e AAAA removidos; sem plantahub.com.br.plantahub.com.br
[ ] 07-ssl api: certificado emitido; renew --dry-run OK
[ ] frontend instalado; asset JS 200 application/javascript
[ ] 07-ssl frontend: raiz e http redirecionam para https://www.plantahub.com.br
Fases 11–13
[ ] verify-deployment.sh: 0 falhas
[ ] Secrets: VM_HOST, VM_USER=deploy, VM_PORT, VM_SSH_KEY, VM_KNOWN_HOSTS
[ ] main mergeada em prod; workflow verde; "verificado pela Internet"
Pós-migração
[ ] InfinitePay aponta o webhook para https://api.plantahub.com.br/v1/webhooks/infinitepay
[ ] CORS do bucket S3 inclui https://www.plantahub.com.br e https://plantahub.com.br (docs/production.md)
[ ] Diagnóstico de armazenamento do painel admin sem erros
[ ] VM antiga desligada só depois de alguns dias estável (e com o último dump guardado)
```
