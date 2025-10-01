package com.example;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;
import javafx.stage.Stage;

/**
 * Classe principale dell'applicazione client JavaFX.
 * 
 * <p>Estende {@link Application} di JavaFX e implementa il punto di ingresso
 * dell'interfaccia utente. Gestisce la finestra principale e coordina
 * la navigazione tra le diverse schermate dell'applicazione.
 * 
 * <p>La classe non ha accesso diretto al database, mantenendo una chiara
 * separazione tra livello di presentazione e livello di business logic.
 * Tutte le operazioni sui dati vengono delegate al server remoto.
 * 
 * <p><strong>Responsabilità:</strong>
 * <ul>
 *   <li>Inizializzazione dell'interfaccia utente principale</li>
 *   <li>Gestione degli eventi dei pulsanti di navigazione</li>
 *   <li>Configurazione del layout e dello stile dell'applicazione</li>
 *   <li>Coordinamento con il NavigationManager</li>
 * </ul>
 * 
 * @see Application
 * @see NavigationManager
 * @see LoginForm
 * @see RegisterForm
 * @see GuestPage
 */
public class ClientApp extends Application {
    
    /**
     * Costruttore di default per l'applicazione client.
     * 
     * <p>Crea una nuova istanza dell'applicazione client senza
     * inizializzare componenti dell'interfaccia utente. L'inizializzazione
     * dell'UI viene gestita dal metodo {@link #start(Stage)} quando
     * JavaFX avvia l'applicazione.
     * 
     * @since 1.0
     */
    public ClientApp() {}

    /**
     * Avvia la finestra principale dell'applicazione client.
     * 
     * <p>Questo metodo viene chiamato automaticamente da JavaFX quando
     * l'applicazione viene lanciata. Si occupa di:
     * <ol>
     *   <li>Configurare il titolo e le proprietà della finestra</li>
     *   <li>Inizializzare il NavigationManager</li>
     *   <li>Creare e configurare tutti i componenti dell'interfaccia</li>
     *   <li>Definire i gestori eventi per i pulsanti</li>
     *   <li>Applicare stili CSS e layout responsive</li>
     *   <li>Mostrare la finestra all'utente</li>
     * </ol>
     * 
     * <p><strong>Layout Structure:</strong>
     * <pre>
     * BorderPane (main container)
     * ├── Top: Title Label
     * ├── Center: VBox with navigation buttons
     * └── Bottom: HBox with author info and version
     * </pre>
     * 
     * <p><strong>Funzionalità implementate:</strong>
     * <ul>
     *   <li>Layout responsive che si adatta alle dimensioni della finestra</li>
     *   <li>Stile coerente con tema verde dell'applicazione</li>
     *   <li>Navigazione verso form di login, registrazione e accesso ospite</li>
     *   <li>Information footer con dettagli autori e versione</li>
     * </ul>
     * 
     * @param primaryStage lo stage principale di JavaFX fornito dal framework.
     *                     Rappresenta la finestra principale dell'applicazione
     *                     su cui verranno visualizzati tutti i componenti UI.
     *                     Non può essere null.
     * 
     * @throws IllegalArgumentException se primaryStage è null
     * @throws RuntimeException se si verificano errori durante l'inizializzazione dell'UI
     * 
     * @see NavigationManager#setStage(Stage)
     * @see LoginForm#start(Stage)
     * @see RegisterForm#start(Stage)
     * @see GuestPage#start(Stage)
     * @since 1.0
     */
    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("TheKnife - Client");
        NavigationManager.getInstance().setStage(primaryStage);

        /**
         * Etichetta del titolo dell'applicazione.
         * Visualizza il nome dell'applicazione con styling personalizzato
         * per identificare chiaramente la versione client.
         */
        Label titleLabel = new Label("The Knife Application - Client");
        titleLabel.setStyle("-fx-text-fill: #43a047; -fx-font-size: 28; -fx-font-weight: bold;");
        titleLabel.setAlignment(javafx.geometry.Pos.CENTER);

        /**
         * Pulsante per la registrazione di un nuovo utente.
         * Permette agli utenti non registrati di creare un nuovo account
         * per accedere alle funzionalità complete dell'applicazione.
         */
        Button registerBtn = new Button("Registrati");
        
        /**
         * Pulsante per accedere come utente registrato.
         * Consente agli utenti esistenti di autenticarsi nel sistema
         * per accedere al proprio profilo e funzionalità personalizzate.
         */
        Button loginBtn = new Button("Accedi");
        
        /**
         * Pulsante per accedere come ospite.
         * Offre accesso limitato alle funzionalità dell'applicazione
         * senza richiedere registrazione o autenticazione.
         */
        Button guestBtn = new Button("Ospite");

        // Stile comune per tutti i pulsanti con tema verde coordinato
        String btnStyle = "-fx-background-color: #43a047; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 16; -fx-border-color: #43a047; -fx-border-width: 3px; -fx-background-radius: 0; -fx-border-radius: 0;";
        registerBtn.setStyle(btnStyle);
        loginBtn.setStyle(btnStyle);
        guestBtn.setStyle(btnStyle);

        /**
         * Gestore evento per il pulsante di registrazione.
         * Naviga verso il form di registrazione per la creazione
         * di un nuovo account utente nel sistema.
         * 
         * @see RegisterForm#start(Stage)
         */
        registerBtn.setOnAction(_ -> {
            RegisterForm registerForm = new RegisterForm();
            registerForm.start(primaryStage);
        });

        /**
         * Gestore evento per il pulsante di login.
         * Naviga verso il form di autenticazione per l'accesso
         * di utenti già registrati nel sistema.
         * 
         * @see LoginForm#start(Stage)
         */
        loginBtn.setOnAction(_ -> {
            LoginForm loginForm = new LoginForm();
            loginForm.start(primaryStage);
        });

