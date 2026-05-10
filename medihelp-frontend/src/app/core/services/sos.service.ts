import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/api-response.model';

export interface SosResponse {
  message: string;
  shareableLink?: string;
  contactsNotified?: number;
}

@Injectable({ providedIn: 'root' })
export class SosService {
  private base = `${environment.apiUrl}/api/v1/users/me/sos`;

  constructor(private http: HttpClient) {}

  trigger(latitude?: number, longitude?: number): Observable<SosResponse> {
    const body = (latitude !== undefined && longitude !== undefined)
      ? { latitude, longitude }
      : {};
    return this.http.post<ApiResponse<SosResponse>>(this.base, body).pipe(map(r => r.data));
  }
}
