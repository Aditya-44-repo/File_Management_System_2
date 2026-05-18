import { Injectable } from '@angular/core';
import { Observable, map } from 'rxjs';
import { FileLoadService } from './file-load.service';
import { DashboardOverview } from '../models/dashboard-overview.model';

export interface UploadStatus {
  name: string;
  count: number;
  percentage: number;
  color: string;
}

export interface FileUploadStats {
  total: number;
  statuses: UploadStatus[];
}

@Injectable({ providedIn: 'root' })
export class FileUploadStatsService {
  constructor(private fileLoadService: FileLoadService) {}

  getUploadStats(): Observable<FileUploadStats> {
    return this.fileLoadService.getDashboardOverview().pipe(
      map((overview: DashboardOverview) => {
        const total = Number(overview.totalUploads ?? 0);
        const pendingCount = Number(overview.pendingCount ?? 0);
        const processingCount = Number(overview.processingCount ?? 0);
        const successCount = Number(overview.successCount ?? 0);
        const failedCount = Math.max(0, total - pendingCount - processingCount - successCount);

        const statuses: UploadStatus[] = [
          {
            name: 'Pending',
            count: pendingCount,
            percentage: total === 0 ? 0 : (pendingCount / total) * 100,
            color: '#0062cc'
          },
          {
            name: 'Processing',
            count: processingCount,
            percentage: total === 0 ? 0 : (processingCount / total) * 100,
            color: '#198754'
          },
          {
            name: 'Success',
            count: successCount,
            percentage: total === 0 ? 0 : (successCount / total) * 100,
            color: '#0d6efd'
          }
        ];

        if (failedCount > 0) {
          statuses.push({
            name: 'Failed',
            count: failedCount,
            percentage: total === 0 ? 0 : (failedCount / total) * 100,
            color: '#dc3545'
          });
        }

        return {
          total,
          statuses
        };
      })
    );
  }
}
