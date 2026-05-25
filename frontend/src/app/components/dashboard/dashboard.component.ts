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

  // Ring chart date range
  ringStartDate: string = '';
  ringEndDate: string = '';
  ringStartDateObj: Date | null = null;
  ringEndDateObj: Date | null = null;

  // Ring chart live metrics
  ringMetrics = {
    success: 0,
    failed: 0,
    successPercent: 0,
    failedPercent: 0,
    loading: true
  };

  trendSuccessPoints = '';
  trendFailedPoints = '';
  private readonly trendWidth = 220;
  private readonly trendHeight = 70;

  constructor(private router: Router, private auth: AuthService, private fileLoadService: FileLoadService) {}

  ngOnInit(): void {
    // Initialize ring dates to today
    const now = new Date();
    const isoToday = now.toISOString().slice(0, 10);
    this.ringStartDate = isoToday;
    this.ringEndDate = isoToday;
    this.ringStartDateObj = new Date(now);
    this.ringEndDateObj = new Date(now);

    this.fetchRingChartData();

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

  onRingDateChange(): void {
    if (!this.ringStartDateObj || !this.ringEndDateObj) {
      return;
    }

    this.ringStartDate = this.formatIsoDate(this.ringStartDateObj);
    this.ringEndDate = this.formatIsoDate(this.ringEndDateObj);
    this.fetchRingChartData();
  }

  private fetchRingChartData(): void {
    if (!this.auth.isAuthenticated()) {
      this.ringMetrics.loading = false;
      return;
    }

    this.ringMetrics.loading = true;
    
    // Parse dates locally to avoid UTC offset shifting the day backward
    const start = this.ringStartDateObj
      ? new Date(this.ringStartDateObj.getFullYear(), this.ringStartDateObj.getMonth(), this.ringStartDateObj.getDate(), 0, 0, 0, 0)
      : new Date();

    const end = this.ringEndDateObj
      ? new Date(this.ringEndDateObj.getFullYear(), this.ringEndDateObj.getMonth(), this.ringEndDateObj.getDate(), 23, 59, 59, 999)
      : new Date();

    const formatLocal = (d: Date) => {
      const pad = (n: number) => n.toString().padStart(2, '0');
      return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
    };

    const base: SearchCriteria = { 
      page: 0, 
      size: 1, 
      startDate: formatLocal(start), 
      endDate: formatLocal(end)
    };

    const callSuccess = this.hasRecordOverviewAccess() 
      ? this.fileLoadService.list({ ...base, status: 'SUCCESS' }) 
      : this.fileLoadService.myList({ ...base, status: 'SUCCESS' });
      
    const callFailed = this.hasRecordOverviewAccess() 
      ? this.fileLoadService.list({ ...base, status: 'FAILED' }) 
      : this.fileLoadService.myList({ ...base, status: 'FAILED' });

    forkJoin({
      success: callSuccess,
      failed: callFailed
    }).subscribe({
      next: (res) => {
        const sCount = Number(res.success?.total ?? 0);
        const fCount = Number(res.failed?.total ?? 0);
        const total = sCount + fCount;
        
        this.ringMetrics = {
          success: sCount,
          failed: fCount,
          successPercent: total === 0 ? 0 : (sCount / total) * 100,
          failedPercent: total === 0 ? 0 : (fCount / total) * 100,
          loading: false
        };

        this.updateTrendLines();
      },
      error: () => {
        this.ringMetrics.loading = false;
        this.updateTrendLines();
      }
    });
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

  formatDisplayDate(value: Date | null): string {
    if (!value) return '';
    const day = String(value.getDate()).padStart(2, '0');
    const month = String(value.getMonth() + 1).padStart(2, '0');
    const year = value.getFullYear();
    return `${day} - ${month} - ${year}`;
  }

  // Dummy applySelectionToOverview removed

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

  private updateTrendLines(): void {
    const successSeries = this.buildTrendSeries(this.ringMetrics.successPercent, [-6, -2, 3, -1, 4]);
    const failedSeries = this.buildTrendSeries(this.ringMetrics.failedPercent, [4, 1, -3, 2, -2]);
    this.trendSuccessPoints = this.toPolylinePoints(successSeries);
    this.trendFailedPoints = this.toPolylinePoints(failedSeries);
  }

  private buildTrendSeries(base: number, deltas: number[]): number[] {
    return deltas.map((delta) => this.clampPercent(base + delta));
  }

  private toPolylinePoints(values: number[]): string {
    const width = this.trendWidth;
    const height = this.trendHeight;
    const padX = 4;
    const padY = 6;
    const step = (width - padX * 2) / (values.length - 1 || 1);
    return values
      .map((value, index) => {
        const x = padX + step * index;
        const y = padY + (1 - value / 100) * (height - padY * 2);
        return `${x.toFixed(1)},${y.toFixed(1)}`;
      })
      .join(' ');
  }

  private clampPercent(value: number): number {
    return Math.max(0, Math.min(100, value));
  }

  private formatIsoDate(value: Date): string {
    const pad = (n: number) => n.toString().padStart(2, '0');
    return `${value.getFullYear()}-${pad(value.getMonth() + 1)}-${pad(value.getDate())}`;
  }
}
