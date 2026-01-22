// src/data/products.ts

import type { Product } from "../types/ProductData";

/**
 * NOTE
 * - Casa Confort: baseado no conteúdo da página que já tínhamos.
 * - Casa Prime/Diamond + Chalés (Confort/Prime/Diamond): mock data criado.
 * - Quando o backend estiver pronto, você troca somente este arquivo (ou passa a consumir API).
 */

// -------------------------
// CASAS
// -------------------------

export const CASA_CONFORT: Product = {
  id: "casa-confort-80m2",
  slug: "confort",
  category: "casas",

  name: "Confort",
  shortDescription: "Kitnet Moderna",

  heroImageUrl: "../assets/CASA-CONFORT.png",

  areaM2: 80,
  fileFormats: ["BIM", "DWG", "PDF"],
  delivery: "Download digital imediato na confirmação da compra.",
  customizable: true,

  page: {
    headline: "Confort - 80 m2",
    subheadline: "Kitnet Moderna",
    description:
      "Transforme sua experiência de construir uma casa com um projeto arquitetônico maravilhoso.",

    whyChooseTitle: "Por Quê escolher a Casa Confort?",
    whyChooseIntro: "Detalhes minuciosamente desenvolvidos para uma habitação moderna.",
    whyChooseFeatures: [
      {
        title: "Design Compreensivo",
        description:
          "Inclui todas as plantas essenciais: Arquitetônica, Hidráulica, Elétrica, Estrutural e Paisagística. Soluções ecológicas garantindo soluções sustentáveis. Lista de materiais detalhada e manual de construção.",
      },
      {
        title: "Acesso Imediato",
        description:
          "Download digital imediato na confirmação da compra. Arquivos em formatos BIM, DWG e PDF para uso versátil.",
      },
      {
        title: "Extremamente customizavel",
      },
    ],

    includesTitle: "O que está incluso na sua compra?",
    includesIntro: "Fique pronto para construir com um kit completo de plantas.",
    includedItems: [
      { title: "Planta Arquitetônica", description: "Layout detalhado das dimensões dos cômodos." },
      { title: "Plata Hidráulica", description: "Planta otimizada para sistemas de agua e drenagem." },
      { title: "Planta Elétrica", description: "Layouts compreensivos de cabeamento para serviços eficientes." },
      { title: "Planta Estrutural", description: "Fundações sólidas assegurando segurança e durabilidade." },
      { title: "Planta Paisagística", description: "Lindas ideias de paisagismo para complementar sua casa." },
      { title: "Soluções Ecológicas", description: "Abordagens inovadoras para uma vida sustentável." },
    ],

    keyFactsTitle: "Fatos Chave sobre a Casa Confort",
    keyFactsIntro: "Descubra o essencial da sua nova casa.",
    keyFacts: [
      { value: "80", label: "metros quadrados de espaço habitavel" },
      { value: "5+", label: "Plantas diferentes inclusas" },
    ],

    testimonialsTitle: "Escute nossos clientes felizes",
    testimonialsIntro: "Veja o que os outros falam sobre nossa casa Confort.",
    testimonials: [
      { quote: "“Serviço rápido e prático, além do design incrivel! Recomendo demais!”", authorName: "James Foster" },
      { quote: "“As plantas são tão faceis, nossos pedreiros amaram elas!”", authorName: "Dr. Aisha Patel" },
      { quote: "“Casa Confort me deu um design incrível! É funcional e ao mesmo tempo estilosa!” .", authorName: "Marcus Rodriguez" },
      { quote: "“Serviço rápido e prático, além do design incrivel! Recomendo demais!”", authorName: "James Foster" },
    ],

    faqTitle: "Perguntas Frequentes",
    faqIntro: "Encontre aqui suas respostas.",
    faq: [
      { question: "Quais formatos de arquivo eu consigo com a minha compra?" },
      { question: "Em quanto tempo posso fazer download das minhas plantas?" },
      { question: "Posso modificar minhas plantas?" },
      { question: "Qual o suporte oferecido depois da compra?" },
    ],

    finalCtaTitle: "Pronto para construir a casa dos seus sonhos?",
    finalCtaSubtitle: "Compre sua planta agora e comece sua jornada.",
  },

  tags: ["casas", "confort", "80m2", "kitnet", "moderna"],
};

export const CASA_PRIME: Product = {
  id: "casa-prime-150m2",
  slug: "prime",
  category: "casas",

  name: "Prime",
  shortDescription: "Casa Moderna Familiar",

    heroImageUrl: "../assets/CASA-PRIME.png",

  areaM2: 150,
  fileFormats: ["BIM", "DWG", "PDF"],
  delivery: "Download digital imediato na confirmação da compra.",
  customizable: true,

  page: {
    headline: "Prime - 150 m2",
    subheadline: "Casa Moderna Familiar",
    description:
      "Um projeto completo com espaços amplos, integração inteligente e estética contemporânea para vida em família.",

    whyChooseTitle: "Por Quê escolher a Casa Prime?",
    whyChooseIntro: "Equilíbrio entre conforto, funcionalidade e um visual moderno.",
    whyChooseFeatures: [
      {
        title: "Ambientes amplos e integrados",
        description: "Conceito aberto para sala/cozinha e melhor aproveitamento do espaço.",
      },
      {
        title: "Projeto completo e compatível",
        description: "Pacote de plantas essenciais para obra bem planejada e execução segura.",
      },
      {
        title: "Acesso imediato + formatos versáteis",
        description: "Entrega digital imediata em BIM, DWG e PDF para uso profissional.",
      },
    ],

    includesTitle: "O que está incluso na sua compra?",
    includesIntro: "Documentação pronta para você orçar, aprovar e construir com clareza.",
    includedItems: [
      { title: "Planta Arquitetônica", description: "Layout detalhado com dimensões e organização dos ambientes." },
      { title: "Planta Hidráulica", description: "Pontos de água, esgoto e drenagem otimizados." },
      { title: "Planta Elétrica", description: "Distribuição de circuitos, tomadas e iluminação." },
      { title: "Planta Estrutural", description: "Referência estrutural para segurança e durabilidade." },
      { title: "Planta Paisagística", description: "Sugestões para área externa e valorização do imóvel." },
      { title: "Lista de Materiais + Guia", description: "Base para orçamento e orientações gerais de execução." },
    ],

    keyFactsTitle: "Fatos Chave sobre a Casa Prime",
    keyFactsIntro: "Um resumo rápido do que você leva.",
    keyFacts: [
      { value: "150", label: "metros quadrados de espaço habitavel" },
      { value: "BIM/DWG/PDF", label: "formatos inclusos" },
      { value: "5+", label: "itens e plantas inclusos" },
    ],

    testimonialsTitle: "Escute nossos clientes",
    testimonialsIntro: "Depoimentos exemplo (mock) até termos depoimentos reais.",
    testimonials: [
      { quote: "“O projeto veio muito organizado e facilitou o orçamento.”", authorName: "Cliente Prime" },
      { quote: "“A integração dos ambientes ficou excelente para a família.”", authorName: "Cliente Prime" },
    ],

    faqTitle: "Perguntas Frequentes",
    faqIntro: "Dúvidas comuns antes de comprar.",
    faq: [
      { question: "Quais formatos de arquivo eu recebo?" },
      { question: "Posso adaptar a planta para meu terreno?" },
      { question: "Em quanto tempo recebo o acesso?" },
      { question: "Qual o suporte após a compra?" },
    ],

    finalCtaTitle: "Pronto para evoluir seu projeto?",
    finalCtaSubtitle: "Compre a Prime e construa com segurança.",
  },

  tags: ["casas", "prime", "150m2", "moderna", "familiar"],
};

