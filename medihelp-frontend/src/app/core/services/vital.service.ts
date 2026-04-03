import { Injectable } from '@angular/core';
import { HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiService } from './api.service';
import { VitalRequest, VitalResponse, VitalTrendResponse } from '../models/health.model';
import { PagedResponse } from '../models/api-response.model';

@Injectable({ providedIn: 'root' })
export class VitalService {
  private path = '/api/v1/health';

  constructor(private api: ApiService) {}

  recordVital(req: VitalRequest): Observable<VitalResponse> {
    return this.api.post<VitalResponse>(`${this.path}/vitals`, req);
  }

  getVitals(type?: string, page = 0, size = 20): Observable<PagedResponse<VitalResponse>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    if (type) {
      params = params.set('type', type);
    }
    return this.api.get<PagedResponse<VitalResponse>>(`${this.path}/vitals`, params);
  }

  getLatestVitals(): Observable<{ [key: string]: VitalResponse }> {
    return this.api.get<{ [key: string]: VitalResponse }>(`${this.path}/vitals/latest`);
  }

  getVitalTrends(type: string, days = 7): Observable<VitalTrendResponse> {
    const params = new HttpParams()
      .set('type', type)
      .set('days', days.toString());
    return this.api.get<VitalTrendResponse>(`${this.path}/vitals/trends`, params);
  }
}
