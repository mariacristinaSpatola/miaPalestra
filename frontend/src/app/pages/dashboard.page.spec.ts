import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { AuthApi } from '../core/auth.api';
import { Utente } from '../core/models';
import { PalestraApi } from '../core/palestra.api';
import { DashboardPage } from './dashboard.page';

describe('DashboardPage', () => {
    it('precompila i dati del cliente selezionato e salva le modifiche sul suo account', async () => {
        const cliente: Utente = {
            id: 8,
            username: 'cliente-otto',
            firstName: 'Ada',
            lastName: 'Lovelace',
            email: 'ada@example.com',
            phone: '3331234567',
            role: 'CUSTOMER',
            active: true,
        };
        const utenteCorrente: Utente = { ...cliente, id: 1, username: 'admin', role: 'ADMIN' };
        const aggiornaUtente = vi.fn((_id: number, dati: { firstName: string; lastName: string; email: string | null; phone: string | null; password?: string }) => of({ ...cliente, ...dati }));
        const api = {
            corsi: () => of([]),
            programmi: () => of([]),
            utenti: () => of([cliente]),
            lezioni: () => of([]),
            prenotazioni: () => of([]),
            creaUtente: vi.fn(),
            aggiornaUtente,
        };

        TestBed.configureTestingModule({
            imports: [DashboardPage],
            providers: [
                provideRouter([]),
                { provide: AuthApi, useValue: { user: signal(utenteCorrente), loadCurrent: () => of(utenteCorrente) } },
                { provide: PalestraApi, useValue: api },
            ],
        });
        const fixture = TestBed.createComponent(DashboardPage);
        fixture.detectChanges();
        await fixture.whenStable();

        const pagina = fixture.componentInstance;
        pagina.selezionaCliente(cliente);
        fixture.detectChanges();
        await fixture.whenStable();
        fixture.detectChanges();

        const nome = fixture.nativeElement.querySelector('input[name="firstName"]') as HTMLInputElement;
        expect(nome.value).toBe('Ada');
        nome.value = 'Ada Byron';
        nome.dispatchEvent(new Event('input'));
        fixture.detectChanges();

        pagina.creaUtente();

        expect(api.creaUtente).not.toHaveBeenCalled();
        expect(aggiornaUtente).toHaveBeenCalledWith(8, {
            firstName: 'Ada Byron',
            lastName: 'Lovelace',
            email: 'ada@example.com',
            phone: '3331234567',
        });
        expect(pagina.messaggio()).toBe('Dati cliente aggiornati.');
        expect(pagina.utenti()[0].firstName).toBe('Ada Byron');
    });

    it('riattiva un account disattivato aggiornandone lo stato', async () => {
        const account: Utente = {
            id: 8,
            username: 'cliente-otto',
            firstName: 'Ada',
            lastName: 'Lovelace',
            email: 'ada@example.com',
            phone: '3331234567',
            role: 'CUSTOMER',
            active: false,
        };
        const amministratore: Utente = { ...account, id: 1, username: 'admin', role: 'ADMIN', active: true };
        const aggiornaUtente = vi.fn((_id: number, dati: { firstName: string; lastName: string; email: string | null; phone: string | null; password?: string; active?: boolean }) => of({ ...account, ...dati, active: true }));

        TestBed.configureTestingModule({
            imports: [DashboardPage],
            providers: [
                provideRouter([]),
                { provide: AuthApi, useValue: { user: signal(amministratore), loadCurrent: () => of(amministratore) } },
                {
                    provide: PalestraApi,
                    useValue: {
                        corsi: () => of([]),
                        programmi: () => of([]),
                        utenti: () => of([account]),
                        lezioni: () => of([]),
                        prenotazioni: () => of([]),
                        aggiornaUtente,
                    },
                },
            ],
        });
        const fixture = TestBed.createComponent(DashboardPage);
        fixture.detectChanges();
        await fixture.whenStable();

        const pulsante = fixture.nativeElement.querySelector('.manage-list .manage-row .small-action') as HTMLButtonElement;
        expect(pulsante.textContent).toContain('Riattiva');
        expect(pulsante.disabled).toBe(false);
        pulsante.click();

        expect(aggiornaUtente).toHaveBeenCalledWith(8, {
            firstName: 'Ada',
            lastName: 'Lovelace',
            email: 'ada@example.com',
            phone: '3331234567',
            active: true,
        });
        expect(fixture.componentInstance.utenti()[0].active).toBe(true);
        expect(fixture.componentInstance.messaggio()).toBe('Account cliente-otto riattivato.');
    });

    it('consente una ricerca senza testo e carica tutti i clienti', async () => {
        const cliente: Utente = {
            id: 8,
            username: 'cliente-otto',
            firstName: 'Ada',
            lastName: 'Lovelace',
            email: 'ada@example.com',
            phone: null,
            role: 'CUSTOMER',
            active: true,
        };
        const clienteDue: Utente = { ...cliente, id: 9, username: 'cliente-nove', firstName: 'Grace', lastName: 'Hopper' };
        const segretaria: Utente = { ...cliente, id: 1, username: 'segretaria', role: 'SECRETARY' };
        const cercaClienti = vi.fn(() => of([cliente, clienteDue]));

        TestBed.configureTestingModule({
            imports: [DashboardPage],
            providers: [
                provideRouter([]),
                { provide: AuthApi, useValue: { user: signal(segretaria), loadCurrent: () => of(segretaria) } },
                {
                    provide: PalestraApi,
                    useValue: {
                        lezioni: () => of([]),
                        prenotazioni: () => of([]),
                        clienti: cercaClienti,
                    },
                },
            ],
        });
        const fixture = TestBed.createComponent(DashboardPage);
        fixture.detectChanges();
        await fixture.whenStable();

        const input = fixture.nativeElement.querySelector('input[name="query"]') as HTMLInputElement;
        expect(input.required).toBe(false);
        expect(input.hasAttribute('minlength')).toBe(false);

        fixture.componentInstance.queryClienti = '';
        fixture.componentInstance.cercaClienti();

        expect(cercaClienti).toHaveBeenCalledWith('');
        expect(fixture.componentInstance.clienti()).toHaveLength(2);
    });
});