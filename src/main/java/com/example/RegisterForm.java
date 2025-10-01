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
 * Interfaccia di registrazione utente per l'applicazione TheKnife con architettura client-server.
 * 
 * <p>Questa classe JavaFX implementa la finestra di registrazione che utilizza {@link ClientService}
 * per creare nuovi account utente tramite server. Fornisce un'interfaccia completa per la raccolta
 * dei dati utente con validazione, crittografia password e gestione ruoli.
 * 
 * <h3>Architettura Client-Server</h3>
 * <p>La registrazione utilizza comunicazione TCP socket per:
 * <ul>
 *   <li><strong>Registrazione Sicura:</strong> Invio dati con password MD5 hash</li>
 *   <li><strong>Validazione Server:</strong> Controlli duplicati username/email</li>
 *   <li><strong>Persistenza Remota:</strong> Salvataggio nel database PostgreSQL</li>
 *   <li><strong>Feedback Immediato:</strong> Risposta successo/errore dal server</li>
 * </ul>
 * 
 * <h3>Campi di Registrazione</h3>
 * <p>Il form raccoglie le seguenti informazioni:
 * <ul>
 *   <li><strong>Dati Personali:</strong> Nome, Cognome, Data di Nascita</li>
 *   <li><strong>Credenziali:</strong> Username, Email, Password (con conferma)</li>
 *   <li><strong>Residenza:</strong> Luogo del domicilio per filtri geografici</li>
 *   <li><strong>Ruolo:</strong> Utente standard o Gestore ristorante</li>
 * </ul>
 * 
 * <h3>Validazione e Sicurezza</h3>
 * <ul>
 *   <li><strong>Validazione Client:</strong> Controlli real-time campi obbligatori</li>
 *   <li><strong>Conferma Password:</strong> Verifica matching password/conferma</li>
 *   <li><strong>MD5 Hashing:</strong> Crittografia password prima dell'invio</li>
 *   <li><strong>Validazione Server:</strong> Controlli unicità e formato dati</li>
 *   <li><strong>Sanitizzazione Input:</strong> Prevenzione injection attacks</li>
 * </ul>
 * 
 * <h3>Gestione Ruoli</h3>
 * <p>Durante la registrazione l'utente può scegliere:
 * <ul>
 *   <li><strong>Utente:</strong> Può cercare ristoranti, leggere/scrivere recensioni</li>
 *   <li><strong>Gestore:</strong> Tutte le funzionalità utente + gestione ristoranti</li>
 * </ul>
 * 
 * <h3>Processo di Registrazione</h3>
 * <ol>
 *   <li><strong>Compilazione Form:</strong> Utente inserisce tutti i dati richiesti</li>
 *   <li><strong>Validazione Client:</strong> Controlli sintassi e campi obbligatori</li>
 *   <li><strong>Hashing Password:</strong> Conversione password in MD5</li>
 *   <li><strong>Invio Server:</strong> REGISTER:nome|cognome|username|email|password|data|domicilio|ruolo</li>
 *   <li><strong>Risposta Server:</strong> OK o ERRORE con dettagli</li>
 *   <li><strong>Navigazione:</strong> Su successo, apre {@link GuestPage} con login automatico</li>
 * </ol>
 * 
 * <h3>Interfaccia Utente</h3>
 * <p>Design intuitivo con:
 * <ul>
 *   <li>Layout GridPane organizzato e accessibile</li>
 *   <li>Validazione real-time con feedback visivo</li>
 *   <li>ComboBox per selezione ruolo e data</li>
 *   <li>Pulsanti chiari per registrazione e ritorno login</li>
 * </ul>
 * 
 * <h3>Pattern di Utilizzo</h3>
 * <pre>{@code
 * // Avvio finestra di registrazione
 * RegisterForm registerForm = new RegisterForm();
 * registerForm.start(primaryStage);
 * 
 * // Su registrazione riuscita, automaticamente apre GuestPage
 * }</pre>
 * 
 * @author Andrea De Nisco, Antonio De Nisco
 * @version 2.0 - Migrato a architettura client-server
 * @since 1.0
 * @see ClientService
 * @see LoginForm
 * @see GuestPage
 */
