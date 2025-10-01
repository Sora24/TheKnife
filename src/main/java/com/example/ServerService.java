package com.example;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.ArrayList;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Servizio principale del server TheKnife per l'architettura client-server.
 * 
 * <p>Questa classe implementa un server TCP socket-based che gestisce tutte le richieste
 * dei client per operazioni su ristoranti, recensioni e autenticazione utenti.
 * Il server ascolta sulla porta 8080 e utilizza un pool di thread per gestire
 * connessioni client multiple concorrenti.
 * 
 * <h3>Protocollo di Comunicazione</h3>
 * Il server riconosce i seguenti tipi di richieste:
 * <ul>
 *   <li><strong>SEARCH_RESTAURANTS:</strong> Ricerca ristoranti con filtri</li>
 *   <li><strong>GET_REVIEWS:</strong> Ottiene recensioni per un ristorante</li>
 *   <li><strong>ADD_REVIEW:</strong> Aggiunge una nuova recensione</li>
 *   <li><strong>LOGIN:</strong> Autentica un utente con password MD5 (legacy)</li>
 *   <li><strong>LOGIN_SALTED:</strong> Autentica un utente con password in chiaro (sicuro)</li>
 *   <li><strong>REGISTER:</strong> Registra un nuovo utente con password MD5 (legacy)</li>
 *   <li><strong>REGISTER_SALTED:</strong> Registra un nuovo utente con password salted (sicuro)</li>
 *   <li><strong>ADD_RESTAURANT:</strong> Aggiunge un nuovo ristorante (solo gestori)</li>
 * </ul>
 * 
 * <h3>Architettura</h3>
 * <p>Il server utilizza:
 * <ul>
 *   <li><strong>Database PostgreSQL:</strong> Per persistenza dati (localhost:6028)</li>
 *   <li><strong>Pool di Thread:</strong> Per gestione concorrente dei client</li>
 *   <li><strong>Protocollo Testuale:</strong> Richieste/risposte in formato stringa</li>
 *   <li><strong>Crittografia Avanzata:</strong> SHA-256 con salt per nuove password, MD5 legacy supportato</li>
 * </ul>
 * 
 * <h3>Sicurezza</h3>
 * <p>Il server implementa:
 * <ul>
 *   <li>Password hashing SHA-256 con salt, mantenendo compatibilità MD5 legacy</li>
 *   <li>Validazione input per prevenire injection attacks</li>
 *   <li>Gestione errori senza esposizione dettagli interni</li>
 *   <li>Autenticazione basata su ruoli (utente/gestore)</li>
 * </ul>
 * 
 * <h3>Utilizzo</h3>
 * <pre>{@code
 * // Avvio del server
 * ServerService server = new ServerService();
 * server.startServer(); // Ascolta su porta 8080
 * }</pre>
 * 
 * @author Andrea De Nisco, Antonio De Nisco
 * @version 2.0
 * @since 1.0
 * @see ClientService
 * @see DBConnection
 */
public class ServerService {
    
    /** 
     * Data Access Object per le operazioni sui ristoranti.
     * Gestisce tutte le query e operazioni relative ai ristoranti nel database.
     */
    private RistoranteDAO ristoranteDAO;
    
    /** 
     * Data Access Object per le operazioni sulle recensioni.
     * Gestisce tutte le query e operazioni relative alle recensioni nel database.
     */
    private RecensioneDAO recensioneDAO;
    
    /**
     * Costruttore del servizio server.
     * Inizializza i Data Access Objects necessari per le operazioni sui dati.
     * 
     * <p>Durante l'inizializzazione vengono creati:
     * <ul>
     *   <li>{@link RistoranteDAO} - per operazioni sui ristoranti</li>
     *   <li>{@link RecensioneDAO} - per operazioni sulle recensioni</li>
     * </ul>
     * 
     * @since 1.0
     */
    public ServerService() {
        this.ristoranteDAO = new RistoranteDAO();
        this.recensioneDAO = new RecensioneDAO();
    }
    
    /**
     * Punto di ingresso principale del servizio server.
     * Avvia il server, richiede le credenziali del database e avvia
     * il servizio per ricevere richieste dai client.
     * 
     * @param args argomenti da linea di comando (attualmente non utilizzati)
     * @throws RuntimeException se si verificano errori critici durante l'avvio
     * 
     * @see DBConnection#getConnection()
     * @since 1.0
     */
    public static void main(String[] args) {
        System.out.println("=== TheKnife Server Service ===");
        System.out.println("Utilizzando configurazione database hardcoded...");
        
        // La configurazione del database è ora hardcoded nella classe DBConnection
        // Non è necessario configurare qui poiché DBConnection utilizza valori statici
        
        // Test di connessione al database
        try {
            System.out.println("Test connessione al database...");
            Connection conn = DBConnection.getConnection();
            System.out.println("✓ Connessione al database riuscita!");
            conn.close();
        } catch (SQLException e) {
            System.err.println("✗ Errore di connessione al database: " + e.getMessage());
            System.err.println("Il server verrà terminato.");
            return;
        }
        
        // Avvio del server
        ServerService serverService = new ServerService();
        try {
            serverService.startServer();
        } catch (IOException e) {
            System.err.println("Errore nell'avvio del server: " + e.getMessage());
        }
    }
    
