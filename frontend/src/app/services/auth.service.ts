import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { Router } from '@angular/router';
import { environment } from '../../environments/environment';
import { User } from '../models/user.model';
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly KEY = 'fl_user';
  private readonly DEFAULT_AVATAR = 'assets/default-avatar.svg';

  private currentUserSubject = new BehaviorSubject<User | null>(this.loadUser());
  currentUser$ = this.currentUserSubject.asObservable();

  constructor(private http: HttpClient, private router: Router) {}

  private loadUser(): User | null {
    const raw = localStorage.getItem(this.KEY);
    try {
      return raw ? JSON.parse(raw) as User : null;
    } catch {
      return null;
    }
  }

  private saveUser(user: User | null) {
    if (user) localStorage.setItem(this.KEY, JSON.stringify(user));
    else localStorage.removeItem(this.KEY);
  }

  // ✅ NEW METHOD (IMPORTANT)
  updateUser(user: User) {
    this.saveUser(user);
    this.currentUserSubject.next(user);
  }

  getCurrentUser(): User | null {
    return this.currentUserSubject.value;
  }

  login(payload: { login: string; password: string }): Observable<User> {
    console.log('[AuthService] Login attempt for:', payload.login);
    return this.http.post<User>(`${environment.apiBaseUrl}/auth/login`, payload).pipe(
      tap({
        next: (user) => {
          console.log('[AuthService] Login successful for:', user.email || user.username);
          this.saveUser(user);
          this.currentUserSubject.next(user);
        },
        error: (err) => {
          console.error('[AuthService] Login failed for:', payload.login, err);
        }
      })
    );
  }

  register(payload: { name: string; email: string; password: string }): Observable<User> {
    console.log('[AuthService] Register attempt for:', payload.email);
    const body = {
      username: payload.name,
      email: payload.email,
      password: payload.password
    };
    return this.http.post<User>(`${environment.apiBaseUrl}/auth/register`, body).pipe(
      tap({
        next: (user) => console.log('[AuthService] Registration successful for:', user.email || user.username),
        error: (err) => console.error('[AuthService] Registration failed for:', payload.email, err)
      })
    );
  }

  logout() {
    console.log('[AuthService] Logging out user:', this.currentUserSubject.value?.email || this.currentUserSubject.value?.username);
    this.saveUser(null);
    this.currentUserSubject.next(null);
    this.router.navigate(['/']);
  }

  isAuthenticated(): boolean {
    return !!this.getToken();
  }

  hasAdminPermission(...permissions: string[]): boolean {
    const current = this.currentUserSubject.value;
    if (!current) {
      return false;
    }

    if ((current.role || '').toUpperCase() === 'SUPER_ADMIN') {
      return true;
    }

    const granted = (current.adminPermissions || '')
      .split(',')
      .map((value) => value.trim())
      .filter(Boolean);

    return permissions.some((permission) => granted.includes(permission));
  }

  hasAnyAdminPermission(...permissions: string[]): boolean {
    return this.hasAdminPermission(...permissions);
  }

  getToken(): string | null {
    return this.currentUserSubject.value?.token ?? null;
  }

  // ✅ FIXED (no hardcoded URL)
  uploadProfileImage(formData: FormData) {
    return this.http.post(`${environment.apiBaseUrl}/auth/upload-profile`, formData);
  }

  // Fetch the latest user profile from the backend (including profileImage)
  fetchProfile(): Observable<User> {
    return this.http.get<User>(`${environment.apiBaseUrl}/auth/profile`).pipe(
      tap({
        next: (user) => {
          this.saveUser(user);
          this.currentUserSubject.next(user);
        },
        error: (err) => {
          console.error('[AuthService] Failed to fetch user profile:', err);
        }
      })
    );
  }

  inviteAdmin(payload: { email: string; permissions: string[] }) {
    return this.http.post<{ email: string; generatedPassword: string; token: string; expiresAt: string; inviteLink: string }>(
      `${environment.apiBaseUrl}/super-admin/admin-invites`,
      payload
    );
  }

  validateAdminInvite(token: string) {
    return this.http.get<{ valid: boolean }>(
      `${environment.apiBaseUrl}/super-admin/admin-invites/${encodeURIComponent(token)}/validate`
    );
  }

  acceptAdminInvite(token: string) {
    return this.http.post(`${environment.apiBaseUrl}/super-admin/admin-invites/accept`, { token });
  }

  /**
   * Returns the correct profile image URL for a user, with cache-busting.
   * Falls back to default avatar if not set.
   */
  getProfileImageUrl(user: User | null): string {
    const profileImage = user?.profileImage?.trim();

    if (profileImage) {
      return this.addCacheBuster(this.resolveProfileImageUrl(profileImage));
    }

    return this.getProfileImageFallback(user);
  }

  getProfileImageFallback(user: User | null): string {
    return this.buildRoleAwareAvatar(user);
  }

  private resolveProfileImageUrl(profileImage: string): string {
    if (/^(data:|blob:|https?:)/i.test(profileImage)) {
      return profileImage;
    }

    const protocol = window.location.protocol;
    const port = protocol === 'https:' ? '8080' : '8082';
    const normalizedPath = profileImage.startsWith('/') ? profileImage : `/${profileImage}`;
    return `${protocol}//localhost:${port}${normalizedPath}`;
  }

  private addCacheBuster(url: string): string {
    if (/^(data:|blob:)/i.test(url)) {
      return url;
    }

    const separator = url.includes('?') ? '&' : '?';
    return `${url}${separator}t=${Date.now()}`;
  }

  private buildRoleAwareAvatar(user: User | null): string {
    const role = (user?.role || '').toUpperCase();
    const label = this.getAvatarLabel(user);
    const palette = this.getAvatarPalette(role);
    const svg = `
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 128 128" role="img" aria-label="${this.escapeXml(this.getAvatarDescription(role))}">
        <defs>
          <linearGradient id="avatarGradient" x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" stop-color="${palette.start}" />
            <stop offset="100%" stop-color="${palette.end}" />
          </linearGradient>
        </defs>
        <rect width="128" height="128" rx="64" fill="url(#avatarGradient)" />
        <circle cx="64" cy="46" r="22" fill="rgba(255,255,255,0.18)" />
        <path d="M28 112c5-19 20-31 36-31s31 12 36 31" fill="rgba(255,255,255,0.18)" />
        <text x="64" y="57" text-anchor="middle" font-family="Arial, Helvetica, sans-serif" font-size="30" font-weight="700" fill="${palette.text}">${this.escapeXml(label)}</text>
        <text x="64" y="92" text-anchor="middle" font-family="Arial, Helvetica, sans-serif" font-size="12" font-weight="700" letter-spacing="1.6" fill="${palette.text}" opacity="0.88">${this.escapeXml(this.getAvatarDescription(role))}</text>
      </svg>`;

    return `data:image/svg+xml;charset=UTF-8,${encodeURIComponent(svg.trim())}`;
  }

  private getAvatarLabel(user: User | null): string {
    const role = (user?.role || '').toUpperCase();
    if (role === 'SUPER_ADMIN') {
      return 'SA';
    }

    if (role === 'ADMIN') {
      return 'AD';
    }

    const source = (user?.name || user?.username || user?.email || 'User').trim();
    const initials = source
      .split(/\s+/)
      .filter(Boolean)
      .slice(0, 2)
      .map((part) => part.charAt(0))
      .join('');

    return (initials || 'U').toUpperCase();
  }

  private getAvatarPalette(role: string): { start: string; end: string; text: string } {
    if (role === 'SUPER_ADMIN') {
      return { start: '#5b21b6', end: '#8b5cf6', text: '#ffffff' };
    }

    if (role === 'ADMIN') {
      return { start: '#0f766e', end: '#14b8a6', text: '#ffffff' };
    }

    return { start: '#334155', end: '#64748b', text: '#ffffff' };
  }

  private getAvatarDescription(role: string): string {
    if (role === 'SUPER_ADMIN') {
      return 'Super Admin';
    }

    if (role === 'ADMIN') {
      return 'Admin';
    }

    return 'User';
  }

  private escapeXml(value: string): string {
    return value
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&apos;');
  }
}