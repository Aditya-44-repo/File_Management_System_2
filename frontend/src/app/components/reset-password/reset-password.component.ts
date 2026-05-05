import { Component, OnInit } from '@angular/core';
import { FormBuilder, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { PasswordResetService } from '../../services/password-reset.service';

@Component({
  selector: 'app-reset-password',
  templateUrl: './reset-password.component.html',
  styleUrls: ['./reset-password.component.scss']
})
export class ResetPasswordComponent implements OnInit {
  loading = false;
  validating = true;
  tokenValid = false;
  resetSuccess = false;
  token = '';
  hidePassword = true;
  hideConfirmPassword = true;

  form = this.fb.group({
    newPassword: ['', [Validators.required, Validators.minLength(6)]],
    confirmPassword: ['', [Validators.required]]
  }, { validators: this.passwordMatchValidator.bind(this) });

  constructor(
    private fb: FormBuilder,
    private passwordResetService: PasswordResetService,
    private route: ActivatedRoute,
    private router: Router,
    private snack: MatSnackBar
  ) {}

  ngOnInit() {
    this.token = this.route.snapshot.queryParamMap.get('token') || '';

    if (!this.token) {
      this.snack.open('Invalid reset link', 'Dismiss', { duration: 3000 });
      this.router.navigate(['/login']);
      return;
    }

    this.validateToken();
  }

  validateToken() {
    this.validating = true;
    this.passwordResetService.validateResetToken(this.token).subscribe({
      next: (response) => {
        this.validating = false;
        if (response.success) {
          this.tokenValid = true;
        } else {
          this.snack.open('Reset link is invalid or expired', 'Dismiss', { duration: 3000 });
          this.router.navigate(['/forgot-password']);
        }
      },
      error: (err) => {
        this.validating = false;
        this.snack.open('Reset link is invalid or expired', 'Dismiss', { duration: 3000 });
        this.router.navigate(['/forgot-password']);
      }
    });
  }

  submit() {
    if (this.form.invalid) return;

    this.loading = true;
    const newPassword = this.form.get('newPassword')?.value as string;

    this.passwordResetService.resetPassword(this.token, newPassword).subscribe({
      next: (response) => {
        this.loading = false;
        this.resetSuccess = true;
        this.snack.open('Password reset successfully!', 'OK', { duration: 3000 });
        setTimeout(() => this.router.navigate(['/login']), 2000);
      },
      error: (err) => {
        this.loading = false;
        this.snack.open(err?.error?.message ?? 'Failed to reset password', 'Dismiss', { duration: 3500 });
      }
    });
  }

  backToLogin() {
    this.router.navigate(['/login']);
  }

  private passwordMatchValidator(group: AbstractControl): ValidationErrors | null {
    const password = group.get('newPassword')?.value;
    const confirmPassword = group.get('confirmPassword')?.value;
    return password === confirmPassword ? null : { passwordMismatch: true };
  }
}