import { Injectable } from '@angular/core';
import { Observable, map, of } from 'rxjs';
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

  getUploadStats(period?: string, year?: string, date?: string): Observable<FileUploadStats> {
    // If a filter is provided, return quick dummy data tailored to the selection
    if (period || year || date) {
      // simple deterministic dummy generation based on selection text
      let seed = 100;
      if (period) seed += period.length * 7;
      if (year) seed += Number(year) % 100;
      if (date) seed += date.length * 3;

      const total = Math.max(20, (seed % 400) + 50);
      const success = Math.round(total * (0.6 + ((seed % 20) / 100)));
      const processing = Math.round(total * (0.15 + ((seed % 7) / 100)));
      const pending = Math.round(total * (0.1 + ((seed % 5) / 100)));
      const failed = Math.max(0, total - success - processing - pending);

      const statuses: UploadStatus[] = [
        { name: 'Failed', count: pending, percentage: (pending / total) * 100, color: '#dc3545' },
        { name: 'Processing', count: processing, percentage: (processing / total) * 100, color: '#198754' },
        { name: 'Success', count: success, percentage: (success / total) * 100, color: '#0d6efd' }
      ];

      if (failed > 0) {
        statuses[0].count += failed;
        statuses[0].percentage = (statuses[0].count / total) * 100;
      }

      return of({ total, statuses });
    }

    // No filters — use aggregated overview
    return this.fileLoadService.getDashboardOverview().pipe(
      map((overview: DashboardOverview) => {
        const total = Number(overview.totalUploads ?? 0);
        const pendingCount = Number(overview.pendingCount ?? 0);
        const processingCount = Number(overview.processingCount ?? 0);
        const successCount = Number(overview.successCount ?? 0);
        const failedCount = Math.max(0, total - pendingCount - processingCount - successCount);

        const statuses: UploadStatus[] = [
          {
            name: 'Failed',
            count: pendingCount,
            percentage: total === 0 ? 0 : (pendingCount / total) * 100,
            color: '#dc3545'
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
          statuses[0].count += failedCount;
          statuses[0].percentage = total === 0 ? 0 : (statuses[0].count / total) * 100;
        }

        return {
          total,
          statuses
        };
      })
    );
  }
}
