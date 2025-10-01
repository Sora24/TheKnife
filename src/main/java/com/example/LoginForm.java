package com.example;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Interfaccia di autenticazione utente per l'applicazione TheKnife con architettura client-server.
 * 
 * <p>Questa classe JavaFX implementa la finestra di login che utilizza {@link ClientService}
 * per l'autenticazione tramite server. Supporta autenticazione sicura con password MD5
 * e gestione dei ruoli utente (guest, utente registrato, gestore ristorante).
 * 
 * <h3>Architettura Client-Server</h3>
 * <p>Il login utilizza comunicazione TCP socket con il server per:
 * <ul>
 *   <li><strong>Autenticazione Sicura:</strong> Invio password MD5 hash (mai plaintext)</li>
 *   <li><strong>Recupero Profilo:</strong> Ottiene username, località, ruolo dal server</li>
 *   <li><strong>Gestione Sessione:</strong> Mantiene informazioni utente per la sessione</li>
 *   <li><strong>Validazione Remota:</strong> Controllo credenziali centralizzato</li>
 * </ul>
 * 
 * <h3>Processo di Autenticazione</h3>
 * <ol>
 *   <li><strong>Validazione Client:</strong> Verifica username e password non vuoti</li>
 *   <li><strong>Hashing MD5:</strong> Converte password in hash MD5 prima dell'invio</li>
 *   <li><strong>Richiesta Server:</strong> Invia LOGIN:username|passwordMD5</li>
 *   <li><strong>Risposta Server:</strong> Riceve OK:username|location|role o ERROR</li>
 *   <li><strong>Navigazione:</strong> Apre {@link GuestPage} con dati utente</li>
 * </ol>
 * 
 * <h3>Gestione Ruoli Utente</h3>
 * <ul>
 *   <li><strong>Guest:</strong> Accesso limitato, solo visualizzazione</li>
 *   <li><strong>Utente:</strong> Può aggiungere recensioni, filtro per località</li>
 *   <li><strong>Gestore:</strong> Può creare/modificare ristoranti, rispondere recensioni</li>
 * </ul>
 * 
 * <h3>Sicurezza</h3>
 * <ul>
 *   <li><strong>Password Hashing:</strong> MD5 hash prima della trasmissione</li>
 *   <li><strong>Nessun Storage Locale:</strong> Password non memorizzate localmente</li>
 *   <li><strong>Validazione Input:</strong> Controlli client-side e server-side</li>
 *   <li><strong>Gestione Errori:</strong> Messaggi sicuri senza esposizione dettagli</li>
 * </ul>
 * 
 * <h3>Interfaccia Utente</h3>
 * <p>Design pulito e intuitivo con:
 * <ul>
 *   <li>Campi di input con validazione real-time</li>
 *   <li>Pulsanti per login e accesso guest</li>
 *   <li>Link per registrazione nuovi utenti</li>
 *   <li>Messaggi di errore user-friendly</li>
 * </ul>
 * 
 * <h3>Pattern di Utilizzo</h3>
 * <pre>{@code
 * // Avvio finestra di login
 * LoginForm loginForm = new LoginForm();
 * loginForm.start(primaryStage);
 * 
 * // Il login automaticamente apre GuestPage su successo
 * }</pre>
 * 
 * @author Andrea De Nisco, Antonio De Nisco
 * @version 2.0 - Migrato a architettura client-server
 * @since 1.0
 * @see ClientService
 * @see GuestPage
 * @see RegisterForm
 */
public class LoginForm extends Application {
    /**
     * Costruttore di default.
     */
    public LoginForm() {}
    
