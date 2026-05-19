import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-sidebar',
  templateUrl: './sidebar.component.html',
  styleUrls: ['./sidebar.component.scss']
})
export class SidebarComponent implements OnInit {
  isOpen: boolean = false;
  activeRoute: string = '';
  currentUserRole: string = '';
  isLoggedIn: boolean = false;
  isDashboardRoute: boolean = false;

  menuItems = [
    { label: 'Dashboard', icon: 'dashboard', route: '/dashboard' },
    { label: 'Upload File', icon: 'cloud_upload', route: '/upload' },
    { label: 'Reports', icon: 'assessment', route: '/files' },
    { label: 'Settings', icon: 'settings', route: '/profile' }
  ];

  constructor(
    private router: Router,
    private auth: AuthService
  ) {
    this.router.events.subscribe(() => {
      this.activeRoute = this.router.url;
      this.isDashboardRoute = this.activeRoute.startsWith('/dashboard');
    });
  }

  ngOnInit(): void {
    this.auth.currentUser$.subscribe((user) => {
      this.isLoggedIn = !!user;
      if (user) {
        this.currentUserRole = user.role || 'USER';
      }
    });
  }

  toggleSidebar(): void {
    this.isOpen = !this.isOpen;
  }

  closeSidebar(): void {
    this.isOpen = false;
  }

  navigateTo(route: string): void {
    this.router.navigate([route]);
    this.closeSidebar();
  }

  isActive(route: string): boolean {
    return this.activeRoute.includes(route);
  }

  logout(): void {
    this.auth.logout();
    this.closeSidebar();
  }
}
