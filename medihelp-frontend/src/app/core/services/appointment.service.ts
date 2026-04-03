import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from './api.service';
import { AppointmentRequest, AppointmentResponse } from '../models/prescription.model';

@Injectable({ providedIn: 'root' })
export class AppointmentService {
  private path = '/api/v1/prescriptions/appointments';

  constructor(private api: ApiService) {}

  getAppointments(): Observable<AppointmentResponse[]> {
    return this.api.get<AppointmentResponse[]>(this.path);
  }

  getUpcoming(): Observable<AppointmentResponse[]> {
    return this.api.get<AppointmentResponse[]>(`${this.path}/upcoming`);
  }

  createAppointment(req: AppointmentRequest): Observable<AppointmentResponse> {
    return this.api.post<AppointmentResponse>(this.path, req);
  }

  cancelAppointment(id: string): Observable<any> {
    return this.api.delete<any>(`${this.path}/${id}`);
  }
}
