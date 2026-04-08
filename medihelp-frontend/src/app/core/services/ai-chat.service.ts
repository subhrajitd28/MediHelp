import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface TriageRequest {
  symptoms: string;
  age?: number;
  gender?: string;
  existing_conditions?: string[];
  current_medications?: string[];
}

export interface TriageResponse {
  urgency: string;
  possible_conditions: string[];
  recommended_actions: string[];
  disclaimer: string;
}

export interface DietRequest {
  conditions: string[];
  allergies?: string[];
  dietary_restrictions?: string[];
  goal?: string;
}

export interface DietResponse {
  recommendations: any[];
  disclaimer: string;
}

export interface ExerciseRequest {
  conditions: string[];
  fitness_level?: string;
  restrictions?: string[];
}

export interface ExerciseResponse {
  recommendations: any[];
  disclaimer: string;
}

@Injectable({ providedIn: 'root' })
export class AiChatService {
  private baseUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  triageSymptoms(request: TriageRequest): Observable<TriageResponse> {
    return this.http.post<TriageResponse>(
      `${this.baseUrl}/api/v1/ai/symptoms/triage`,
      request
    );
  }

  getDietRecommendations(request: DietRequest): Observable<DietResponse> {
    return this.http.post<DietResponse>(
      `${this.baseUrl}/api/v1/ai/diet/recommendations`,
      request
    );
  }

  getExerciseRecommendations(request: ExerciseRequest): Observable<ExerciseResponse> {
    return this.http.post<ExerciseResponse>(
      `${this.baseUrl}/api/v1/ai/exercise/recommendations`,
      request
    );
  }
}
