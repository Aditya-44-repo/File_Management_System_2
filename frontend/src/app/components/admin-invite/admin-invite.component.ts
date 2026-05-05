import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { AuthService } from '../../services/auth.service';
import { User } from '../../models/user.model';

@Component({
  selector: 'app-admin-invite',
  templateUrl: './admin-invite.component.html',
  styleUrls: ['./admin-invite.component.scss']
})
export class AdminInviteComponent implements OnInit {
  inviteForm: FormGroup;
  acceptToken = '';
  currentUser: User | null = null;
  inviteResult: { inviteLink: string; email: string; expiresAt: string; generatedPassword: string } | null = null;
  inviteTokenStatus: 'idle' | 'checking' | 'valid' | 'invalid' = 'idle';
  loading = false;
  message = '';
  error = '';

  readonly permissionOptions = [
    { key: 'USER_ACCESS_CONTROL', label: 'Block or unblock users' },
    { key: 'USER_RECORDS_OVERVIEW', label: 'View users records and file counts' },
    { key: 'USER_FILES_DELETE_ALL', label: 'Delete all files of a user' }
  ];

  constructor(
    private fb: FormBuilder,
    private auth: AuthService,
    private route: ActivatedRoute
  ) {
    this.inviteForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      permissions: this.fb.group({
        USER_ACCESS_CONTROL: [true],
        USER_RECORDS_OVERVIEW: [false],
        USER_FILES_DELETE_ALL: [false]
      })
    });
  }

  ngOnInit(): void {
    this.currentUser = this.auth.getCurrentUser();
    this.route.queryParamMap.subscribe((params) => {
      const token = params.get('token');
      if (token) {
        this.acceptToken = token;
        this.checkToken(token);
      }
    });
  }

  get isSuperAdmin(): boolean {
    return (this.currentUser?.role || '').toUpperCase() === 'SUPER_ADMIN';
  }

  createInvite(): void {
    if (this.inviteForm.invalid) {
      this.error = 'Please fill email and at least one permission.';
      return;
    }

    const permissions = Object.entries(this.inviteForm.value.permissions || {})
      .filter(([, enabled]) => !!enabled)
      .map(([key]) => key);

    if (!permissions.length) {
      this.error = 'Select at least one permission.';
      return;
    }

    this.loading = true;
    this.error = '';
    this.message = '';

    this.auth.inviteAdmin({
      email: this.inviteForm.value.email,
      permissions
    }).pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: (result) => {
          this.inviteResult = {
            inviteLink: result.inviteLink,
            email: result.email,
            expiresAt: result.expiresAt,
            generatedPassword: result.generatedPassword
          };
          this.message = 'Invite sent successfully.';
        },
        error: (err) => {
          this.error = err?.error?.message || 'Failed to send invite.';
        }
      });
  }

  acceptInvite(): void {
    if (!this.acceptToken) {
      this.error = 'Missing invite token.';
      return;
    }

    this.loading = true;
    this.error = '';
    this.message = '';

    this.auth.acceptAdminInvite(this.acceptToken)
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: () => {
          this.message = 'Invite accepted. You can now login as admin.';
          this.inviteTokenStatus = 'valid';
        },
        error: (err) => {
          this.error = err?.error?.message || 'Invite acceptance failed.';
          this.inviteTokenStatus = 'invalid';
        }
      });
  }

  private checkToken(token: string): void {
    this.inviteTokenStatus = 'checking';
    this.auth.validateAdminInvite(token).subscribe({
      next: (res) => {
        this.inviteTokenStatus = res.valid ? 'valid' : 'invalid';
      },
      error: () => {
        this.inviteTokenStatus = 'invalid';
      }
    });
  }
}