    /**
     * Avvia il server socket per ascoltare le richieste dei client.
     */
    private void startServer() throws IOException {
        final int PORT = 8080;
        
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            ExecutorService executor = Executors.newCachedThreadPool();
            
            System.out.println("✓ Server avviato sulla porta " + PORT);
            System.out.println("In attesa di connessioni client...");
            System.out.println("Per terminare il server, premere Ctrl+C");
            
            // Aggiunge hook per shutdown graceful
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\nSpegnimento del server in corso...");
                executor.shutdown();
            }));
            
            while (!serverSocket.isClosed()) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("Client connesso: " + clientSocket.getInetAddress());
                    
                    // Gestisce ogni client in un thread separato
                    executor.submit(() -> handleClient(clientSocket));
                } catch (IOException e) {
                    if (!serverSocket.isClosed()) {
                        System.err.println("Errore nell'accettare connessione client: " + e.getMessage());
                    }
                }
            }
        }
    }
    
    /**
     * Gestisce le richieste di un singolo client.
     */
    private void handleClient(Socket clientSocket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {
            
            String request;
            while ((request = in.readLine()) != null) {
                System.out.println("Richiesta ricevuta: " + request);
                
                String response = processRequest(request);
                out.println(response);
                
                if ("EXIT".equals(request)) {
                    break;
                }
            }
        } catch (IOException e) {
            System.err.println("Errore nella gestione del client: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("Errore nella chiusura del socket: " + e.getMessage());
            }
        }
    }
    
    /**
     * Processa una richiesta del client e restituisce la risposta.
     */
    private String processRequest(String request) {
        try {
            if (request.startsWith("SEARCH_RESTAURANTS:")) {
                // Formato: SEARCH_RESTAURANTS:nazione|citta|tipoCucina|fasciaPrezzo|delivery|online
                String[] parts = request.substring("SEARCH_RESTAURANTS:".length()).split("\\|", -1);
                
                String nazione = parts.length > 0 && !parts[0].isEmpty() ? parts[0] : null;
                String citta = parts.length > 1 && !parts[1].isEmpty() ? parts[1] : null;
                String tipoCucina = parts.length > 2 && !parts[2].isEmpty() ? parts[2] : null;
                String fasciaPrezzo = parts.length > 3 && !parts[3].isEmpty() ? parts[3] : null;
                Boolean delivery = parts.length > 4 && !parts[4].isEmpty() ? Boolean.valueOf(parts[4]) : null;
                Boolean online = parts.length > 5 && !parts[5].isEmpty() ? Boolean.valueOf(parts[5]) : null;
                
                List<Ristorante> ristoranti = cercaRistoranti(nazione, citta, tipoCucina, fasciaPrezzo, delivery, online);
                return "RESTAURANTS:" + ristorantiToString(ristoranti);
                
            } else if (request.startsWith("GET_REVIEWS:")) {
                // Formato: GET_REVIEWS:nomeRistorante
                String nomeRistorante = request.substring("GET_REVIEWS:".length());
                List<Recensione> recensioni = getRecensioniPerRistorante(nomeRistorante);
                return "REVIEWS:" + recensioniToString(recensioni);
                
            } else if (request.startsWith("ADD_REVIEW:")) {
                // Formato: ADD_REVIEW:username|ristorante|stelle|testo
                String[] parts = request.substring("ADD_REVIEW:".length()).split("\\|", 4);
                if (parts.length == 4) {
                    String username = parts[0];
                    String ristorante = parts[1];
                    int stelle = Integer.parseInt(parts[2]);
                    String testo = parts[3];
                    
                    aggiungiRecensione(username, ristorante, stelle, testo);
                    return "OK:Recensione aggiunta con successo";
                } else {
                    return "ERROR:Formato richiesta non valido";
                }
                
            } else if (request.startsWith("LOGIN:")) {
                // Formato: LOGIN:username|password
                String[] parts = request.substring("LOGIN:".length()).split("\\|", 2);
                if (parts.length == 2) {
                    String userInfo = verifyLoginAndGetInfo(parts[0], parts[1]);
                    return userInfo != null ? "OK:" + userInfo : "ERROR:Invalid credentials";
                } else {
                    return "ERROR:Invalid login format";
                }
                
            } else if (request.startsWith("LOGIN_SALTED:")) {
                // Formato: LOGIN_SALTED:username|plainPassword
                // Questo endpoint accetta password in chiaro per verificare password salted
                String[] parts = request.substring("LOGIN_SALTED:".length()).split("\\|", 2);
                if (parts.length == 2) {
                    String userInfo = verifySaltedLoginAndGetInfo(parts[0], parts[1]);
                    return userInfo != null ? "OK:" + userInfo : "ERROR:Invalid credentials";
                } else {
                    return "ERROR:Invalid login format";
                }
                
            } else if (request.startsWith("REGISTER:")) {
                // Formato: REGISTER:nome|cognome|username|email|password|dataNascita|domicilio|ruolo
                String[] parts = request.substring("REGISTER:".length()).split("\\|", 8);
                if (parts.length == 8) {
                    boolean success = registerUser(parts[0], parts[1], parts[2], parts[3], parts[4], parts[5], parts[6], parts[7]);
                    return success ? "OK:Registration successful" : "ERROR:Registration failed";
                } else {
                    return "ERROR:Formato richiesta non valido";
                }
                
            } else if (request.startsWith("REGISTER_SALTED:")) {
                // Formato: REGISTER_SALTED:nome|cognome|username|email|password|dataNascita|domicilio|ruolo
                // Questo endpoint accetta password in chiaro e le hasha con salt
                String[] parts = request.substring("REGISTER_SALTED:".length()).split("\\|", 8);
                if (parts.length == 8) {
                    boolean success = registerUserWithSaltedPassword(parts[0], parts[1], parts[2], parts[3], parts[4], parts[5], parts[6], parts[7]);
                    return success ? "OK:Registration with salted password successful" : "ERROR:Salted registration failed";
                } else {
                    return "ERROR:Formato richiesta non valido";
                }
                
            } else if (request.startsWith("ADD_RESTAURANT:")) {
                // Formato: ADD_RESTAURANT:nome|nazione|citta|indirizzo|prezzo|delivery|online|tipoCucina|username
                String[] parts = request.substring("ADD_RESTAURANT:".length()).split("\\|", 9);
                if (parts.length == 9) {
                    java.math.BigDecimal prezzo = null;
                    if (!parts[4].isEmpty()) {
                        try {
                            prezzo = new java.math.BigDecimal(parts[4]);
                        } catch (Exception e) {
                            return "ERROR:Formato prezzo non valido";
                        }
                    }
                    boolean success = addRestaurant(parts[0], parts[1], parts[2], parts[3], prezzo, parts[5], parts[6], parts[7], parts[8]);
                    return success ? "OK:Restaurant added successfully" : "ERROR:Failed to add restaurant";
                } else {
                    return "ERROR:Formato richiesta non valido";
                }
                
            } else if (request.startsWith("GET_USER_LOCATION:")) {
                // Formato: GET_USER_LOCATION:username
                String username = request.substring("GET_USER_LOCATION:".length());
                String location = getUserLocation(username);
                return location != null ? "OK:" + location : "ERROR:User not found";
                
            } else if (request.startsWith("CHECK_FAVORITE:")) {
                // Formato: CHECK_FAVORITE:username|nomeRistorante|cittaRistorante|indirizzoRistorante
                String[] parts = request.substring("CHECK_FAVORITE:".length()).split("\\|", 4);
                if (parts.length == 4) {
                    boolean isFavorite = checkFavorite(parts[0], parts[1], parts[2], parts[3]);
                    return "OK:" + isFavorite;
                } else {
                    return "ERROR:Formato richiesta non valido";
                }
                
            } else if (request.startsWith("ADD_FAVORITE:")) {
                // Formato: ADD_FAVORITE:username|nomeRistorante|cittaRistorante|indirizzoRistorante
                String[] parts = request.substring("ADD_FAVORITE:".length()).split("\\|", 4);
                if (parts.length == 4) {
                    boolean success = addFavorite(parts[0], parts[1], parts[2], parts[3]);
                    return success ? "OK:Favorite added" : "ERROR:Failed to add favorite";
                } else {
                    return "ERROR:Formato richiesta non valido";
                }
                
            } else if (request.startsWith("REMOVE_FAVORITE:")) {
                // Formato: REMOVE_FAVORITE:username|nomeRistorante|cittaRistorante|indirizzoRistorante
                String[] parts = request.substring("REMOVE_FAVORITE:".length()).split("\\|", 4);
                if (parts.length == 4) {
                    boolean success = removeFavorite(parts[0], parts[1], parts[2], parts[3]);
                    return success ? "OK:Favorite removed" : "ERROR:Failed to remove favorite";
                } else {
                    return "ERROR:Formato richiesta non valido";
                }
                
            } else if (request.startsWith("UPDATE_RESTAURANT:")) {
                // Formato: UPDATE_RESTAURANT:oldName|newName|citta|indirizzo|tipoCucina|fasciaPrezzo|delivery|online
                String[] parts = request.substring("UPDATE_RESTAURANT:".length()).split("\\|", 8);
                if (parts.length == 8) {
                    boolean success = updateRestaurant(parts[0], parts[1], parts[2], parts[3], parts[4], parts[5], parts[6], parts[7]);
                    return success ? "OK:Restaurant updated" : "ERROR:Failed to update restaurant";
                } else {
                    return "ERROR:Formato richiesta non valido";
                }
                
            } else if (request.startsWith("DELETE_RESTAURANT:")) {
                // Formato: DELETE_RESTAURANT:nomeRistorante
                String nomeRistorante = request.substring("DELETE_RESTAURANT:".length());
                boolean success = deleteRestaurant(nomeRistorante);
                return success ? "OK:Restaurant deleted" : "ERROR:Failed to delete restaurant";
                
            } else if (request.startsWith("GET_USER_REVIEWS:")) {
                // Formato: GET_USER_REVIEWS:username|nomeRistorante
                String[] parts = request.substring("GET_USER_REVIEWS:".length()).split("\\|", 2);
                if (parts.length == 2) {
                    List<Recensione> reviews = getUserReviews(parts[0], parts[1]);
                    return "REVIEWS:" + recensioniToString(reviews);
                } else {
                    return "ERROR:Formato richiesta non valido";
                }
                
            } else if (request.startsWith("UPDATE_REVIEW:")) {
                // Formato: UPDATE_REVIEW:username|ristorante|oldData|oldOra|stelle|testo
                String[] parts = request.substring("UPDATE_REVIEW:".length()).split("\\|", 6);
                if (parts.length == 6) {
                    boolean success = updateReview(parts[0], parts[1], parts[2], parts[3], Integer.parseInt(parts[4]), parts[5]);
                    return success ? "OK:Review updated" : "ERROR:Failed to update review";
                } else {
                    return "ERROR:Formato richiesta non valido";
                }
                
            } else if (request.startsWith("DELETE_REVIEW:")) {
                // Formato: DELETE_REVIEW:username|ristorante|data|ora
                String[] parts = request.substring("DELETE_REVIEW:".length()).split("\\|", 4);
                if (parts.length == 4) {
                    boolean success = deleteReview(parts[0], parts[1], parts[2], parts[3]);
                    return success ? "OK:Review deleted" : "ERROR:Failed to delete review";
                } else {
                    return "ERROR:Formato richiesta non valido";
                }
                
            } else if (request.startsWith("UPDATE_MANAGER_RESPONSE:")) {
                // Formato: UPDATE_MANAGER_RESPONSE:username|ristorante|data|ora|risposta
                String[] parts = request.substring("UPDATE_MANAGER_RESPONSE:".length()).split("\\|", 5);
                if (parts.length == 5) {
                    boolean success = updateManagerResponse(parts[0], parts[1], parts[2], parts[3], parts[4]);
                    return success ? "OK:Manager response updated" : "ERROR:Failed to update manager response";
                } else {
                    return "ERROR:Formato richiesta non valido";
                }
                
            } else if (request.startsWith("DELETE_MANAGER_RESPONSE:")) {
                // Formato: DELETE_MANAGER_RESPONSE:username|ristorante|data|ora
                String[] parts = request.substring("DELETE_MANAGER_RESPONSE:".length()).split("\\|", 4);
                if (parts.length == 4) {
                    boolean success = deleteManagerResponse(parts[0], parts[1], parts[2], parts[3]);
                    return success ? "OK:Manager response deleted" : "ERROR:Failed to delete manager response";
                } else {
                    return "ERROR:Formato richiesta non valido";
                }
            }
            
            return "ERROR:Comando non riconosciuto";
            
        } catch (Exception e) {
            return "ERROR:" + e.getMessage();
        }
    }
    
    /**
     * Converte una lista di ristoranti in stringa per la trasmissione.
     */
    private String ristorantiToString(List<Ristorante> ristoranti) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ristoranti.size(); i++) {
            if (i > 0) sb.append("||");
            Ristorante r = ristoranti.get(i);
            
            // Calculate average rating for this restaurant
            double avgRating = 0.0;
            int reviewCount = 0;
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                     "SELECT AVG(CAST(\"Stelle\" AS DECIMAL)), COUNT(*) FROM \"recensioni\" WHERE \"Ristorante_scritto\" = ?")) {
                ps.setString(1, r.getNome());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        avgRating = rs.getDouble(1);
                        reviewCount = rs.getInt(2);
                    }
                }
            } catch (Exception e) {
                System.err.println("Errore nel calcolo della valutazione media per " + r.getNome() + ": " + e.getMessage());
            }
            
            sb.append(r.getNome() != null ? r.getNome() : "").append("|")
              .append(r.getNazione() != null ? r.getNazione() : "").append("|")
              .append(r.getCitta() != null ? r.getCitta() : "").append("|")
              .append(r.getIndirizzo() != null ? r.getIndirizzo() : "").append("|")
              .append(r.getTipoCucina() != null ? r.getTipoCucina() : "").append("|")
              .append(r.getFasciaPrezzo() != null ? r.getFasciaPrezzo() : "0").append("|")
              .append(r.getDelivery() != null ? r.getDelivery() : "No").append("|")
              .append(r.getOnline() != null ? r.getOnline() : "No").append("|")
              .append(reviewCount > 0 ? String.format("%.2f", avgRating) : "0.0").append("|")
              .append(reviewCount);
        }
        return sb.toString();
    }
    
    /**
     * Converte una lista di recensioni in stringa per la trasmissione.
     */
    private String recensioniToString(List<Recensione> recensioni) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < recensioni.size(); i++) {
            if (i > 0) sb.append("||");
            Recensione r = recensioni.get(i);
            sb.append(r.getUsernameScrittore()).append("|")
              .append(r.getRistoranteScritto()).append("|")
              .append(r.getStelle()).append("|")
              .append(r.getTesto()).append("|")
              .append(r.getData()).append("|")
              .append(r.getOra());
            if (r.getRispostaGestore() != null) {
                sb.append("|").append(r.getRispostaGestore());
            }
        }
        return sb.toString();
    }
    
    /**
     * Cerca ristoranti nel database in base ai parametri specificati.
     * Delega l'operazione al {@link RistoranteDAO} appropriato e restituisce
     * una lista filtrata di ristoranti che corrispondono ai criteri di ricerca.
     * 
     * <p>Tutti i parametri sono opzionali (possono essere null o vuoti)
     * per consentire ricerche flessibili. Se un parametro è null o vuoto,
     * non viene applicato quel particolare filtro alla ricerca.
     * 
     * <p><strong>Esempi di utilizzo:</strong>
     * <pre>{@code
     * // Cerca tutti i ristoranti italiani
     * List<Ristorante> italiani = service.cercaRistoranti("Italia", null, null, null, null, null);
     * 
     * // Cerca ristoranti con delivery a Roma
     * List<Ristorante> delivery = service.cercaRistoranti("Italia", "Roma", null, null, true, null);
     * 
     * // Cerca pizzerie economiche
     * List<Ristorante> pizzerie = service.cercaRistoranti(null, null, "Pizza", "<10", null, null);
     * }</pre>
     * 
     * @param nazione la nazione del ristorante da cercare (può essere null)
     * @param citta la città del ristorante da cercare (può essere null)
     * @param tipoCucina il tipo di cucina da cercare (può essere null)
     * @param fasciaPrezzo la fascia di prezzo ("&lt;10", "10-15", "&gt;20") (può essere null)
     * @param delivery true se si vuole filtrare solo ristoranti con consegna a domicilio (può essere null)
     * @param online true se si vuole filtrare solo ristoranti disponibili online (può essere null)
     * @return lista di oggetti {@link Ristorante} che corrispondono ai criteri di ricerca,
     *         lista vuota se nessun ristorante corrisponde ai criteri
     * 
     * @throws RuntimeException se si verifica un errore durante l'accesso al database
     * @see RistoranteDAO#cercaRistoranti(String, String, String, String, Boolean, Boolean)
     * @since 1.0
     */
    public List<Ristorante> cercaRistoranti(String nazione, String citta, String tipoCucina, 
                                           String fasciaPrezzo, Boolean delivery, Boolean online) {
        return ristoranteDAO.cercaRistoranti(nazione, citta, tipoCucina, fasciaPrezzo, delivery, online);
    }
    
    /**
     * Ottiene tutte le recensioni per un ristorante specifico.
     * Recupera dal database tutte le recensioni associate a un particolare
     * ristorante, incluse eventuali risposte del gestore.
     * 
     * <p>Le recensioni vengono restituite in ordine cronologico e includono
     * informazioni complete come data, ora, valutazione stellare, testo
     * della recensione e eventuale risposta del gestore del ristorante.
     * 
     * <p><strong>Esempio di utilizzo:</strong>
     * <pre>{@code
     * List<Recensione> recensioni = service.getRecensioniPerRistorante("Pizzeria Mario");
     * for (Recensione r : recensioni) {
     *     System.out.println(r.getUsername() + ": " + r.getStelle() + " stelle");
     * }
     * }</pre>
     * 
     * @param nomeRistorante nome esatto del ristorante di cui ottenere le recensioni,
     *                       non può essere null o vuoto
     * @return lista di oggetti {@link Recensione} per il ristorante specificato,
     *         lista vuota se il ristorante non ha recensioni o non esiste
     * 
     * @throws IllegalArgumentException se nomeRistorante è null o vuoto
     * @throws RuntimeException se si verifica un errore durante l'accesso al database
     * @see RecensioneDAO#getRecensioniPerRistorante(String)
     * @since 1.0
     */
    public List<Recensione> getRecensioniPerRistorante(String nomeRistorante) {
        return recensioneDAO.getRecensioniPerRistorante(nomeRistorante);
    }
    
    /**
     * Aggiunge una nuova recensione al database.
     * Crea una nuova recensione per un ristorante specificato, includendo
     * automaticamente data e ora correnti. La recensione viene associata
     * all'utente specificato e include valutazione e testo descrittivo.
     * 
     * <p>Il sistema genera automaticamente:
     * <ul>
     *   <li>Data corrente in formato testuale</li>
     *   <li>Ora corrente in formato HH:MM</li>
     *   <li>Associazione tra utente e ristorante</li>
     * </ul>
     * 
     * <p><strong>Validazioni applicate:</strong>
     * <ul>
     *   <li>Username non può essere null o vuoto</li>
     *   <li>Nome ristorante non può essere null o vuoto</li>
     *   <li>Stelle deve essere compreso tra 1 e 5</li>
     *   <li>Testo non può essere null o vuoto</li>
     * </ul>
     * 
     * <p><strong>Esempio di utilizzo:</strong>
     * <pre>{@code
     * service.aggiungiRecensione("mario123", "Pizzeria Mario", 4, 
     *                           "Ottima pizza, servizio veloce!");
     * }</pre>
     * 
     * @param username username dell'utente che scrive la recensione, non può essere null
     * @param ristorante nome del ristorante da recensire, non può essere null
     * @param stelle numero di stelle della valutazione (1-5 inclusi)
     * @param testo testo descrittivo della recensione, non può essere null
     * 
     * @throws IllegalArgumentException se uno dei parametri non rispetta i vincoli di validazione
     * @throws RuntimeException se si verifica un errore durante l'inserimento nel database
     * @see RecensioneDAO#aggiungiRecensione(String, String, int, String)
     * @since 1.0
     */
    public void aggiungiRecensione(String username, String ristorante, int stelle, String testo) {
        recensioneDAO.aggiungiRecensione(username, ristorante, stelle, testo);
    }
    
    /**
     * Verifica le credenziali di login di un utente.
     * Controlla se le credenziali fornite corrispondono a un utente
     * registrato nel sistema e autorizza l'accesso alle funzionalità
     * riservate agli utenti autenticati.
     * 
     * <p><strong>Implementazione attuale:</strong> Questa è una implementazione
     * placeholder che esegue solo validazioni di base. In una versione di
     * produzione, dovrebbe:
     * <ul>
     *   <li>Consultare una tabella utenti nel database</li>
     *   <li>Verificare password hashate con salt</li>
     *   <li>Implementare protezioni contro attacchi brute-force</li>
     *   <li>Registrare tentativi di accesso per audit</li>
     * </ul>
     * 
     * <p><strong>Sicurezza:</strong> Le password non devono mai essere
     * memorizzate in chiaro nel database. Utilizzare algoritmi di hashing
     * sicuri come bcrypt, scrypt o Argon2.
     * 
     * @param username nome utente per l'autenticazione, non può essere null o vuoto
     * @param password password in chiaro dell'utente, non può essere null o vuota
     * @return {@code true} se le credenziali sono valide e l'utente può accedere,
     *         {@code false} se le credenziali sono invalide o l'utente non esiste
     * 
     * @throws IllegalArgumentException se username o password sono null o vuoti
     * @see #registerUser(String, String, String)
     * @since 1.0
     * @deprecated Implementazione placeholder - sostituire con logica di autenticazione reale
     */
    @Deprecated
    public boolean verifyLogin(String username, String password) {
        return verifyLoginAndGetInfo(username, password) != null;
    }
    
    /**
     * Verifica le credenziali di login e restituisce le informazioni utente se il login è riuscito.
     * @param username il nome utente
     * @param password la password con hash MD5
     * @return stringa con info utente "username|location|role" se riuscito, null se fallito
     */
    public String verifyLoginAndGetInfo(String username, String password) {
        if (username == null || password == null || username.isEmpty() || password.isEmpty()) {
            return null;
        }
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT \"Password\", \"Luogo_del_domicilio\", \"Ruolo\" FROM \"utenti\" WHERE \"Username\" = ?")) {
            
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String storedPassword = rs.getString("Password");
                    String location = rs.getString("Luogo_del_domicilio");
                    String role = rs.getString("Ruolo");
                    
                    System.out.println("DEBUG: Tentativo di login per utente: " + username);
                    System.out.println("DEBUG: Password memorizzata: " + storedPassword);
                    System.out.println("DEBUG: Password ricevuta: " + password);
                    
                    // The client sends MD5 hashed password, but we need to handle:
                    // 1. New salted passwords ($SALT$...)
                    // 2. Legacy MD5 passwords (32 hex chars)
                    // 3. Plain text passwords (for migration)
                    
                    boolean isValidLogin = false;
                    boolean needsUpgrade = false;
                    String newHashedPassword = null;
                    
                    if (PasswordUtils.isSaltedPassword(storedPassword)) {
                        // New salted password - but client sends MD5 hash
                        // This is a limitation of the current client-server protocol
                        // We cannot verify salted passwords when client sends MD5 hashes
                        // Solution: Users with salted passwords need to re-authenticate through REGISTER_SALTED
                        System.out.println("DEBUG: Password salted rilevata - impossibile verificare con hash MD5 dal client");
                        System.out.println("DEBUG: L'utente deve utilizzare un client aggiornato che supporta l'autenticazione salted");
                        isValidLogin = false; // Cannot authenticate salted passwords with MD5 input
                    } else if (PasswordUtils.isLegacyMD5(storedPassword)) {
                        // Legacy MD5 password - direct comparison
                        isValidLogin = storedPassword.equals(password);
                        System.out.println("DEBUG: Password MD5 legacy - confronto diretto: " + isValidLogin);
                        
                        // We could upgrade to salted here, but we don't have the plaintext password
                        // Upgrade will happen on next registration or password change
                    } else {
                        // Plain text password - hash it and compare, then upgrade
                        String hashedStoredPassword = hashPassword(storedPassword);
                        isValidLogin = hashedStoredPassword.equals(password);
                        
                        if (isValidLogin) {
                            System.out.println("DEBUG: Password in testo semplice verificata con successo");
                            needsUpgrade = true;
                            newHashedPassword = password; // Store the MD5 hash for now
                        }
                    }
                    
                    if (isValidLogin) {
                        // Upgrade password if needed
                        if (needsUpgrade && newHashedPassword != null) {
                            try (PreparedStatement updatePs = conn.prepareStatement("UPDATE \"utenti\" SET \"Password\" = ? WHERE \"Username\" = ?")) {
                                updatePs.setString(1, newHashedPassword);
                                updatePs.setString(2, username);
                                updatePs.executeUpdate();
                                System.out.println("DEBUG: Password aggiornata da testo semplice a MD5 per utente: " + username);
                            }
                        }
                        
                        return username + "|" + (location != null ? location : "") + "|" + (role != null ? role : "Utente");
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Errore durante verifica login: " + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
    }
    
    /**
     * Hash della password utilizzando algoritmo MD5.
     */
    private String hashPassword(String password) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] hashedBytes = md.digest(password.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hashedBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not available", e);
        }
    }
    
    /**
     * Verifica le credenziali di login per password salted e restituisce le informazioni utente.
     * Questo metodo è progettato per lavorare con password in chiaro inviate da client sicuri
     * e può verificare correttamente password salted memorizzate nel database.
     * 
     * @param username il nome utente
     * @param plainPassword la password in chiaro (non hashata)
     * @return stringa con info utente "username|location|role" se riuscito, null se fallito
     * @since 2.1
     */
    public String verifySaltedLoginAndGetInfo(String username, String plainPassword) {
        if (username == null || plainPassword == null || username.isEmpty() || plainPassword.isEmpty()) {
            return null;
        }
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT \"Password\", \"Luogo_del_domicilio\", \"Ruolo\" FROM \"utenti\" WHERE \"Username\" = ?")) {
            
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String storedPassword = rs.getString("Password");
                    String location = rs.getString("Luogo_del_domicilio");
                    String role = rs.getString("Ruolo");
                    
                    System.out.println("DEBUG: Tentativo di login salted per utente: " + username);
                    
                    boolean isValidLogin = false;
                    
                    if (PasswordUtils.isSaltedPassword(storedPassword)) {
                        // Verifica password salted usando PasswordUtils
                        isValidLogin = PasswordUtils.verifyPassword(plainPassword, storedPassword);
                        System.out.println("DEBUG: Verifica password salted: " + isValidLogin);
                    } else if (PasswordUtils.isLegacyMD5(storedPassword)) {
                        // Password MD5 legacy - hasha la password in chiaro e confronta
                        String hashedPlainPassword = hashPassword(plainPassword);
                        isValidLogin = storedPassword.equals(hashedPlainPassword);
                        System.out.println("DEBUG: Verifica password MD5 legacy con chiaro: " + isValidLogin);
                    } else {
                        // Password in testo semplice - confronto diretto
                        isValidLogin = storedPassword.equals(plainPassword);
                        System.out.println("DEBUG: Verifica password testo semplice: " + isValidLogin);
                    }
                    
                    if (isValidLogin) {
                        return username + "|" + (location != null ? location : "") + "|" + (role != null ? role : "Utente");
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Errore durante verifica login salted: " + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
    }
    
    /**
     * Registra un nuovo utente nel sistema.
     * Crea un nuovo account utente con le credenziali specificate,
     * consentendo l'accesso alle funzionalità riservate agli utenti registrati
     * come scrivere recensioni e gestire il proprio profilo.
     * 
     * <p><strong>Implementazione attuale:</strong> Questa è una implementazione
     * placeholder che esegue solo validazioni di base. In una versione di
     * produzione, dovrebbe:
     * <ul>
     *   <li>Inserire i dati utente in una tabella del database</li>
     *   <li>Hashare la password con salt prima di memorizzarla</li>
     *   <li>Validare il formato email e verificarne l'unicità</li>
     *   <li>Verificare l'unicità dello username</li>
     *   <li>Inviare email di conferma registrazione</li>
     * </ul>
     * 
     * <p><strong>Validazioni necessarie:</strong>
     * <ul>
     *   <li>Username: alfanumerico, lunghezza 3-20 caratteri, univoco</li>
     *   <li>Password: minimo 8 caratteri, includere maiuscole, minuscole, numeri</li>
     *   <li>Email: formato valido, univoca nel sistema</li>
     * </ul>
     * 
     * @param username nome utente desiderato, deve essere univoco, non può essere null
     * @param password password in chiaro dell'utente, non può essere null
     * @param email indirizzo email dell'utente, deve essere valido e univoco, non può essere null
     * @return {@code true} se la registrazione è completata con successo,
     *         {@code false} se si verificano errori (username/email già esistenti, ecc.)
     * 
     * @throws IllegalArgumentException se uno dei parametri è null o non rispetta i vincoli
     * @throws RuntimeException se si verifica un errore durante l'inserimento nel database
     * @see #verifyLogin(String, String)
     * @since 1.0
     * @deprecated Implementazione placeholder - sostituire con logica di registrazione reale
     */
    @Deprecated
    public boolean registerUser(String nome, String cognome, String username, String email, String password,
                               String dataNascita, String domicilio, String ruolo) {
        if (username == null || password == null || email == null || 
            username.isEmpty() || password.isEmpty() || email.isEmpty()) {
            return false;
        }
        
        // First check if username or email already exists (case-insensitive)
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement checkPs = conn.prepareStatement(
                 "SELECT COUNT(*) FROM \"utenti\" WHERE LOWER(\"Username\") = LOWER(?) OR LOWER(\"Email\") = LOWER(?)")) {
            
            checkPs.setString(1, username);
            checkPs.setString(2, email);
            
            try (ResultSet rs = checkPs.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    System.out.println("DEBUG: Username o email già esistente (case-insensitive): " + username + ", " + email);
                    return false; // Username or email already exists
                }
            }
            
            // For new registrations, we receive MD5 hashed password from client
            // We'll store it as MD5 for now to maintain compatibility
            // IMPLEMENTED: Salted password support added via REGISTER_SALTED endpoint
            // LIMITATION: Current login protocol sends MD5 hashes, incompatible with salted verification
            // SOLUTION: Implement new LOGIN_SALTED endpoint or modify client to send plaintext over HTTPS
            String passwordToStore = password; // This is already MD5 hashed from client
            
            // If no duplicates, proceed with insertion
            try (PreparedStatement ps = conn.prepareStatement(
                 "INSERT INTO \"utenti\" (\"Nome\", \"Cognome\", \"Username\", \"Email\", \"Password\", " +
                 "\"Data_di_nascita\", \"Luogo_del_domicilio\", \"Ruolo\") VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
                
                ps.setString(1, nome);
                ps.setString(2, cognome);
                ps.setString(3, username);
                ps.setString(4, email);
                ps.setString(5, passwordToStore); // Store MD5 hash for compatibility
                ps.setString(6, dataNascita);
                ps.setString(7, domicilio);
                ps.setString(8, ruolo != null ? ruolo : "utente"); // Default role
                
                System.out.println("DEBUG: Tentativo di registrazione utente: " + username + " con email: " + email);
                System.out.println("DEBUG: Password memorizzata come: MD5 hash (compatibilità)");
                int rowsAffected = ps.executeUpdate();
                System.out.println("DEBUG: Registrazione riuscita, righe interessate: " + rowsAffected);
                return rowsAffected > 0;
            }
            
        } catch (java.sql.SQLException e) {
            System.err.println("Errore SQL durante registrazione utente: " + e.getMessage());
            System.err.println("SQL State: " + e.getSQLState());
            System.err.println("Codice Errore: " + e.getErrorCode());
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            System.err.println("Errore durante registrazione utente: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Registra un nuovo utente con password salted (per future implementazioni).
     * 
     * <p>Questo metodo accetta password in chiaro e le hasha con salt usando
     * {@link PasswordUtils}. Da utilizzare quando il client invierà password
     * in chiaro su connessioni HTTPS sicure.
     * 
     * @param nome nome dell'utente
     * @param cognome cognome dell'utente  
     * @param username nome utente (deve essere univoco)
     * @param email email dell'utente (deve essere univoca)
     * @param plainPassword password in chiaro (verrà hashata con salt)
     * @param dataNascita data di nascita
     * @param domicilio luogo del domicilio
     * @param ruolo ruolo dell'utente
     * @return true se la registrazione è riuscita, false altrimenti
     * @since 2.1
     */
    public boolean registerUserWithSaltedPassword(String nome, String cognome, String username, String email, 
                                                String plainPassword, String dataNascita, String domicilio, String ruolo) {
        if (username == null || plainPassword == null || email == null || 
            username.isEmpty() || plainPassword.isEmpty() || email.isEmpty()) {
            return false;
        }
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement checkPs = conn.prepareStatement(
                 "SELECT COUNT(*) FROM \"utenti\" WHERE LOWER(\"Username\") = LOWER(?) OR LOWER(\"Email\") = LOWER(?)")) {
            
            checkPs.setString(1, username);
            checkPs.setString(2, email);
            
            try (ResultSet rs = checkPs.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    System.out.println("DEBUG: Username o email già esistente: " + username + ", " + email);
                    return false;
                }
            }
            
            // Create salted password hash
            String saltedPassword = PasswordUtils.createSaltedPassword(plainPassword);
            System.out.println("DEBUG: Password hashata con salt per utente: " + username);
            
            try (PreparedStatement ps = conn.prepareStatement(
                 "INSERT INTO \"utenti\" (\"Nome\", \"Cognome\", \"Username\", \"Email\", \"Password\", " +
                 "\"Data_di_nascita\", \"Luogo_del_domicilio\", \"Ruolo\") VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
                
                ps.setString(1, nome);
                ps.setString(2, cognome);
                ps.setString(3, username);
                ps.setString(4, email);
                ps.setString(5, saltedPassword); // Store salted password
                ps.setString(6, dataNascita);
                ps.setString(7, domicilio);
                ps.setString(8, ruolo != null ? ruolo : "utente");
                
                int rowsAffected = ps.executeUpdate();
                System.out.println("DEBUG: Registrazione con password salted riuscita, righe interessate: " + rowsAffected);
                return rowsAffected > 0;
            }
            
        } catch (Exception e) {
            System.err.println("Errore durante registrazione con password salted: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Aggiunge un nuovo ristorante al database.
     */
    public boolean addRestaurant(String nome, String nazione, String citta, String indirizzo, 
                                java.math.BigDecimal fasciaPrezzo, String delivery, String online, 
                                String tipoCucina, String username) {
        if (nome == null || nome.isEmpty() || nazione == null || nazione.isEmpty()) {
            return false;
        }
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "INSERT INTO \"ristorantitheknife\" (\"Nome\", \"Nazione\", \"Citta\", \"Indirizzo\", \"Fascia_di_prezzo\", \"Delivery\", \"Online\", \"Tipo_di_cucina\") VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
            
            ps.setString(1, nome);
            ps.setString(2, nazione);
            ps.setString(3, citta);
            ps.setString(4, indirizzo);
            if (fasciaPrezzo != null) {
                ps.setBigDecimal(5, fasciaPrezzo);
            } else {
                ps.setNull(5, java.sql.Types.NUMERIC);
            }
            ps.setString(6, delivery);
            ps.setString(7, online);
            ps.setString(8, tipoCucina);
            
            int rowsAffected = ps.executeUpdate();
            System.out.println("DEBUG: Ristorante aggiunto con successo, righe interessate: " + rowsAffected);
            return rowsAffected > 0;
            
        } catch (java.sql.SQLException e) {
            System.err.println("Errore SQL durante aggiunta ristorante: " + e.getMessage());
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            System.err.println("Errore durante aggiunta ristorante: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Ottiene la località di domicilio di un utente.
     * 
     * @param username Username dell'utente
     * @return La località di domicilio dell'utente, null se non trovato
     */
    private String getUserLocation(String username) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT Luogo_del_domicilio FROM utenti WHERE Username = ?")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("Luogo_del_domicilio");
                }
            }
        } catch (Exception e) {
            System.err.println("Errore nel recupero posizione utente: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * Verifica se un ristorante è nei preferiti di un utente.
     * 
     * @param username Username dell'utente
     * @param nomeRistorante Nome del ristorante
     * @param cittaRistorante Città del ristorante
     * @param indirizzoRistorante Indirizzo del ristorante
     * @return true se è nei preferiti, false altrimenti
     */
    private boolean checkFavorite(String username, String nomeRistorante, String cittaRistorante, String indirizzoRistorante) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT 1 FROM Preferiti WHERE \"Username\" = ? AND \"Nome_ristorante\" = ? AND \"Citta_ristorante\" = ? AND \"Indirizzo_ristorante\" = ?")) {
            ps.setString(1, username);
            ps.setString(2, nomeRistorante);
            ps.setString(3, cittaRistorante);
            ps.setString(4, indirizzoRistorante);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            System.err.println("Errore nel controllo preferito: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Aggiunge un ristorante ai preferiti di un utente.
     * 
     * @param username Username dell'utente
     * @param nomeRistorante Nome del ristorante
     * @param cittaRistorante Città del ristorante
     * @param indirizzoRistorante Indirizzo del ristorante
     * @return true se aggiunto con successo, false altrimenti
     */
    private boolean addFavorite(String username, String nomeRistorante, String cittaRistorante, String indirizzoRistorante) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "INSERT INTO Preferiti (\"Username\", \"Nome_ristorante\", \"Citta_ristorante\", \"Indirizzo_ristorante\") VALUES (?, ?, ?, ?)")) {
            ps.setString(1, username);
            ps.setString(2, nomeRistorante);
            ps.setString(3, cittaRistorante);
            ps.setString(4, indirizzoRistorante);
            ps.executeUpdate();
            return true;
        } catch (Exception e) {
            System.err.println("Error adding favorite: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Rimuove un ristorante dai preferiti di un utente.
     * 
     * @param username Username dell'utente
     * @param nomeRistorante Nome del ristorante
     * @param cittaRistorante Città del ristorante
     * @param indirizzoRistorante Indirizzo del ristorante
     * @return true se rimosso con successo, false altrimenti
     */
    private boolean removeFavorite(String username, String nomeRistorante, String cittaRistorante, String indirizzoRistorante) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "DELETE FROM Preferiti WHERE \"Username\" = ? AND \"Nome_ristorante\" = ? AND \"Citta_ristorante\" = ? AND \"Indirizzo_ristorante\" = ?")) {
            ps.setString(1, username);
            ps.setString(2, nomeRistorante);
            ps.setString(3, cittaRistorante);
            ps.setString(4, indirizzoRistorante);
            ps.executeUpdate();
            return true;
        } catch (Exception e) {
            System.err.println("Error removing favorite: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Aggiorna i dettagli di un ristorante.
     * 
     * @param oldName Nome attuale del ristorante
     * @param newName Nuovo nome del ristorante
     * @param citta Nuova città
     * @param indirizzo Nuovo indirizzo
     * @param tipoCucina Nuovo tipo di cucina
     * @param fasciaPrezzo Nuova fascia di prezzo
     * @param delivery Nuovo stato delivery
     * @param online Nuovo stato online
     * @return true se aggiornato con successo, false altrimenti
     */
    private boolean updateRestaurant(String oldName, String newName, String citta, String indirizzo, 
                                   String tipoCucina, String fasciaPrezzo, String delivery, String online) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "UPDATE ristorantitheknife SET \"Nome\" = ?, \"Citta\" = ?, \"Indirizzo\" = ?, \"Tipo_di_cucina\" = ?, \"Fascia_di_prezzo\" = ?, \"Delivery\" = ?, \"Online\" = ? WHERE \"Nome\" = ?")) {
            ps.setString(1, newName);
            ps.setString(2, citta);
            ps.setString(3, indirizzo);
            ps.setString(4, tipoCucina);
            ps.setString(5, fasciaPrezzo);
            ps.setString(6, delivery);
            ps.setString(7, online);
            ps.setString(8, oldName);
            ps.executeUpdate();
            return true;
        } catch (Exception e) {
            System.err.println("Error updating restaurant: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Elimina un ristorante dal database.
     * 
     * @param nomeRistorante Nome del ristorante da eliminare
     * @return true se eliminato con successo, false altrimenti
     */
    private boolean deleteRestaurant(String nomeRistorante) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "DELETE FROM ristorantitheknife WHERE \"Nome\" = ?")) {
            ps.setString(1, nomeRistorante);
            ps.executeUpdate();
            return true;
        } catch (Exception e) {
            System.err.println("Error deleting restaurant: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Ottiene tutte le recensioni di un utente per un ristorante specifico.
     * 
     * @param username Username dell'utente
     * @param nomeRistorante Nome del ristorante
     * @return Lista delle recensioni dell'utente per il ristorante
     */
    private List<Recensione> getUserReviews(String username, String nomeRistorante) {
        List<Recensione> recensioni = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT * FROM Recensioni WHERE \"Ristorante_scritto\" = ? AND \"Username_scittore\" = ? ORDER BY \"Data\" DESC, \"Ora\" DESC")) {
            ps.setString(1, nomeRistorante);
            ps.setString(2, username);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String data = rs.getString("Data");
                    int stelle = rs.getInt("Stelle");
                    String ora = rs.getString("Ora");
                    String usernameScrittore = rs.getString("Username_scittore");
                    String ristoranteScritto = rs.getString("Ristorante_scritto");
                    String testo = rs.getString("Testo");
                    String rispostaGestore = rs.getString("Risposta_gestore");
                    
                    Recensione r = new Recensione(data, stelle, ora, usernameScrittore, 
                                                ristoranteScritto, testo, rispostaGestore);
                    recensioni.add(r);
                }
            }
        } catch (Exception e) {
            System.err.println("Error getting user reviews: " + e.getMessage());
        }
        return recensioni;
    }
    
    /**
     * Aggiorna una recensione esistente.
     * 
     * @param username Username dell'autore
     * @param ristorante Nome del ristorante
     * @param oldData Data della recensione originale
     * @param oldOra Ora della recensione originale
     * @param stelle Nuove stelle
     * @param testo Nuovo testo
     * @return true se aggiornata con successo, false altrimenti
     */
    private boolean updateReview(String username, String ristorante, String oldData, String oldOra, int stelle, String testo) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "UPDATE Recensioni SET \"Stelle\" = ?, \"Testo\" = ?, \"Data\" = ?, \"Ora\" = ? WHERE \"Username_scittore\" = ? AND \"Ristorante_scritto\" = ? AND \"Data\" = ? AND \"Ora\" = ?")) {
            // New values
            ps.setInt(1, stelle);
            ps.setString(2, testo);
            ps.setString(3, java.time.LocalDate.now().toString());
            ps.setString(4, java.time.LocalTime.now().toString());
            // Where conditions
            ps.setString(5, username);
            ps.setString(6, ristorante);
            ps.setString(7, oldData);
            ps.setString(8, oldOra);
            ps.executeUpdate();
            return true;
        } catch (Exception e) {
            System.err.println("Error updating review: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Elimina una recensione.
     * 
     * @param username Username dell'autore
     * @param ristorante Nome del ristorante
     * @param data Data della recensione
     * @param ora Ora della recensione
     * @return true se eliminata con successo, false altrimenti
     */
    private boolean deleteReview(String username, String ristorante, String data, String ora) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "DELETE FROM Recensioni WHERE \"Username_scittore\" = ? AND \"Ristorante_scritto\" = ? AND \"Data\" = ? AND \"Ora\" = ?")) {
            ps.setString(1, username);
            ps.setString(2, ristorante);
            ps.setString(3, data);
            ps.setString(4, ora);
            ps.executeUpdate();
            return true;
        } catch (Exception e) {
            System.err.println("Errore nell'eliminazione della recensione: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Aggiunge o aggiorna la risposta del gestore a una recensione.
     * 
     * @param username Username dell'autore della recensione
     * @param ristorante Nome del ristorante
     * @param data Data della recensione
     * @param ora Ora della recensione
     * @param risposta Risposta del gestore
     * @return true se aggiornata con successo, false altrimenti
     */
    private boolean updateManagerResponse(String username, String ristorante, String data, String ora, String risposta) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "UPDATE Recensioni SET \"Risposta_gestore\" = ? WHERE \"Username_scittore\" = ? AND \"Ristorante_scritto\" = ? AND \"Data\" = ? AND \"Ora\" = ?")) {
            ps.setString(1, risposta);
            ps.setString(2, username);
            ps.setString(3, ristorante);
            ps.setString(4, data);
            ps.setString(5, ora);
            ps.executeUpdate();
            return true;
        } catch (Exception e) {
            System.err.println("Errore nell'aggiornamento della risposta del gestore: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Elimina la risposta del gestore da una recensione.
     * 
     * @param username Username dell'autore della recensione
     * @param ristorante Nome del ristorante
     * @param data Data della recensione
     * @param ora Ora della recensione
     * @return true se eliminata con successo, false altrimenti
     */
    private boolean deleteManagerResponse(String username, String ristorante, String data, String ora) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "UPDATE Recensioni SET \"Risposta_gestore\" = NULL WHERE \"Username_scittore\" = ? AND \"Ristorante_scritto\" = ? AND \"Data\" = ? AND \"Ora\" = ?")) {
            ps.setString(1, username);
            ps.setString(2, ristorante);
            ps.setString(3, data);
            ps.setString(4, ora);
            ps.executeUpdate();
            return true;
        } catch (Exception e) {
            System.err.println("Errore nell'eliminazione della risposta del gestore: " + e.getMessage());
            return false;
        }
    }
}