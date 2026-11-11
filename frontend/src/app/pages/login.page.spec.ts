import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { AuthApi } from '../core/auth.api';
import { csrfInterceptor } from '../core/csrf.interceptor';
import { LoginPage } from './login.page';

describe('LoginPage', () => {
    let backend: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [LoginPage],
            providers: [
                provideHttpClient(withInterceptors([csrfInterceptor])),
                provideHttpClientTesting(),
                provideRouter([{ path: 'gestione', component: LoginPage }]),
            ],
        });
        backend = TestBed.inject(HttpTestingController);
    });

    afterEach(() => backend.verify());

    it('accede come amministratore e apre la gestione', async () => {
        const pagina = TestBed.createComponent(LoginPage).componentInstance;
        const navigazione = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
        pagina.username = 'admin';
        pagina.password = '12345678';

        pagina.invia();

        expect(pagina.caricamento()).toBe(true);
        backend.expectOne('/api/auth/csrf').flush({ token: 'csrf-login' });
        const accesso = backend.expectOne('/api/auth/login');
        expect(accesso.request.headers.get('X-CSRF-TOKEN')).toBe('csrf-login');
        expect(accesso.request.body).toEqual({ username: 'admin', password: '12345678' });
        accesso.flush({
            id: 1, username: 'admin', firstName: 'Admin', lastName: 'Admin',
            email: null, phone: null, role: 'ADMIN', active: true,
        });

        expect(TestBed.inject(AuthApi).user()?.role).toBe('ADMIN');
        expect(navigazione).toHaveBeenCalledWith('/gestione');
        expect(pagina.caricamento()).toBe(false);
        expect(pagina.errore()).toBe('');
    });

    for (const [stato, messaggio] of [
        [401, 'Username o password non validi.'],
        [403, 'Accesso bloccato dai controlli di sicurezza. Ricarica la pagina e riprova.'],
        [503, 'Servizio di accesso non disponibile. Riprova tra poco.'],
    ] as const) {
        it(`mostra un errore specifico quando il login risponde ${stato}`, () => {
            const pagina = TestBed.createComponent(LoginPage).componentInstance;
            pagina.username = 'admin';
            pagina.password = '12345678';
            pagina.invia();

            backend.expectOne('/api/auth/csrf').flush({ token: 'csrf-login' });
            backend.expectOne('/api/auth/login').flush('Errore', { status: stato, statusText: 'Errore' });

            expect(pagina.errore()).toBe(messaggio);
            expect(pagina.caricamento()).toBe(false);
            expect(TestBed.inject(AuthApi).user()).toBeNull();
        });
    }

    it('registra senza inviare il ruolo e poi avvia la sessione cliente', () => {
        const pagina = TestBed.createComponent(LoginPage).componentInstance;
        pagina.mostraRegistrazione(true);
        pagina.username = 'cliente-test';
        pagina.firstName = 'Nome';
        pagina.lastName = 'Cliente';
        pagina.password = 'Forza1234';
        pagina.confermaPassword = 'Forza1234';
        pagina.invia();

        backend.expectOne('/api/auth/csrf').flush({ token: 'csrf-register' });
        const registrazione = backend.expectOne('/api/auth/register');
        expect(registrazione.request.method).toBe('POST');
        expect(registrazione.request.headers.get('X-CSRF-TOKEN')).toBe('csrf-register');
        expect(registrazione.request.body).toEqual({
            username: 'cliente-test',
            password: 'Forza1234',
            firstName: 'Nome',
            lastName: 'Cliente',
            email: null,
            phone: null,
        });

        const cliente = {
            id: 21,
            username: 'cliente-test',
            firstName: 'Nome',
            lastName: 'Cliente',
            email: null,
            phone: null,
            role: 'CUSTOMER' as const,
            active: true,
        };
        registrazione.flush(cliente);

        backend.expectOne('/api/auth/csrf').flush({ token: 'csrf-login' });
        const accesso = backend.expectOne('/api/auth/login');
        expect(accesso.request.body).toEqual({ username: 'cliente-test', password: 'Forza1234' });
        accesso.flush(cliente);

        expect(TestBed.inject(AuthApi).user()?.role).toBe('CUSTOMER');
    });

    it('non invia la richiesta se le password non coincidono', () => {
        const pagina = TestBed.createComponent(LoginPage).componentInstance;
        pagina.mostraRegistrazione(true);
        pagina.password = 'Forza1234';
        pagina.confermaPassword = 'Diversa123';

        pagina.invia();

        expect(pagina.errore()).toBe('Le password inserite non coincidono.');
        backend.expectNone('/api/auth/csrf');
    });
});