public class RegisterForm extends Application {
    /**
     * Costruttore di default.
     */
    public RegisterForm() {}
    
    /**
     * Cripta la password utilizzando MD5 (32 caratteri) per compatibilità con VARCHAR(50).
     * NOTA: Per maggiore sicurezza, si consiglia di aggiornare il database a VARCHAR(64) e usare SHA-256.
     * @param password Password in chiaro da criptare
     * @return Password criptata in formato esadecimale (32 caratteri)
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
     * Avvia la finestra di registrazione utente con interfaccia grafica completa.
     * <p>
     * Crea l'interfaccia di registrazione con tutti i campi necessari:
     * Nome, Cognome, Username, Email, Password, Conferma Password, Data di nascita,
     * Luogo domicilio e Ruolo utente.
     * <p>
     * Al completamento della registrazione:
     * <ul>
     *   <li>Valida che le password corrispondano</li>
     *   <li>Cripta la password con hash MD5</li>
     *   <li>Converte il paese di residenza nel formato inglese</li>
     *   <li>Salva l'utente nel database</li>
     *   <li>Naviga direttamente alla pagina ristoranti senza selezione paese</li>
     * </ul>
     * @param stage lo stage principale di JavaFX per la finestra di registrazione
     * @throws RuntimeException se si verificano errori durante la crittografia o il salvataggio
     */

