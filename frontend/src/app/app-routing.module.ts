import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { DashboardComponent } from './components/dashboard/dashboard.component';
import { LoginComponent } from './components/login/login.component';
import { RegisterComponent } from './components/register/register.component';
import { ForgotPasswordComponent } from './components/forgot-password/forgot-password.component';
import { ResetPasswordComponent } from './components/reset-password/reset-password.component';
import { FileListComponent } from './components/file-list/file-list.component';
import { FileUploadComponent } from './components/file-upload/file-upload.component';
import { FileDetailsComponent } from './components/file-details/file-details.component';
import { ProfileComponent } from './components/profile/profile.component';
import { OauthCallbackComponent } from './components/oauth-callback/oauth-callback.component';
import { AdminInviteComponent } from './components/admin-invite/admin-invite.component';
import { AdminUsersComponent } from './components/admin-users/admin-users.component';
import { AuthGuard } from './guards/auth.guard';
import { AdminScopeGuard } from './guards/admin-scope.guard';
import { SuperAdminGuard } from './guards/super-admin.guard';

const routes: Routes = [
  { path: '', redirectTo: 'dashboard', pathMatch: 'full' },

  { path: 'login', component: LoginComponent },
  { path: 'oauth/callback', component: OauthCallbackComponent },
  { path: 'register', component: RegisterComponent },
  { path: 'forgot-password', component: ForgotPasswordComponent },
  { path: 'reset-password', component: ResetPasswordComponent },

  { path: 'dashboard', component: DashboardComponent },
  { path: 'admin/users', component: AdminUsersComponent, canActivate: [AdminScopeGuard] },
  { path: 'super-admin/admin-invites', component: AdminInviteComponent, canActivate: [SuperAdminGuard] },
  { path: 'admin-invite', component: AdminInviteComponent },

  { path: 'files', component: FileListComponent, canActivate: [AuthGuard] },
  { path: 'files/:id', component: FileDetailsComponent, canActivate: [AuthGuard] },
  { path: 'upload', component: FileUploadComponent, canActivate: [AuthGuard] },
  { path: 'profile', component: ProfileComponent, canActivate: [AuthGuard] },

  { path: '**', redirectTo: 'profile' }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule {}