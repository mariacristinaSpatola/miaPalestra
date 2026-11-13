import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthApi } from '../core/auth.api';
import { Corso, IscrittoLezione, Lezione, Prenotazione, Programma, Utente } from '../core/models';
import { PalestraApi } from '../core/palestra.api';

@Component({
  selector: 'app-dashboard-page',
  imports: [DatePipe, FormsModule],
  template: `
    <section class="dashboard">
      <header class="dashboard-head">
        <div><p class="eyebrow">FORMAFIT / AREA RISERVATA</p><h1>Ciao, {{ user()?.firstName }}.</h1><p>{{ labelRuolo() }} · Via Sansone 1</p></div>
        <button class="button button-outline" type="button" (click)="aggiorna()">Aggiorna dati <span aria-hidden="true">↻</span></button>
      </header>
      @if (messaggio()) { <p class="notice" role="status">{{ messaggio() }}</p> }
      @if (errore()) { <p class="form-error" role="alert">{{ errore() }}</p> }

      @if (user()?.role === 'ADMIN') {
        <div class="dashboard-grid">
          <section class="manage-panel"><p class="eyebrow">01 / UTENTI</p><h2>{{ utenteInModifica() ? 'Modifica account' : 'Crea un account' }}</h2>
            <form (ngSubmit)="creaUtente()" class="form-grid">
              <label>Username<input name="username" [(ngModel)]="nuovoUtente.username" minlength="3" [readonly]="!!utenteInModifica()" required></label>
              <label>{{ utenteInModifica() ? 'Nuova password (opzionale)' : 'Password iniziale' }}<input name="password" [(ngModel)]="nuovoUtente.password" type="password" minlength="8" [required]="!utenteInModifica()"></label>
              <label>Nome<input name="firstName" [(ngModel)]="nuovoUtente.firstName" required></label>
              <label>Cognome<input name="lastName" [(ngModel)]="nuovoUtente.lastName" required></label>
              <label>Email<input name="email" [(ngModel)]="nuovoUtente.email" type="email"></label>
              <label>Telefono<input name="phone" [(ngModel)]="nuovoUtente.phone"></label>
              <label class="wide">Ruolo<select name="role" [(ngModel)]="nuovoUtente.role" [disabled]="!!utenteInModifica()"><option value="CUSTOMER">Cliente</option><option value="SECRETARY">Segretaria</option><option value="ADMIN">Amministratore</option></select></label>
              <button class="button button-dark wide" type="submit">{{ utenteInModifica() ? 'Salva modifiche' : 'Crea account' }} <span aria-hidden="true">↗</span></button>
              @if (utenteInModifica()) { <button class="small-action wide" type="button" (click)="annullaModificaUtente()">Annulla modifica</button> }
            </form>
            <div class="manage-list"><p class="eyebrow">ACCOUNT ESISTENTI</p>@for (account of utenti(); track account.id) { <div class="manage-row"><span><strong>{{ account.firstName }} {{ account.lastName }}</strong><small>{{ account.username }} · {{ account.role }}</small></span><button class="small-action" type="button" [disabled]="account.id === user()?.id" (click)="account.active ? disattivaUtente(account) : riattivaUtente(account)">{{ account.active ? 'Disattiva' : 'Riattiva' }}</button></div> }</div>
          </section>
          <section class="manage-panel"><p class="eyebrow">02 / CATALOGO</p><h2>Aggiungi un corso</h2>
            <form (ngSubmit)="creaCorso()" class="form-grid">
              <label class="wide">Nome corso<input name="courseName" [(ngModel)]="nuovoCorso.nome" required maxlength="100"></label>
              <label class="wide">Descrizione<textarea name="courseDescription" [(ngModel)]="nuovoCorso.descrizione" rows="3" maxlength="1000"></textarea></label>
              <button class="button button-dark wide" type="submit">Salva corso <span aria-hidden="true">↗</span></button>
            </form>
            <div class="manage-list"><p class="eyebrow">CORSI PUBBLICATI</p>@for (corso of corsi(); track corso.id) { <div class="manage-row"><span><strong>{{ corso.nome }}</strong><small>{{ corso.attivo ? 'Attivo' : 'Disattivato' }}</small></span><button class="small-action" type="button" [disabled]="!corso.attivo" (click)="disattivaCorso(corso)">Disattiva</button></div> }</div>
            <hr>
            <p class="eyebrow">03 / PROGRAMMA SETTIMANALE</p><h2>Nuova lezione ricorrente</h2>
            <form (ngSubmit)="creaProgramma()" class="form-grid">
              <label class="wide">Corso<select name="courseId" [(ngModel)]="nuovoProgramma.corsoId" required><option [ngValue]="0" disabled>Seleziona un corso</option>@for (corso of corsi(); track corso.id) { <option [ngValue]="corso.id">{{ corso.nome }}</option> }</select></label>
              <label>Giorno<select name="day" [(ngModel)]="nuovoProgramma.giorno"><option [ngValue]="1">Lunedì</option><option [ngValue]="2">Martedì</option><option [ngValue]="3">Mercoledì</option><option [ngValue]="4">Giovedì</option><option [ngValue]="5">Venerdì</option><option [ngValue]="6">Sabato</option><option [ngValue]="7">Domenica</option></select></label>
              <label>Ora<input name="time" [(ngModel)]="nuovoProgramma.ora" type="time" required></label>
              <label>Capienza<input name="capacity" [(ngModel)]="nuovoProgramma.capienzaPredefinita" type="number" min="1" required></label>
              <label>Valido dal<input name="validFrom" [(ngModel)]="nuovoProgramma.validoDal" type="date" required></label>
              <button class="button button-dark wide" type="submit">Crea programma <span aria-hidden="true">↗</span></button>
            </form>
            <div class="manage-list"><p class="eyebrow">PROGRAMMI SETTIMANALI</p>@for (programma of programmi(); track programma.id) { <div class="manage-row"><span><strong>{{ programma.nomeCorso }} · {{ nomeGiorno(programma.giorno) }} {{ programma.ora.slice(0, 5) }}</strong><small>Dal {{ programma.validoDal }}{{ programma.validoAl ? ' al ' + programma.validoAl : '' }} · {{ programma.capienzaPredefinita }} posti</small></span><button class="small-action" type="button" [disabled]="!programma.attivo" (click)="disattivaProgramma(programma)">Disattiva</button></div> }</div>
          </section>
          <section class="manage-panel publish-panel"><p class="eyebrow">04 / PUBBLICAZIONE</p><h2>Calendario 30 giorni solari</h2>
            <form (ngSubmit)="pubblica()" class="inline-form"><label>Primo giorno<input name="publishFrom" [(ngModel)]="dal" type="date" required></label><button class="button button-accent" type="submit">Pubblica <span aria-hidden="true">↗</span></button></form>
            <p class="helper-text">La pubblicazione genera le lezioni datate per i 30 giorni consecutivi a partire dalla data scelta.</p>
          </section>
        </div>
      }

      @if (user()?.role === 'SECRETARY' || user()?.role === 'ADMIN') {
        <section class="manage-panel secretary-panel"><p class="eyebrow">GESTIONE CLIENTI</p><h2>Trova un cliente</h2>
          <form class="inline-form" (ngSubmit)="cercaClienti()"><label>Nome, cognome o email<input name="query" [(ngModel)]="queryClienti"></label><button class="button button-dark" type="submit">Cerca <span aria-hidden="true">⌕</span></button></form>
          @if (clienti().length) { <div class="client-list">@for (cliente of clienti(); track cliente.id) { <button type="button" class="client-option" [class.selected]="clienteSelezionato()?.id === cliente.id" (click)="selezionaCliente(cliente)"><strong>{{ cliente.firstName }} {{ cliente.lastName }}</strong><span>{{ cliente.email || cliente.username }}</span></button> }</div> }
          @if (clienteSelezionato()) { <p class="selected-client">Cliente selezionato: <strong>{{ clienteSelezionato()?.firstName }} {{ clienteSelezionato()?.lastName }}</strong></p> }
        </section>
      }

      @if (user()?.role === 'CUSTOMER' || user()?.role === 'SECRETARY' || user()?.role === 'ADMIN') {
        <section class="booking-section"><div class="section-heading"><div><p class="eyebrow">PRENOTA UN POSTO</p><h2>Lezioni <span>disponibili</span></h2></div><span class="schedule-note">{{ user()?.role === 'CUSTOMER' ? 'I posti si aggiornano dopo la conferma.' : 'Seleziona un cliente per prenotare.' }}</span></div>
          <div class="lesson-list">@for (lezione of lezioni(); track lezione.id) {
            <div class="lesson-row"><span>{{ lezione.data | date:'EEE d MMM':'':'it' }}</span><strong>{{ lezione.ora.slice(0, 5) }}</strong><b>{{ lezione.nomeCorso }}</b><span>{{ lezione.postiDisponibili }} posti</span><div class="lesson-actions"><button class="small-action" type="button" [disabled]="!lezione.operazioniConsentite || !lezione.postiDisponibili || (user()?.role !== 'CUSTOMER' && !clienteSelezionato())" (click)="prenota(lezione)">Prenota</button>
              @if (user()?.role === 'ADMIN' || user()?.role === 'SECRETARY') { <button class="small-action" type="button" [disabled]="operazioneLezioneInCorso()" (click)="iniziaModificaLezione(lezione)">Modifica</button><button class="small-action cancel-action" type="button" [disabled]="operazioneLezioneInCorso()" (click)="annullaLezione(lezione)">Annulla lezione</button> }
            </div></div>
            @if (lezioneInModifica()?.id === lezione.id) {
              <form class="lesson-edit form-grid" #modificaForm="ngForm" (ngSubmit)="salvaLezione()">
                <label>Ora<input name="oraLezione" type="time" [(ngModel)]="modificaLezione.ora" required></label>
                <label>Durata (minuti)<input name="durataLezione" type="number" min="1" max="1260" step="1" [(ngModel)]="modificaLezione.durataMinuti" required></label>
                <label>Capienza<input name="capienzaLezione" type="number" min="1" step="1" [(ngModel)]="modificaLezione.capienza" required></label>
                <div class="lesson-edit-actions"><button class="button button-dark" type="submit" [disabled]="modificaForm.invalid || operazioneLezioneInCorso()">Salva modifiche</button><button class="small-action" type="button" [disabled]="operazioneLezioneInCorso()" (click)="chiudiModificaLezione()">Annulla modifica</button></div>
              </form>
            }
          } @empty { <p class="empty-state">Nessuna lezione disponibile nel periodo pubblicato.</p> }</div>
          @if (user()?.role === 'ADMIN' || user()?.role === 'SECRETARY') {
            @if (iscrittiLezione(); as iscritti) {
              <section class="manage-panel lesson-registrants" aria-label="Iscritti interessati dalla lezione">
                <p class="eyebrow">ISCRITTI INTERESSATI</p><h2>{{ rosterDopoAnnullamento() ? 'Lezione annullata' : 'Lezione aggiornata' }}</h2>
                @if (iscritti.length > 0) {
                  <div class="manage-list">@for (iscritto of iscritti; track $index) {
                    <div class="manage-row"><span><strong>{{ iscritto.nome }} {{ iscritto.cognome }}</strong>
                      @if (iscritto.telefono || iscritto.email) {
                        <small>@if (iscritto.telefono) { <a [href]="'tel:' + iscritto.telefono">{{ iscritto.telefono }}</a> } @if (iscritto.email) { <a [href]="'mailto:' + iscritto.email">{{ iscritto.email }}</a> }</small>
                      } @else { <small>Contatto non disponibile.</small> }
                    </span></div>
                  }</div>
                } @else { <p class="empty-state">Nessun iscritto confermato per questa lezione.</p> }
              </section>
            }
          }
        </section>
      }

      @if (user()) {
        <section class="booking-section"><div class="section-heading"><div><p class="eyebrow">IL TUO POSTO È QUI</p><h2>{{ user()?.role === 'CUSTOMER' ? 'Le mie' : 'Prenotazioni cliente' }} <span>prenotazioni</span></h2></div><button class="text-link" type="button" (click)="caricaPrenotazioni()">Ricarica <span aria-hidden="true">↻</span></button></div>
          <div class="lesson-list">@for (booking of prenotazioni(); track booking.id) { <div class="lesson-row booking-row"><span>{{ booking.data | date:'EEE d MMM':'':'it' }}</span><strong>{{ booking.ora.slice(0, 5) }}</strong><b>{{ booking.nomeCorso }}</b>@if (user()?.role !== 'CUSTOMER') { <span>{{ booking.nomeCliente }}</span> }<span class="lesson-status" [class.full]="booking.stato === 'CANCELLED'">{{ booking.stato === 'CONFIRMED' ? 'CONFERMATA' : 'CANCELLATA' }}</span><button class="small-action cancel-action" type="button" [disabled]="!booking.cancellabile" (click)="cancella(booking)">Annulla</button></div> } @empty { <p class="empty-state">Nessuna prenotazione trovata.</p> }</div>
        </section>
      }
    </section>
  `,
})
export class DashboardPage implements OnInit {
  private readonly auth = inject(AuthApi);
  private readonly api = inject(PalestraApi);
  private readonly router = inject(Router);
  readonly user = this.auth.user;
  readonly corsi = signal<Corso[]>([]);
  readonly programmi = signal<Programma[]>([]);
  readonly utenti = signal<Utente[]>([]);
  readonly lezioni = signal<Lezione[]>([]);
  readonly prenotazioni = signal<Prenotazione[]>([]);
  readonly clienti = signal<Utente[]>([]);
  readonly clienteSelezionato = signal<Utente | null>(null);
  readonly utenteInModifica = signal<Utente | null>(null);
  readonly lezioneInModifica = signal<Lezione | null>(null);
  readonly iscrittiLezione = signal<IscrittoLezione[] | null>(null);
  readonly rosterDopoAnnullamento = signal(false);
  readonly operazioneLezioneInCorso = signal(false);
  readonly errore = signal('');
  readonly messaggio = signal('');
  queryClienti = '';
  dal = this.today();
  nuovoUtente = { username: '', password: '', firstName: '', lastName: '', email: '', phone: '', role: 'CUSTOMER' as Utente['role'] };
  nuovoCorso = { nome: '', descrizione: '' };
  nuovoProgramma = { corsoId: 0, giorno: 1, ora: '18:00', capienzaPredefinita: 12, validoDal: this.today() };
  modificaLezione = { ora: '', durataMinuti: 0, capienza: 0 };