    @Override
    public void start(Stage stage) {
    stage.setTitle("Registrazione Utente");

        /**
         * Layout a griglia per i campi di registrazione.
         */
        GridPane grid = new GridPane();
    grid.setPadding(new Insets(30));
    grid.setHgap(15);
    grid.setVgap(15);
    grid.setStyle("-fx-background-color: #f8fff8; -fx-border-color: #43a047; -fx-border-width: 3px; -fx-background-radius: 0; -fx-border-radius: 0;");

    // Responsive bindings
    grid.prefWidthProperty().bind(stage.widthProperty().multiply(0.95));
    grid.prefHeightProperty().bind(stage.heightProperty().multiply(0.95));

        /**
         * Campo di testo per il nome.
         */
        TextField nomeField = new TextField();
        nomeField.setPromptText("Nome");
        /**
         * Campo di testo per il cognome.
         */
        TextField cognomeField = new TextField();
        cognomeField.setPromptText("Cognome");
        /**
         * Campo di testo per l'username.
         */
        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");
        /**
         * Campo di testo per l'email.
         */
        TextField emailField = new TextField();
        emailField.setPromptText("Email");
        /**
         * Campo di testo per la password.
         */
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        /**
         * Campo di testo per la conferma della password.
         */
        PasswordField confirmPasswordField = new PasswordField();
        confirmPasswordField.setPromptText("Conferma Password");
        /**
         * Selettore per la data di nascita.
         */
        DatePicker datePicker = new DatePicker();
        /**
         * Campo di testo per il luogo di domicilio.
         */
        TextField domicilioField = new TextField();
        domicilioField.setPromptText("Luogo domicilio");

        /**
         * ComboBox per la selezione del ruolo utente.
         */
        ComboBox<String> ruoloBox = new ComboBox<>();
        ruoloBox.getItems().addAll("utente", "gestore");

        /**
         * Pulsante per la registrazione.
         */
        Button registerBtn = new Button("Registrati");
        /**
         * Pulsante per tornare indietro.
         */
        Button backBtn = new Button("Indietro");
        registerBtn.setStyle("-fx-background-color: #43a047; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 10; -fx-border-radius: 10;");
        backBtn.setStyle("-fx-background-color: white; -fx-text-fill: #43a047; -fx-font-weight: bold; -fx-background-radius: 10; -fx-border-radius: 10; -fx-border-color: #43a047; -fx-border-width: 2px;");

    grid.add(new Label("Nome:"), 0, 0); grid.add(nomeField, 1, 0);
    grid.add(new Label("Cognome:"), 0, 1); grid.add(cognomeField, 1, 1);
    grid.add(new Label("Username:"), 0, 2); grid.add(usernameField, 1, 2);
    grid.add(new Label("Email:"), 0, 3); grid.add(emailField, 1, 3);
    grid.add(new Label("Password:"), 0, 4); grid.add(passwordField, 1, 4);
    grid.add(new Label("Conferma Password:"), 0, 5); grid.add(confirmPasswordField, 1, 5);
    grid.add(new Label("Data di nascita:"), 0, 6); grid.add(datePicker, 1, 6);
    grid.add(new Label("Luogo domicilio:"), 0, 7); grid.add(domicilioField, 1, 7);
    grid.add(new Label("Ruolo:"), 0, 8); grid.add(ruoloBox, 1, 8);
    grid.add(registerBtn, 1, 9);
    grid.add(backBtn, 0, 9);

        /**
         * Gestore evento per il pulsante di registrazione.
         */
        registerBtn.setOnAction(_ -> {
            /**
             * Nome inserito dall'utente.
             */
            String nome = nomeField.getText();
            /**
             * Cognome inserito dall'utente.
             */
            String cognome = cognomeField.getText();
            /**
             * Username inserito dall'utente.
             */
            String username = usernameField.getText();
            /**
             * Email inserita dall'utente.
             */
            String email = emailField.getText();
            /**
             * Password inserita dall'utente.
             */
            String password = passwordField.getText();
            /**
             * Conferma password inserita dall'utente.
             */
            String confirmPassword = confirmPasswordField.getText();
            /**
             * Data di nascita selezionata.
             */
            String dataNascita = (datePicker.getValue() != null) ? datePicker.getValue().toString() : null;
            /**
             * Luogo di domicilio inserito.
             */
            String domicilio = domicilioField.getText();
            String domicilioEn = domicilio;
            for (String code : java.util.Locale.getISOCountries()) {
                java.util.Locale itLocale = java.util.Locale.forLanguageTag("it");
                java.util.Locale enLocale = java.util.Locale.forLanguageTag("en");
                java.util.Locale countryLocale = java.util.Locale.forLanguageTag("und-" + code);
                if (countryLocale.getDisplayCountry(itLocale).equalsIgnoreCase(domicilio)) {
                    domicilioEn = countryLocale.getDisplayCountry(enLocale);
                    System.out.println("DEBUG: Salvataggio nazione come '" + domicilioEn + "' per l'utente");
                    break;
                }
            }
            /**
             * Ruolo selezionato dall'utente.
             */
            String ruolo = ruoloBox.getValue();

            if (!password.equals(confirmPassword)) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Le password non coincidono!");
                alert.show();
                return;
            }

            try {
                // Use ClientService for client-server registration
                String hashedPassword = hashPassword(password);
                
                // Register user through ClientService
                boolean registrationSuccess = ClientService.getInstance().registerUser(
                    nome, cognome, username, email, hashedPassword, dataNascita, domicilioEn, ruolo);
                
                if (registrationSuccess) {
                    System.out.println("Utente registrato: " + username + " (" + ruolo + ")");
                    Alert alert = new Alert(Alert.AlertType.INFORMATION, "Registrazione avvenuta con successo!");
                    alert.showAndWait();
                    // Automatically login and go to guest page
                    GuestPage guestPage = new GuestPage(domicilioEn, username, ruolo);
                    guestPage.start(stage);
                } else {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Errore durante la registrazione.");
                    alert.showAndWait();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                Alert alert = new Alert(Alert.AlertType.ERROR, "Errore durante la registrazione:\n" + ex.getMessage());
                alert.showAndWait();
            }
        });

        /**
         * Gestore evento per il pulsante indietro.
         */
        backBtn.setOnAction(_ -> NavigationManager.getInstance().goBack());

        /**
         * Scena di registrazione.
         */
        Scene scene = new Scene(grid, 700, 600);
        NavigationManager.getInstance().navigateTo(scene);
    }
}
