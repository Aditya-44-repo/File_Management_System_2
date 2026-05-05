import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-oauth-callback',
  templateUrl: './oauth-callback.component.html',
  styleUrls: ['./oauth-callback.component.scss']
})
export class OauthCallbackComponent implements OnInit {
  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private snack: MatSnackBar,
    private auth: AuthService
  ) {}

  ngOnInit(): void {
    const token = this.route.snapshot.queryParamMap.get('token');
    const email = this.route.snapshot.queryParamMap.get('email') || '';
    const username = this.route.snapshot.queryParamMap.get('username') || email;
    const id = Number(this.route.snapshot.queryParamMap.get('id') || 0);
    const role = this.route.snapshot.queryParamMap.get('role') || 'USER';
    const adminPermissions = this.route.snapshot.queryParamMap.get('adminPermissions') || '';

    if (!token || !email) {
      this.snack.open('Google login failed. Please try again.', 'Dismiss', { duration: 3500 });
      this.router.navigate(['/login'], { replaceUrl: true });
      return;
    }

    // Store a minimal user with token so we can make authenticated requests
    this.auth.updateUser({
      id,
      email,
      username,
      role,
        adminPermissions,
      token
    });

    // Now fetch the full user profile (including profileImage)
    this.auth.fetchProfile().subscribe({
      next: () => {
        this.snack.open('Google login successful', 'OK', { duration: 2000 });
        this.router.navigate(['/files'], { replaceUrl: true });
      },
      error: () => {
        this.snack.open('Google login failed to fetch profile.', 'Dismiss', { duration: 3500 });
        this.router.navigate(['/login'], { replaceUrl: true });
      }
    });
  }
}
