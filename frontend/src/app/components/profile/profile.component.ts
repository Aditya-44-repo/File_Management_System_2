import { Component, OnInit } from '@angular/core';
import { AuthService, EmailChangeRequest } from '../../services/auth.service';
import { FileLoadService } from '../../services/file-load.service';
import { User } from '../../models/user.model';
import { SearchCriteria } from '../../models/search-criteria.model';
import {
  ApexNonAxisChartSeries,
  ApexResponsive,
  ApexChart,
  ApexLegend,
  ApexDataLabels
} from 'ng-apexcharts';

@Component({
  selector: 'app-profile',
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.scss']
})
export class ProfileComponent implements OnInit {
  currentUser: User | null = null;
  isEditing = false;
  newName: string = '';
  currentPassword: string = '';
  newPassword: string = '';
  confirmPassword: string = '';
  currentPasswordError = false;
  passwordMismatchError = false;
  profileImage: string = 'assets/default-avatar.svg';
  emailChangeRequest: EmailChangeRequest | null = null;
  showEmailChangeForm = false;
  requestedNewEmail = '';
  emailChangeSubmitting = false;

  // Statistics
  totalFiles = 0;
  pendingFiles = 0;
  successFiles = 0;
  failedFiles = 0;
  successRate = 0;
  //Be
  totalDownloads = 542;


  constructor(
    private auth: AuthService,
    private fileService: FileLoadService
  ) { }

  ngOnInit(): void {
    this.auth.currentUser$.subscribe(user => {
      this.currentUser = user || null;
      if (user) {
        this.newName = user.name || user.username || '';
        this.loadProfileImage();
        this.loadProfileData();
      } else {
        this.profileImage = 'assets/default-avatar.svg';
      }
    });
  }

  getBackendBaseUrl(): string {
    const protocol = window.location.protocol;
    let port = protocol === 'https:' ? '8080' : '8082';
    return `${protocol}//localhost:${port}`;
  }

  loadProfileImage(): void {
    this.profileImage = this.auth.getProfileImageUrl(this.currentUser);
  }

  getProfileImageFallback(): string {
    return this.auth.getProfileImageFallback(this.currentUser);
  }

  handleProfileImageError(): void {
    this.profileImage = this.getProfileImageFallback();
  }

  getRoleLabel(role?: string | null): string {
    if (!role) {
      return 'User';
    }

    return role
      .replace(/_/g, ' ')
      .toLowerCase()
      .replace(/\b\w/g, (match) => match.toUpperCase());
  }

  loadProfileData(): void {
    this.auth.getFullProfile().subscribe({
      next: (data) => {
        if (data.stats) {
          this.totalFiles = data.stats.totalUploads;
          this.successFiles = data.stats.successfulUploads;
          this.failedFiles = data.stats.failedUploads;
          this.totalDownloads = data.stats.totalDownloads;
          this.successRate = this.totalFiles > 0
            ? Math.round((this.successFiles / this.totalFiles) * 100)
            : 0;

          // Update chart series dynamically
          this.uploadChartSeries = [
            this.successFiles,
            this.failedFiles
          ];
        }

        if (data.user) {
          const updatedUser = {
            ...(this.currentUser || {}),
            ...data.user,
            name: data.user.username
          } as User;
          const shouldUpdateUser =
            this.currentUser?.email !== updatedUser.email ||
            this.currentUser?.username !== updatedUser.username ||
            this.currentUser?.name !== updatedUser.name ||
            this.currentUser?.profileImage !== updatedUser.profileImage;

          this.currentUser = updatedUser;
          if (shouldUpdateUser) {
            this.auth.updateUser(updatedUser);
          }
        }

        this.emailChangeRequest = data.emailChangeRequest || null;

        if (data.recentActivities) {
          this.recentActivities = [...data.recentActivities]
            .sort((a: any, b: any) => this.getTimeValue(b.timestamp) - this.getTimeValue(a.timestamp))
            .slice(0, 10)
            .map((act: any) => ({
            icon: act.icon === 'upload' ? 'cloud_upload' : act.icon,
            text: this.formatActivityText(act.text),
            time: this.formatTime(act.timestamp)
          }));
        }

        if (data.loginHistory) {
          this.loginHistory = data.loginHistory.map((log: any) => ({
            device: log.device,
            browser: log.browser,
            ip: log.ip,
            time: this.formatLoginTime(log.time),
            status: this.normalizeLoginStatus(log.status)
          }));
        }
      },
      error: (err) => {
        console.error('[ProfileComponent] Error loading profile data:', err);
      }
    });
  }