export const CASA_DIAMOND: Product = {
  id: "casa-diamond-300m2",
  slug: "diamond",
  category: "casas",

  name: "Diamond",
  shortDescription: "Casa Premium de Alto Padrão",

  heroImageUrl: "../assets/CASA-DIAMOND.png",

  areaM2: 300,
  fileFormats: ["BIM", "DWG", "PDF"],
  delivery: "Download digital imediato na confirmação da compra.",
  customizable: true,

  page: {
    headline: "Diamond - 300 m2",
    subheadline: "Casa Premium de Alto Padrão",
    description:
      "Um projeto premium com presença arquitetônica forte, alto nível de detalhamento e base perfeita para personalizações.",

    whyChooseTitle: "Por Quê escolher a Casa Diamond?",
    whyChooseIntro: "Pensada para quem quer alto padrão, estética marcante e documentação completa.",
    whyChooseFeatures: [
      { title: "Arquitetura premium", description: "Linhas contemporâneas e proposta sofisticada." },
      { title: "Detalhamento para execução", description: "Estrutura de documentação para uma obra mais previsível." },
      { title: "Personalização facilitada", description: "Base robusta para adaptar acabamentos e layout." },
    ],

    includesTitle: "O que está incluso na sua compra?",
    includesIntro: "Um kit completo para você ter clareza e controle na construção.",
    includedItems: [
      { title: "Planta Arquitetônica", description: "Distribuição completa e cotas de referência." },
      { title: "Planta Hidráulica", description: "Pontos e prumadas com foco em eficiência." },
      { title: "Planta Elétrica", description: "Pontos elétricos e iluminação com organização." },
      { title: "Planta Estrutural", description: "Base estrutural para segurança e durabilidade." },
      { title: "Planta Paisagística", description: "Ideias para área externa e valorização estética." },
      { title: "Memorial + Lista de Materiais", description: "Apoio para orçamento, execução e alinhamento técnico." },
    ],

    keyFactsTitle: "Fatos Chave sobre a Casa Diamond",
    keyFactsIntro: "O essencial do pacote premium.",
    keyFacts: [
      { value: "300", label: "metros quadrados de espaço habitavel" },
      { value: "BIM/DWG/PDF", label: "formatos inclusos" },
      { value: "Premium", label: "projeto alto padrão" },
    ],

    testimonialsTitle: "O que dizem (mock)",
    testimonialsIntro: "Exemplos até termos avaliações reais.",
    testimonials: [
      { quote: "“Material muito completo, ajudou a alinhar toda a obra.”", authorName: "Cliente Diamond" },
      { quote: "“A base é excelente para personalizar sem dor de cabeça.”", authorName: "Cliente Diamond" },
    ],

    faqTitle: "Perguntas Frequentes",
    faqIntro: "Respostas rápidas.",
    faq: [
      { question: "Posso personalizar layout e fachada?" },
      { question: "O projeto serve para aprovação?" },
      { question: "Recebo os arquivos imediatamente?" },
      { question: "Vocês dão suporte técnico?" },
    ],

    finalCtaTitle: "Pronto para construir algo extraordinário?",
    finalCtaSubtitle: "Compre a Diamond e leve seu projeto ao próximo nível.",
  },

  tags: ["casas", "diamond", "300m2", "premium", "alto-padrao"],
};

// -------------------------
// CHALÉS (mesmas classes: confort / prime / diamond)
// -------------------------

