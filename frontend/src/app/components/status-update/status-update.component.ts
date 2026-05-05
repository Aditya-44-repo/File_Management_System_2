import { Component, EventEmitter, Inject, Input, OnChanges, Optional, Output } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { FileLoadService } from '../../services/file-load.service';

@Component({
  selector: 'app-status-update',
  templateUrl: './status-update.component.html',
  styleUrls: ['./status-update.component.scss']
})
export class StatusUpdateComponent implements OnChanges {
  @Input() fileId!: string;
  @Input() currentStatus!: string;
  @Output() updated = new EventEmitter<void>();

  form = this.fb.group({
    status: ['', Validators.required],
    comment: ['']
  });

  isDialog = false;
  submitting = false;

  constructor(
    private fb: FormBuilder,
    private api: FileLoadService,
    private snack: MatSnackBar,
    @Optional() private dialogRef: MatDialogRef<StatusUpdateComponent>,
    @Optional() @Inject(MAT_DIALOG_DATA) public dialogData: { fileId: string; currentStatus: string }
  ) {
    if (dialogData) {
      this.isDialog = true;
      this.fileId = dialogData.fileId;
      this.currentStatus = dialogData.currentStatus;
      this.form.patchValue({ status: dialogData.currentStatus });
    }
  }

  ngOnChanges() {
    if (this.currentStatus) {
      this.form.patchValue({ status: this.currentStatus });
    }
  }

  submit() {
    if (this.form.invalid || !this.fileId || this.submitting) return;
    this.submitting = true;
    const { status, comment } = this.form.value as any;
    this.api.updateStatus(this.fileId, status, comment).subscribe({
      next: () => {
        this.snack.open('Status updated', 'OK', { duration: 1500 });
        this.submitting = false;
        if (this.isDialog) {
          this.dialogRef.close(true);
        } else {
          this.updated.emit();
        }
      },
      error: () => {
        this.submitting = false;
        this.snack.open('Failed to update status', 'Dismiss', { duration: 3000 });
      }
    });
  }

  cancel() {
    if (this.dialogRef) this.dialogRef.close(false);
  }
}