  formatTime(dateString: any): string {
    if (!dateString) return '';
    const date = new Date(dateString);
    const now = new Date();
    const seconds = Math.floor((now.getTime() - date.getTime()) / 1000);

    if (seconds < 0) return 'Just now';
    if (seconds < 60) return `${seconds}s ago`;
    const minutes = Math.floor(seconds / 60);
    if (minutes < 60) return `${minutes}m ago`;
    const hours = Math.floor(minutes / 60);
    if (hours < 24) return `${hours}h ago`;
    const days = Math.floor(hours / 24);
    if (days === 1) return 'Yesterday';
    if (days < 7) return `${days} days ago`;

    return date.toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric'
    });
  }

  private formatActivityText(text: string): string {
    if (!text) return '';

    const failedPrefix = 'Failed to upload ';
    if (text.startsWith(failedPrefix)) {
      return `Status failed for ${text.slice(failedPrefix.length)}`;
    }

    return text;
  }

  formatLoginTime(dateString: any): string {
    if (!dateString) return '';
    const date = new Date(dateString);
    const now = new Date();

    const isToday = date.toDateString() === now.toDateString();
    const yesterday = new Date(now);
    yesterday.setDate(now.getDate() - 1);
    const isYesterday = date.toDateString() === yesterday.toDateString();

    const timeStr = date.toLocaleTimeString('en-US', {
      hour: '2-digit',
      minute: '2-digit',
      hour12: true
    });

    if (isToday) {
      return `Today, ${timeStr}`;
    }
    if (isYesterday) {
      return `Yesterday, ${timeStr}`;
    }

    return date.toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric'
    }) + `, ${timeStr}`;
  }

  getTimeValue(dateString: any): number {
    const value = new Date(dateString).getTime();
    return Number.isNaN(value) ? 0 : value;
  }

  normalizeLoginStatus(status: string): string {
    const normalized = (status || '').toLowerCase();
    if (normalized === 'success') {
      return 'Success';
    }
    if (normalized === 'failed' || normalized === 'failure') {
      return 'Failed';
    }
    return status;
  }

  getLoginStatusClass(status: string): string {
    return (status || '').toLowerCase() === 'failed' ? 'failed-status' : 'success-status';
  }

  enableEdit(): void {
    this.isEditing = true;
    // Scroll to the edit form at the bottom
    setTimeout(() => {
      const editSection = document.getElementById('edit-section');
      if (editSection) {
        editSection.scrollIntoView({ behavior: 'smooth' });
      }
    }, 0);
  }

  cancelEdit(): void {
    this.isEditing = false;
    this.newName = this.currentUser?.name || this.currentUser?.username || '';
    this.currentPassword = '';
    this.newPassword = '';
    this.confirmPassword = '';
    this.currentPasswordError = false;
    this.passwordMismatchError = false;
    this.loadProfileImage();
  }

  clearPasswordErrors(): void {
    this.currentPasswordError = false;
    this.passwordMismatchError = false;
  }

  validateCurrentPassword(): boolean {
    if (!this.newPassword) {
      this.currentPasswordError = false;
      return true;
    }

    this.currentPasswordError = !this.currentPassword.trim();
    return !this.currentPasswordError;
  }

  validateConfirmPassword(): boolean {
    if (!this.newPassword) {
      this.passwordMismatchError = false;
      return true;
    }

    this.passwordMismatchError = this.newPassword !== this.confirmPassword;
    return !this.passwordMismatchError;
  }

  saveProfile(): void {
    if (!this.currentUser) return;

    if (!this.validateCurrentPassword() || !this.validateConfirmPassword()) {
      return;
    }

    this.finishProfileUpdate();
  }

  finishProfileUpdate(): void {
    if (this.newName.trim() && this.currentUser) {
      this.auth.updateProfileDetails(
        this.newName.trim(),
        this.newPassword || undefined,
        this.currentPassword || undefined
      ).subscribe({
        next: (updatedUser) => {
          this.currentUser = updatedUser;
          this.currentUser.name = updatedUser.username; // Bind frontend 'name' to the updated username
          this.auth.updateUser(this.currentUser);
          this.loadProfileImage();
          this.isEditing = false;
          this.currentPassword = '';
          this.newPassword = '';
          this.confirmPassword = '';
          this.currentPasswordError = false;
          this.passwordMismatchError = false;
          alert('Profile updated successfully!');
          this.loadProfileData(); // Refresh history/activities
        },
        error: (err) => {
          console.error('[ProfileComponent] Failed to update profile:', err);
          alert('Failed to update profile name/username or password.');
        }
      });
    } else {
      this.isEditing = false;
    }
  }

  openEmailChangeForm(): void {
    this.showEmailChangeForm = true;
    this.requestedNewEmail = '';
  }

  cancelEmailChangeForm(): void {
    this.showEmailChangeForm = false;
    this.requestedNewEmail = '';
  }

  submitEmailChangeRequest(): void {
    const newEmail = this.requestedNewEmail.trim();
    if (!newEmail) {
      alert('New email is required.');
      return;
    }

    this.emailChangeSubmitting = true;
    this.auth.submitEmailChangeRequest(newEmail).subscribe({
      next: (request) => {
        this.emailChangeRequest = request;
        this.showEmailChangeForm = false;
        this.requestedNewEmail = '';
        this.emailChangeSubmitting = false;
      },
      error: (err) => {
        console.error('[ProfileComponent] Failed to submit email change request:', err);
        alert(this.formatApiError(err, 'Failed to submit email change request.'));
        this.emailChangeSubmitting = false;
      }
    });
  }

  cancelPendingEmailChange(): void {
    if (!this.emailChangeRequest || this.emailChangeRequest.status !== 'pending') {
      return;
    }

    this.auth.cancelEmailChangeRequest(this.emailChangeRequest.id).subscribe({
      next: () => {
        this.emailChangeRequest = null;
      },
      error: (err) => {
        console.error('[ProfileComponent] Failed to cancel email change request:', err);
        alert(this.formatApiError(err, 'Failed to cancel email change request.'));
      }
    });
  }

  private formatApiError(err: any, fallback: string): string {
    const message = err?.error?.message || fallback;
    if (err?.status === 404) {
      return `${message}. Restart the backend so the new email-change API is loaded.`;
    }
    return message;
  }

  loginHistory: any[] = [];
  recentActivities: any[] = [];

  uploadChartSeries: ApexNonAxisChartSeries = [0, 100];

  uploadChart: ApexChart = {
    type: 'donut',
    height: 260
  };

  uploadChartLabels = ['Successful', 'Failed'];
  uploadChartColors = ['#10b981', '#ef4444'];
  uploadChartLegend: ApexLegend = {
    position: 'bottom'
  };
  uploadChartDataLabels: ApexDataLabels = {
    enabled: true
  };
  uploadChartResponsive: ApexResponsive[] = [
    {
      breakpoint: 480,
      options: {
        chart: {
          width: 260
        },
        legend: {
          position: 'bottom'
        }
      }
    }
  ];
}

