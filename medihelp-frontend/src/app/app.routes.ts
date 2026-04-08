import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./features/landing/landing.component').then(m => m.LandingComponent),
    pathMatch: 'full'
  },
  {
    path: 'login',
    loadComponent: () =>
      import('./features/auth/login/login.component').then(m => m.LoginComponent)
  },
  {
    path: 'register',
    loadComponent: () =>
      import('./features/auth/register/register.component').then(m => m.RegisterComponent)
  },
  {
    path: 'dashboard',
    loadComponent: () =>
      import('./features/dashboard/dashboard.component').then(m => m.DashboardComponent),
    canActivate: [authGuard]
  },
  {
    path: 'vitals',
    loadComponent: () =>
      import('./features/vitals/vitals.component').then(m => m.VitalsComponent),
    canActivate: [authGuard]
  },
  {
    path: 'medications',
    loadComponent: () =>
      import('./features/medications/medications.component').then(m => m.MedicationsComponent),
    canActivate: [authGuard]
  },
  {
    path: 'appointments',
    loadComponent: () =>
      import('./features/appointments/appointments.component').then(m => m.AppointmentsComponent),
    canActivate: [authGuard]
  },
  {
    path: 'profile',
    loadComponent: () =>
      import('./features/profile/profile.component').then(m => m.ProfileComponent),
    canActivate: [authGuard]
  },
  {
    path: 'notifications',
    loadComponent: () =>
      import('./features/notifications/notifications.component').then(m => m.NotificationsComponent),
    canActivate: [authGuard]
  },
  {
    path: 'ai-chat',
    loadComponent: () =>
      import('./features/ai-chat/ai-chat.component').then(m => m.AiChatComponent),
    canActivate: [authGuard]
  },
  {
    path: 'scan',
    loadComponent: () =>
      import('./features/prescription-scan/prescription-scan.component').then(m => m.PrescriptionScanComponent),
    canActivate: [authGuard]
  },
  {
    path: 'prescriptions',
    loadComponent: () =>
      import('./features/prescriptions/prescriptions.component').then(m => m.PrescriptionsComponent),
    canActivate: [authGuard]
  },
  {
    path: 'health-records',
    loadComponent: () =>
      import('./features/health-records/health-records.component').then(m => m.HealthRecordsComponent),
    canActivate: [authGuard]
  },
  { path: '**', redirectTo: '/' }
];
