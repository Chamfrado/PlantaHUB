import type { ProductPageData } from "../types/productPage"

export const products: ProductPageData[] = [
  {
    id: "casa-confort-80",
    tier: "Comfort",
    category: "houses",

    name: "Casa Confort - 80 m²",
    headline: "Kitnet Moderna",
    description:
      "Transforme sua experiência de construir uma casa com um projeto arquitetônico maravilhoso.",

    areaM2: 80,
    files: ["BIM", "DWG", "PDF"],
    delivery: "Download digital imediato na confirmação da compra.",
    customizable: true,

    whyChooseTitle: "Por que escolher a Casa Confort?",
    whyChooseSubtitle:
      "Detalhes minuciosamente desenvolvidos para uma habitação moderna.",
    whyChooseFeatures: [
      {
        title: "Design Compreensivo",
        description:
          "Inclui todas as plantas essenciais: Arquitetônica, Hidráulica, Elétrica, Estrutural e Paisagística.",
      },
      {
        title: "Soluções ecológicas",
        description: "Abordagens inovadoras garantindo uma vida mais sustentável.",
      },
      {
        title: "Lista de materiais e manual",
        description: "Lista de materiais detalhada e manual de construção.",
      },
      {
        title: "Acesso Imediato",
        description: "Download imediato após a confirmação da compra.",
      },
      {
        title: "Arquivos versáteis",
        description: "Arquivos em formatos BIM, DWG e PDF para uso versátil.",
      },
      {
        title: "Extremamente customizável",
        description: "Base pronta para ajustes e adaptações no seu projeto.",
      },
    ],

    includesTitle: "O que está incluso na sua compra?",
    includesSubtitle: "Fique pronto para construir com um kit completo de plantas.",
    includedItems: [
      {
        title: "Planta Arquitetônica",
        description: "Layout detalhado com dimensões e organização dos cômodos.",
      },
      {
        title: "Planta Hidráulica",
        description: "Projeto otimizado para sistemas de água e drenagem.",
      },
      {
        title: "Planta Elétrica",
        description: "Layout completo de cabeamento para serviços eficientes.",
      },
      {
        title: "Planta Estrutural",
        description: "Fundações sólidas garantindo segurança e durabilidade.",
      },
      {
        title: "Planta Paisagística",
        description: "Ideias de paisagismo para complementar sua casa.",
      },
      {
        title: "Soluções Ecológicas",
        description: "Abordagens inovadoras para uma vida sustentável.",
      },
    ],

    statsTitle: "Fatos chave sobre a Casa Confort",
    statsSubtitle: "Descubra o essencial da sua nova casa.",
    stats: [
      { value: "80", label: "m² de espaço habitável" },
      { value: "5+", label: "plantas diferentes inclusas" },
    ],
  },
]
