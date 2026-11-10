import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { csrfInterceptor } from './csrf.interceptor';

describe('csrfInterceptor', () => {
  let http: HttpClient;
  let backend: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptors([csrfInterceptor])), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpClient);
    backend = TestBed.inject(HttpTestingController);
  });

  afterEach(() => backend.verify());

  it('richiede il token e lo inoltra nelle richieste modificative', () => {
    let completata = false;
    http.post('/api/prenotazioni', {}).subscribe(() => completata = true);

    backend.expectOne('/api/auth/csrf').flush({ token: 'csrf-test' });
    const richiesta = backend.expectOne('/api/prenotazioni');
    expect(richiesta.request.headers.get('X-CSRF-TOKEN')).toBe('csrf-test');
    expect(richiesta.request.withCredentials).toBe(true);
    richiesta.flush({});

    expect(completata).toBe(true);
  });
});