  ngOnInit(): void {
    this.auth.loadCurrent().subscribe({
      next: user => {
        if (user.role === 'ADMIN') {
          this.api.corsi().subscribe({ next: values => this.corsi.set(values), error: error => this.fail(error) });
          this.api.programmi().subscribe({ next: values => this.programmi.set(values), error: error => this.fail(error) });
          this.api.utenti().subscribe({ next: values => this.utenti.set(values), error: error => this.fail(error) });
        }
        if (user.role === 'CUSTOMER') this.caricaPrenotazioni();
        this.caricaLezioni();
      },
      error: () => void this.router.navigateByUrl('/accedi'),
    });
  }

  labelRuolo(): string { return this.user()?.role === 'ADMIN' ? 'Amministratore' : this.user()?.role === 'SECRETARY' ? 'Segreteria' : 'Area cliente'; }
  nomeGiorno(giorno: number): string { return ['Lunedì', 'Martedì', 'Mercoledì', 'Giovedì', 'Venerdì', 'Sabato', 'Domenica'][giorno - 1] ?? ''; }

  aggiorna(): void { this.errore.set(''); this.caricaLezioni(); this.caricaPrenotazioni(); }

  cercaClienti(): void {
    this.errore.set('');
    this.api.clienti(this.queryClienti).subscribe({ next: values => this.clienti.set(values), error: error => this.fail(error) });
  }

