import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from './api.service';
import { HealthScoreResponse, StreakResponse, BadgeResponse } from '../models/health.model';

@Injectable({ providedIn: 'root' })
export class HealthScoreService {
  private path = '/api/v1/health';

  constructor(private api: ApiService) {}

  getLatestScore(): Observable<HealthScoreResponse> {
    return this.api.get<HealthScoreResponse>(`${this.path}/score`);
  }

  calculateScore(): Observable<HealthScoreResponse> {
    return this.api.post<HealthScoreResponse>(`${this.path}/score/calculate`);
  }

  getStreaks(): Observable<StreakResponse[]> {
    return this.api.get<StreakResponse[]>(`${this.path}/streaks`);
  }

  getBadges(): Observable<BadgeResponse[]> {
    return this.api.get<BadgeResponse[]>(`${this.path}/badges`);
  }
}
