import { Component, OnInit } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { AuthService } from '../../services/auth.service';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss']
})
export class LoginComponent implements OnInit {
  loading = false;
  oauthLoading = false;

  form = this.fb.group({
    login: ['', [Validators.required]],
    password: ['', [Validators.required, Validators.minLength(6)]]
  });

  constructor(
    private fb: FormBuilder,
    private auth: AuthService,
    private router: Router,
    private route: ActivatedRoute,
    private snack: MatSnackBar
  ) {}

  ngOnInit(): void {
    const error = this.route.snapshot.queryParamMap.get('error');
    if (error) {
      this.snack.open(this.normalizeAuthMessage(error), 'Dismiss', { duration: 3500 });
    }
  }

  submit() {
  if (this.form.invalid) return;
  this.loading = true;

  this.auth.login(this.form.value as any).subscribe({
    next: () => {
      this.snack.open('Welcome back!', 'OK', { duration: 2000 });

      // Get the returnUrl if it exists (e.g., if redirected from a specific page)
      const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl');

      // UPDATE: Change the fallback from '/files' to '/dashboard'
      this.router.navigateByUrl(returnUrl || '/dashboard');
    },
    error: (err) => {
      this.snack.open(this.normalizeAuthMessage(err?.error?.message), 'Dismiss', { duration: 3500 });
      this.loading = false;
    }
  });
}


  loginWithGoogle() {
    this.oauthLoading = true;
    window.location.href = `${environment.apiBaseUrl}/auth/oauth2/google`;
  }

  private normalizeAuthMessage(message?: string | null): string {
    const normalized = (message || '').trim().toLowerCase();
    if (normalized.includes('blocked')) {
      return 'User is blocked';
    }

    return message?.trim() || 'Login failed';
  }
}
