import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { PalestraApi } from './palestra.api';

describe('PalestraApi', () => {
    beforeEach(() => TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] }));
    afterEach(() => TestBed.inject(HttpTestingController).verify());

    it('invia la modifica della lezione con le credenziali', () => {
        const dati = { ora: '19:00', durataMinuti: 45, capienza: 10 };
        TestBed.inject(PalestraApi).modificaLezione(12, dati).subscribe();
        const richiesta = TestBed.inject(HttpTestingController).expectOne('/api/calendario/lezioni/12');
        expect(richiesta.request.method).toBe('PUT');
        expect(richiesta.request.body).toEqual(dati);
        expect(richiesta.request.withCredentials).toBe(true);
        richiesta.flush({});
    });

    it('invia l’annullamento della lezione con le credenziali', () => {
        TestBed.inject(PalestraApi).annullaLezione(12).subscribe();
        const richiesta = TestBed.inject(HttpTestingController).expectOne('/api/calendario/lezioni/12/annulla');
        expect(richiesta.request.method).toBe('POST');
        expect(richiesta.request.withCredentials).toBe(true);
        richiesta.flush(null, { status: 204, statusText: 'No Content' });
    });
});