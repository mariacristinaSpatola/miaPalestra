import { HttpClient } from '@angular/common/http';
import { inject, Injectable, signal } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, tap, of } from 'rxjs';
import { Utente } from './models';

@Injectable({ providedIn: 'root' })
export class AuthApi {
    private readonly http = inject(HttpClient);
    private readonly router = inject(Router);
    readonly user = signal<Utente | null>(null);

    restore(): void {
        this.loadCurrent().pipe(
            tap(user => this.user.set(user)),
            catchError(() => {
                this.user.set(null);
                return of(null);
            }),
        ).subscribe();
    }

    loadCurrent() {
        return this.http.get<Utente>('/api/utenti/me', { withCredentials: true }).pipe(
            tap(user => this.user.set(user)),
        );
    }

    login(username: string, password: string) {
        return this.http.post<Utente>('/api/auth/login', { username, password }, { withCredentials: true }).pipe(
            tap(user => this.user.set(user)),
        );
    }

    register(payload: { username: string; password: string; firstName: string; lastName: string; email: string | null; phone: string | null }) {
        return this.http.post<Utente>('/api/auth/register', payload, { withCredentials: true });
    }

    logout(): void {
        this.http.post<void>('/api/auth/logout', {}, { withCredentials: true }).pipe(
            catchError(() => of(void 0)),
            tap(() => {
                this.user.set(null);
                void this.router.navigateByUrl('/');
            }),
        ).subscribe();
    }
}