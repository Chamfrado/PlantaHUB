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
APP_S3_BUCKET=...
APP_S3_REGION=...
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