  selezionaCliente(cliente: Utente): void {
    this.clienteSelezionato.set(cliente);
    if (this.user()?.role === 'ADMIN') {
      this.utenteInModifica.set(cliente);
      this.nuovoUtente = {
        username: cliente.username,
        password: '',
        firstName: cliente.firstName,
        lastName: cliente.lastName,
        email: cliente.email ?? '',
        phone: cliente.phone ?? '',
        role: cliente.role,
      };
    }
    this.caricaPrenotazioni(cliente.id);
  }

  annullaModificaUtente(): void {
    this.utenteInModifica.set(null);
    this.nuovoUtente = { username: '', password: '', firstName: '', lastName: '', email: '', phone: '', role: 'CUSTOMER' };
  }

  creaUtente(): void {
    this.errore.set('');
    const utenteInModifica = this.utenteInModifica();
    if (utenteInModifica) {
      const dati = {
        firstName: this.nuovoUtente.firstName.trim(),
        lastName: this.nuovoUtente.lastName.trim(),
        email: this.nuovoUtente.email.trim() || null,
        phone: this.nuovoUtente.phone.trim() || null,
        ...(this.nuovoUtente.password.trim() ? { password: this.nuovoUtente.password } : {}),
      };
      this.api.aggiornaUtente(utenteInModifica.id, dati).subscribe({
        next: aggiornato => {
          this.utenteInModifica.set(aggiornato);
          this.clienteSelezionato.set(aggiornato);
          this.clienti.update(clienti => clienti.map(cliente => cliente.id === aggiornato.id ? aggiornato : cliente));
          this.utenti.update(utenti => utenti.map(utente => utente.id === aggiornato.id ? aggiornato : utente));
          this.nuovoUtente.password = '';
          this.ok('Dati cliente aggiornati.');
        },
        error: error => this.fail(error),
      });
      return;
    }
    const payload = { ...this.nuovoUtente, email: this.nuovoUtente.email.trim() || null, phone: this.nuovoUtente.phone.trim() || null };
    this.api.creaUtente(payload).subscribe({ next: () => { this.nuovoUtente = { username: '', password: '', firstName: '', lastName: '', email: '', phone: '', role: 'CUSTOMER' }; this.api.utenti().subscribe(values => this.utenti.set(values)); this.ok('Account creato. Comunica le credenziali iniziali in modo sicuro.'); }, error: error => this.fail(error) });
  }

