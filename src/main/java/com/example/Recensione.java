package com.example;

/**
 * Classe modello per rappresentare una recensione di ristorante nell'applicazione TheKnife.
 * <p>
 * Questa classe incapsula tutte le informazioni necessarie per una recensione completa,
 * inclusi i dati temporali (data e ora), le valutazioni numeriche su diverse categorie,
 * il testo descrittivo e le informazioni di associazione (utente e ristorante).
 * <p>
 * Il sistema di valutazione è strutturato su scale numeriche che permettono agli utenti
 * di esprimere giudizi dettagliati su diversi aspetti dell'esperienza culinaria.
 * Le recensioni sono persistite nel database PostgreSQL e scambiate tra client e server
 * tramite il protocollo di comunicazione dell'applicazione.
 * <p>
 * <strong>Architettura:</strong> Questa classe fa parte del modello dati condiviso
 * nell'architettura client-server, utilizzata sia per l'inserimento di nuove recensioni
 * che per la visualizzazione e calcolo delle medie dei ristoranti.
 * 
 * @author Andrea De Nisco, Antonio De Nisco
 * @version 2.0 - Aggiornato per architettura client-server
 * @since 1.0
 * @see RecensioneDAO
 * @see Ristorante
 * @see GuestPage
 */

public class Recensione {
    /**
     * Data della recensione.
     */
    private String data;
    /**
     * Numero di stelle assegnate.
     */
    private int stelle;
    /**
     * Ora della recensione.
     */
    private String ora;
    /**
     * Username dello scrittore della recensione.
     */
    private String usernameScrittore;
    /**
     * Nome del ristorante recensito.
     */
    private String ristoranteScritto;
    /**
     * Testo della recensione.
     */
    private String testo;
    /**
     * Risposta del gestore alla recensione.
     */
    private String rispostaGestore;

    /**
     * Costruttore completo per il modello Recensione.
     * @param data data della recensione
     * @param stelle numero di stelle
     * @param ora ora della recensione
     * @param usernameScrittore username dello scrittore
     * @param ristoranteScritto nome del ristorante recensito
     * @param testo testo della recensione
     * @param rispostaGestore risposta del gestore
     */
    public Recensione(String data, int stelle, String ora, String usernameScrittore,
                      String ristoranteScritto, String testo, String rispostaGestore) {
        this.data = data;
        this.stelle = stelle;
        this.ora = ora;
        this.usernameScrittore = usernameScrittore;
        this.ristoranteScritto = ristoranteScritto;
        this.testo = testo;
        this.rispostaGestore = rispostaGestore;
    }

    /**
     * Restituisce la data della recensione.
     * @return data
     */
    public String getData() { return data; }
    /**
     * Restituisce il numero di stelle assegnate.
     * @return stelle
     */
    public int getStelle() { return stelle; }
    /**
     * Restituisce l'ora della recensione.
     * @return ora
     */
    public String getOra() { return ora; }
    /**
     * Restituisce l'username dello scrittore.
     * @return usernameScrittore
     */
    public String getUsernameScrittore() { return usernameScrittore; }
    /**
     * Restituisce il nome del ristorante recensito.
     * @return ristoranteScritto
     */
    public String getRistoranteScritto() { return ristoranteScritto; }
    /**
     * Restituisce il testo della recensione.
     * @return testo
     */
    public String getTesto() { return testo; }
    /**
     * Restituisce la risposta del gestore.
     * @return rispostaGestore
     */
    public String getRispostaGestore() { return rispostaGestore; }

}
