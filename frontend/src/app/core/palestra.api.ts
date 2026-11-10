import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Corso, Lezione, Prenotazione, Programma, Utente } from './models';

@Injectable({ providedIn: 'root' })
export class PalestraApi {
    private readonly http = inject(HttpClient);

    corsi() { return this.http.get<Corso[]>('/api/pubblico/corsi'); }

    lezioni(dal: string, al: string) {
        const params = new HttpParams().set('dal', dal).set('al', al);
        return this.http.get<Lezione[]>('/api/pubblico/lezioni', { params });
    }

    prenotazioni(clienteId?: number) {
        let params = new HttpParams();
        if (clienteId !== undefined) params = params.set('clienteId', clienteId);
        return this.http.get<Prenotazione[]>('/api/prenotazioni', { params, withCredentials: true });
    }

    clienti(query: string) {
        return this.http.get<Utente[]>('/api/utenti/clienti', { params: { query }, withCredentials: true });
    }

    utenti() { return this.http.get<Utente[]>('/api/utenti', { withCredentials: true }); }
    programmi() { return this.http.get<Programma[]>('/api/programmi-settimanali', { withCredentials: true }); }

    prenota(occorrenzaId: number, clienteId?: number) {
        return this.http.post<Prenotazione>('/api/prenotazioni', { occorrenzaId, clienteId: clienteId ?? null }, { withCredentials: true });
    }

    cancella(prenotazioneId: number) {
        return this.http.delete<Prenotazione>(`/api/prenotazioni/${prenotazioneId}`, { withCredentials: true });
    }

    creaCorso(nome: string, descrizione: string) {
        return this.http.post<Corso>('/api/corsi', { nome, descrizione }, { withCredentials: true });
    }

    aggiornaCorso(id: number, nome: string, descrizione: string) {
        return this.http.put<Corso>(`/api/corsi/${id}`, { nome, descrizione }, { withCredentials: true });
    }

    disattivaCorso(id: number) { return this.http.delete<void>(`/api/corsi/${id}`, { withCredentials: true }); }
    disattivaProgramma(id: number) { return this.http.delete<void>(`/api/programmi-settimanali/${id}`, { withCredentials: true }); }
    disattivaUtente(id: number) { return this.http.delete<void>(`/api/utenti/${id}`, { withCredentials: true }); }

    creaProgramma(payload: { corsoId: number; giorno: number; ora: string; capienzaPredefinita: number; validoDal: string; validoAl: string | null }) {
        return this.http.post('/api/programmi-settimanali', payload, { withCredentials: true });
    }

    creaUtente(payload: { username: string; password: string; firstName: string; lastName: string; email: string | null; phone: string | null; role: Utente['role'] }) {
        return this.http.post<Utente>('/api/utenti', payload, { withCredentials: true });
    }

    pubblicaCalendario(dal: string) {
        return this.http.post<{ dal: string; al: string; create: number; annullate: number; conflitti: number[] }>(
            '/api/calendario/pubblica', { dal }, { withCredentials: true },
        );
    }
}