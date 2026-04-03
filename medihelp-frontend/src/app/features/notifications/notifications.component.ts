import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatChipsModule } from '@angular/material/chips';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { NotificationService } from '../../core/services/notification.service';
import { NotificationResponse } from '../../core/models/prescription.model';

@Component({
  selector: 'app-notifications',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatListModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatChipsModule,
    MatPaginatorModule
  ],
  templateUrl: './notifications.component.html',
  styleUrl: './notifications.component.scss'
})
export class NotificationsComponent implements OnInit {
  notifications: NotificationResponse[] = [];
  loading = true;
  totalElements = 0;
  pageSize = 20;
  pageIndex = 0;

  categoryIcons: { [key: string]: string } = {
    WELCOME: 'waving_hand',
    MEDICATION_REMINDER: 'medication',
    VITALS_ALERT: 'monitor_heart',
    APPOINTMENT_REMINDER: 'calendar_month',
    HEALTH_TIP: 'tips_and_updates',
    STREAK: 'local_fire_department'
  };

  categoryColors: { [key: string]: string } = {
    WELCOME: '#4caf50',
    MEDICATION_REMINDER: '#2196f3',
    VITALS_ALERT: '#f44336',
    APPOINTMENT_REMINDER: '#ff9800',
    HEALTH_TIP: '#9c27b0',
    STREAK: '#ff5722'
  };

  constructor(
    private notificationService: NotificationService,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.loadNotifications();
  }

  loadNotifications(): void {
    this.loading = true;
    this.notificationService.getNotifications(this.pageIndex, this.pageSize).subscribe({
      next: page => {
        this.notifications = page.content;
        this.totalElements = page.totalElements;
        this.loading = false;
      },
      error: () => {
        this.notifications = [];
        this.loading = false;
      }
    });
  }

  markAsRead(notification: NotificationResponse): void {
    if (notification.read) return;

    this.notificationService.markAsRead(notification.id).subscribe({
      next: () => {
        notification.read = true;
      },
      error: () => {}
    });
  }

  markAllAsRead(): void {
    this.notificationService.markAllAsRead().subscribe({
      next: () => {
        this.notifications.forEach(n => n.read = true);
        this.snackBar.open('All notifications marked as read.', 'Close', { duration: 3000 });
      },
      error: (err) => {
        this.snackBar.open(err.error?.message || 'Failed to mark all as read.', 'Close', { duration: 5000 });
      }
    });
  }

  onPageChange(event: PageEvent): void {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadNotifications();
  }

  getCategoryIcon(category: string): string {
    return this.categoryIcons[category] || 'notifications';
  }

  getCategoryColor(category: string): string {
    return this.categoryColors[category] || '#666';
  }
}
