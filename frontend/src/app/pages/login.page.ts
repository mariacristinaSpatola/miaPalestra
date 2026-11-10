import { Component, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthApi } from '../core/auth.api';

@Component({
    selector: 'app-login-page',
    imports: [FormsModule, RouterLink],
    template: `
    <section class="login-page">
      <div class="login-aside"><p class="eyebrow">FORMAFIT / MEMBER ACCESS</p><h1>OGNI<br>GIORNO<br><span>PIÙ FORTE.</span></h1><p>Entra nella tua area o unisciti alla community.</p></div>
      <form class="login-form" (ngSubmit)="invia()"><a class="back-link" routerLink="/">← Torna al sito</a>
        <div class="auth-switch" role="group" aria-label="Accesso o registrazione">
          <button type="button" [class.active]="!registrazione()" [attr.aria-pressed]="!registrazione()" (click)="mostraRegistrazione(false)">Accedi</button>
          <button type="button" [class.active]="registrazione()" [attr.aria-pressed]="registrazione()" (click)="mostraRegistrazione(true)">Registrati</button>
        </div>
        <p class="eyebrow">{{ registrazione() ? 'NUOVO ACCOUNT CLIENTE' : 'AREA RISERVATA' }}</p>
        <h2>{{ registrazione() ? 'Entra nella community.' : 'Bentornato.' }}</h2>
        @if (registrazione()) {
          <label>Nome<input name="firstName" [(ngModel)]="firstName" autocomplete="given-name" maxlength="100" required></label>
          <label>Cognome<input name="lastName" [(ngModel)]="lastName" autocomplete="family-name" maxlength="100" required></label>
          <label>Username<input name="username" [(ngModel)]="username" autocomplete="username" minlength="3" maxlength="80" required></label>
          <label>Email <span class="optional">Facoltativa</span><input name="email" [(ngModel)]="email" type="email" autocomplete="email" maxlength="254"></label>
          <label>Telefono <span class="optional">Facoltativo</span><input name="phone" [(ngModel)]="phone" type="tel" autocomplete="tel" maxlength="30"></label>
          <label>Password<input name="password" [(ngModel)]="password" type="password" autocomplete="new-password" minlength="8" maxlength="72" pattern="(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9]).{8,}" required><small class="form-hint">Almeno 8 caratteri, una maiuscola, una minuscola e un numero.</small></label>
          <label>Conferma password<input name="confirmPassword" [(ngModel)]="confermaPassword" type="password" autocomplete="new-password" required></label>
        } @else {
          <label>Username<input name="username" [(ngModel)]="username" autocomplete="username" required></label>
          <label>Password<input name="password" [(ngModel)]="password" type="password" autocomplete="current-password" required></label>
        }
        @if (errore()) { <p class="form-error" role="alert">{{ errore() }}</p> }
        @if (messaggio()) { <p class="form-success" role="status">{{ messaggio() }}</p> }
        <button class="button button-dark" type="submit" [disabled]="caricamento()">{{ testoPulsante() }} <span aria-hidden="true">↗</span></button>
        @if (registrazione()) { <p class="login-footnote">La registrazione crea un profilo cliente. Account staff e amministratori sono gestiti dalla palestra.</p> }
      </form>
    </section>
  `,
})
export class LoginPage {
    private readonly auth = inject(AuthApi);
    private readonly router = inject(Router);
    username = '';
    password = '';
    firstName = '';
    lastName = '';
    email = '';
    phone = '';
    confermaPassword = '';
    readonly registrazione = signal(false);
    readonly errore = signal('');
    readonly messaggio = signal('');
    readonly caricamento = signal(false);

    mostraRegistrazione(attiva: boolean): void {
        this.registrazione.set(attiva);
        this.errore.set('');
        this.messaggio.set('');
    }

    testoPulsante(): string {
        if (this.caricamento()) return this.registrazione() ? 'Creazione account…' : 'Accesso in corso…';
        return this.registrazione() ? 'Crea account cliente' : 'Accedi';
    }

    invia(): void {
        if (this.registrazione()) this.registra();
        else this.accedi();
    }

    private accedi(): void {
        this.errore.set('');
        this.messaggio.set('');
        this.caricamento.set(true);
        this.auth.login(this.username, this.password).subscribe({
            next: () => { this.caricamento.set(false); void this.router.navigateByUrl('/gestione'); },
            error: () => { this.caricamento.set(false); this.errore.set('Username o password non validi.'); },
        });
    }

    private registra(): void {
        this.errore.set('');
        this.messaggio.set('');
        this.username = this.username.trim();
        if (this.password !== this.confermaPassword) {
            this.errore.set('Le password inserite non coincidono.');
            return;
        }
        this.caricamento.set(true);
        this.auth.register({
            username: this.username,
            password: this.password,
            firstName: this.firstName,
            lastName: this.lastName,
            email: this.email.trim() || null,
            phone: this.phone.trim() || null,
        }).subscribe({
            next: () => this.auth.login(this.username, this.password).subscribe({
                next: () => { this.caricamento.set(false); void this.router.navigateByUrl('/gestione'); },
                error: () => {
                    this.caricamento.set(false);
                    this.password = '';
                    this.confermaPassword = '';
                    this.registrazione.set(false);
                    this.messaggio.set('Account creato. Accedi con username e password scelti.');
                },
            }),
            error: error => {
                this.caricamento.set(false);
                this.errore.set(this.messaggioErrore(error));
            },
        });
    }

    private messaggioErrore(error: HttpErrorResponse): string {
        if (error.status === 409) return 'Username o email già associati a un account.';
        return error.error?.errore ?? 'Registrazione non riuscita. Controlla i dati e riprova.';
    }
}