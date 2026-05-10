import { HttpInterceptorFn, HttpErrorResponse, HttpRequest, HttpEvent, HttpHandlerFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Observable, catchError, switchMap, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

// Paths the interceptor should NOT auto-add a Bearer to and should NOT try
// to refresh on 401 — those would loop. (Login/register naturally have no
// token; refresh has its own credential.)
const PUBLIC_AUTH_PATHS = [
  '/api/v1/auth/login',
  '/api/v1/auth/register',
  '/api/v1/auth/verify-otp',
  '/api/v1/auth/refresh',
  '/api/v1/auth/forgot-password',
  '/api/v1/auth/reset-password',
  '/api/v1/public',
];

const isPublicAuthPath = (url: string): boolean =>
  PUBLIC_AUTH_PATHS.some(p => url.includes(p));

const withBearer = (req: HttpRequest<unknown>, token: string): HttpRequest<unknown> =>
  req.clone({ setHeaders: { Authorization: `Bearer ${token}` } });

/**
 * Auth interceptor — adds the access token, and on 401 transparently
 * refreshes once before retrying the original request. Only logs the user
 * out if the refresh itself fails (refresh token expired or revoked).
 *
 * This replaces the previous behaviour of hard-logout on every 401, which
 * was kicking users out within minutes when any single endpoint blipped.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);

  // Public paths: no token, no refresh-on-401 dance.
  if (isPublicAuthPath(req.url)) {
    return next(req);
  }

  const token = authService.getToken();
  const authReq = token ? withBearer(req, token) : req;

  return next(authReq).pipe(
    catchError((error: HttpErrorResponse) => {
      // Only attempt refresh on 401 from a protected endpoint with a refresh token available.
      if (error.status !== 401 || !localStorage.getItem('refreshToken')) {
        return throwError(() => error);
      }
      return tryRefreshAndRetry(authReq, next, authService);
    })
  );
};

function tryRefreshAndRetry(
  originalReq: HttpRequest<unknown>,
  next: HttpHandlerFn,
  authService: AuthService
): Observable<HttpEvent<unknown>> {
  return authService.refreshTokenShared().pipe(
    switchMap(auth => next(withBearer(originalReq, auth.accessToken))),
    catchError(refreshErr => {
      // Refresh itself failed (token expired/revoked) — now we really log out.
      authService.logout();
      return throwError(() => refreshErr);
    })
  );
}
