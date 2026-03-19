export type FileDTO = {
  filename: string;
  storageKey: string;
  url: string;
  sizeBytes: number;
};

export type DownloadResponseDTO = {
  productId: string;
  planTypeCode: string;
  files: FileDTO[];
};

export type DownloadBundleItemRequest = {
  productId: string;
  planTypeCodes: string[];
};

export type DownloadBundleRequest = {
  items: DownloadBundleItemRequest[];
};

export type DownloadBundleResponseDTO = {
  filename: string;
  storageKey: string;
  url: string;
  expiresInSeconds: number;
};