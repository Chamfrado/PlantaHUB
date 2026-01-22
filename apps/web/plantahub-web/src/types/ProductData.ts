// src/types/product.ts

export type ProductCategory = "casas" | "chales" | "studios";
export type ProductFileFormat = "BIM" | "DWG" | "PDF";

export type ProductFeature = {
  title: string;
  description?: string;
};

export type ProductIncludedItem = {
  title: string;
  description?: string;
  /** Optional icon/image URL (the page shows an image for each included item) */
  imageUrl?: string;
};

export type ProductKeyFact = {
  value: string; // e.g. "80", "5+"
  label: string; // e.g. "metros quadrados de espaço habitavel"
};

export type ProductTestimonial = {
  quote: string;
  authorName: string;
  authorTitle?: string; // e.g. "Dr."
  avatarUrl?: string;
};

export type ProductFaqItem = {
  question: string;
  answer?: string; // page shows questions; answers may be hidden/collapsed
};

export type ProductCTA = {
  primaryLabel: string; // e.g. "Comprar Agora"
  /** If your CTA goes to checkout/stripe/hotmart, store the link here */
  primaryHref?: string;
};

export type ProductPageCopy = {
  /** Big title like "Confort - 80 m2" */
  headline: string;
  /** Smaller title like "Kitnet Moderna" */
  subheadline?: string;
  /** Hero paragraph */
  description?: string;

  /** Section: "Por Quê escolher..." */
  whyChooseTitle?: string;
  whyChooseIntro?: string;
  whyChooseFeatures?: ProductFeature[];

  /** Section: "O que está incluso..." */
  includesTitle?: string;
  includesIntro?: string;
  includedItems?: ProductIncludedItem[];

  /** Section: "Fatos Chave..." */
  keyFactsTitle?: string;
  keyFactsIntro?: string;
  keyFacts?: ProductKeyFact[];

  /** Section: testimonials */
  testimonialsTitle?: string;
  testimonialsIntro?: string;
  testimonials?: ProductTestimonial[];

  /** Section: FAQ */
  faqTitle?: string;
  faqIntro?: string;
  faq?: ProductFaqItem[];

  /** Final CTA */
  finalCtaTitle?: string;
  finalCtaSubtitle?: string;
};

export type Product = {
  id: string;
  slug: string; // "confort"
  category: ProductCategory;

  /** Useful for cards/listing pages */
  name: string; // "Confort"
  shortDescription?: string;

  /** Commercial / metadata */
  areaM2?: number;
  fileFormats?: ProductFileFormat[];
  delivery?: string; // "Download digital imediato..."
  customizable?: boolean;

  /** If you later add prices (not visible in the page text we can read) */
  price?: {
    currency: "BRL" | "USD" | "EUR";
    amount: number;
    /** Optional: "starting from" style */
    isStartingFrom?: boolean;
  };

  /** Images */
  heroImageUrl?: string;
  galleryImageUrls?: string[];

  /** Content used to render the full product page */
  page: ProductPageCopy;

  /** Optional tags for filtering/search */
  tags?: string[];
};

// -------------------------
// Example: Casa Confort page
// -------------------------

export const CONFORT_PRODUCT: Product = {
  id: "casa-confort-80m2",
  slug: "confort",
  category: "casas",

  name: "Confort",
  shortDescription: "Kitnet Moderna",

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
      {
        title: "Planta Arquitetônica",
        description: "Layout detalhado das dimensões dos cômodos.",
      },
      {
        title: "Plata Hidráulica",
        description: "Planta otimizada para sistemas de agua e drenagem.",
      },
      {
        title: "Planta Elétrica",
        description: "Layouts compreensivos de cabeamento para serviços eficientes.",
      },
      {
        title: "Planta Estrutural",
        description: "Fundações sólidas assegurando segurança e durabilidade.",
      },
      {
        title: "Planta Paisagística",
        description: "Lindas ideias de paisagismo para complementar sua casa.",
      },
      {
        title: "Soluções Ecológicas",
        description: "Abordagens inovadoras para uma vida sustentável.",
      },
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
      {
        quote: "“Serviço rápido e prático, além do design incrivel! Recomendo demais!”",
        authorName: "James Foster",
      },
      {
        quote: "“As plantas são tão faceis, nossos pedreiros amaram elas!”",
        authorName: "Dr. Aisha Patel",
      },
      {
        quote:
          "“Casa Confort me deu um design incrível! É funcional e ao mesmo tempo estilosa!” .",
        authorName: "Marcus Rodriguez",
      },
      {
        quote: "“Serviço rápido e prático, além do design incrivel! Recomendo demais!”",
        authorName: "James Foster",
      },
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

  tags: ["80m2", "kitnet", "moderna", "plantas-completas"],
};
