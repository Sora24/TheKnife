package com.example;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Classe di utilità per la gestione centralizzata delle connessioni al database PostgreSQL.
 * <p>
 * Questa classe fornisce un punto di accesso unificato per ottenere connessioni al database
 * dell'applicazione TheKnife. Gestisce i parametri di connessione (URL, credenziali) e
 * implementa il pattern Factory per la creazione delle connessioni JDBC.
 * <p>
 * La classe è progettata per funzionare nell'architettura client-server dove il server
 * necessita di accesso diretto al database PostgreSQL per servire le richieste dei client.
 * Tutte le operazioni di database (autenticazione, ricerca ristoranti, gestione recensioni)
 * passano attraverso questa classe per garantire configurazione consistente.
 * <p>
 * <strong>Configurazione Database:</strong>
 * <ul>
 *   <li>Server: localhost:6028</li>
 *   <li>Database: TheKnife</li>
 *   <li>Driver: PostgreSQL JDBC</li>
 * </ul>
 * <p>
 * <strong>Sicurezza:</strong> Le credenziali sono hardcoded per semplicità in questo
 * progetto educativo. In un ambiente di produzione dovrebbero essere esternalizzate
 * in file di configurazione o variabili d'ambiente.
 * 
 * @author Andrea De Nisco, Antonio De Nisco
 * @version 2.0 - Ottimizzato per architettura client-server
 * @since 1.0
 * @see ServerService
 * @see RistoranteDAO
 * @see RecensioneDAO
 */
public class DBConnection {
    /**
     * URL di connessione al database PostgreSQL.
     */
    private static String URL = "jdbc:postgresql://localhost:6028/TheKnife"; 
    /**
     * Username per la connessione al database.
     */
    private static String USER = "postgres"; 
    /**
     * Password per la connessione al database.
     */
    private static String PASSWORD = "andrea"; 

    /**
     * Costruttore di default.
     */
    public DBConnection() {}

    /**
     * Configura i parametri di connessione al database.
     * @param host l'host del database (es: localhost:5432)
     * @param database il nome del database
     * @param username nome utente per il database
     * @param password password per il database
     */
    public static void configure(String host, String database, String username, String password) {
        URL = "jdbc:postgresql://" + host + "/" + database;
        USER = username;
        PASSWORD = password;
    }

    /**
     * Restituisce una connessione al database PostgreSQL.
     * @return Connection oggetto di connessione
     * @throws SQLException se la connessione fallisce
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
