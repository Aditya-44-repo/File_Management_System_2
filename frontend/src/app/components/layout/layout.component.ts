import { Component, OnInit } from '@angular/core';
import { Router, NavigationEnd } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { filter } from 'rxjs/operators';

@Component({
  selector: 'app-layout',
  templateUrl: './layout.component.html',
  styleUrls: ['./layout.component.scss']
})
export class LayoutComponent implements OnInit {
  sidebarOpen: boolean = false;
  isLoggedIn: boolean = false;
  currentRoute: string = '';
  currentUserRole: string = '';

  menuItems = [
    { label: 'Dashboard', icon: 'dashboard', route: '/dashboard', roles: ['USER', 'ADMIN'] },
    { label: 'Upload File', icon: 'cloud_upload', route: '/upload', roles: ['USER', 'ADMIN'] },
    { label: 'Files', icon: 'folder_open', route: '/files', roles: ['USER', 'ADMIN'] },
    { label: 'Reports', icon: 'assessment', route: '/files', roles: ['USER', 'ADMIN'] },
    { label: 'Settings', icon: 'settings', route: '/profile', roles: ['USER', 'ADMIN'] },
    { label: 'Admin Panel', icon: 'admin_panel_settings', route: '/admin/users', roles: ['ADMIN'] }
  ];

  constructor(
    private router: Router,
    private auth: AuthService
  ) {
    // Track route changes
    this.router.events
      .pipe(filter(event => event instanceof NavigationEnd))
      .subscribe((event: any) => {
        this.currentRoute = event.url;
        // Auto-close sidebar on mobile after navigation
        if (window.innerWidth < 768) {
          this.closeSidebar();
        }
      });
  }

  ngOnInit(): void {
    // Subscribe to authentication state
    this.auth.currentUser$.subscribe((user) => {
      this.isLoggedIn = !!user;
      if (user) {
        this.currentUserRole = user.role || 'USER';
      }
    });

    // Handle window resize
    window.addEventListener('resize', () => {
      if (window.innerWidth >= 768) {
        this.closeSidebar();
      }
    });
  }

  toggleSidebar(): void {
    this.sidebarOpen = !this.sidebarOpen;
  }

  closeSidebar(): void {
    this.sidebarOpen = false;
  }

  openSidebar(): void {
    this.sidebarOpen = true;
  }

  isActive(route: string): boolean {
    return this.currentRoute.includes(route);
  }

  navigateTo(route: string): void {
    this.router.navigate([route]);
  }

  isMenuItemVisible(item: any): boolean {
    if (!this.isLoggedIn) return false;
    return item.roles.includes(this.currentUserRole);
  }

  logout(): void {
    this.auth.logout();
    this.closeSidebar();
  }
}
