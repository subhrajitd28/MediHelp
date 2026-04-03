import { Injectable } from '@angular/core';
import { HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiService } from './api.service';
import { NotificationResponse } from '../models/prescription.model';
import { PagedResponse } from '../models/api-response.model';

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private path = '/api/v1/notifications';

  constructor(private api: ApiService) {}

  getNotifications(page = 0, size = 20): Observable<PagedResponse<NotificationResponse>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.api.get<PagedResponse<NotificationResponse>>(this.path, params);
  }

  getUnread(): Observable<NotificationResponse[]> {
    return this.api.get<NotificationResponse[]>(`${this.path}/unread`);
  }

  getUnreadCount(): Observable<number> {
    return this.api.get<number>(`${this.path}/unread-count`);
  }

  markAsRead(id: string): Observable<any> {
    return this.api.patch<any>(`${this.path}/${id}/read`);
  }

  markAllAsRead(): Observable<any> {
    return this.api.patch<any>(`${this.path}/read-all`);
  }
}