export const CHALE_CONFORT: Product = {
  id: "chale-confort-55m2",
  slug: "confort",
  category: "chales",

  name: "Confort",
  shortDescription: "Chalé Compacto Aconchegante",

  heroImageUrl: "../assets/CHALE-CONFORT.png",


  areaM2: 55,
  fileFormats: ["BIM", "DWG", "PDF"],
  delivery: "Download digital imediato na confirmação da compra.",
  customizable: true,

  page: {
    headline: "Confort - 55 m2",
    subheadline: "Chalé Compacto Aconchegante",
    description:
      "Um chalé compacto, aconchegante e perfeito para sítio, serra ou projeto de hospedagem com construção otimizada.",

    whyChooseTitle: "Por Quê escolher o Chalé Confort?",
    whyChooseIntro: "Foco em custo-benefício, simplicidade e beleza.",
    whyChooseFeatures: [
      { title: "Compacto e funcional", description: "Ótimo aproveitamento interno, com circulação inteligente." },
      { title: "Pronto para construir", description: "Documentação base para execução com menos retrabalho." },
      { title: "Ideal para locação", description: "Perfeito para chalé de final de semana e Airbnb." },
    ],

    includesTitle: "O que está incluso na sua compra?",
    includesIntro: "Kit de plantas para tirar o chalé do papel com segurança.",
    includedItems: [
      { title: "Planta Arquitetônica", description: "Layout, medidas e organização dos ambientes." },
      { title: "Planta Hidráulica", description: "Pontos de água, esgoto e drenagem." },
      { title: "Planta Elétrica", description: "Tomadas, iluminação e circuitos com organização." },
      { title: "Planta Estrutural", description: "Base estrutural para segurança e durabilidade." },
      { title: "Lista de Materiais", description: "Referência para orçamento e compras." },
      { title: "Guia de Ajustes", description: "Orientações de personalização e adaptação." },
    ],

    keyFactsTitle: "Fatos Chave sobre o Chalé Confort",
    keyFactsIntro: "O essencial em números.",
    keyFacts: [
      { value: "55", label: "metros quadrados de área útil" },
      { value: "BIM/DWG/PDF", label: "formatos inclusos" },
    ],

    testimonialsTitle: "O que dizem (mock)",
    testimonialsIntro: "Exemplos até termos avaliações reais.",
    testimonials: [
      { quote: "“Projeto prático e bonito, deu pra orçar rápido.”", authorName: "Cliente Chalé" },
      { quote: "“Ótimo para hospedagem, bem distribuído.”", authorName: "Cliente Chalé" },
    ],

    faqTitle: "Perguntas Frequentes",
    faqIntro: "Dúvidas comuns.",
    faq: [
      { question: "Posso adaptar para terreno inclinado?" },
      { question: "Posso aumentar a área do chalé?" },
      { question: "Como recebo os arquivos?" },
      { question: "O projeto é customizável?" },
    ],

    finalCtaTitle: "Pronto para construir seu chalé?",
    finalCtaSubtitle: "Compre o projeto e comece hoje mesmo.",
  },

  tags: ["chales", "confort", "55m2", "compacto", "airbnb"],
};

export const CHALE_PRIME: Product = {
  id: "chale-prime-85m2",
  slug: "prime",
  category: "chales",

  name: "Prime",
  shortDescription: "Chalé Familiar com Varanda",

    heroImageUrl: "../assets/CHALE-PRIME.png",

  areaM2: 85,
  fileFormats: ["BIM", "DWG", "PDF"],
  delivery: "Download digital imediato na confirmação da compra.",
  customizable: true,

  page: {
    headline: "Prime - 85 m2",
    subheadline: "Chalé Familiar com Varanda",
    description:
      "Mais espaço, varanda valorizada e integração sala/cozinha para um chalé confortável e com alto potencial de locação.",

    whyChooseTitle: "Por Quê escolher o Chalé Prime?",
    whyChooseIntro: "O equilíbrio perfeito entre conforto, estética e praticidade.",
    whyChooseFeatures: [
      { title: "Varanda valorizada", description: "Aumenta conforto e agrega valor ao imóvel." },
      { title: "Planta bem distribuída", description: "Separação clara entre social e íntimo." },
      { title: "Base completa", description: "Documentação para obra com menos incertezas." },
    ],

    includesTitle: "O que está incluso na sua compra?",
    includesIntro: "Documentação completa para construir com confiança.",
    includedItems: [
      { title: "Planta Arquitetônica", description: "Layout detalhado e dimensões." },
      { title: "Planta Hidráulica", description: "Distribuição de pontos e drenagem." },
      { title: "Planta Elétrica", description: "Iluminação e pontos elétricos." },
      { title: "Planta Estrutural", description: "Referência estrutural para execução." },
      { title: "Planta Paisagística", description: "Ideias para compor área externa." },
      { title: "Lista de Materiais + Guia", description: "Apoio no orçamento e execução." },
    ],

    keyFactsTitle: "Fatos Chave sobre o Chalé Prime",
    keyFactsIntro: "Resumo do pacote.",
    keyFacts: [
      { value: "85", label: "metros quadrados de área útil" },
      { value: "5+", label: "itens e plantas inclusos" },
    ],

    testimonialsTitle: "O que dizem (mock)",
    testimonialsIntro: "Exemplos até termos avaliações reais.",
    testimonials: [
      { quote: "“A varanda fez toda a diferença no projeto.”", authorName: "Cliente Chalé Prime" },
      { quote: "“Documentação bem feita e fácil de repassar para a equipe.”", authorName: "Cliente Chalé Prime" },
    ],

    faqTitle: "Perguntas Frequentes",
    faqIntro: "Respostas rápidas.",
    faq: [
      { question: "Posso espelhar a planta (inverter lados)?" },
      { question: "Posso adaptar para mais quartos?" },
      { question: "Como funciona o suporte pós-compra?" },
      { question: "Recebo imediatamente?" },
    ],

    finalCtaTitle: "Quer um chalé completo e pronto para obra?",
    finalCtaSubtitle: "Compre o Prime e avance com segurança.",
  },

  tags: ["chales", "prime", "85m2", "varanda", "familia"],
};

