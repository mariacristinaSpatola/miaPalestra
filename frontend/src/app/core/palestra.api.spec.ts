import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { IscrittoLezione } from './models';
import { PalestraApi } from './palestra.api';

describe('PalestraApi', () => {
    beforeEach(() => TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] }));
    afterEach(() => TestBed.inject(HttpTestingController).verify());

    it('invia la modifica della lezione con le credenziali', () => {
        const dati = { ora: '19:00', durataMinuti: 45, capienza: 10 };
        const iscritti = [
            { nome: 'Ada', cognome: 'Rossi', telefono: '3331234567', email: null },
            { nome: 'Grace', cognome: 'Hopper', telefono: null, email: 'grace@example.com' },
        ];
        const risposta = {
            id: 12, corsoId: 3, nomeCorso: 'Pilates', data: '2026-12-10', ora: '19:00:00',
            durataMinuti: 45, capienza: 10, postiDisponibili: 8, stato: 'SCHEDULED' as const,
            scadenza: '2026-12-10T17:30:00', operazioniConsentite: false, conflittoProgramma: false, iscritti,
        };
        let risultato: { iscritti: IscrittoLezione[] } | undefined;
        TestBed.inject(PalestraApi).modificaLezione(12, dati).subscribe(value => { risultato = value; });
        const richiesta = TestBed.inject(HttpTestingController).expectOne('/api/calendario/lezioni/12');
        expect(richiesta.request.method).toBe('PUT');
        expect(richiesta.request.body).toEqual(dati);
        expect(richiesta.request.withCredentials).toBe(true);
        richiesta.flush(risposta);
        expect(risultato?.iscritti).toEqual(iscritti);
    });

    it('invia l’annullamento della lezione con le credenziali', () => {
        const iscritti = [{ nome: 'Ada', cognome: 'Rossi', telefono: null, email: 'ada@example.com' }];
        let risultato: { iscritti: IscrittoLezione[] } | undefined;
        TestBed.inject(PalestraApi).annullaLezione(12).subscribe(value => { risultato = value; });
        const richiesta = TestBed.inject(HttpTestingController).expectOne('/api/calendario/lezioni/12/annulla');
        expect(richiesta.request.method).toBe('POST');
        expect(richiesta.request.withCredentials).toBe(true);
        richiesta.flush({ iscritti });
        expect(risultato?.iscritti).toEqual(iscritti);
    });
});