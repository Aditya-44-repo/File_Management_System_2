export interface SearchCriteria {
  fileId?: string;
  filename?: string;
  status?: string;
  startDate?: string;
  endDate?: string;
  recordCountMin?: number;
  recordCountMax?: number;
  page?: number;
  size?: number;
  sort?: string;
}