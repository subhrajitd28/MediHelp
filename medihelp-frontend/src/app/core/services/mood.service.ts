import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/api-response.model';

export interface MoodEntry {
  id?: string;
  mood: number;             // 1-5
  journalText?: string;
  tags?: string[];
  sleepHours?: number;
  exerciseMinutes?: number;
  recordedAt?: string;
}

@Injectable({ providedIn: 'root' })
export class MoodService {
  private base = `${environment.apiUrl}/api/v1/health/mood`;

  constructor(private http: HttpClient) {}

  add(entry: MoodEntry): Observable<MoodEntry> {
    return this.http.post<ApiResponse<MoodEntry>>(this.base, entry).pipe(map(r => r.data));
  }

  list(from?: string, to?: string): Observable<MoodEntry[]> {
    let params = new HttpParams();
    if (from) params = params.set('from', from);
    if (to) params = params.set('to', to);
    return this.http.get<ApiResponse<MoodEntry[]>>(this.base, { params }).pipe(map(r => r.data));
  }
}
