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
}

/**
 * Posts a prescription image to the chatbot's 3-layer image pipeline
 * (Tesseract → Groq Vision → RAG cross-check) and adapts the response into
 * the OcrResponse shape the prescription-scan UI was originally designed for.
 *
 * The chatbot returns a richer free-form analysis than the old OCR service did,
 * so `medications` stays empty — the user types each medicine into the form
 * after reviewing the analysis. The form was already built for manual entry.
 */
@Injectable({ providedIn: 'root' })
export class PrescriptionScanService {
  private chatbotUrl = `${environment.apiUrl}/api/v1/chatbot/get/image`;

  constructor(private http: HttpClient) {}

  scanPrescription(file: File): Observable<OcrResponse> {
    const formData = new FormData();
    formData.append('image', file);
    return this.http.post<ChatbotImageResponse>(this.chatbotUrl, formData).pipe(
      map((res) => ({
        extracted_text: res.image_analysis || res.reply || '',
        medications: [],
        confidence: res.image_analysis ? 0.85 : 0.0,
        needs_confirmation: true,
      }))
    );
  }
}
