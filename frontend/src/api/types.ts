// 商品状态枚举
export enum ProductStatus {
  ON_SALE = 'ON_SALE',       // 在售
  OFF_SALE = 'OFF_SALE',     // 下架
  OUT_OF_STOCK = 'OUT_OF_STOCK'  // 售罄
}

// 商品实体
export interface Product {
  id: number
  sellerId: number
  sellerName: string
  name: string
  description: string
  price: number
  originalPrice: number
  stock: number
  mainImage: string
  images: string[]
  category: string
  tags: string[]
  discount: number
  isOnSale: boolean
  isFeatured: boolean
  salesCount: number
  viewCount: number
  status: ProductStatus
  createdAt: string
  updatedAt: string
}

// 商品状态文本映射
export const ProductStatusText: Record<ProductStatus, string> = {
  [ProductStatus.ON_SALE]: '在售',
  [ProductStatus.OFF_SALE]: '下架',
  [ProductStatus.OUT_OF_STOCK]: '售罄'
}

// 商品状态颜色映射
export const ProductStatusColor: Record<ProductStatus, string> = {
  [ProductStatus.ON_SALE]: 'success',
  [ProductStatus.OFF_SALE]: 'info',
  [ProductStatus.OUT_OF_STOCK]: 'danger'
}
