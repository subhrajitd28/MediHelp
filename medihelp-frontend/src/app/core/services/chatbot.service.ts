import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

// ─────────────────────────────────────────────────────────────────────────────
// Talks to medihelp-chatbot-service via the gateway. The gateway strips the
// /api/v1/chatbot prefix, so /api/v1/chatbot/get hits Flask's /get etc.
// All requests carry the JWT (HTTP interceptor); the gateway converts it into
// the X-User-Id header that the chatbot reads in get_current_user().
// ─────────────────────────────────────────────────────────────────────────────

export interface SeverityInfo {
  severity: 'CRITICAL' | 'URGENT' | 'MODERATE' | 'MILD';
  show_alert: boolean;
  alert_color: string;
  alert_icon: string;
  alert_title: string;
  alert_message: string;
  emergency_call: string | null;
  hospital_url: string;
  reason: string;
}

export interface ChatbotResponse {
  session_id: string;
  disease: string;
  disease_info: string;
  home_care: string;
  reply: string;
  severity: SeverityInfo;
  transcript?: string;       // voice
  image_analysis?: string;   // image
}

export interface SessionSummary {
  session_id: string;
  created_at: string;
  msg_count: number;
  preview: string;
}

export interface SessionMessages {
  session_id: string;
  messages: { role: 'user' | 'assistant'; content: string; ts: string }[];
  // Populated when a meal plan was previously generated for this session — the
  // frontend reattaches it to the most recent assistant message so the
  // "cultural food advice" card survives across reloads of past conversations.
  last_disease?: string;
  meal_plan?: MealPlan;
}

export interface CheckinPayload {
  show: boolean;
  reason?: string;
  days_ago?: number;
  last_date?: string;
  last_disease?: string;
  last_severity?: string;
  last_session?: string;
  question?: string;
}

export interface CheckinReply {
  session_id: string;
  progress: 'RECOVERING' | 'STABLE' | 'WORSENING' | 'NEW_SYMPTOMS';
  icon: string;
  color: string;
  label: string;
  assessment: string;
  advice: string;
}

export interface MealItem {
  food: string;
  quantity_g?: number;
  carbs_g?: number;
  protein_g?: number;
  fat_g?: number;
  fiber_g?: number;
  note?: string;
}

export interface MealPlan {
  disease: string;
  region: string;
  daily_targets: { [k: string]: number };
  // Each meal's items are objects (food, quantity, macros, note) — NOT plain
  // strings as the prior model assumed. Rendering as {{ item }} produced
  // "[object Object]" which looked like template placeholders.
  meal_plan: { [meal: string]: { target_calories?: string; items: MealItem[] } };
  foods_to_avoid: string[];
  hydration_note: string;
}

@Injectable({ providedIn: 'root' })
export class ChatbotService {
  private base = `${environment.apiUrl}/api/v1/chatbot`;

  constructor(private http: HttpClient) {}

  sendText(msg: string, sessionId?: string): Observable<ChatbotResponse> {
    const body = new FormData();
    body.append('msg', msg);
    if (sessionId) body.append('session_id', sessionId);
    return this.http.post<ChatbotResponse>(`${this.base}/get`, body);
  }

  sendVoice(audio: Blob, sessionId?: string): Observable<ChatbotResponse> {
    const body = new FormData();
    body.append('audio', audio, 'voice.webm');
    if (sessionId) body.append('session_id', sessionId);
    return this.http.post<ChatbotResponse>(`${this.base}/get/voice`, body);
  }

  sendImage(image: File, message?: string, sessionId?: string): Observable<ChatbotResponse> {
    const body = new FormData();
    body.append('image', image);
    if (message) body.append('message', message);
    if (sessionId) body.append('session_id', sessionId);
    return this.http.post<ChatbotResponse>(`${this.base}/get/image`, body);
  }

  listSessions(): Observable<{ sessions: SessionSummary[] }> {
    return this.http.get<{ sessions: SessionSummary[] }>(`${this.base}/history`);
  }

  loadSession(sessionId: string): Observable<SessionMessages> {
    const params = new HttpParams().set('session_id', sessionId);
    return this.http.get<SessionMessages>(`${this.base}/history`, { params });
  }

  deleteSession(sessionId: string): Observable<{ deleted: string }> {
    const params = new HttpParams().set('session_id', sessionId);
    return this.http.delete<{ deleted: string }>(`${this.base}/history`, { params });
  }

  deleteAllSessions(): Observable<{ deleted: string }> {
    return this.http.delete<{ deleted: string }>(`${this.base}/history`);
  }

  getCheckin(): Observable<CheckinPayload> {
    return this.http.get<CheckinPayload>(`${this.base}/checkin`);
  }

  submitCheckinReply(reply: string, payload: CheckinPayload, sessionId?: string): Observable<CheckinReply> {
    const body = new FormData();
    body.append('reply', reply);
    body.append('last_disease', payload.last_disease || '');
    body.append('last_severity', payload.last_severity || '');
    body.append('last_date', payload.last_date || '');
    if (sessionId) body.append('session_id', sessionId);
    return this.http.post<CheckinReply>(`${this.base}/checkin/reply`, body);
  }

  // Full cultural context: state, diet, age, gender. Backend uses these to
  // pick traditionally regional dishes (TN dosa vs Bengali bhaat) and respect
  // diet preference strictly (no chicken for a vegetarian user).
  getFoodSuggestions(
    userId: string,
    region: string,
    sessionId?: string,
    extras?: { dietPreference?: string; age?: number | string; gender?: string }
  ): Observable<MealPlan> {
    return this.http.post<MealPlan>(`${this.base}/api/food-suggestions`, {
      user_id:         userId,
      region,
      session_id:      sessionId,
      diet_preference: extras?.dietPreference,
      age:             extras?.age,
      gender:          extras?.gender,
    });
  }
}
