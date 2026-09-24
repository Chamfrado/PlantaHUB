package com.plantahub.api.domain.site;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * Contatos e redes sociais do site, num lugar so.
 *
 * <p>Existe por causa de um defeito concreto: estes dados estavam escritos em pelo menos
 * tres componentes e <b>divergiam entre si</b>. O rodape apontava para um perfil do
 * Instagram e a pagina de Contato para outro; o Facebook tambem discordava; e o link de
 * WhatsApp era {@code wa.me/5500000000000}, um numero de exemplo, no ar.
 *
 * <p>Dado repetido em tres lugares nao fica igual — fica igual ate alguem mudar um deles.
 * Com uma fonte unica, corrigir passa a ser uma edicao, e a divergencia deixa de estar
 * disponivel.
 */
public record SiteSettings(

        @Email @Size(max = 200) String email,

        /** Caixa de parcerias, usada na pagina Trabalhe Conosco. */
        @Email @Size(max = 200) String partnershipsEmail,

        @Size(max = 40) String phone,

        /**
         * Numero do WhatsApp em formato internacional, so digitos — {@code 5511988887777}.
         *
         * <p>Guardado cru, e nao como URL: o link e montado por quem renderiza, entao mudar
         * de {@code wa.me} para outro formato nao exige reescrever o dado.
         */
        @Size(max = 20) String whatsapp,

        @Size(max = 300) String address,
        @Size(max = 200) String businessHours,

        @Size(max = 500) String instagramUrl,
        @Size(max = 500) String facebookUrl,
        @Size(max = 500) String linkedinUrl,
        @Size(max = 500) String youtubeUrl
) {

    public static SiteSettings empty() {
        return new SiteSettings(null, null, null, null, null, null, null, null, null, null);
    }
}
