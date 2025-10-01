package com.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.List;
import java.math.BigDecimal;

/**
 * Servizio client per la comunicazione con il server TheKnife nell'architettura client-server.
 * 
 * <p>Questa classe implementa il pattern Singleton e fornisce un'interfaccia unificata
 * per tutte le operazioni client-server, inclusa la gestione di ristoranti, recensioni
 * e autenticazione utenti. Il client comunica con il server tramite socket TCP sulla porta 8080.
 * 
 * <h3>Funzionalità Principali</h3>
 * <ul>
 *   <li><strong>Ricerca Ristoranti:</strong> Filtri avanzati per nazione, città, cucina, prezzo</li>
 *   <li><strong>Gestione Recensioni:</strong> Visualizzazione e aggiunta recensioni utenti</li>
 *   <li><strong>Autenticazione:</strong> Login/registrazione con supporto ruoli utente</li>
 *   <li><strong>Gestione Ristoranti:</strong> Creazione ristoranti per utenti gestore</li>
 *   <li><strong>Caching Locale:</strong> Cache delle valutazioni e conteggi recensioni</li>
 * </ul>
 * 
 * <h3>Protocollo di Comunicazione</h3>
 * <p>Il client invia richieste in formato testuale strutturato:
 * <ul>
 *   <li><code>SEARCH_RESTAURANTS:nazione|città|cucina|prezzo|delivery|online</code></li>
 *   <li><code>GET_REVIEWS:nomeRistorante</code></li>
 *   <li><code>ADD_REVIEW:username|ristorante|stelle|testo</code></li>
 *   <li><code>LOGIN:username|passwordMD5</code></li>
 *   <li><code>REGISTER:nome|cognome|username|email|password|data|domicilio|ruolo</code></li>
 *   <li><code>ADD_RESTAURANT:nome|nazione|città|indirizzo|prezzo|delivery|online|cucina|owner</code></li>
 * </ul>
 * 
 * <h3>Gestione Errori</h3>
 * <p>Il servizio gestisce automaticamente:
 * <ul>
 *   <li>Errori di connessione TCP con messaggi utente-friendly</li>
 *   <li>Timeout di rete e recupero automatico</li>
 *   <li>Parsing delle risposte server con validazione formato</li>
 *   <li>Fallback per operazioni non riuscite</li>
 * </ul>
 * 
 * <h3>Pattern di Utilizzo</h3>
 * <pre>{@code
 * // Ottenimento istanza singleton
 * ClientService client = ClientService.getInstance();
 * 
 * // Ricerca ristoranti
 * List<Ristorante> ristoranti = client.cercaRistoranti("Italia", "Roma", null, null, null, null);
 * 
 * // Autenticazione utente
 * String[] userInfo = client.verifyLoginAndGetInfo("username", "hashedPassword");
 * 
 * // Aggiunta recensione
 * boolean success = client.aggiungiRecensione("user", "ristorante", 5, "Ottimo!");
 * }</pre>
 * 
 * <h3>Thread Safety</h3>
 * <p>La classe è thread-safe per l'istanza singleton, ma ogni operazione di rete
 * utilizza connessioni socket separate per evitare conflitti concorrenti.
 * 
 * @author Andrea De Nisco, Antonio De Nisco
 * @version 2.0
 * @since 1.0
 * @see ServerService
 * @see GuestPage
 * @see LoginForm
 */
public class ClientService {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 8080;
    
    private static ClientService instance;
    
    // Store ratings for restaurants
    private java.util.Map<String, Double> restaurantRatings = new java.util.HashMap<>();
    private java.util.Map<String, Integer> restaurantReviewCounts = new java.util.HashMap<>();
    
    /**
     * Costruttore privato per implementare Singleton.
     */
    private ClientService() {}
    
    /**
     * Restituisce l'istanza singleton del servizio client.
     * @return istanza del ClientService
     */
    public static ClientService getInstance() {
        if (instance == null) {
            instance = new ClientService();
        }
        return instance;
    }
    
