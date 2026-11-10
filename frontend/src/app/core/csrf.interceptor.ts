import { HttpBackend, HttpClient, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { switchMap } from 'rxjs';

const metodiModificativi = new Set(['POST', 'PUT', 'PATCH', 'DELETE']);

export const csrfInterceptor: HttpInterceptorFn = (request, next) => {
  if (!request.url.startsWith('/api/') || !metodiModificativi.has(request.method)) {
    return next(request);
  }

  const csrfClient = new HttpClient(inject(HttpBackend));
  return csrfClient.get<{ token: string }>('/api/auth/csrf', { withCredentials: true }).pipe(
    switchMap(({ token }) => next(request.clone({
      withCredentials: true,
      setHeaders: { 'X-CSRF-TOKEN': token },
    }))),
  );
};