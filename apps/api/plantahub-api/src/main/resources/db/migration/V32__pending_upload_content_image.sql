-- Imagem avulsa usada dentro do conteudo da pagina (ex.: foto de quem deu um depoimento).
-- Vai para o prefixo publico como MEDIA, mas nao cria linha em product_media: a URL fica
-- gravada no proprio JSON do conteudo.
ALTER TABLE pending_upload DROP CONSTRAINT ck_pending_upload_kind;
ALTER TABLE pending_upload
  ADD CONSTRAINT ck_pending_upload_kind CHECK (target_kind IN ('ASSET', 'MEDIA', 'CONTENT_IMAGE'));
