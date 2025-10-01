package com.example;

import java.math.BigDecimal;

/**
 * Classe modello per rappresentare un ristorante nell'applicazione TheKnife.
 * <p>
 * Questa classe incapsula tutte le informazioni necessarie per descrivere un ristorante,
 * incluse le informazioni di base (nome, posizione, descrizione) e i dati di valutazione
 * aggregati ottenuti dalle recensioni degli utenti. La classe supporta la comunicazione
 * client-server tramite serializzazione delle informazioni per lo scambio di dati.
 * <p>
 * Le valutazioni sono memorizzate come valori BigDecimal per garantire precisione
 * nei calcoli matematici delle medie delle recensioni. Il sistema di rating copre
 * diverse categorie di valutazione del ristorante.
 * <p>
 * <strong>Architettura:</strong> Questa classe fa parte del modello dati condiviso
 * tra client e server nell'architettura client-server dell'applicazione.
 * 
 * @author Andrea De Nisco, Antonio De Nisco
 * @version 2.0 - Aggiornato per architettura client-server
 * @since 1.0
 * @see RecensioneDAO
 * @see RistoranteDAO
 * @see ClientService
 */
public class Ristorante {
    /**
     * Costruttore di default.
     */
    public Ristorante() {}
    /**
     * Nome del ristorante.
     */
    private String nome;
    /**
     * Nazione del ristorante.
     */
    private String nazione;
    /**
     * Città del ristorante.
     */
    private String citta;
    /**
     * Indirizzo del ristorante.
     */
    private String indirizzo;
    /**
     * Fascia di prezzo del ristorante.
     */
    private BigDecimal fasciaPrezzo;
    /**
     * Informazione sulla consegna a domicilio.
     */
    private String delivery;
    /**
     * Informazione sulla disponibilità online.
     */
    private String online;
    /**
     * Tipo di cucina offerta dal ristorante.
     */
    private String tipoCucina;

    /**
     * Costruttore completo per il modello Ristorante.
     * @param nome nome del ristorante
     * @param nazione nazione del ristorante
     * @param citta città del ristorante
     * @param indirizzo indirizzo del ristorante
     * @param fasciaPrezzo fascia di prezzo
     * @param delivery consegna a domicilio
     * @param online disponibilità online
     * @param tipoCucina tipo di cucina
     */
    public Ristorante(String nome, String nazione, String citta, String indirizzo, 
                      BigDecimal fasciaPrezzo, String delivery, String online, String tipoCucina) {
        this.nome = nome;
        this.nazione = nazione;
        this.citta = citta;
        this.indirizzo = indirizzo;
        this.fasciaPrezzo = fasciaPrezzo;
        this.delivery = delivery;
        this.online = online;
        this.tipoCucina = tipoCucina;
    }

    /**
     * Restituisce il nome del ristorante.
     * @return nome
     */
    public String getNome() { return nome; }
    /**
     * Restituisce la nazione del ristorante.
     * @return nazione
     */
    public String getNazione() { return nazione; }
    /**
     * Restituisce la città del ristorante.
     * @return città
     */
    public String getCitta() { return citta; }
    /**
     * Restituisce l'indirizzo del ristorante.
     * @return indirizzo
     */
    public String getIndirizzo() { return indirizzo; }
    /**
     * Restituisce la fascia di prezzo.
     * @return fasciaPrezzo
     */
    public BigDecimal getFasciaPrezzo() { return fasciaPrezzo; }
    /**
     * Restituisce l'informazione sulla consegna a domicilio.
     * @return delivery
     */
    public String getDelivery() { return delivery; }
    /**
     * Restituisce l'informazione sulla disponibilità online.
     * @return online
     */
    public String getOnline() { return online; }
    /**
     * Restituisce il tipo di cucina.
     * @return tipoCucina
     */
    public String getTipoCucina() { return tipoCucina; }

    /**
     * Restituisce una rappresentazione leggibile del ristorante per le liste.
     * @return stringa leggibile
     */
    @Override
    public String toString() {
        return nome + " - " + citta + " (" + indirizzo + ")";
    }
}
