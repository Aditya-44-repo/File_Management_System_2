import { Injectable } from '@angular/core';
import { HttpClient, HttpEvent, HttpHeaders, HttpParams, HttpRequest } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from '../../environments/environment';
import { DashboardOverview } from '../models/dashboard-overview.model';
import { FileItem, PagedResult } from '../models/file-load.model';
import { SearchCriteria } from '../models/search-criteria.model';
import { AuthService } from './auth.service';

@Injectable({ providedIn: 'root' })
export class FileLoadService {
  constructor(private http: HttpClient, private auth: AuthService) {}

  private authHeaders(): HttpHeaders {
    const token = this.auth.getToken();
    let headers = new HttpHeaders();
    if (token) headers = headers.set('Authorization', `Bearer ${token}`);
    return headers;
  }

  private normalizeFile(item: any): FileItem {
    const name = item?.filename ?? item?.name ?? 'Unknown file';
    const size = Number(item?.fileSize ?? item?.size ?? 0);
    const mimeType = item?.fileType ?? item?.mimeType ?? 'application/octet-stream';
    const uploadedAt = item?.uploadDate ?? item?.uploadedAt ?? new Date().toISOString();
    const status = item?.status ?? 'PENDING';

    return {
      id: item?.id,
      name,
      size,
      mimeType,
      uploadedById: item?.uploadedById,
      uploadedBy: item?.uploadedBy ?? item?.uploadedByName ?? 'System',
      uploadedAt,
      status,
      recordCount: Number(item?.recordCount ?? 0),
      errors: item?.errors ?? '',
      description: item?.description,
      tags: item?.tags ?? [],
      checksum: item?.checksum,
      version: item?.version,
      filename: item?.filename ?? name,
      fileType: item?.fileType ?? mimeType,
      fileSize: item?.fileSize ?? size,
      uploadDate: item?.uploadDate ?? uploadedAt
    };
  }

  list(criteria: SearchCriteria): Observable<PagedResult<FileItem>> {
    let params = new HttpParams();
    Object.entries(criteria).forEach(([k, v]) => {
      if (v !== undefined && v !== null && v !== '') params = params.set(k, String(v));
    });

    return this.http.get<any>(`${environment.apiBaseUrl}/file-loads`, { params, headers: this.authHeaders() }).pipe(
      map((res) => {
        const items = (res?.items ?? res?.content ?? []).map((item: any) => this.normalizeFile(item));
        return {
          items,
          total: Number(res?.total ?? res?.totalElements ?? items.length),
          page: Number(res?.page ?? res?.number ?? criteria.page ?? 0),
          pageSize: Number(res?.pageSize ?? res?.size ?? criteria.size ?? 10)
        };
      })
    );
  }

  myList(criteria: SearchCriteria): Observable<PagedResult<FileItem>> {
    let params = new HttpParams();
    Object.entries(criteria).forEach(([k, v]) => {
      if (v !== undefined && v !== null && v !== '') params = params.set(k, String(v));
    });

    return this.http.get<any>(`${environment.apiBaseUrl}/file-loads/my`, { params, headers: this.authHeaders() }).pipe(
      map((res) => {
        const items = (res?.items ?? res?.content ?? []).map((item: any) => this.normalizeFile(item));
        return {
          items,
          total: Number(res?.total ?? res?.totalElements ?? items.length),
          page: Number(res?.page ?? res?.number ?? criteria.page ?? 0),
          pageSize: Number(res?.pageSize ?? res?.size ?? criteria.size ?? 10)
        };
      })
    );
  }

  details(id: string | number): Observable<FileItem> {
    return this.http
      .get<any>(`${environment.apiBaseUrl}/file-loads/${id}`, { headers: this.authHeaders() })
      .pipe(map((res) => this.normalizeFile(res)));
  }

  updateMetadata(id: string | number, body: Partial<FileItem>) {
    return this.http
      .patch<any>(`${environment.apiBaseUrl}/file-loads/${id}`, body, { headers: this.authHeaders() })
      .pipe(map((res) => this.normalizeFile(res)));
  }

  updateStatus(id: string | number, status: string, comment?: string) {
    return this.http
      .put<any>(`${environment.apiBaseUrl}/file-loads/${id}/status`, { status, comment }, { headers: this.authHeaders() })
      .pipe(map((res) => this.normalizeFile(res)));
  }


  delete(id: string | number) {
    return this.http.delete<void>(`${environment.apiBaseUrl}/file-loads/${id}`, { headers: this.authHeaders() });
  }

  upload(file: File, extra?: { description?: string; tags?: string[] }): Observable<HttpEvent<any>> {
    const form = new FormData();
    form.append('file', file);
    if (extra?.description) form.append('description', extra.description);
    if (extra?.tags?.length) extra.tags.forEach((t) => form.append('tags', t));

    const req = new HttpRequest('POST', `${environment.apiBaseUrl}/file-loads`, form, {
      reportProgress: true,
      headers: this.authHeaders()
    });

    return this.http.request(req);
  }

  getDashboardOverview(): Observable<DashboardOverview> {
    return this.http.get<DashboardOverview>(`${environment.apiBaseUrl}/file-loads/overview`, {
      headers: this.authHeaders()
    });
  }

  download(id: string | number): Observable<Blob> {
    return this.http.get(`${environment.apiBaseUrl}/file-loads/${id}/download`, {
      responseType: 'blob',
      headers: this.authHeaders()
    }) as unknown as Observable<Blob>;
  }
}