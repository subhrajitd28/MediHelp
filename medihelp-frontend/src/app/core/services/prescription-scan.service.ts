import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
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

interface ChatbotImageResponse {
  image_analysis?: string;
  reply?: string;
  medications?: OcrMedication[];
}

/**
 * Posts a prescription image to the chatbot's 3-layer image pipeline
 * (Tesseract → Groq Vision → RAG cross-check) and surfaces the structured
 * medication list the backend now extracts. The user reviews + edits the
 * pre-filled rows before saving.
 */
@Injectable({ providedIn: 'root' })
export class PrescriptionScanService {
  private chatbotUrl = `${environment.apiUrl}/api/v1/chatbot/get/image`;

  constructor(private http: HttpClient) {}

  scanPrescription(file: File): Observable<OcrResponse> {
    const formData = new FormData();
    formData.append('image', file);
    return this.http.post<ChatbotImageResponse>(this.chatbotUrl, formData).pipe(
      map((res) => {
        const meds = Array.isArray(res.medications) ? res.medications : [];
        return {
          extracted_text: res.image_analysis || res.reply || '',
          medications: meds,
          confidence: meds.length > 0 ? 0.9 : (res.image_analysis ? 0.6 : 0.0),
          needs_confirmation: true,
        };
      })
    );
  }
}