export const CHALE_DIAMOND: Product = {
  id: "chale-diamond-120m2",
  slug: "diamond",
  category: "chales",

  name: "Diamond",
  shortDescription: "Chalé Premium para Locação de Alto Padrão",

  heroImageUrl: "../assets/CHALE-DIAMOND.png",

  areaM2: 120,
  fileFormats: ["BIM", "DWG", "PDF"],
  delivery: "Download digital imediato na confirmação da compra.",
  customizable: true,

  page: {
    headline: "Diamond - 120 m2",
    subheadline: "Chalé Premium de Alto Padrão",
    description:
      "Um chalé premium com presença arquitetônica forte, ideal para experiências exclusivas e locação de alto valor agregado.",

    whyChooseTitle: "Por Quê escolher o Chalé Diamond?",
    whyChooseIntro: "Pensado para alto padrão, estética marcante e personalização.",
    whyChooseFeatures: [
      { title: "Arquitetura marcante", description: "Proposta premium para se destacar na locação." },
      { title: "Experiência valorizada", description: "Ambientes pensados para conforto e percepção de luxo." },
      { title: "Base robusta para customização", description: "Adapte acabamentos e layout com facilidade." },
    ],

    includesTitle: "O que está incluso na sua compra?",
    includesIntro: "Kit completo para executar com previsibilidade e qualidade.",
    includedItems: [
      { title: "Planta Arquitetônica", description: "Distribuição completa e cotas de referência." },
      { title: "Planta Hidráulica", description: "Pontos e prumadas com foco em eficiência." },
      { title: "Planta Elétrica", description: "Iluminação, tomadas e circuitos organizados." },
      { title: "Planta Estrutural", description: "Base estrutural para segurança e durabilidade." },
      { title: "Planta Paisagística", description: "Ideias para área externa premium." },
      { title: "Memorial + Lista de Materiais", description: "Apoio de orçamento e execução." },
    ],

    keyFactsTitle: "Fatos Chave sobre o Chalé Diamond",
    keyFactsIntro: "O essencial do premium.",
    keyFacts: [
      { value: "120", label: "metros quadrados de área útil" },
      { value: "Premium", label: "foco em alto padrão" },
      { value: "BIM/DWG/PDF", label: "formatos inclusos" },
    ],

    testimonialsTitle: "O que dizem (mock)",
    testimonialsIntro: "Exemplos até termos avaliações reais.",
    testimonials: [
      { quote: "“Projeto perfeito para locação premium, muito bem pensado.”", authorName: "Cliente Chalé Diamond" },
      { quote: "“Base excelente para personalizar e executar sem surpresas.”", authorName: "Cliente Chalé Diamond" },
    ],

    faqTitle: "Perguntas Frequentes",
    faqIntro: "Dúvidas comuns.",
    faq: [
      { question: "Posso adaptar o projeto para terreno em declive?" },
      { question: "Como funciona aprovação/regularização?" },
      { question: "O arquivo BIM vem completo?" },
      { question: "Qual o suporte oferecido após a compra?" },
    ],

    finalCtaTitle: "Pronto para um chalé premium?",
    finalCtaSubtitle: "Compre o Diamond e crie uma experiência única.",
  },

  tags: ["chales", "diamond", "120m2", "premium", "alto-padrao"],
};

// -------------------------
// EXPORT
// -------------------------

export const PRODUCTS: Product[] = [
  CASA_CONFORT,
  CASA_PRIME,
  CASA_DIAMOND,
  CHALE_CONFORT,
  CHALE_PRIME,
  CHALE_DIAMOND,
];
