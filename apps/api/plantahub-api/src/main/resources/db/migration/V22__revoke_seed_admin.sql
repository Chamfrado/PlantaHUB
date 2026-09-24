-- =========================================================================
-- V22 - Tira o ADMIN cravado em migration
--
-- A V8 promoveu 'test@plantahub.com' a ADMIN por email fixo. Era o unico
-- caminho para ADMIN no sistema inteiro. Agora o acesso vem de
-- APP_ADMIN_BOOTSTRAP_EMAILS, que e configuracao e nao codigo versionado.
--
-- O rebaixamento e condicional de proposito: se essa conta tiver pedidos, ela
-- e de uma pessoa de verdade e mexer no papel dela as cegas seria destrutivo.
-- =========================================================================

UPDATE app_user
SET role = 'USER'
WHERE email = 'test@plantahub.com'
  AND role = 'ADMIN'
  AND NOT EXISTS (
      SELECT 1 FROM orders o WHERE o.user_id = app_user.id
  );