  creaCorso(): void {
    this.errore.set('');
    this.api.creaCorso(this.nuovoCorso.nome, this.nuovoCorso.descrizione).subscribe({ next: () => { this.nuovoCorso = { nome: '', descrizione: '' }; this.api.corsi().subscribe(values => this.corsi.set(values)); this.ok('Corso aggiunto al catalogo.'); }, error: error => this.fail(error) });
  }

  creaProgramma(): void {
    this.errore.set('');
    this.api.creaProgramma({ ...this.nuovoProgramma, validoAl: null }).subscribe({ next: () => { this.api.programmi().subscribe(values => this.programmi.set(values)); this.ok('Programma settimanale creato.'); }, error: error => this.fail(error) });
  }

  disattivaUtente(account: Utente): void { this.api.disattivaUtente(account.id).subscribe({ next: () => { this.api.utenti().subscribe(values => this.utenti.set(values)); this.ok(`Account ${account.username} disattivato.`); }, error: error => this.fail(error) }); }
  riattivaUtente(account: Utente): void {
    this.api.aggiornaUtente(account.id, {
      firstName: account.firstName,
      lastName: account.lastName,
      email: account.email,
      phone: account.phone,
      active: true,
    }).subscribe({
      next: aggiornato => {
        this.utenti.update(utenti => utenti.map(utente => utente.id === aggiornato.id ? aggiornato : utente));
        this.ok(`Account ${aggiornato.username} riattivato.`);
      },
      error: error => this.fail(error),
    });
  }
  disattivaCorso(corso: Corso): void { this.api.disattivaCorso(corso.id).subscribe({ next: () => { this.api.corsi().subscribe(values => this.corsi.set(values)); this.ok(`Corso ${corso.nome} disattivato.`); }, error: error => this.fail(error) }); }
  disattivaProgramma(programma: Programma): void { this.api.disattivaProgramma(programma.id).subscribe({ next: () => { this.api.programmi().subscribe(values => this.programmi.set(values)); this.ok(`Programma ${programma.nomeCorso} disattivato.`); }, error: error => this.fail(error) }); }

