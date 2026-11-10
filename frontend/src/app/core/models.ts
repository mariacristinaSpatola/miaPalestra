export interface Utente {
    id: number;
    username: string;
    firstName: string;
    lastName: string;
    email: string | null;
    phone: string | null;
    role: 'ADMIN' | 'SECRETARY' | 'CUSTOMER';
    active: boolean;
}

export interface Corso {
    id: number;
    nome: string;
    descrizione: string | null;
    attivo: boolean;
}

export interface Lezione {
    id: number;
    corsoId: number;
    nomeCorso: string;
    data: string;
    ora: string;
    capienza: number;
    postiDisponibili: number;
    stato: 'SCHEDULED' | 'CANCELLED';
    scadenza: string;
    operazioniConsentite: boolean;
    conflittoProgramma: boolean;
}

export interface Prenotazione {
    id: number;
    clienteId: number;
    nomeCliente: string;
    occorrenzaId: number;
    nomeCorso: string;
    data: string;
    ora: string;
    stato: 'CONFIRMED' | 'CANCELLED';
    cancellabile: boolean;
}

export interface Programma {
    id: number;
    corsoId: number;
    nomeCorso: string;
    giorno: number;
    ora: string;
    capienzaPredefinita: number;
    validoDal: string;
    validoAl: string | null;
    attivo: boolean;
}