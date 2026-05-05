import { Component, OnInit } from '@angular/core';
import { AuthService } from '../../services/auth.service';
import { FileLoadService } from '../../services/file-load.service';
import { User } from '../../models/user.model';
import { SearchCriteria } from '../../models/search-criteria.model';

@Component({
  selector: 'app-profile',
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.scss']
})
export class ProfileComponent implements OnInit {
  currentUser: User | null = null;
  isEditing = false;
  newName: string = '';
  profileImage: string = 'assets/default-avatar.svg';
  selectedFile: File | null = null;

  // Statistics
  totalFiles = 0;
  pendingFiles = 0;
  successFiles = 0;
  failedFiles = 0;
  successRate = 0;

  constructor(
    private auth: AuthService,
    private fileService: FileLoadService
  ) {}

  ngOnInit(): void {
    this.auth.currentUser$.subscribe(user => {
      this.currentUser = user || null;
      if (user) {
        this.newName = user.name || user.username || '';
        this.loadProfileImage();
        this.loadFileStatistics();
      } else {
        // Reset profile image to default on logout or no user
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

  loadFileStatistics(): void {
    // Fetch all files with default criteria
    const criteria: SearchCriteria = {
      page: 0,
      size: 1000  // Get all files
    };

    this.fileService.myList(criteria).subscribe(
      (result) => {
        const currentUserId = Number(this.currentUser?.id);
        const files = result.items.filter((file) => {
          const uploadedById = Number(file.uploadedById);
          if (!Number.isNaN(currentUserId) && !Number.isNaN(uploadedById)) {
            return uploadedById === currentUserId;
          }
          return false;
        });
        this.totalFiles = files.length;
        this.pendingFiles = files.filter(f => f.status === 'PENDING').length;
        this.successFiles = files.filter(f => f.status === 'SUCCESS').length;
        this.failedFiles = files.filter(f => f.status === 'FAILED').length;

        this.successRate = this.totalFiles > 0
          ? Math.round((this.successFiles / this.totalFiles) * 100)
          : 0;
      },
      (error) => {
        console.error('[ProfileComponent] Error loading file statistics:', error);
      }
    );
  }

  enableEdit(): void {
    this.isEditing = true;
  }

  cancelEdit(): void {
    this.isEditing = false;
    this.newName = this.currentUser?.name || this.currentUser?.username || '';
    this.selectedFile = null;
    this.loadProfileImage();
  }

 onFileSelected(event: any): void {
   const file: File = event.target.files[0];

   if (!file || !file.type.startsWith('image/')) {
     alert('Please select a valid image file');
     return;
   }

   // Limit size (optional but recommended)
   if (file.size > 2 * 1024 * 1024) {
     alert('Image should be less than 2MB');
     return;
   }

   this.selectedFile = file;

   // Show preview only until upload
   const reader = new FileReader();
   reader.onload = (e: any) => {
     this.profileImage = e.target.result; // base64 preview
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
          // Always fetch the latest profile from backend after upload
          this.auth.fetchProfile().subscribe({
            next: (user) => {
              this.currentUser = user;
              this.auth.updateUser(user); // <--- ensure all components get the update
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
    this.currentUser.name = this.newName;


    this.auth.updateUser(this.currentUser!);
    this.loadProfileImage();
  }

  this.isEditing = false;
  alert('Profile updated successfully!');
}}
