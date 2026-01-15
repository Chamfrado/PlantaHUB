export type ProductCategory = 'houses' | 'chalets' | 'studios';

export type ProductFile = 'BIM' | 'DWG' | 'PDF';

export type ProductDetails = {
  id: string;
  category: ProductCategory;

  name: string;
  shortDescription: string;

  areaM2: number;
  startingFrom: number; // valor base

  // detalhes para o painel expandido
  includes: string[]; // o que vem no pacote
  technical: string[]; // infos técnicas (pavimentos, quartos, etc)
  files: ProductFile[]; // formatos disponíveis
  delivery: string; // ex: "Download imediato"
  certification?: string; // ex: "CAU/CREA"
};