        /**
         * Gestore evento per il pulsante ospite.
         * Naviga verso la modalità ospite che permette accesso
         * limitato senza autenticazione per consultare i ristoranti.
         * 
         * @see GuestPage#start(Stage)
         */
        guestBtn.setOnAction(_ -> {
            GuestPage guestPage = new GuestPage();
            guestPage.start(primaryStage);
        });

        /**
         * Contenitore verticale per i pulsanti principali.
         * Organizza i pulsanti di navigazione in layout verticale centrato
         * con spaziatura uniforme per una presentazione ordinata.
         */
        VBox buttonBox = new VBox(30, registerBtn, loginBtn, guestBtn);
        buttonBox.setAlignment(javafx.geometry.Pos.CENTER);

        /**
         * Etichetta con i nomi degli autori.
         * Visualizza informazioni sui creatori dell'applicazione
         * inclusi nomi completi e numeri di matricola universitaria.
         */
        Label leftLabel = new Label("made by Andrea De Nisco 752452 CO & Antonio De Nisco 752445 CO");
        leftLabel.setStyle("-fx-text-fill: black; -fx-font-size: 10;");
        
        /**
         * Etichetta con la versione dell'applicazione.
         * Mostra il numero di versione corrente e identifica
         * specificamente la build client dell'applicazione.
         */
        Label rightLabel = new Label("version 1.0 - CLIENT");
        rightLabel.setStyle("-fx-text-fill: black; -fx-font-size: 10;");

        /**
         * Layout principale dell'applicazione.
         * Utilizza BorderPane per organizzare i componenti in regioni
         * specifiche (top, center, bottom) con styling coordinato.
         */
        BorderPane layout = new BorderPane();
        layout.setStyle("-fx-background-color: #f8fff8; -fx-padding: 40; -fx-border-color: #43a047; -fx-border-width: 3px; -fx-background-radius: 0; -fx-border-radius: 0;");
        layout.setTop(titleLabel);
        BorderPane.setAlignment(titleLabel, javafx.geometry.Pos.CENTER);
        layout.setCenter(buttonBox);

        /**
         * Contenitore orizzontale per le etichette di informazioni nel footer.
         * Organizza le informazioni degli autori e versione in layout orizzontale
         * con spaziatura appropriata e allineamento agli estremi.
         */
        HBox bottomBox = new HBox();
        bottomBox.setSpacing(10);
        bottomBox.setPadding(new Insets(0, 10, 10, 10));
        bottomBox.getChildren().addAll(leftLabel, new Label(), rightLabel);
        HBox.setHgrow(bottomBox.getChildren().get(1), Priority.ALWAYS);
        bottomBox.setAlignment(javafx.geometry.Pos.BOTTOM_LEFT);
        layout.setBottom(bottomBox);

        /**
         * Scena principale dell'applicazione.
         * Rappresenta il container root di tutti i componenti UI
         * con dimensioni iniziali ottimizzate per desktop.
         */
        Scene scene = new Scene(layout, 500, 400);
        primaryStage.setScene(scene);

        layout.prefWidthProperty().bind(scene.widthProperty());
        layout.prefHeightProperty().bind(scene.heightProperty());
        buttonBox.prefWidthProperty().bind(layout.widthProperty().multiply(0.6));
        buttonBox.prefHeightProperty().bind(layout.heightProperty().multiply(0.5));
        titleLabel.prefWidthProperty().bind(layout.widthProperty());
        leftLabel.prefWidthProperty().bind(layout.widthProperty().multiply(0.5));
        rightLabel.prefWidthProperty().bind(layout.widthProperty().multiply(0.5));
        bottomBox.prefWidthProperty().bind(layout.widthProperty());

        BorderPane.setAlignment(titleLabel, javafx.geometry.Pos.CENTER);
        HBox.setHgrow(leftLabel, Priority.ALWAYS);
        HBox.setHgrow(rightLabel, Priority.ALWAYS);
        rightLabel.setAlignment(javafx.geometry.Pos.BOTTOM_RIGHT);
        leftLabel.setAlignment(javafx.geometry.Pos.BOTTOM_LEFT);

        primaryStage.show();
    }

    /**
     * Punto di ingresso principale dell'applicazione client.  
     * 
     * <p>Avvia l'applicazione JavaFX chiamando il metodo {@link Application#launch(String[])}.
     * Questo metodo è il primo codice eseguito quando si avvia il JAR client
     * e si occupa di inizializzare il framework JavaFX e chiamare 
     * successivamente il metodo {@link #start(Stage)}.
     * 
     * <p><strong>Flusso di esecuzione:</strong>
     * <ol>
     *   <li>JVM carica la classe ClientApp</li>
     *   <li>Viene chiamato questo metodo main</li>
     *   <li>JavaFX viene inizializzato</li>
     *   <li>Viene creata un'istanza di ClientApp</li>
     *   <li>Viene chiamato il metodo start() con il Stage principale</li>
     * </ol>
     * 
     * <p><strong>Utilizzo:</strong>
     * <pre>{@code
     * java -jar TheKnife-client.jar
     * }</pre>
     * 
     * <p><strong>Requisiti di sistema:</strong>
     * <ul>
     *   <li>Java 21 o superiore</li>
     *   <li>JavaFX runtime environment</li>  
     *   <li>Sistema operativo con supporto per GUI</li>
     * </ul>
     * 
     * @param args argomenti da linea di comando passati all'applicazione.
     *             Possono includere parametri JavaFX standard o parametri
     *             personalizzati dell'applicazione. Attualmente non utilizzati.
     * 
     * @see Application#launch(String[])
     * @see #start(Stage)
     * @since 1.0
     */
    public static void main(String[] args) {
        launch(args);
    }
}