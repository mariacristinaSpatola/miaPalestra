import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
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