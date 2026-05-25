import { Component, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { MatPaginator, PageEvent } from '@angular/material/paginator';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatSort, Sort } from '@angular/material/sort';
import { MatTableDataSource } from '@angular/material/table';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { Subject, timer } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { FileItem, PagedResult } from '../../models/file-load.model';
import { SearchCriteria } from '../../models/search-criteria.model';
import { AuthService } from '../../services/auth.service';
import { FileLoadService } from '../../services/file-load.service';

@Component({
  selector: 'app-file-list',
  templateUrl: './file-list.component.html',
  styleUrls: ['./file-list.component.scss']
})
export class FileListComponent implements OnInit, OnDestroy {
  displayedColumns = ['select', 'id', 'filename', 'uploadDate', 'status', 'recordCount', 'actions'];
  dataSource = new MatTableDataSource<FileItem>([]);
  total = 0;
  loading = false;
  isBulkDeleting = false;
  private readonly selectedIds = new Set<number>();

  criteria: SearchCriteria = { page: 0, size: 10, sort: 'uploadDate,desc' };
  private readonly destroy$ = new Subject<void>();

  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort!: MatSort;

  constructor(
    private api: FileLoadService,
    private snack: MatSnackBar,
    private router: Router,
    private auth: AuthService
  ) {}

  ngOnInit(): void {
    if (!this.auth.isAuthenticated()) {
      this.router.navigate(['/login']);
      return;
    }

    this.fetch(true);

    // Poll quickly so PENDING and PROCESSING transitions are visible in UI.
    timer(1000, 1000)
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => {
        if (!this.isBulkDeleting) {
          this.fetch(false);
        }
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  onSearch(c: SearchCriteria) {
    this.criteria = {
      ...this.criteria,
      ...c,
      page: 0
    };
    this.fetch(true);
  }

  fetch(showLoader = true) {
    if (showLoader) {
      this.loading = true;
    }
    this.api.myList(this.criteria).subscribe({
      next: (res: PagedResult<FileItem>) => {
        this.dataSource.data = res.items;
        this.total = res.total;
        this.retainSelectionForVisibleRows();
        if (showLoader) {
          this.loading = false;
        }
      },
      error: (err) => {
        console.error('[FileListComponent] Error loading files:', err, {
          criteria: this.criteria
        });
        this.snack.open(err?.error?.message ?? 'Failed to load files', 'Dismiss', { duration: 3500 });
        if (showLoader) {
          this.loading = false;
        }
      }
    });
  }

  pageChange(ev: PageEvent) {
    this.criteria.page = ev.pageIndex;
    this.criteria.size = ev.pageSize;
    this.fetch(true);
  }

  sortChange(ev: Sort) {
    const direction = ev.direction || 'desc';
    const fieldMap: Record<string, string> = {
      id: 'id',
      filename: 'filename',
      uploadDate: 'uploadDate',
      status: 'status',
      recordCount: 'recordCount'
    };
    this.criteria.sort = `${fieldMap[ev.active] || ev.active},${direction}`;
    this.criteria.page = 0;
    this.fetch(true);
  }

  view(row: FileItem) {
    this.router.navigate(['/files', row.id]);
  }

  download(row: FileItem) {
    this.api.download(row.id).subscribe({
      next: (blob) => {
        this.startDownload(blob, row.filename || row.name || `file-${row.id}`);
        this.snack.open('Download started', 'OK', { duration: 1500 });
      },
      error: (err) => {
        console.error('[FileListComponent] Download failed:', err, { fileId: row.id, fileName: row.filename || row.name });
        this.snack.open('Download failed', 'Dismiss', { duration: 3000 });
      }
    });
  }

  async downloadSelected(): Promise<void> {
    if (this.selectedIds.size === 0) {
      this.snack.open('Select the files to download', 'OK', { duration: 2500 });
      return;
    }

    const ids = Array.from(this.selectedIds);
    let failureCount = 0;

    this.snack.open(`Downloading ${ids.length} file(s)`, 'OK', { duration: 1500 });

    for (const id of ids) {
      const row = this.dataSource.data.find((item) => Number(item.id) === id);
      const filename = row?.filename || row?.name || `file-${id}`;

      try {
        const blob = await firstValueFrom(this.api.download(id));
        this.startDownload(blob, filename);
      } catch (err) {
        console.error('[FileListComponent] Bulk download failed for file:', id, err);
        failureCount++;
      }
    }

    if (failureCount > 0) {
      this.snack.open(`${failureCount} file(s) failed to download`, 'Dismiss', { duration: 3000 });
    }
  }

  delete(row: FileItem) {
    if (this.isBulkDeleting) {
      return;
    }
    if (!confirm(`Delete "${row.filename || row.name}"? This cannot be undone.`)) return;
    this.api.delete(row.id).subscribe({
      next: () => {
        this.selectedIds.delete(Number(row.id));
        this.snack.open('File deleted', 'OK', { duration: 1500 });
        this.fetch(true);
      },
      error: (err) => {
        console.error('[FileListComponent] Delete failed:', err, { fileId: row.id, fileName: row.filename || row.name });
        this.snack.open('Delete failed', 'Dismiss', { duration: 3000 });
      }
    });
  }

  reupload(row: FileItem) {
    if (!row?.id) return;
    this.router.navigate(['/upload'], { queryParams: { reuploadId: row.id } });
  }

  isRowSelected(row: FileItem): boolean {
    return this.selectedIds.has(Number(row.id));
  }

  toggleRowSelection(row: FileItem, checked: boolean): void {
    const id = Number(row.id);
    if (checked) {
      this.selectedIds.add(id);
    } else {
      this.selectedIds.delete(id);
    }
  }

  toggleSelectAllVisible(checked: boolean): void {
    for (const row of this.dataSource.data) {
      const id = Number(row.id);
      if (checked) {
        this.selectedIds.add(id);
      } else {
        this.selectedIds.delete(id);
      }
    }
  }

  allVisibleSelected(): boolean {
    return this.dataSource.data.length > 0 && this.dataSource.data.every((row) => this.selectedIds.has(Number(row.id)));
  }

  someVisibleSelected(): boolean {
    const selectedVisible = this.dataSource.data.filter((row) => this.selectedIds.has(Number(row.id))).length;
    return selectedVisible > 0 && selectedVisible < this.dataSource.data.length;
  }

  selectedCount(): number {
    return this.selectedIds.size;
  }

  async deleteSelected(): Promise<void> {
    if (this.isBulkDeleting) {
      return;
    }
    if (this.selectedIds.size === 0) {
      this.snack.open('Select the files to delete', 'OK', { duration: 2500 });
      return;
    }
    if (!confirm(`Delete ${this.selectedIds.size} selected file(s)? This cannot be undone.`)) {
      return;
    }

    this.isBulkDeleting = true;
    this.loading = true;
    let successCount = 0;
    let failureCount = 0;

    const ids = Array.from(this.selectedIds);
    for (const id of ids) {
      try {
        await firstValueFrom(this.api.delete(id));
        this.selectedIds.delete(id);
        successCount++;
      } catch (err) {
        console.error('[FileListComponent] Bulk delete failed for file:', id, err);
        failureCount++;
      }
    }

    this.isBulkDeleting = false;
    this.loading = false;
    await this.refreshAfterBulkDelete(successCount, failureCount);
  }

  async deleteAllMatching(): Promise<void> {
    if (this.isBulkDeleting || this.total === 0) {
      return;
    }
    if (!confirm(`Delete ALL ${this.total} file(s) from current search result? This cannot be undone.`)) {
      return;
    }

    this.isBulkDeleting = true;
    this.loading = true;
    let successCount = 0;
    let failureCount = 0;

    try {
      const ids = await this.fetchAllMatchingIds();
      for (const id of ids) {
        try {
          await firstValueFrom(this.api.delete(id));
          this.selectedIds.delete(id);
          successCount++;
        } catch (err) {
          console.error('[FileListComponent] Bulk deleteAllMatching failed for file:', id, err);
          failureCount++;
        }
      }
    } catch (err) {
      console.error('[FileListComponent] Error fetching all matching IDs for deleteAllMatching:', err);
      failureCount = this.total;
    } finally {
      this.isBulkDeleting = false;
      this.loading = false;
    }

    await this.refreshAfterBulkDelete(successCount, failureCount);
  }

  private async fetchAllMatchingIds(): Promise<number[]> {
    const ids: number[] = [];
    const pageSize = 200;
    let page = 0;
    let totalPages = 1;

    while (page < totalPages) {
      const criteria = { ...this.criteria, page, size: pageSize };
      try {
        const res = await firstValueFrom(this.api.myList(criteria));
        res.items.forEach((item) => ids.push(Number(item.id)));
        totalPages = Math.max(1, Math.ceil((res.total || 0) / pageSize));
        page++;
      } catch (err) {
        console.error('[FileListComponent] Error fetching page for fetchAllMatchingIds:', { page, criteria }, err);
        throw err;
      }
    }

    return ids;
  }

  private async refreshAfterBulkDelete(successCount: number, failureCount: number): Promise<void> {
    if (failureCount === 0) {
      this.snack.open(`Deleted ${successCount} file(s)`, 'OK', { duration: 2500 });
    } else {
      this.snack.open(`Deleted ${successCount} file(s), ${failureCount} failed`, 'Dismiss', { duration: 3500 });
    }
    this.selectedIds.clear();
    this.fetch(true);
  }

  private retainSelectionForVisibleRows(): void {
    for (const id of Array.from(this.selectedIds)) {
      if (!Number.isFinite(id)) {
        this.selectedIds.delete(id);
      }
    }
  }

  fileName(row: FileItem): string {
    return row.filename || row.name;
  }

  uploadDate(row: FileItem): string {
    return row.uploadDate || row.uploadedAt;
  }

  statusClass(status: string): string {
    const map: Record<string, string> = {
      PENDING: 'badge-pending',
      PROCESSING: 'badge-processing',
      COMPLETED: 'badge-success',
      SUCCESS: 'badge-success',
      FAILED: 'badge-failed'
    };
    return map[status] || 'badge-default';
  }

  private startDownload(blob: Blob, filename: string): void {
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    a.remove();
    URL.revokeObjectURL(url);
  }
}