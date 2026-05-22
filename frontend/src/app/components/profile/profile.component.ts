import { Component, OnInit } from '@angular/core';
import { AuthService } from '../../services/auth.service';
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
  newPassword: string = '';
  confirmPassword: string = '';
  profileImage: string = 'assets/default-avatar.svg';
  selectedFile: File | null = null;

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

        if (data.recentActivities) {
          this.recentActivities = data.recentActivities.map((act: any) => ({
            icon: act.icon === 'upload' ? 'cloud_upload' : act.icon,
            text: act.text,
            time: this.formatTime(act.timestamp)
          }));
        }

        if (data.loginHistory) {
          this.loginHistory = data.loginHistory.map((log: any) => ({
            device: log.device,
            browser: log.browser,
            ip: log.ip,
            time: this.formatLoginTime(log.time),
            status: log.status === 'SUCCESS' ? 'Success' : (log.status === 'FAILED' ? 'Failed' : log.status)
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
    this.newPassword = '';
    this.selectedFile = null;
    this.loadProfileImage();
  }

  onFileSelected(event: any): void {
    const file: File = event.target.files[0];

    if (!file || !file.type.startsWith('image/')) {
      alert('Please select a valid image file');
      return;
    }

    if (file.size > 2 * 1024 * 1024) {
      alert('Image should be less than 2MB');
      return;
    }

    this.selectedFile = file;

    const reader = new FileReader();
    reader.onload = (e: any) => {
      this.profileImage = e.target.result;
    };
    reader.readAsDataURL(file);
  }

  saveProfile(): void {
    if (!this.currentUser) return;

    if (this.selectedFile) {
      const formData = new FormData();
      formData.append('file', this.selectedFile);
      formData.append('userId', this.currentUser.id.toString());

      this.auth.uploadProfileImage(formData).subscribe({
        next: (res: any) => {
          this.auth.fetchProfile().subscribe({
            next: (user) => {
              this.currentUser = user;
              this.auth.updateUser(user);
              this.loadProfileImage();
              this.finishProfileUpdate();
            },
            error: () => {
              this.loadProfileImage();
              this.finishProfileUpdate();
            }
          });
        },
        error: () => {
          this.loadProfileImage();
          this.finishProfileUpdate();
        }
      });
    } else {
      this.finishProfileUpdate();
    }
  }

  finishProfileUpdate(): void {
    if (this.newName.trim() && this.currentUser) {
      if (this.newPassword && this.newPassword !== this.confirmPassword) {
        alert('Password and Confirm Password do not match.');
        return;
      }
      this.auth.updateProfileDetails(this.newName.trim(), this.newPassword || undefined).subscribe({
        next: (updatedUser) => {
          this.currentUser = updatedUser;
          this.currentUser.name = updatedUser.username; // Bind frontend 'name' to the updated username
          this.auth.updateUser(this.currentUser);
          this.loadProfileImage();
          this.isEditing = false;
          this.newPassword = '';
          this.confirmPassword = '';
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

