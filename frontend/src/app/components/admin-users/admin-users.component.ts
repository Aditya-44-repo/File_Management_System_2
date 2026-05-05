import { Component, OnInit, ViewChild } from '@angular/core';
import { MatPaginator, PageEvent } from '@angular/material/paginator';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTableDataSource } from '@angular/material/table';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';
import { AdminService, AdminUserSummary } from '../../services/admin.service';
import { AuthService } from '../../services/auth.service';

interface AdminUserRow extends AdminUserSummary {
  fileCount?: number;
  loadingCount?: boolean;
}

@Component({
  selector: 'app-admin-users',
  templateUrl: './admin-users.component.html',
  styleUrls: ['./admin-users.component.scss']
})
export class AdminUsersComponent implements OnInit {
  displayedColumns = ['id', 'username', 'email', 'role', 'enabled', 'failedLoginAttempts', 'fileCount', 'actions'];
  dataSource = new MatTableDataSource<AdminUserRow>([]);
  total = 0;
  loading = false;
  query = '';
  pageIndex = 0;
  pageSize = 10;
  get permissionNotes(): string[] {
    const notes = ['Block or unblock users'];
    if (this.canViewFileCounts()) {
      notes.push('View users records and file counts');
    }
    if (this.canDeleteAllFiles()) {
      notes.push('Delete all files of a user');
    }
    return notes;
  }

  @ViewChild(MatPaginator) paginator!: MatPaginator;

  constructor(
    private adminService: AdminService,
    private auth: AuthService,
    private router: Router,
    private snack: MatSnackBar
  ) {}

  ngOnInit(): void {
    if (!this.auth.isAuthenticated()) {
      this.router.navigate(['/login']);
      return;
    }

    if (!this.canAccessAdminUsers()) {
      this.snack.open('You do not have permission to open admin users page', 'Dismiss', { duration: 3500 });
      this.router.navigate(['/dashboard']);
      return;
    }

    this.loadUsers();
  }

  canAccessAdminUsers(): boolean {
    return this.auth.hasAnyAdminPermission('USER_ACCESS_CONTROL', 'USER_RECORDS_OVERVIEW', 'USER_FILES_DELETE_ALL')
      || (this.auth.getCurrentUser()?.role || '').toUpperCase() === 'SUPER_ADMIN';
  }

  canViewFileCounts(): boolean {
    return (this.auth.getCurrentUser()?.role || '').toUpperCase() === 'SUPER_ADMIN'
      || this.auth.hasAnyAdminPermission('USER_RECORDS_OVERVIEW');
  }

  canDeleteAllFiles(): boolean {
    return (this.auth.getCurrentUser()?.role || '').toUpperCase() === 'SUPER_ADMIN'
      || this.auth.hasAnyAdminPermission('USER_FILES_DELETE_ALL');
  }

  isSuperAdminViewer(): boolean {
    return (this.auth.getCurrentUser()?.role || '').toUpperCase() === 'SUPER_ADMIN';
  }

  canActOnRow(row: AdminUserRow): boolean {
    return this.isSuperAdminViewer() || (row.role || '').toUpperCase() !== 'SUPER_ADMIN';
  }

  search(): void {
    this.pageIndex = 0;
    this.loadUsers();
  }

  pageChange(ev: PageEvent): void {
    this.pageIndex = ev.pageIndex;
    this.pageSize = ev.pageSize;
    this.loadUsers();
  }

  loadUsers(): void {
    this.loading = true;
    this.adminService.listUsers(this.query.trim(), this.pageIndex, this.pageSize)
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: async (res) => {
          const rows = (res.content || [])
            .filter((u) => (u.role || '').toUpperCase() !== 'SUPER_ADMIN')
            .map((u) => ({ ...u, fileCount: undefined, loadingCount: false }));
          this.dataSource.data = rows;
          this.total = Number(res.totalElements || 0);
          if (this.canViewFileCounts()) {
            await this.loadCountsForVisibleRows();
          }
        },
        error: (err) => {
          console.error('[AdminUsersComponent] Failed to load users:', err);
          this.snack.open(err?.error?.message || 'Failed to load users', 'Dismiss', { duration: 3500 });
        }
      });
  }

  async loadCountsForVisibleRows(): Promise<void> {
    if (!this.canViewFileCounts()) {
      return;
    }

    for (const row of this.dataSource.data) {
      row.loadingCount = true;
      this.adminService.getUserFileCount(Number(row.id)).subscribe({
        next: (res) => {
          row.fileCount = res.fileCount;
          row.loadingCount = false;
        },
        error: () => {
          row.fileCount = 0;
          row.loadingCount = false;
        }
      });
    }
  }

  toggleEnabled(row: AdminUserRow): void {
    if (!this.canActOnRow(row)) {
      this.snack.open('Only SUPER_ADMIN can manage this account', 'Dismiss', { duration: 3000 });
      return;
    }

    if (!this.auth.hasAnyAdminPermission('USER_ACCESS_CONTROL') && (this.auth.getCurrentUser()?.role || '').toUpperCase() !== 'SUPER_ADMIN') {
      this.snack.open('You do not have permission to block or unblock users', 'Dismiss', { duration: 3000 });
      return;
    }

    const action = row.enabled ? 'block' : 'unblock';
    if (!confirm(`Are you sure you want to ${action} ${row.username}?`)) {
      return;
    }

    this.adminService.updateUserEnabled(Number(row.id), !row.enabled).subscribe({
      next: () => {
        this.snack.open(`User ${action}ed`, 'OK', { duration: 2000 });
        this.loadUsers();
      },
      error: (err) => {
        console.error('[AdminUsersComponent] Update enabled failed:', err);
        this.snack.open(err?.error?.message || 'Failed to update user state', 'Dismiss', { duration: 3500 });
      }
    });
  }

  deleteAllFiles(row: AdminUserRow): void {
    if (!this.canActOnRow(row)) {
      this.snack.open('Only SUPER_ADMIN can delete files for this account', 'Dismiss', { duration: 3000 });
      return;
    }

    if (!this.canDeleteAllFiles()) {
      this.snack.open('You do not have permission to delete all files for a user', 'Dismiss', { duration: 3000 });
      return;
    }

    if (!confirm(`Delete ALL files uploaded by ${row.username}? This cannot be undone.`)) {
      return;
    }

    this.adminService.deleteAllUserFiles(Number(row.id)).subscribe({
      next: () => {
        this.snack.open('All user files deleted', 'OK', { duration: 2500 });
        this.loadUsers();
      },
      error: (err) => {
        console.error('[AdminUsersComponent] Delete all files failed:', err);
        this.snack.open(err?.error?.message || 'Failed to delete user files', 'Dismiss', { duration: 3500 });
      }
    });
  }
}



