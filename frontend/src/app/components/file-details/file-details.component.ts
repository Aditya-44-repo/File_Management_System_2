import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FormBuilder } from '@angular/forms';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatDialog } from '@angular/material/dialog';
import { FileLoadService } from '../../services/file-load.service';
import { FileItem } from '../../models/file-load.model';
import { StatusUpdateComponent } from '../status-update/status-update.component';

@Component({
  selector: 'app-file-details',
  templateUrl: './file-details.component.html',
  styleUrls: ['./file-details.component.scss']
})
export class FileDetailsComponent implements OnInit {
  id!: string;
  file?: FileItem;
  saving = false;
  loading = true;
  isMetadataEditing = false;

  form = this.fb.group({
    description: [''],
    tags: ['']
  });

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private api: FileLoadService,
    private fb: FormBuilder,
    private snack: MatSnackBar,
    private dialog: MatDialog
  ) {}

  ngOnInit(): void {
    this.id = this.route.snapshot.params['id'];
    this.load();
  }

  load() {
    this.loading = true;
    this.api.details(this.id).subscribe({
      next: (f) => {
        this.file = f;
        this.form.patchValue({
          description: f.description || '',
          tags: (f.tags || []).join(', ')
        });
        this.form.disable({ emitEvent: false });
        this.isMetadataEditing = false;
        this.loading = false;
      },
      error: () => {
        this.snack.open('Failed to load file', 'Dismiss', { duration: 3000 });
        this.loading = false;
      }
    });
  }

  startMetadataEdit() {
    if (!this.file) return;
    this.isMetadataEditing = true;
    this.form.enable({ emitEvent: false });
  }

  cancelMetadataEdit() {
    if (!this.file) return;

    this.form.patchValue({
      description: this.file.description || '',
      tags: (this.file.tags || []).join(', ')
    });

    this.form.disable({ emitEvent: false });
    this.isMetadataEditing = false;
  }

  save() {
    if (!this.file || !this.isMetadataEditing) return;
    this.saving = true;
    const { description, tags } = this.form.value;
    const body: Partial<FileItem> = {
      description: description || undefined,
      tags: (tags || '').split(',').map((t: string) => t.trim()).filter(Boolean)
    };
    this.api.updateMetadata(this.file.id, body).subscribe({
      next: (res) => {
        this.snack.open('Saved successfully', 'OK', { duration: 1500 });
        this.file = res;
        this.form.patchValue({
          description: res.description || '',
          tags: (res.tags || []).join(', ')
        });
        this.form.disable({ emitEvent: false });
        this.isMetadataEditing = false;
        this.saving = false;
      },
      error: () => {
        this.snack.open('Save failed', 'Dismiss', { duration: 3000 });
        this.saving = false;
      }
    });
  }

  openStatusDialog() {
    if (!this.file) return;
    this.dialog.open(StatusUpdateComponent, {
      width: '420px',
      data: { fileId: this.file.id, currentStatus: this.file.status }
    }).afterClosed().subscribe((updated) => {
      if (updated) this.load();
    });
  }


  download() {
    if (!this.file) return;
    this.api.download(this.file.id).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = this.fileName;
        document.body.appendChild(a);
        a.click();
        a.remove();
        URL.revokeObjectURL(url);
        this.snack.open('Download started', 'OK', { duration: 1500 });
      },
      error: () => this.snack.open('Download failed', 'Dismiss', { duration: 3000 })
    });
  }

  delete() {
    if (!this.file || !confirm(`Delete "${this.fileName}"? This cannot be undone.`)) return;
    this.api.delete(this.file.id).subscribe({
      next: () => {
        this.snack.open('File deleted', 'OK', { duration: 1500 });
        this.router.navigate(['/files']);
      },
      error: () => this.snack.open('Delete failed', 'Dismiss', { duration: 3000 })
    });
  }

  get fileName(): string {
    return this.file?.filename || this.file?.name || 'Unknown file';
  }

  get uploadDate(): string {
    return this.file?.uploadDate || this.file?.uploadedAt || '';
  }

  get fileType(): string {
    return this.file?.fileType || this.file?.mimeType || '—';
  }

  formatSize(bytes: number): string {
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1048576) return (bytes / 1024).toFixed(1) + ' KB';
    return (bytes / 1048576).toFixed(1) + ' MB';
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
}