  pubblica(): void {
    this.errore.set('');
    this.api.pubblicaCalendario(this.dal).subscribe({ next: result => { this.ok(`Calendario ${result.dal} – ${result.al}: ${result.create} lezioni create, ${result.annullate} annullate, ${result.conflitti.length} da verificare.`); this.caricaLezioni(); }, error: error => this.fail(error) });
  }

  iniziaModificaLezione(lezione: Lezione): void {
    this.iscrittiLezione.set(null);
    this.rosterDopoAnnullamento.set(false);
    this.lezioneInModifica.set(lezione);
    this.modificaLezione = { ora: lezione.ora.slice(0, 5), durataMinuti: lezione.durataMinuti, capienza: lezione.capienza };
  }

  chiudiModificaLezione(): void { this.lezioneInModifica.set(null); }

  salvaLezione(): void {
    const lezione = this.lezioneInModifica();
    if (!lezione || this.operazioneLezioneInCorso()) return;
    this.errore.set('');
    this.operazioneLezioneInCorso.set(true);
    this.api.modificaLezione(lezione.id, this.modificaLezione).subscribe({
      next: risposta => {
        this.operazioneLezioneInCorso.set(false);
        this.iscrittiLezione.set(risposta.iscritti);
        this.rosterDopoAnnullamento.set(false);
        this.chiudiModificaLezione();
        this.ok('Lezione aggiornata.');
        this.caricaLezioni();
        if (this.clienteSelezionato()) this.caricaPrenotazioni(this.clienteSelezionato()!.id);
      },
      error: error => { this.operazioneLezioneInCorso.set(false); this.fail(error); },
    });
  }