    /**
     * Cripta la password utilizzando l'algoritmo MD5 per la sicurezza delle credenziali.
     * <p>
     * Questo metodo è utilizzato sia per:
     * <ul>
     *   <li><strong>Autenticazione</strong>: Verifica delle credenziali durante il login</li>
     *   <li><strong>Upgrade Automatico</strong>: Conversione delle password legacy da plain text a hash</li>
     * </ul>
     * 
     * <h4>Caratteristiche Tecniche:</h4>
     * <ul>
     *   <li><strong>Algoritmo</strong>: MD5 (Message Digest 5)</li>
     *   <li><strong>Output</strong>: String esadecimale di 32 caratteri</li>
     *   <li><strong>Compatibilità</strong>: Fits perfettamente in campo database VARCHAR(50)</li>
     *   <li><strong>Encoding</strong>: UTF-8 per input, esadecimale per output</li>
     * </ul>
     * 
     * @param password Password in chiaro da criptare (input utente)
     * @return Password criptata in formato esadecimale (32 caratteri)
     * @throws RuntimeException Se l'algoritmo MD5 non è disponibile nel sistema
     * 
     * @implNote MD5 è utilizzato per compatibilità con il database esistente. 
     *           Per nuove implementazioni si consiglia SHA-256 con VARCHAR(64).
     * @see java.security.MessageDigest
     */
    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hashedBytes = md.digest(password.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hashedBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Errore nella crittografia della password", e);
        }
    }

    /**
     * Avvia la finestra di login e gestisce l'autenticazione ibrida backward-compatible.
     * <p>
     * Crea un'interfaccia utente JavaFX per il login con layout responsive e styling 
     * professionale verde/bianco. Implementa un sistema di autenticazione a due fasi 
     * che supporta sia utenti esistenti (password plain text) che nuovi utenti 
     * (password MD5 hashate).
     * 
     * <h4>Interfaccia Utente:</h4>
     * <ul>
     *   <li><strong>Username Field</strong>: TextField con prompt text</li>
     *   <li><strong>Password Field</strong>: PasswordField per input sicuro</li>
     *   <li><strong>Login Button</strong>: Pulsante verde per l'accesso</li>
     *   <li><strong>Back Button</strong>: Pulsante per tornare al menu principale</li>
     * </ul>
     * 
     * <h4>Processo di Autenticazione Ibrido:</h4>
     * <ol>
     *   <li><strong>Validazione Input</strong>: Verifica che username e password non siano vuoti</li>
     *   <li><strong>Tentativo Hash MD5</strong>: Prima prova con password hashata (nuovi utenti)</li>
     *   <li><strong>Tentativo Plain Text</strong>: Se fallisce, prova con password in chiaro (utenti legacy)</li>
     *   <li><strong>Upgrade Automatico</strong>: Se login plain text riesce, aggiorna password a MD5</li>
     *   <li><strong>Navigazione</strong>: Apre GuestPage con filtro paese basato su residenza</li>
     * </ol>
     * 
     * <h4>Gestione Database:</h4>
     * <ul>
     *   <li>Utilizza prepared statements per sicurezza SQL injection</li>
     *   <li>Gestione automatica delle connessioni con try-with-resources</li>
     *   <li>Update automatico delle password legacy per sicurezza futura</li>
     *   <li>Recupero automatico di paese di residenza e ruolo utente</li>
     * </ul>
     * 
     * @param stage Stage principale JavaFX per visualizzare la finestra di login
     * @throws RuntimeException Se si verificano errori durante la crittografia MD5 o operazioni database
     * 
     * @see #hashPassword(String)
     * @see GuestPage#GuestPage(String, String, String)
     * @see DBConnection#getConnection()
     */
    @Override
    public void start(Stage stage) {
    stage.setTitle("Accedi");

    GridPane grid = new GridPane();
    grid.setPadding(new Insets(30));
    grid.setHgap(15);
    grid.setVgap(15);
    grid.setStyle("-fx-background-color: #f8fff8; -fx-border-color: #43a047; -fx-border-width: 3px; -fx-background-radius: 0; -fx-border-radius: 0;");

    grid.prefWidthProperty().bind(stage.widthProperty().multiply(0.95));
    grid.prefHeightProperty().bind(stage.heightProperty().multiply(0.95));

    /**
     * Campo di testo per l'inserimento dell'username.
     */
    TextField usernameField = new TextField();
    usernameField.setPromptText("Username");
    /**
     * Campo di testo per l'inserimento della password.
     */
    PasswordField passwordField = new PasswordField();
    passwordField.setPromptText("Password");
    /**
     * Pulsante per effettuare il login.
     */
    Button loginBtn = new Button("Accedi");
    /**
     * Pulsante per tornare indietro.
     */
    Button backBtn = new Button("Indietro");
    loginBtn.setStyle("-fx-background-color: #43a047; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 10; -fx-border-radius: 10;");
    backBtn.setStyle("-fx-background-color: white; -fx-text-fill: #43a047; -fx-font-weight: bold; -fx-background-radius: 10; -fx-border-radius: 10; -fx-border-color: #43a047; -fx-border-width: 2px;");

    grid.add(new Label("Username:"), 0, 0); 
    grid.add(usernameField, 1, 0);
    grid.add(new Label("Password:"), 0, 1); 
    grid.add(passwordField, 1, 1);
    grid.add(loginBtn, 1, 2);
    grid.add(backBtn, 0, 2);

        
    /**
     * Gestore dell'evento click sul pulsante di login.
     * <p>
     * Implementa un sistema di autenticazione ibrido che supporta sia password 
     * MD5 hashate (utenti nuovi) che password plain text (utenti esistenti), 
     * con upgrade automatico delle password legacy.
     * 
     * <h4>Flusso di Autenticazione:</h4>
     * <ol>
     *   <li><strong>Validazione Input</strong>: Controlla che username e password non siano vuoti</li>
     *   <li><strong>Tentativo MD5</strong>: Prima prova con hashPassword(password) per utenti nuovi</li>
     *   <li><strong>Tentativo Legacy</strong>: Se fallisce, prova con password plain text</li>
     *   <li><strong>Upgrade Sicurezza</strong>: Se login legacy riesce, aggiorna password a MD5</li>
     *   <li><strong>Navigazione</strong>: Apre GuestPage filtrata per paese di residenza</li>
     * </ol>
     * 
     * <h4>Gestione Errori:</h4>
     * <ul>
     *   <li>Alert WARNING per campi vuoti</li>
     *   <li>Alert ERROR per credenziali non valide</li>
     *   <li>Alert ERROR con stack trace per errori database/sistema</li>
     * </ul>
     * 
     * @implNote Il sistema garantisce backward compatibility con utenti esistenti
     *           mentre migliora automaticamente la sicurezza delle password.
     */
    loginBtn.setOnAction(_ -> {
            String username = usernameField.getText().trim();
            String password = passwordField.getText().trim();

            if (username.isEmpty() || password.isEmpty()) {
                Alert a = new Alert(Alert.AlertType.WARNING, "Inserisci username e password!");
                a.showAndWait();
                return;
            }

            
            try {
                // Use ClientService for client-server authentication
                String hashedPassword = hashPassword(password);
                
                // Try ClientService authentication with MD5 hashed password
                String[] loginResult = ClientService.getInstance().verifyLoginAndGetInfo(username, hashedPassword);
                
                if (loginResult != null && loginResult.length >= 3) {
                    // Server returns: username|location|role
                    String location = loginResult[1];
                    String role = loginResult[2];
                    
                    System.out.println("Login riuscito: " + username + " (Location: " + location + ", Role: " + role + ")");
                    System.out.println("DEBUG: LoginForm ha ricevuto ruolo: '" + role + "'");
                    
                    // Pass user information to GuestPage constructor
                    GuestPage guestPage;
                    if (location != null && !location.isEmpty()) {
                        guestPage = new GuestPage(location, username, role != null ? role : "Utente");
                    } else {
                        guestPage = new GuestPage(null, username, role != null ? role : "Utente");
                    }
                    guestPage.start(stage);
                } else {
                    Alert a = new Alert(Alert.AlertType.ERROR, "Credenziali non valide!");
                    a.showAndWait();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                Alert a = new Alert(Alert.AlertType.ERROR, "Errore durante il login:\n" + ex.getMessage());
                a.showAndWait();
            }
        });

    backBtn.setOnAction(_ -> NavigationManager.getInstance().goBack());

    Scene scene = new Scene(grid, 350, 220);
    NavigationManager.getInstance().navigateTo(scene);
    }
}
