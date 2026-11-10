import { Component, inject, OnInit, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { PalestraApi } from '../core/palestra.api';
import { Corso, Lezione } from '../core/models';

@Component({
    selector: 'app-home-page',
    imports: [DatePipe],
    template: `
    <section class="hero">
      <div class="hero-copy">
        <p class="eyebrow"><span class="live-dot"></span> Via Sansone 1</p>
        <h1>QUI SI<br>FA <span>SUL SERIO.</span></h1>
        <p class="hero-lede">La palestra è il punto di partenza. Quello che diventi, lo decidi tu.</p>
        <a class="button button-accent" href="#corsi">Trova il tuo corso <span aria-hidden="true">↘</span></a>
        <div class="hero-proof"><strong>OGNI ETÀ.</strong><span>OGNI LIVELLO. IL TUO RITMO.</span></div>
      </div>
      <div class="hero-image" role="img" aria-label="Atleta durante un allenamento funzionale">
        <div class="image-tag">MOVE WITH INTENT <span>01 / 04</span></div>
        <div class="image-stamp">FF<br><small>TRAIN CLUB</small></div>
      </div>
      <span class="hero-index" aria-hidden="true">01 — TRAIN TOGETHER</span>
    </section>

    <section class="intro section-pad" id="chi-siamo">
      <p class="eyebrow">NON È SOLO ALLENAMENTO</p>
      <div class="intro-grid"><h2>Una palestra.<br><span>Una community.</span></h2>
        <p>formaFit è uno spazio per chi vuole muoversi meglio, sentirsi più forte e trovare la propria squadra. Atleti, principianti, curiosi: qui si cresce insieme, senza scorciatoie.</p>
      </div>
      <div class="values-row"><span>01 / ENERGIA</span><span>02 / COSTANZA</span><span>03 / INSIEME</span></div>
    </section>

    <section class="courses section-pad" id="corsi">
      <div class="section-heading"><div><p class="eyebrow">IL TUO PROSSIMO ALLENAMENTO</p><h2>Corsi <span>& calendario</span></h2></div>
        <a class="text-link" href="#orari">Vai agli orari <span aria-hidden="true">↘</span></a></div>
      @if (errore()) { <p class="notice">{{ errore() }}</p> }
      @if (corsi().length) {
        <div class="course-grid">
          @for (corso of corsi(); track corso.id) {
            <article class="course-item"><span class="course-number">{{ ('0' + ($index + 1)).slice(-2) }}</span><div><h3>{{ corso.nome }}</h3><p>{{ corso.descrizione || 'Allenati al tuo ritmo, insieme alla community formaFit.' }}</p></div><span class="course-arrow" aria-hidden="true">↗</span></article>
          }
        </div>
      } @else if (!errore()) { <p class="empty-state">I corsi saranno pubblicati a breve.</p> }
    </section>

    <section class="schedule section-pad" id="orari">
      <div class="section-heading"><div><p class="eyebrow">SETTIMANA IN CORSO</p><h2>In agenda <span>adesso</span></h2></div><span class="schedule-note">Orari e posti aggiornati dal gestionale</span></div>
      @if (lezioni().length) {
        <div class="lesson-list">@for (lezione of lezioni(); track lezione.id) {
          <div class="lesson-row"><span>{{ lezione.data | date:'EEE d MMM':'':'it' }}</span><strong>{{ lezione.ora.slice(0, 5) }}</strong><b>{{ lezione.nomeCorso }}</b><span>{{ lezione.postiDisponibili }} posti</span><span class="lesson-status" [class.full]="lezione.postiDisponibili === 0">{{ lezione.postiDisponibili ? 'DISPONIBILE' : 'COMPLETO' }}</span></div>
        }</div>
      } @else if (!errore()) { <p class="empty-state">Il prossimo calendario non è ancora stato pubblicato.</p> }
    </section>

    <section class="contact section-pad" id="contatti">
      <div><p class="eyebrow">CI VEDIAMO IN PALESTRA?</p><h2>Fatti trovare<br><span>pronto.</span></h2></div>
      <div class="contact-details"><a href="tel:+3905556677">055 56677 <span aria-hidden="true">↗</span></a><a href="mailto:topforma@hotmail.it">topforma&#64;hotmail.it <span aria-hidden="true">↗</span></a><a href="https://maps.google.com/?q=Via+Sansone+1" target="_blank" rel="noreferrer">Via Sansone 1 <span aria-hidden="true">↗</span></a><p>Scrivici o passa a trovarci: il primo passo è il più importante.</p></div>
    </section>
    <div class="bottom-band"><span>FORMAFIT / VIA SANSONE 1</span><span>ALLENATI PER LA TUA PROSSIMA VERSIONE.</span><span>LA TUA PROSSIMA VERSIONE.</span></div>
  `,
})
export class HomePage implements OnInit {
    private readonly api = inject(PalestraApi);
    readonly corsi = signal<Corso[]>([]);
    readonly lezioni = signal<Lezione[]>([]);
    readonly errore = signal('');

    ngOnInit(): void {
        this.api.corsi().subscribe({ next: value => this.corsi.set(value), error: () => this.errore.set('Il catalogo corsi non è al momento raggiungibile.') });
        const now = new Date();
        const start = new Date(now.getFullYear(), now.getMonth(), now.getDate());
        const end = new Date(start);
        end.setDate(end.getDate() + 6);
        this.api.lezioni(this.iso(start), this.iso(end)).subscribe({ next: value => this.lezioni.set(value), error: () => this.errore.set('Il calendario non è al momento raggiungibile.') });
    }

    private iso(date: Date): string { return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`; }
}