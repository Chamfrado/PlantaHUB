export type ProductFile = "BIM" | "DWG" | "PDF"

export type Feature = {
  title: string
  description: string
}

export type IncludedItem = {
  title: string
  description: string
}

export type Stat = {
  value: string
  label: string
}

export type ProductPageData = {
  id: string
  tier: "Comfort" | "Prime" | "Diamond"
  category: "houses" | "chalets" | "studios"

  name: string
  headline: string
  description: string

  areaM2: number
  files: ProductFile[]
  delivery: string
  customizable: boolean

  whyChooseTitle: string
  whyChooseSubtitle?: string
  whyChooseFeatures: Feature[]

  includesTitle: string
  includesSubtitle?: string
  includedItems: IncludedItem[]

  statsTitle: string
  statsSubtitle?: string
  stats: Stat[]
}
