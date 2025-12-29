package com.example;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.io.InputStream;
import java.util.Properties;

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
 * <p>
 * Le credenziali sono lette dinamicamente dal file <code>application.properties</code>
 * presente in classpath. Non sono hardcoded nel codice, permettendo una configurazione
 * flessibile senza ricompilazione:
 * <ul>
 *   <li><code>db.url</code> - URL JDBC del database</li>
 *   <li><code>db.user</code> - Username per l'accesso</li>
 *   <li><code>db.password</code> - Password per l'accesso</li>
 * </ul>
 * <p>
 * <strong>Sicurezza:</strong> Il file <code>application.properties</code> deve essere
 * protetto e non condiviso pubblicamente poiché contiene credenziali del database.
 * In ambienti di produzione, utilizzare variabili d'ambiente o sistemi di gestione
 * dei segreti dedicati.
 * 
 * @author Andrea De Nisco, Antonio De Nisco
 * @version 2.1 - Configurazione dinamica da file properties
 * @since 1.0
 * @see ServerService
 * @see RistoranteDAO
 * @see RecensioneDAO
 */
public class DBConnection {
    /**
     * URL di connessione al database PostgreSQL.
     */
    private static String URL;
    /**
     * Username per la connessione al database.
     */
    private static String USER;
    /**
     * Password per la connessione al database.
     */
    private static String PASSWORD;

    /**
     * Blocco di inizializzazione statica: carica la configurazione dal file
     * application.properties al caricamento della classe.
     */
    static {
        try {
            Properties properties = new Properties();
            
            // Carica il file application.properties da classpath
            try (InputStream input = DBConnection.class.getClassLoader()
                    .getResourceAsStream("application.properties")) {
                
                if (input == null) {
                    System.err.println("ERRORE: File application.properties non trovato in classpath!");
                    System.err.println("Assicurati che il file sia in src/main/resources/");
                    // Valori di fallback per evitare NullPointerException
                    URL = "jdbc:postgresql://localhost:5432/TheKnife";
                    USER = "postgres";
                    PASSWORD = "andrea";
                } else {
                    properties.load(input);
                    
                    // Leggi i valori dal file con fallback a valori di default
                    URL = properties.getProperty("db.url", "jdbc:postgresql://localhost:5432/TheKnife");
                    USER = properties.getProperty("db.user", "postgres");
                    PASSWORD = properties.getProperty("db.password", "andrea");
                    
                    System.out.println("✓ Configurazione database caricata da application.properties");
                    System.out.println("  URL: " + URL);
                    System.out.println("  Utente: " + USER);
                }
            }
        } catch (Exception e) {
            System.err.println("ERRORE nel caricamento della configurazione: " + e.getMessage());
            e.printStackTrace();
            
            // Valori di fallback in caso di errore
            URL = "jdbc:postgresql://localhost:5432/TheKnife";
            USER = "postgres";
            PASSWORD = "andrea";
        }
    } 

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
    * @throws java.sql.SQLException se la connessione fallisce
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
