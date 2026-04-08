import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from './api.service';

export interface HealthRecordResponse {
  id: string; title: string; category: string; description: string;
  doctorName: string; hospital: string; recordDate: string;
  fileName: string; fileType: string; fileSize: number;
  hasFile: boolean; createdAt: string;
}

@Injectable({ providedIn: 'root' })
export class HealthRecordService {
  constructor(private api: ApiService) {}

  getRecords(category?: string): Observable<HealthRecordResponse[]> {
    const params = category ? `?category=${category}` : '';
    return this.api.get<HealthRecordResponse[]>(`/api/v1/health/records${params}`);
  }

  createRecord(data: any): Observable<HealthRecordResponse> {
    return this.api.post<HealthRecordResponse>('/api/v1/health/records', data);
  }

  deleteRecord(id: string): Observable<void> {
    return this.api.delete<void>(`/api/v1/health/records/${id}`);
  }
}
