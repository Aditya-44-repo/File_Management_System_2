export type FileStatus = 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'SUCCESS' | 'FAILED';

export interface FileItem {
  id: string | number;
  name: string;
  size: number;
  mimeType: string;
  uploadedById?: string | number;
  uploadedBy: string;
  uploadedAt: string;
  status: FileStatus;
  recordCount: number;
  errors?: string;
  description?: string;
  tags?: string[];
  checksum?: string;
  version?: number;
  filename?: string;
  fileType?: string;
  fileSize?: number;
  uploadDate?: string;
}

export interface PagedResult<T> {
  items: T[];
  total: number;
  page: number;
  pageSize: number;
}