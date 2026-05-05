import { Injectable } from '@angular/core';
import { CanActivate, ActivatedRouteSnapshot, Router, RouterStateSnapshot } from '@angular/router';
import { AuthService } from '../services/auth.service';

@Injectable({ providedIn: 'root' })
export class SuperAdminGuard implements CanActivate {
  constructor(private auth: AuthService, private router: Router) {}

  canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): boolean {
    const user = this.auth.getCurrentUser();
    if (this.auth.isAuthenticated() && (user?.role || '').toUpperCase() === 'SUPER_ADMIN') {
      return true;
    }
    this.router.navigate(['/dashboard']);
    return false;
  }
}