  annullaLezione(lezione: Lezione): void {
    if (this.operazioneLezioneInCorso() || !window.confirm(`Annullare la lezione ${lezione.nomeCorso}? Anche le prenotazioni confermate saranno annullate.`)) return;
    this.errore.set('');
    this.iscrittiLezione.set(null);
    this.rosterDopoAnnullamento.set(false);
    this.operazioneLezioneInCorso.set(true);
    this.api.annullaLezione(lezione.id).subscribe({
      next: risposta => {
        this.operazioneLezioneInCorso.set(false);
        this.iscrittiLezione.set(risposta.iscritti);
        this.rosterDopoAnnullamento.set(true);
        if (this.lezioneInModifica()?.id === lezione.id) this.chiudiModificaLezione();
        this.ok('Lezione annullata.');
        this.caricaLezioni();
        if (this.clienteSelezionato()) this.caricaPrenotazioni(this.clienteSelezionato()!.id);
      },
      error: error => { this.operazioneLezioneInCorso.set(false); this.fail(error); },
    });
  }

  prenota(lezione: Lezione): void {
    this.errore.set('');
    this.api.prenota(lezione.id, this.user()?.role !== 'CUSTOMER' ? this.clienteSelezionato()?.id : undefined).subscribe({ next: () => { this.ok('Posto prenotato.'); this.caricaLezioni(); this.caricaPrenotazioni(this.clienteSelezionato()?.id); }, error: error => this.fail(error) });
  }

  cancella(booking: Prenotazione): void {
    this.errore.set('');
    this.api.cancella(booking.id).subscribe({ next: () => { this.ok('Prenotazione annullata.'); this.caricaPrenotazioni(this.clienteSelezionato()?.id); this.caricaLezioni(); }, error: error => this.fail(error) });
  }

  caricaPrenotazioni(clienteId?: number): void {
    if (this.user()?.role === 'ADMIN' && clienteId === undefined) return;
    this.api.prenotazioni(clienteId).subscribe({ next: values => this.prenotazioni.set(values), error: error => this.fail(error) });
  }

  private caricaLezioni(): void {
    const dal = this.today();
    const al = new Date();
    al.setDate(al.getDate() + 30);
    this.api.lezioni(dal, this.iso(al)).subscribe({ next: values => this.lezioni.set(values.filter(lesson => lesson.stato === 'SCHEDULED')), error: error => this.fail(error) });
  }

  private today(): string { return this.iso(new Date()); }
  private iso(date: Date): string { return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`; }
  private ok(message: string): void { this.errore.set(''); this.messaggio.set(message); }
  private fail(error: HttpErrorResponse): void { this.messaggio.set(''); this.errore.set(error.error?.errore || (error.status === 401 ? 'Sessione scaduta: accedi di nuovo.' : error.status === 403 ? 'Non hai i permessi per questa operazione.' : 'Operazione non riuscita. Riprova.')); }
}