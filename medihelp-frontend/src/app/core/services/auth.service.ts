import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { map } from 'rxjs/operators';
import { Router } from '@angular/router';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/api-response.model';
import { LoginRequest, RegisterRequest, OtpVerifyRequest, AuthResponse } from '../models/auth.model';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private apiUrl = `${environment.apiUrl}/api/v1/auth`;
  private isAuthenticatedSubject = new BehaviorSubject<boolean>(this.isLoggedIn());
  isAuthenticated$ = this.isAuthenticatedSubject.asObservable();

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
      map(res => res.data)
    );
  }

  verifyOtp(req: OtpVerifyRequest): Observable<any> {
    return this.http.post<ApiResponse<any>>(`${this.apiUrl}/verify-otp`, req).pipe(
      map(res => res.data)
    );
  }

  refreshToken(): Observable<AuthResponse> {
    const refreshToken = localStorage.getItem('refreshToken');
    return this.http.post<ApiResponse<AuthResponse>>(`${this.apiUrl}/refresh`, { refreshToken }).pipe(
      map(res => res.data),
      tap(auth => this.storeAuth(auth))
    );
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
    const token = this.getToken();
    if (!token) return false;
    const expiry = localStorage.getItem('tokenExpiry');
    if (expiry && Date.now() > parseInt(expiry, 10)) {
      this.clearAuth();
      return false;
    }
    return true;
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
