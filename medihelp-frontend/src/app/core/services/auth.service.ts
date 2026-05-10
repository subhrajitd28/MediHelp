import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, of, shareReplay, tap, throwError } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { Router } from '@angular/router';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/api-response.model';
import { LoginRequest, RegisterRequest, OtpVerifyRequest, AuthResponse } from '../models/auth.model';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private apiUrl = `${environment.apiUrl}/api/v1/auth`;
  private isAuthenticatedSubject = new BehaviorSubject<boolean>(this.isLoggedIn());
  isAuthenticated$ = this.isAuthenticatedSubject.asObservable();

  // Shared refresh-in-flight observable. When several requests fire in
  // parallel and all hit 401 at once (e.g. dashboard load), they should all
  // wait on the SAME refresh call instead of stampeding the auth service
  // with N concurrent refresh requests.
  private refreshInFlight$: Observable<AuthResponse> | null = null;

  constructor(private http: HttpClient, private router: Router) {}

  login(req: LoginRequest): Observable<AuthResponse> {
    return this.http.post<ApiResponse<AuthResponse>>(`${this.apiUrl}/login`, req).pipe(
      map(res => res.data),
      tap(auth => {
        this.storeAuth(auth);
        this.isAuthenticatedSubject.next(true);
      })
    );
  }

  register(req: RegisterRequest): Observable<any> {
    return this.http.post<ApiResponse<any>>(`${this.apiUrl}/register`, req).pipe(
      map(res => res)
    );
  }

  verifyOtp(req: OtpVerifyRequest): Observable<any> {
    return this.http.post<ApiResponse<any>>(`${this.apiUrl}/verify-otp`, req).pipe(
      map(res => res)
    );
  }

  refreshToken(): Observable<AuthResponse> {
    const refreshToken = localStorage.getItem('refreshToken');
    return this.http.post<ApiResponse<AuthResponse>>(`${this.apiUrl}/refresh`, { refreshToken }).pipe(
      map(res => res.data),
      tap(auth => this.storeAuth(auth))
    );
  }

  /**
   * Returns a deduplicated refresh observable. While a refresh is in flight,
   * subsequent callers get the same observable and resolve together. Resets
   * the in-flight slot whether the refresh succeeds or fails.
   */
  refreshTokenShared(): Observable<AuthResponse> {
    if (!localStorage.getItem('refreshToken')) {
      return throwError(() => new Error('no refresh token'));
    }
    if (!this.refreshInFlight$) {
      this.refreshInFlight$ = this.refreshToken().pipe(
        tap({
          next:  () => { this.refreshInFlight$ = null; },
          error: () => { this.refreshInFlight$ = null; }
        }),
        shareReplay(1)
      );
    }
    return this.refreshInFlight$;
  }

  logout(): void {
    const token = this.getToken();
    if (token) {
      this.http.post(`${this.apiUrl}/logout`, {}).subscribe({ error: () => {} });
    }
    this.clearAuth();
    this.isAuthenticatedSubject.next(false);
    this.router.navigate(['/login']);
  }

  getToken(): string | null {
    return localStorage.getItem('accessToken');
  }

  isLoggedIn(): boolean {
    // We're "logged in" as long as a refresh token is on file — even if the
    // 15-min access token has expired, the next outgoing request will trigger
    // a refresh through the interceptor, transparent to the user.
    // Hard logout only happens when refreshToken is gone (logout / refresh failure).
    if (!localStorage.getItem('refreshToken')) return false;
    return !!this.getToken();
  }

  getUserId(): string | null {
    return localStorage.getItem('userId');
  }

  getEmail(): string | null {
    return localStorage.getItem('userEmail');
  }

  private storeAuth(auth: AuthResponse): void {
    localStorage.setItem('accessToken', auth.accessToken);
    localStorage.setItem('refreshToken', auth.refreshToken);
    localStorage.setItem('userId', auth.userId);
    localStorage.setItem('userEmail', auth.email);
    localStorage.setItem('userRole', auth.role);
    const expiryTime = Date.now() + auth.expiresIn * 1000;
    localStorage.setItem('tokenExpiry', expiryTime.toString());
  }

  private clearAuth(): void {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('userId');
    localStorage.removeItem('userEmail');
    localStorage.removeItem('userRole');
    localStorage.removeItem('tokenExpiry');
  }
}
