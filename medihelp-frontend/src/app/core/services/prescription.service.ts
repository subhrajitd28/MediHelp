import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from './api.service';
import { PrescriptionResponse } from '../models/prescription.model';

export interface CreatePrescriptionRequest {
  doctorName: string;
  hospital: string;
  prescribedDate: string;
  notes?: string;
  ocrText?: string;
}

@Injectable({ providedIn: 'root' })
export class PrescriptionService {
  private path = '/api/v1/prescriptions';

  constructor(private api: ApiService) {}

  getPrescriptions(): Observable<PrescriptionResponse[]> {
    return this.api.get<PrescriptionResponse[]>(this.path);
  }

  createPrescription(data: CreatePrescriptionRequest): Observable<PrescriptionResponse> {
    return this.api.post<PrescriptionResponse>(this.path, data);
  }
}
