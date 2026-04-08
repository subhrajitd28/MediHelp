import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatListModule,
    MatIconModule
  ],
  templateUrl: './sidebar.component.html',
  styleUrl: './sidebar.component.scss'
})
export class SidebarComponent {
  navItems = [
    { label: 'Dashboard', icon: 'dashboard', route: '/dashboard' },
    { label: 'Vitals', icon: 'monitor_heart', route: '/vitals' },
    { label: 'Medications', icon: 'medication', route: '/medications' },
    { label: 'Appointments', icon: 'calendar_month', route: '/appointments' },
    { label: 'AI Health Chat', icon: 'smart_toy', route: '/ai-chat' },
    { label: 'Scan Prescription', icon: 'document_scanner', route: '/scan' },
    { label: 'Prescriptions', icon: 'description', route: '/prescriptions' },
    { label: 'Health Records', icon: 'folder_shared', route: '/health-records' },
    { label: 'Profile', icon: 'person', route: '/profile' },
    { label: 'Notifications', icon: 'notifications', route: '/notifications' }
  ];
}
