import { Component, OnDestroy, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { Subscription, catchError, forkJoin, interval, map, of, startWith, switchMap, throwError } from 'rxjs';
import { DashboardOverview } from '../../models/dashboard-overview.model';
import { SearchCriteria } from '../../models/search-criteria.model';
import { AuthService } from '../../services/auth.service';
import { FileLoadService } from '../../services/file-load.service';

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss']
})
export class DashboardComponent implements OnInit, OnDestroy {
  overview: DashboardOverview | null = null;
  loadingOverview = true;
  overviewError = '';
  private overviewSub?: Subscription;

  constructor(private router: Router, private auth: AuthService, private fileLoadService: FileLoadService) {}

  ngOnInit(): void {
  // 1. Check if authenticated immediately
  if (!this.auth.isAuthenticated()) {
    this.router.navigate(['/login'], { queryParams: { returnUrl: this.router.url } });
    return; // Stop execution
  }

  // 2. Only if logged in, start the dashboard metrics polling
  this.overviewSub = interval(10000)
    .pipe(
      startWith(0),
      switchMap(() => this.fetchOverview())
)
.subscribe({
      next: (overview) => {
        this.overview = overview;
        this.loadingOverview = false;
        this.overviewError = '';
      },
      error: () => {
        this.loadingOverview = false;
        this.overviewError = 'Unable to load live metrics right now.';
      }
    });
}


  ngOnDestroy(): void {
    this.overviewSub?.unsubscribe();
  }

  navigateTo(target: '/upload' | '/files', event?: Event): void {
    event?.preventDefault();
    if (this.auth.isAuthenticated()) {
      this.router.navigate([target]);
      return;
    }

    this.router.navigate(['/login'], { queryParams: { returnUrl: target } });
  }

  formatCount(value: number | undefined): string {
    return new Intl.NumberFormat('en-US').format(value ?? 0);
  }

  formatRate(value: number | undefined): string {
    return `${(value ?? 0).toFixed(1)}%`;
  }

  private fetchOverview() {
    return this.fileLoadService.getDashboardOverview().pipe(
      switchMap((overview) => {
        // For public dashboard access, avoid hitting protected fallback APIs when logged out.
        if (!this.auth.isAuthenticated() || this.hasMeaningfulData(overview)) {
          return of(overview);
        }
        return this.hasRecordOverviewAccess()
          ? this.fetchOverviewFromListApi()
          : this.fetchOverviewFromMyListApi();
      }),
      catchError(() => {
        if (!this.auth.isAuthenticated()) {
          return of(this.emptyOverview());
        }
        return this.hasRecordOverviewAccess()
          ? this.fetchOverviewFromListApi()
          : this.fetchOverviewFromMyListApi();
      })
    );
  }

  private emptyOverview(): DashboardOverview {
    return {
      totalUploads: 0,
      inProcessing: 0,
      successRate: 0,
      exceptionsToday: 0,
      pendingCount: 0,
      processingCount: 0,
      successCount: 0,
      lastUpdated: new Date().toISOString()
    };
  }

  private hasMeaningfulData(overview: DashboardOverview | null | undefined): boolean {
    if (!overview) return false;
    return (
      (overview.totalUploads ?? 0) > 0 ||
      (overview.inProcessing ?? 0) > 0 ||
      (overview.pendingCount ?? 0) > 0 ||
      (overview.processingCount ?? 0) > 0 ||
      (overview.successCount ?? 0) > 0 ||
      (overview.exceptionsToday ?? 0) > 0
    );
  }

  private fetchOverviewFromListApi() {
    const now = new Date();
    const startOfDay = new Date(now);
    startOfDay.setHours(0, 0, 0, 0);

    const base: SearchCriteria = { page: 0, size: 1, sort: 'uploadDate,desc' };

    return forkJoin({
      all: this.fileLoadService.list(base),
      pending: this.fileLoadService.list({ ...base, status: 'PENDING' }),
      processing: this.fileLoadService.list({ ...base, status: 'PROCESSING' }),
      success: this.fileLoadService.list({ ...base, status: 'SUCCESS' }),
      failedToday: this.fileLoadService.list({
        ...base,
        status: 'FAILED',
        startDate: startOfDay.toISOString(),
        endDate: now.toISOString()
      })
    }).pipe(
      map((res) => {
        const totalUploads = Number(res.all?.total ?? 0);
        const pendingCount = Number(res.pending?.total ?? 0);
        const processingCount = Number(res.processing?.total ?? 0);
        const successCount = Number(res.success?.total ?? 0);
        const exceptionsToday = Number(res.failedToday?.total ?? 0);
        const successRate = totalUploads === 0 ? 0 : (successCount * 100) / totalUploads;

        const overview: DashboardOverview = {
          totalUploads,
          inProcessing: processingCount,
          successRate,
          exceptionsToday,
          pendingCount,
          processingCount,
          successCount,
          lastUpdated: now.toISOString()
        };

        return overview;
      }),
      catchError(() => throwError(() => new Error('Unable to load dashboard metrics from all sources')))
    );
  }

  private fetchOverviewFromMyListApi() {
    const now = new Date();
    const startOfDay = new Date(now);
    startOfDay.setHours(0, 0, 0, 0);

    const base: SearchCriteria = { page: 0, size: 1, sort: 'uploadDate,desc' };

    return forkJoin({
      all: this.fileLoadService.myList(base),
      pending: this.fileLoadService.myList({ ...base, status: 'PENDING' }),
      processing: this.fileLoadService.myList({ ...base, status: 'PROCESSING' }),
      success: this.fileLoadService.myList({ ...base, status: 'SUCCESS' }),
      failedToday: this.fileLoadService.myList({
        ...base,
        status: 'FAILED',
        startDate: startOfDay.toISOString(),
        endDate: now.toISOString()
      })
    }).pipe(
      map((res) => {
        const totalUploads = Number(res.all?.total ?? 0);
        const pendingCount = Number(res.pending?.total ?? 0);
        const processingCount = Number(res.processing?.total ?? 0);
        const successCount = Number(res.success?.total ?? 0);
        const exceptionsToday = Number(res.failedToday?.total ?? 0);
        const successRate = totalUploads === 0 ? 0 : (successCount * 100) / totalUploads;

        return {
          totalUploads,
          inProcessing: processingCount,
          successRate,
          exceptionsToday,
          pendingCount,
          processingCount,
          successCount,
          lastUpdated: now.toISOString()
        } as DashboardOverview;
      }),
      catchError(() => of(this.emptyOverview()))
    );
  }

  private hasRecordOverviewAccess(): boolean {
    return this.auth.hasAnyAdminPermission('USER_RECORDS_OVERVIEW');
  }
}
