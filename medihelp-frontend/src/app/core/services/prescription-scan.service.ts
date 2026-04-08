import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface OcrResponse {
  extracted_text: string;
  medications: OcrMedication[];
  confidence: number;
  needs_confirmation: boolean;
}

export interface OcrMedication {
  name: string;
  dosage: string;
  frequency: string;
}

@Injectable({ providedIn: 'root' })
export class PrescriptionScanService {
  private baseUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  scanPrescription(file: File): Observable<OcrResponse> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<OcrResponse>(
      `${this.baseUrl}/api/v1/ai/ocr/process`,
      formData
    );
  }
}
