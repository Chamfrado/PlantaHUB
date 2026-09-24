-- Paginas institucionais e dados de contato passam a ser conteudo editavel.
--
-- Ate aqui, o texto de Sobre, Contato, FAQ, Trabalhe Conosco, Termos e Privacidade vivia
-- dentro do JSX: trocar uma frase exigia desenvolvedor, commit e deploy.
--
-- As linhas nascem com conteudo vazio; a V34 carrega o texto que hoje esta no codigo.
-- Separar as duas facilita ler o que e estrutura e o que e o bootstrap editorial.

CREATE TABLE site_page (
    slug       VARCHAR(80) PRIMARY KEY,
    title      VARCHAR(200) NOT NULL,
    -- Mesmo formato para todas: cabecalho, secoes de prosa com listas, perguntas e
    -- respostas, e chamada final. O layout continua sendo de cada pagina.
    content    JSONB        NOT NULL DEFAULT '{}'::jsonb,
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_by VARCHAR(255)
);

COMMENT ON TABLE site_page IS
    'Texto das paginas institucionais. O conjunto de paginas e fixo: cada slug tem um componente proprio no frontend.';

-- As seis paginas que existem hoje. Nao ha criacao pelo painel de proposito: cada slug
-- corresponde a um componente com layout proprio, e uma linha sem componente renderizaria
-- em lugar nenhum.
INSERT INTO site_page (slug, title) VALUES
    ('sobre',            'Sobre'),
    ('contato',          'Contato'),
    ('faq',              'Perguntas frequentes'),
    ('trabalhe-conosco', 'Trabalhe conosco'),
    ('termos',           'Termos de uso'),
    ('privacidade',      'Politica de privacidade');

-- Contatos e redes num lugar so.
--
-- Motivo concreto: estes dados estavam escritos em tres componentes e divergiam. O rodape
-- apontava para um perfil do Instagram e a pagina de Contato para outro, o Facebook
-- tambem discordava, e o link de WhatsApp era um numero de exemplo -- no ar.
--
-- Linha unica, travada pelo CHECK: "as configuracoes do site" e um singular, e permitir
-- duas linhas criaria a duvida de qual delas vale.
CREATE TABLE site_settings (
    id         SMALLINT     PRIMARY KEY DEFAULT 1 CHECK (id = 1),
    content    JSONB        NOT NULL DEFAULT '{}'::jsonb,
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_by VARCHAR(255)
);

INSERT INTO site_settings (id) VALUES (1);
