import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface AdminUserSummary {
  id: number;
  username: string;
  email: string;
  role: string;
  enabled: boolean;
  failedLoginAttempts: number;
  accountLockedUntil: string | null;
  tokenVersion: number;
}

export interface AdminUsersPage {
  content: AdminUserSummary[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface UserFileCountResponse {
  userId: number;
  fileCount: number;
}

@Injectable({ providedIn: 'root' })
export class AdminService {
  constructor(private http: HttpClient) {}

  listUsers(query = '', page = 0, size = 10): Observable<AdminUsersPage> {
    return this.http.get<AdminUsersPage>(`${environment.apiBaseUrl}/admin/users`, {
      params: {
        query,
        page,
        size
      }
    });
  }

  updateUserEnabled(userId: number, enabled: boolean) {
    return this.http.patch(`${environment.apiBaseUrl}/admin/users/${userId}/enabled`, { enabled });
  }

  getUserFileCount(userId: number): Observable<UserFileCountResponse> {
    return this.http.get<UserFileCountResponse>(`${environment.apiBaseUrl}/admin/users/${userId}/file-count`);
  }

  deleteAllUserFiles(userId: number) {
    return this.http.delete(`${environment.apiBaseUrl}/admin/users/${userId}/files`);
  }
}

