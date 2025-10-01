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
 * Classe principale dell'applicazione client TheKnife - sistema di recensioni ristoranti.
 * <p>
 * Questa classe rappresenta il punto di ingresso dell'applicazione client JavaFX e gestisce
 * l'inizializzazione dell'interfaccia utente, la configurazione della finestra principale
 * e l'avvio del sistema di navigazione. L'applicazione implementa un'architettura client-server
 * dove questo client si connette al ServerService per tutte le operazioni di database.
 * <p>
 * L'applicazione offre una schermata iniziale di benvenuto con opzioni per:
 * <ul>
 *   <li>Accesso per utenti registrati (Login)</li>
 *   <li>Registrazione per nuovi utenti</li>
 *   <li>Navigazione come ospite senza autenticazione</li>
 * </ul>
 * <p>
 * <strong>Architettura:</strong> Entry point del client nell'architettura client-server.
 * Inizializza il NavigationManager e coordina l'integrazione con il ServerService per
 * le operazioni di backend. L'interfaccia è costruita con JavaFX per offrire un'esperienza
 * utente desktop moderna e reattiva.
 * <p>
 * <strong>Dipendenze:</strong> Richiede che il ServerService sia in esecuzione su
 * localhost:8080 per il funzionamento completo delle funzionalità di backend.
 * 
 * @author Andrea De Nisco, Antonio De Nisco
 * @version 2.0 - Migrato a architettura client-server
 * @since 1.0
 * @see NavigationManager
 * @see ClientService
 * @see LoginForm
 * @see RegisterForm
 * @see GuestPage
 */
public class MainApp extends Application {
    /**
     * Costruttore di default.
     */
    public MainApp() {}

    /**
     * Avvia la finestra principale dell'applicazione.
     * @param primaryStage lo stage principale di JavaFX
     */

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("TheKnife - Ristoranti");
        NavigationManager.getInstance().setStage(primaryStage);

    /**
     * Etichetta del titolo dell'applicazione.
     */
    Label titleLabel = new Label("The Knife Application");
        titleLabel.setStyle("-fx-text-fill: #43a047; -fx-font-size: 28; -fx-font-weight: bold;");
        titleLabel.setAlignment(javafx.geometry.Pos.CENTER);

    /**
     * Pulsante per la registrazione di un nuovo utente.
     */
    Button registerBtn = new Button("Registrati");
    /**
     * Pulsante per accedere come utente registrato.
     */
    Button loginBtn = new Button("Accedi");
    /**
     * Pulsante per accedere come ospite.
     */
    Button guestBtn = new Button("Ospite");

        String btnStyle = "-fx-background-color: #43a047; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 16; -fx-border-color: #43a047; -fx-border-width: 3px; -fx-background-radius: 0; -fx-border-radius: 0;";
        registerBtn.setStyle(btnStyle);
        loginBtn.setStyle(btnStyle);
        guestBtn.setStyle(btnStyle);

        /**
         * Gestore evento per il pulsante di registrazione.
         */
        registerBtn.setOnAction(_ -> {
            RegisterForm registerForm = new RegisterForm();
            registerForm.start(primaryStage);
        });

        /**
         * Gestore evento per il pulsante di login.
         */
        loginBtn.setOnAction(_ -> {
            LoginForm loginForm = new LoginForm();
            loginForm.start(primaryStage);
        });

        /**
         * Gestore evento per il pulsante ospite.
         */
        guestBtn.setOnAction(_ -> {
            GuestPage guestPage = new GuestPage();
            guestPage.start(primaryStage);
        });

    /**
     * Contenitore verticale per i pulsanti principali.
     */
    VBox buttonBox = new VBox(30, registerBtn, loginBtn, guestBtn);
    buttonBox.setAlignment(javafx.geometry.Pos.CENTER);

    /**
     * Etichetta con i nomi degli autori.
     */
    Label leftLabel = new Label("made by Andrea De Nisco 752452 CO & Antonio De Nisco 752445 CO");
    leftLabel.setStyle("-fx-text-fill: black; -fx-font-size: 10;");
    /**
     * Etichetta con la versione dell'applicazione.
     */
    Label rightLabel = new Label("version 1.0");
    rightLabel.setStyle("-fx-text-fill: black; -fx-font-size: 10;");

    /**
     * Layout principale dell'applicazione.
     */
    BorderPane layout = new BorderPane();
    layout.setStyle("-fx-background-color: #f8fff8; -fx-padding: 40; -fx-border-color: #43a047; -fx-border-width: 3px; -fx-background-radius: 0; -fx-border-radius: 0;");
    layout.setTop(titleLabel);
    BorderPane.setAlignment(titleLabel, javafx.geometry.Pos.CENTER);
    layout.setCenter(buttonBox);

        /**
         * Contenitore orizzontale per le etichette di info in basso.
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
     * Metodo main dell'applicazione. Avvia JavaFX.
     * @param args argomenti da linea di comando
     */
    public static void main(String[] args) {
        launch(args);
    }
}