    /**
     * Invia una richiesta al server e riceve la risposta.
     * @param request richiesta da inviare
     * @return risposta del server
     * @throws IOException se si verifica un errore di comunicazione
     */
    private String sendRequest(String request) throws IOException {
        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            
            out.println(request);
            return in.readLine();
            
        } catch (ConnectException e) {
            throw new IOException("Impossibile connettersi al server. Assicurarsi che il server sia avviato.", e);
        }
    }
    
    /**
     * Cerca ristoranti nel database tramite il server.
     * @param nazione la nazione del ristorante
     * @param citta la città del ristorante  
     * @param tipoCucina il tipo di cucina
     * @param fasciaPrezzo la fascia di prezzo
     * @param delivery consegna a domicilio
     * @param online disponibilità online
     * @return lista di ristoranti trovati
     */
    public List<Ristorante> cercaRistoranti(String nazione, String citta, String tipoCucina, 
                                           String fasciaPrezzo, Boolean delivery, Boolean online) {
        try {
            String request = "SEARCH_RESTAURANTS:" + 
                            (nazione != null ? nazione : "") + "|" +
                            (citta != null ? citta : "") + "|" +
                            (tipoCucina != null ? tipoCucina : "") + "|" +
                            (fasciaPrezzo != null ? fasciaPrezzo : "") + "|" +
                            (delivery != null ? delivery.toString() : "") + "|" +
                            (online != null ? online.toString() : "");
            
            String response = sendRequest(request);
            
            if (response.startsWith("RESTAURANTS:")) {
                return parseRistoranti(response.substring("RESTAURANTS:".length()));
            } else if (response.startsWith("ERROR:")) {
                System.err.println("Errore dal server: " + response.substring("ERROR:".length()));
                return new ArrayList<>();
            }
            
        } catch (IOException e) {
            System.err.println("Errore di comunicazione con il server: " + e.getMessage());
        }
        
        return new ArrayList<>();
    }
    
    /**
     * Ottiene le recensioni per un ristorante tramite il server.
     * @param nomeRistorante nome del ristorante
     * @return lista di recensioni
     */
    public List<Recensione> getRecensioniPerRistorante(String nomeRistorante) {
        try {
            String request = "GET_REVIEWS:" + nomeRistorante;
            String response = sendRequest(request);
            
            if (response.startsWith("REVIEWS:")) {
                return parseRecensioni(response.substring("REVIEWS:".length()));
            } else if (response.startsWith("ERROR:")) {
                System.err.println("Errore dal server: " + response.substring("ERROR:".length()));
                return new ArrayList<>();
            }
            
        } catch (IOException e) {
            System.err.println("Errore di comunicazione con il server: " + e.getMessage());
        }
        
        return new ArrayList<>();
    }
    
    /**
     * Aggiunge una recensione tramite il server.
     * @param username username dell'utente
     * @param ristorante nome del ristorante
     * @param stelle numero di stelle
     * @param testo testo della recensione
     * @return true se l'operazione è riuscita
     */
    public boolean aggiungiRecensione(String username, String ristorante, int stelle, String testo) {
        try {
            String request = "ADD_REVIEW:" + username + "|" + ristorante + "|" + stelle + "|" + testo;
            String response = sendRequest(request);
            
            if (response.startsWith("OK:")) {
                return true;
            } else if (response.startsWith("ERROR:")) {
                System.err.println("Errore dal server: " + response.substring("ERROR:".length()));
                return false;
            }
            
        } catch (IOException e) {
            System.err.println("Errore di comunicazione con il server: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Verifica le credenziali di login tramite il server.
     * @param username nome utente
     * @param password password
     * @return true se il login è riuscito
     */
    public boolean verifyLogin(String username, String password) {
        return verifyLoginAndGetInfo(username, password) != null;
    }
    
    /**
     * Verifica le credenziali di login tramite il server e restituisce le informazioni utente.
     * @param username nome utente
     * @param password password
     * @return array [username, location, role] se il login è riuscito, null altrimenti
     */
    public String[] verifyLoginAndGetInfo(String username, String password) {
        try {
            String request = "LOGIN:" + username + "|" + password;
            String response = sendRequest(request);
            
            if (response.startsWith("OK:")) {
                String userInfo = response.substring("OK:".length());
                if (!userInfo.isEmpty()) {
                    return userInfo.split("\\|", 3);
                }
                return new String[]{username, "", "Utente"}; // fallback
            } else if (response.startsWith("ERROR:")) {
                System.err.println("Errore dal server: " + response.substring("ERROR:".length()));
                return null;
            }
            
        } catch (IOException e) {
            System.err.println("Errore di comunicazione con il server: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Registra un nuovo utente tramite il server.
     * @param username nome utente
     * @param password password
     * @param email email
     * @return true se la registrazione è riuscita
     */
    public boolean registerUser(String nome, String cognome, String username, String email, String password, 
                               String dataNascita, String domicilio, String ruolo) {
        try {
            String request = "REGISTER:" + nome + "|" + cognome + "|" + username + "|" + email + "|" + 
                           password + "|" + dataNascita + "|" + domicilio + "|" + ruolo;
            String response = sendRequest(request);
            
            if (response.startsWith("OK:")) {
                return true;
            } else if (response.startsWith("ERROR:")) {
                System.err.println("Errore dal server: " + response.substring("ERROR:".length()));
                return false;
            }
            
        } catch (IOException e) {
            System.err.println("Errore di comunicazione con il server: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Converte una stringa di ristoranti ricevuta dal server in lista di oggetti Ristorante.
     */
    private List<Ristorante> parseRistoranti(String data) {
        List<Ristorante> ristoranti = new ArrayList<>();
        
        if (data.isEmpty()) {
            return ristoranti;
        }
        
        String[] ristorantiStr = data.split("\\|\\|");
        for (String ristoranteStr : ristorantiStr) {
            String[] parts = ristoranteStr.split("\\|");
            if (parts.length >= 8) {
                String nome = parts[0];
                String nazione = parts[1];
                String citta = parts[2];
                String indirizzo = parts[3];
                String tipoCucina = parts[4];
                BigDecimal fasciaPrezzo = null;
                try {
                    fasciaPrezzo = new BigDecimal(parts[5]);
                } catch (NumberFormatException e) {
                    fasciaPrezzo = BigDecimal.ZERO;
                }
                String delivery = parts[6];
                String online = parts[7];
                
                // Parse rating and review count if available (parts 8 and 9)
                double avgRating = 0.0;
                int reviewCount = 0;
                if (parts.length >= 10) {
                    try {
                        avgRating = Double.parseDouble(parts[8]);
                        reviewCount = Integer.parseInt(parts[9]);
                    } catch (NumberFormatException e) {
                        // Keep defaults if parsing fails
                    }
                }
                
                Ristorante ristorante = new Ristorante(nome, nazione, citta, indirizzo, fasciaPrezzo, delivery, online, tipoCucina);
                
                // Store rating information for GuestPage access
                restaurantRatings.put(nome, avgRating);
                restaurantReviewCounts.put(nome, reviewCount);
                System.out.println("DEBUG: Restaurant " + nome + " has rating " + avgRating + " (" + reviewCount + " reviews)");
                
                ristoranti.add(ristorante);
            }
        }
        
        return ristoranti;
    }
    
    /**
     * Converte una stringa di recensioni ricevuta dal server in lista di oggetti Recensione.
     */
    private List<Recensione> parseRecensioni(String data) {
        List<Recensione> recensioni = new ArrayList<>();
        
        if (data.isEmpty()) {
            return recensioni;
        }
        
        String[] recensioniStr = data.split("\\|\\|");
        for (String recensioneStr : recensioniStr) {
            String[] parts = recensioneStr.split("\\|");
            if (parts.length >= 6) {
                String username = parts[0];
                String ristorante = parts[1];
                int stelle = Integer.parseInt(parts[2]);
                String testo = parts[3];
                String data1 = parts[4];
                String ora = parts[5];
                String risposta = parts.length > 6 ? parts[6] : null;
                
                Recensione recensione = new Recensione(data1, stelle, ora, username, ristorante, testo, risposta);
                recensioni.add(recensione);
            }
        }
        
        return recensioni;
    }
    
    /**
     * Ottiene la valutazione media per un ristorante.
     * @param nomeRistorante nome del ristorante
     * @return valutazione media (0.0 se non disponibile)
     */
    public double getRestaurantRating(String nomeRistorante) {
        return restaurantRatings.getOrDefault(nomeRistorante, 0.0);
    }
    
    /**
     * Ottiene il numero di recensioni per un ristorante.
     * @param nomeRistorante nome del ristorante
     * @return numero di recensioni (0 se non disponibile)
     */
    public int getRestaurantReviewCount(String nomeRistorante) {
        return restaurantReviewCounts.getOrDefault(nomeRistorante, 0);
    }
    
    /**
     * Ottiene la mappa delle valutazioni di tutti i ristoranti.
     * @return mappa nome ristorante -> valutazione media
     */
    public java.util.Map<String, Double> getAllRestaurantRatings() {
        return new java.util.HashMap<>(restaurantRatings);
    }
    
    /**
     * Ottiene la mappa del numero di recensioni di tutti i ristoranti.
     * @return mappa nome ristorante -> numero recensioni
     */
    public java.util.Map<String, Integer> getAllRestaurantReviewCounts() {
        return new java.util.HashMap<>(restaurantReviewCounts);
    }
    
    /**
     * Aggiunge un nuovo ristorante.
     * @param nome nome del ristorante
     * @param nazione nazione
     * @param citta città
     * @param indirizzo indirizzo
     * @param fasciaPrezzo fascia di prezzo
     * @param delivery delivery
     * @param online online
     * @param tipoCucina tipo di cucina
     * @param username username del gestore
     * @return true se l'aggiunta è riuscita
     */
    public boolean addRestaurant(String nome, String nazione, String citta, String indirizzo, 
                                java.math.BigDecimal fasciaPrezzo, String delivery, String online, 
                                String tipoCucina, String username) {
        try {
            String priceStr = fasciaPrezzo != null ? fasciaPrezzo.toString() : "";
            String request = "ADD_RESTAURANT:" + nome + "|" + nazione + "|" + citta + "|" + indirizzo + "|" + 
                           priceStr + "|" + delivery + "|" + online + "|" + tipoCucina + "|" + username;
            String response = sendRequest(request);
            
            if (response.startsWith("OK:")) {
                return true;
            } else if (response.startsWith("ERROR:")) {
                System.err.println("Errore dal server: " + response.substring("ERROR:".length()));
                return false;
            }
            
        } catch (IOException e) {
            System.err.println("Errore di comunicazione con il server: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * Testa la connessione al server.
     * @return true se il server è raggiungibile
     */
    public boolean testConnection() {
        try {
            new Socket(SERVER_HOST, SERVER_PORT).close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }
    
    /**
     * Ottiene la località di domicilio di un utente dal server.
     * 
     * @param username Username dell'utente
     * @return La località di domicilio dell'utente, null se non trovato
     */
    public String getUserLocation(String username) {
        try {
            String response = sendRequest("GET_USER_LOCATION:" + username);
            if (response.startsWith("OK:")) {
                return response.substring(3);
            }
        } catch (Exception e) {
            System.err.println("Error getting user location: " + e.getMessage());
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
    public boolean checkFavorite(String username, String nomeRistorante, String cittaRistorante, String indirizzoRistorante) {
        try {
            String response = sendRequest("CHECK_FAVORITE:" + username + "|" + nomeRistorante + "|" + cittaRistorante + "|" + indirizzoRistorante);
            if (response.startsWith("OK:")) {
                return Boolean.parseBoolean(response.substring(3));
            }
        } catch (Exception e) {
            System.err.println("Error checking favorite: " + e.getMessage());
        }
        return false;
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
    public boolean addFavorite(String username, String nomeRistorante, String cittaRistorante, String indirizzoRistorante) {
        try {
            String response = sendRequest("ADD_FAVORITE:" + username + "|" + nomeRistorante + "|" + cittaRistorante + "|" + indirizzoRistorante);
            return response.startsWith("OK:");
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
    public boolean removeFavorite(String username, String nomeRistorante, String cittaRistorante, String indirizzoRistorante) {
        try {
            String response = sendRequest("REMOVE_FAVORITE:" + username + "|" + nomeRistorante + "|" + cittaRistorante + "|" + indirizzoRistorante);
            return response.startsWith("OK:");
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
    public boolean updateRestaurant(String oldName, String newName, String citta, String indirizzo, 
                                  String tipoCucina, String fasciaPrezzo, String delivery, String online) {
        try {
            String response = sendRequest("UPDATE_RESTAURANT:" + oldName + "|" + newName + "|" + citta + "|" + indirizzo + 
                                        "|" + tipoCucina + "|" + fasciaPrezzo + "|" + delivery + "|" + online);
            return response.startsWith("OK:");
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
    public boolean deleteRestaurant(String nomeRistorante) {
        try {
            String response = sendRequest("DELETE_RESTAURANT:" + nomeRistorante);
            return response.startsWith("OK:");
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
    public List<Recensione> getUserReviews(String username, String nomeRistorante) {
        try {
            String response = sendRequest("GET_USER_REVIEWS:" + username + "|" + nomeRistorante);
            if (response.startsWith("REVIEWS:")) {
                return parseRecensioni(response.substring("REVIEWS:".length()));
            }
        } catch (Exception e) {
            System.err.println("Error getting user reviews: " + e.getMessage());
        }
        return new ArrayList<>();
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
    public boolean updateReview(String username, String ristorante, String oldData, String oldOra, int stelle, String testo) {
        try {
            String response = sendRequest("UPDATE_REVIEW:" + username + "|" + ristorante + "|" + oldData + "|" + oldOra + "|" + stelle + "|" + testo);
            return response.startsWith("OK:");
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
    public boolean deleteReview(String username, String ristorante, String data, String ora) {
        try {
            String response = sendRequest("DELETE_REVIEW:" + username + "|" + ristorante + "|" + data + "|" + ora);
            return response.startsWith("OK:");
        } catch (Exception e) {
            System.err.println("Error deleting review: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Aggiorna la risposta del gestore a una recensione.
     * 
     * @param username Username dell'autore della recensione
     * @param ristorante Nome del ristorante
     * @param data Data della recensione
     * @param ora Ora della recensione
     * @param risposta Risposta del gestore
     * @return true se aggiornata con successo, false altrimenti
     */
    public boolean updateManagerResponse(String username, String ristorante, String data, String ora, String risposta) {
        try {
            String response = sendRequest("UPDATE_MANAGER_RESPONSE:" + username + "|" + ristorante + "|" + data + "|" + ora + "|" + risposta);
            return response.startsWith("OK:");
        } catch (Exception e) {
            System.err.println("Error updating manager response: " + e.getMessage());
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
    public boolean deleteManagerResponse(String username, String ristorante, String data, String ora) {
        try {
            String response = sendRequest("DELETE_MANAGER_RESPONSE:" + username + "|" + ristorante + "|" + data + "|" + ora);
            return response.startsWith("OK:");
        } catch (Exception e) {
            System.err.println("Error deleting manager response: " + e.getMessage());
            return false;
        }
    }
    
}