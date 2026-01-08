export type Tier = "Comfort" | "Prime" | "Diamond"

export type ProductFormat = "BIM" | "DWG" | "PDF"

export type Product = {
  id: string
  tier?: Tier
  title: string
  subtitle: string
  areaM2?: number
  tags?: string[]
  price: number
  imageUrl: string
  formats?: ProductFormat[]
}
