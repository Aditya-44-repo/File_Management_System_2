import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivate, Router, RouterStateSnapshot } from '@angular/router';
import { AuthService } from '../services/auth.service';

@Injectable({ providedIn: 'root' })
export class AdminScopeGuard implements CanActivate {
  constructor(private auth: AuthService, private router: Router) {}

  canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): boolean {
    if (this.auth.hasAnyAdminPermission('USER_ACCESS_CONTROL', 'USER_RECORDS_OVERVIEW', 'USER_FILES_DELETE_ALL') || (this.auth.getCurrentUser()?.role || '').toUpperCase() === 'SUPER_ADMIN') {
      return true;
    }
    this.router.navigate(['/dashboard']);
    return false;
  }
}

