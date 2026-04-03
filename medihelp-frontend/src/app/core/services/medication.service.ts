import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from './api.service';
import { MedicationResponse, MedicationLogRequest, AdherenceResponse } from '../models/prescription.model';

@Injectable({ providedIn: 'root' })
export class MedicationService {
  private path = '/api/v1/prescriptions';

  constructor(private api: ApiService) {}

  getActiveMedications(): Observable<MedicationResponse[]> {
    return this.api.get<MedicationResponse[]>(`${this.path}/medications`);
  }

  addMedication(med: Partial<MedicationResponse>): Observable<MedicationResponse> {
    return this.api.post<MedicationResponse>(`${this.path}/medications`, med);
  }

  logMedication(log: MedicationLogRequest): Observable<any> {
    return this.api.post<any>(`${this.path}/medications/log`, log);
  }

  getAdherence(): Observable<AdherenceResponse> {
    return this.api.get<AdherenceResponse>(`${this.path}/medications/log/adherence`);
  }
}
