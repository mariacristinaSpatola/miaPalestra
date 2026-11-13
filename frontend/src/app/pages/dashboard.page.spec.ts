import { registerLocaleData } from '@angular/common';
import localeIt from '@angular/common/locales/it';
import { HttpErrorResponse } from '@angular/common/http';
import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { AuthApi } from '../core/auth.api';
import { Lezione, Utente } from '../core/models';
import { PalestraApi } from '../core/palestra.api';
import { DashboardPage } from './dashboard.page';

describe('DashboardPage', () => {
    const lezione: Lezione = {
        id: 12, corsoId: 3, nomeCorso: 'Pilates', data: '2026-12-10', ora: '18:30:00',
        durataMinuti: 60, capienza: 12, postiDisponibili: 8, stato: 'SCHEDULED',
        scadenza: '2026-12-10T17:30:00', operazioniConsentite: false, conflittoProgramma: false,
    };
    const segretaria: Utente = {
        id: 1, username: 'segreteria', firstName: 'Ada', lastName: 'Rossi',
        email: null, phone: null, role: 'SECRETARY', active: true,
    };

    beforeEach(() => registerLocaleData(localeIt));

    it('permette alla segreteria di modificare una lezione e aggiorna la lista', async () => {
        const aggiornata = {
            ...lezione, ora: '19:00:00', durataMinuti: 45, capienza: 10,
            iscritti: [
                { nome: 'Ada', cognome: 'Rossi', telefono: '3331234567', email: null },
                { nome: 'Grace', cognome: 'Hopper', telefono: null, email: 'grace@example.com' },
                { nome: 'Alan', cognome: 'Turing', telefono: null, email: null },
            ],
        };
        const lezioni = vi.fn().mockReturnValueOnce(of([lezione])).mockReturnValue(of([aggiornata]));
        const modificaLezione = vi.fn(() => of(aggiornata));
        TestBed.configureTestingModule({
            imports: [DashboardPage],
            providers: [provideRouter([]),
            { provide: AuthApi, useValue: { user: signal(segretaria), loadCurrent: () => of(segretaria) } },
            { provide: PalestraApi, useValue: { lezioni, modificaLezione } },
            ],
        });
        const fixture = TestBed.createComponent(DashboardPage);
        fixture.detectChanges();
        await fixture.whenStable();

        const modifica = Array.from(fixture.nativeElement.querySelectorAll('.lesson-actions button') as NodeListOf<HTMLButtonElement>)
            .find(button => button.textContent?.includes('Modifica'))!;
        modifica.click();
        fixture.detectChanges();
        await fixture.whenStable();
        expect(fixture.componentInstance.modificaLezione).toEqual({ ora: '18:30', durataMinuti: 60, capienza: 12 });

        fixture.componentInstance.modificaLezione = { ora: '19:00', durataMinuti: 45, capienza: 10 };
        fixture.componentInstance.salvaLezione();
        expect(modificaLezione).toHaveBeenCalledWith(12, { ora: '19:00', durataMinuti: 45, capienza: 10 });
        expect(fixture.componentInstance.lezioni()[0]).toEqual(aggiornata);
        expect(fixture.componentInstance.lezioneInModifica()).toBeNull();
        expect(fixture.componentInstance.iscrittiLezione()).toEqual(aggiornata.iscritti);
        expect(lezioni).toHaveBeenCalledTimes(2);
        fixture.detectChanges();
        const box = fixture.nativeElement.querySelector('.lesson-registrants') as HTMLElement;
        expect(box.textContent).toContain('Ada Rossi');
        expect(box.textContent).toContain('3331234567');
        expect(box.textContent).toContain('Grace Hopper');
        expect(box.textContent).toContain('grace@example.com');
        expect(box.textContent).toContain('Alan Turing');
        expect(box.textContent).not.toContain('null');
        expect(box.textContent).toContain('—');
    });

    it('mostra il box roster vuoto dopo una modifica senza prenotazioni confermate', async () => {
        const aggiornata = { ...lezione, ora: '19:00:00', iscritti: [] };
        TestBed.configureTestingModule({
            imports: [DashboardPage],
            providers: [provideRouter([]),
            { provide: AuthApi, useValue: { user: signal(segretaria), loadCurrent: () => of(segretaria) } },
            {
                provide: PalestraApi,
                useValue: {
                    lezioni: vi.fn().mockReturnValueOnce(of([lezione])).mockReturnValue(of([aggiornata])),
                    modificaLezione: vi.fn(() => of(aggiornata)),
                },
            },
            ],
        });
        const fixture = TestBed.createComponent(DashboardPage);
        fixture.detectChanges();
        await fixture.whenStable();
        fixture.componentInstance.iniziaModificaLezione(lezione);
        fixture.componentInstance.salvaLezione();
        fixture.detectChanges();

        const box = fixture.nativeElement.querySelector('.lesson-registrants') as HTMLElement;
        expect(box).not.toBeNull();
        expect(box.textContent).toContain('Nessun iscritto confermato per questa lezione.');
    });

    it('mantiene aperta la modifica quando il backend rifiuta i dati', async () => {
        const modificaLezione = vi.fn(() => throwError(() => new HttpErrorResponse({ status: 400, error: { errore: 'Capienza insufficiente.' } })));
        TestBed.configureTestingModule({
            imports: [DashboardPage],
            providers: [provideRouter([]),
            { provide: AuthApi, useValue: { user: signal(segretaria), loadCurrent: () => of(segretaria) } },
            { provide: PalestraApi, useValue: { lezioni: () => of([lezione]), modificaLezione } },
            ],
        });
        const fixture = TestBed.createComponent(DashboardPage);
        fixture.detectChanges();
        await fixture.whenStable();
        fixture.componentInstance.iniziaModificaLezione(lezione);
        fixture.componentInstance.salvaLezione();
        expect(fixture.componentInstance.errore()).toBe('Capienza insufficiente.');
        expect(fixture.componentInstance.lezioneInModifica()).toEqual(lezione);
        expect(fixture.componentInstance.operazioneLezioneInCorso()).toBe(false);
    });

    it('annulla la lezione solo dopo conferma e aggiorna le prenotazioni del cliente selezionato', async () => {
        const lezioni = vi.fn().mockReturnValueOnce(of([lezione])).mockReturnValue(of([]));
        const rispostaAnnullamento = {
            ...lezione,
            stato: 'CANCELLED' as const,
            iscritti: [
                { nome: 'Ada', cognome: 'Rossi', telefono: '3331234567', email: null },
                { nome: 'Luca', cognome: 'Bianchi', telefono: null, email: 'luca@example.com' },
            ],
        };
        const annullaLezione = vi.fn(() => of(rispostaAnnullamento));
        const prenotazioni = vi.fn(() => of([]));
        const conferma = vi.spyOn(window, 'confirm').mockReturnValueOnce(false).mockReturnValueOnce(true);
        TestBed.configureTestingModule({
            imports: [DashboardPage],
            providers: [provideRouter([]),
            { provide: AuthApi, useValue: { user: signal(segretaria), loadCurrent: () => of(segretaria) } },
            { provide: PalestraApi, useValue: { lezioni, annullaLezione, prenotazioni } },
            ],
        });
        const fixture = TestBed.createComponent(DashboardPage);
        fixture.detectChanges();
        await fixture.whenStable();
        fixture.componentInstance.clienteSelezionato.set({ ...segretaria, id: 8, role: 'CUSTOMER' });
        fixture.componentInstance.annullaLezione(lezione);
        expect(annullaLezione).not.toHaveBeenCalled();
        fixture.componentInstance.annullaLezione(lezione);
        expect(conferma).toHaveBeenCalledWith(expect.stringContaining('prenotazioni confermate'));
        expect(annullaLezione).toHaveBeenCalledWith(12);
        expect(fixture.componentInstance.lezioni()).toEqual([]);
        expect(fixture.componentInstance.iscrittiLezione()).toEqual(rispostaAnnullamento.iscritti);
        expect(prenotazioni).toHaveBeenCalledWith(8);
        fixture.detectChanges();
        const tabella = fixture.nativeElement.querySelector('.lesson-registrants table');
        expect(Array.from(tabella.querySelectorAll('thead th')).map((cella: any) => cella.textContent.trim()))
            .toEqual(['Nome', 'Telefono', 'Email']);
        const righe = Array.from(tabella.querySelectorAll('tbody tr')) as HTMLElement[];
        expect(righe).toHaveLength(2);
        expect(righe[0].textContent).toContain('Ada Rossi');
        expect(righe[0].querySelector('a[href="tel:3331234567"]')?.textContent).toBe('3331234567');
        expect(righe[0].textContent).toContain('—');
        expect(righe[1].textContent).toContain('Luca Bianchi');
        expect(righe[1].textContent).toContain('—');
        expect(righe[1].querySelector('a[href="mailto:luca@example.com"]')?.textContent).toBe('luca@example.com');
        conferma.mockRestore();
    });

    it('non mostra le azioni di gestione al cliente', async () => {
        const cliente: Utente = { ...segretaria, role: 'CUSTOMER' };
        TestBed.configureTestingModule({
            imports: [DashboardPage],
            providers: [provideRouter([]),
            { provide: AuthApi, useValue: { user: signal(cliente), loadCurrent: () => of(cliente) } },
            { provide: PalestraApi, useValue: { lezioni: () => of([lezione]), prenotazioni: () => of([]) } },
            ],
        });
        const fixture = TestBed.createComponent(DashboardPage);
        fixture.detectChanges();
        await fixture.whenStable();
        expect(fixture.nativeElement.querySelectorAll('.lesson-actions button')).toHaveLength(1);
    });

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