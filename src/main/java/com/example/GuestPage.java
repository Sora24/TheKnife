package com.example;
// All imports removed as per lint feedback. Restore only those that are actually used if needed.

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.application.Application;
import javafx.stage.Stage;


import java.math.BigDecimal;


import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Interfaccia grafica principale dell'applicazione TheKnife per la gestione ristoranti.
 * 
 * <p>Questa classe JavaFX rappresenta la finestra principale dell'applicazione e fornisce
 * un'interfaccia utente completa per la ricerca, visualizzazione e gestione di ristoranti
 * e recensioni. L'interfaccia si adatta automaticamente al ruolo dell'utente (guest, utente, gestore)
 * mostrando le funzionalità appropriate.
 * 
 * <h3>Funzionalità per Utenti Guest</h3>
 * <ul>
 *   <li>Ricerca ristoranti con filtri avanzati (nazione, città, cucina, prezzo)</li>
 *   <li>Visualizzazione dettagli ristoranti con valutazioni medie</li>
 *   <li>Lettura recensioni di altri utenti</li>
 *   <li>Navigazione paginata dei risultati</li>
 * </ul>
 * 
 * <h3>Funzionalità per Utenti Registrati</h3>
 * <ul>
 *   <li>Tutte le funzionalità guest</li>
 *   <li>Aggiunta recensioni personali con valutazione 1-5 stelle</li>
 *   <li>Filtro automatico per località di residenza</li>
 *   <li>Profilo utente personalizzato</li>
 * </ul>
 * 
 * <h3>Funzionalità per Gestori Ristoranti</h3>
 * <ul>
 *   <li>Tutte le funzionalità utente registrato</li>
 *   <li>Creazione e gestione ristoranti propri</li>
 *   <li>Modifica/eliminazione ristoranti di proprietà</li>
 *   <li>Risposta alle recensioni dei clienti</li>
 *   <li>Visualizzazione statistiche ristorante (media stelle, numero recensioni)</li>
 * </ul>
 * 
 * <h3>Architettura Client-Server</h3>
 * <p>L'interfaccia utilizza {@link ClientService} per tutte le operazioni di rete:
 * <ul>
 *   <li><strong>Ricerca Asincrona:</strong> Ricerca ristoranti senza bloccare UI</li>
 *   <li><strong>Cache Locale:</strong> Caching valutazioni e conteggi per performance</li>
 *   <li><strong>Persistenza Proprietà:</strong> Salvataggio mapping ristorante-gestore</li>
 *   <li><strong>Gestione Errori:</strong> Feedback utente per errori di rete</li>
 * </ul>
 * 
 * <h3>Interfaccia Utente</h3>
 * <p>Design responsivo con:
 * <ul>
 *   <li><strong>Tabella Principale:</strong> Lista ristoranti con sorting e paginazione</li>
 *   <li><strong>Filtri Dinamici:</strong> Form di ricerca con validazione real-time</li>
 *   <li><strong>Dialog Modali:</strong> Popup per recensioni e gestione ristoranti</li>
 *   <li><strong>Toolbar Contestuale:</strong> Azioni basate su ruolo utente</li>
 *   <li><strong>Navigazione Breadcrumb:</strong> Per tornare al login/registrazione</li>
 * </ul>
 * 
 * <h3>Gestione Dati</h3>
 * <p>La classe gestisce:
 * <ul>
 *   <li>Paginazione intelligente con opzioni flessibili (20, 50, 100, Tutti)</li>
 *   <li>Caching locale delle proprietà ristoranti per performance</li>
 *   <li>Sincronizzazione stato UI con server tramite ClientService</li>
 *   <li>Persistenza ownership ristoranti in file properties</li>
 * </ul>
 * 
 * <h3>Pattern di Utilizzo</h3>
 * <pre>{@code
 * // Creazione per utente guest anonimo
 * GuestPage guestPage = new GuestPage();
 * guestPage.start(primaryStage);
 * 
 * // Creazione per utente autenticato
 * GuestPage userPage = new GuestPage("Italia", "username", "utente");
 * userPage.start(primaryStage);
 * 
 * // Creazione per gestore ristorante
 * GuestPage managerPage = new GuestPage("Italia", "manager", "gestore");
 * managerPage.start(primaryStage);
 * }</pre>
 * 
 * @author Andrea De Nisco, Antonio De Nisco
 * @version 2.0
 * @since 1.0
 * @see ClientService
 * @see LoginForm
 * @see RegisterForm
 * @see Ristorante
 * @see Recensione
 */
public class GuestPage extends Application {
    // Pagination state (moved from showRestaurantTable)
    /**
     * Opzioni per la dimensione della pagina di visualizzazione dei ristoranti.
     */
    private final int[] pageSizeOptions = {20, 50, 100, Integer.MAX_VALUE};
    /**
     * Etichette per le opzioni di dimensione pagina.
     */
    private final String[] pageSizeLabels = {"20", "50", "100", "Tutti"};
    /**
     * Pagina corrente visualizzata.
     */
    private int[] currentPage = {1};
    /**
     * Dimensione della pagina corrente.
     */
    private int[] pageSize = {20};
    /**
     * Numero totale di pagine.
     */
    private int[] totalPages = {1};
    /**
     * Lista dei ristoranti filtrati da mostrare.
     */
    private java.util.List<Ristorante> filteredRestaurants = new java.util.ArrayList<>();
    /**
     * Runnable per aggiornare la tabella dei ristoranti.
     */
    private Runnable updateTable;
    // Helper method to show alerts
    /**
     * Mostra una finestra di dialogo di avviso.
     * @param type Tipo di avviso
     * @param message Messaggio da mostrare
     */
    private void showAlert(Alert.AlertType type, String message) {
        Alert alert = new Alert(type, message, ButtonType.OK);
        alert.showAndWait();
    }

    /**
     * Lista osservabile dei ristoranti da mostrare nella tabella.
     */
    private ObservableList<Ristorante> ristorantiObs = FXCollections.observableArrayList();
    /**
     * Paese selezionato dall'utente.
     */
    private String paeseSelezionato = null;
    /**
     * Username dell'utente corrente.
     */
    private String username = null;
    /**
     * Ruolo dell'utente corrente (guest, utente, gestore).
     */
    private String ruolo = null;
    // Map restaurant name to gestore username (owner)
    /**
     * Mappa che associa il nome del ristorante all'username del gestore.
     */
    private static final java.util.Map<String, String> restaurantOwners = new java.util.HashMap<>();
    /**
     * Percorso del file delle proprietà dei gestori dei ristoranti.
     */
    private static final String OWNER_FILE = "restaurant_owners.properties";

    // Map for average stars and review count for all restaurants (for filtering and display)
    /**
     * Mappa delle medie delle stelle per ogni ristorante.
     */
    private java.util.Map<String, Double> avgStarsMap = new java.util.HashMap<>();
    /**
     * Mappa del numero di recensioni per ogni ristorante.
     */
    private java.util.Map<String, Integer> reviewCountMap = new java.util.HashMap<>();

    // Load owners from file
    /**
     * Blocco statico per caricare i gestori dei ristoranti dal file.
     */
    static {
        loadOwnersFromFile();
    }
    
    /**
     * Carica i gestori dei ristoranti dal file properties.
     */
    private static void loadOwnersFromFile() {
        java.util.Properties props = new java.util.Properties();
        
        // Try to load from classpath first (for JAR)
        try (java.io.InputStream is = GuestPage.class.getClassLoader().getResourceAsStream(OWNER_FILE)) {
            if (is != null) {
                props.load(is);
                for (String name : props.stringPropertyNames()) {
                    restaurantOwners.put(name, props.getProperty(name));
                }
                System.out.println("DEBUG: Loaded " + restaurantOwners.size() + " restaurant owners from classpath");
                return;
            }
        } catch (Exception e) {
            System.out.println("DEBUG: Could not load from classpath: " + e.getMessage());
        }
        
        // Try to load from file system (for development)
        try (java.io.FileInputStream fis = new java.io.FileInputStream("src/main/resources/" + OWNER_FILE)) {
            props.load(fis);
            for (String name : props.stringPropertyNames()) {
                restaurantOwners.put(name, props.getProperty(name));
            }
            System.out.println("DEBUG: Loaded " + restaurantOwners.size() + " restaurant owners from file system");
        } catch (Exception e) {
            System.out.println("DEBUG: Could not load from file system: " + e.getMessage());
        }
    }

    // Save owners to file
    /**
     * Salva la mappa dei gestori dei ristoranti nel file di proprietà.
     */
    private static void saveOwnersToFile() {
        java.util.Properties props = new java.util.Properties();
        for (var entry : restaurantOwners.entrySet()) {
            props.setProperty(entry.getKey(), entry.getValue());
        }
        
        // Try to save to current directory (works for both JAR and development)
        try (java.io.FileOutputStream fos = new java.io.FileOutputStream(OWNER_FILE)) {
            props.store(fos, "Restaurant owners");
            System.out.println("DEBUG: Saved " + restaurantOwners.size() + " restaurant owners to " + OWNER_FILE);
        } catch (Exception e) {
            System.out.println("DEBUG: Could not save to current directory: " + e.getMessage());
            // Fallback: try to save to src/main/resources (for development)
            try (java.io.FileOutputStream fos = new java.io.FileOutputStream("src/main/resources/" + OWNER_FILE)) {
                props.store(fos, "Restaurant owners");
                System.out.println("DEBUG: Saved " + restaurantOwners.size() + " restaurant owners to src/main/resources/" + OWNER_FILE);
            } catch (Exception e2) {
                System.out.println("DEBUG: Could not save restaurant owners: " + e2.getMessage());
            }
        }
    }

    /**
     * Costruttore vuoto per guest anonimo (chiede il paese).
     */
    public GuestPage() { }

    /**
     * Costruttore per login: recupera il luogo di domicilio dal database.
     * @param username Username dell'utente
     * @param ruolo Ruolo dell'utente
     */
    public GuestPage(String username, String ruolo) {
        this.username = username;
        this.ruolo = ruolo;
        // Fetch Luogo_del_domicilio from server
        try {
            String location = ClientService.getInstance().getUserLocation(username);
            if (location != null) {
                this.paeseSelezionato = location;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        // Reload ownership data to ensure it's current
        loadOwnersFromFile();
        System.out.println("DEBUG: Current restaurant owners: " + restaurantOwners);
    }
    

    // (removed stray closing brace)
    // (removed stray closing brace)

    /**
     * Costruttore per guest con paese, username e ruolo specificati.
     * @param paese Paese selezionato
     * @param username Username dell'utente
     * @param ruolo Ruolo dell'utente
     */
    public GuestPage(String paese, String username, String ruolo) {
        this.paeseSelezionato = paese;
        this.username = username;
        this.ruolo = ruolo;
        System.out.println("DEBUG: GuestPage initialized with role: '" + ruolo + "' for user: " + username);
        // Reload ownership data to ensure it's current
        loadOwnersFromFile();
        System.out.println("DEBUG: Current restaurant owners: " + restaurantOwners);
    }

    /**
     * Avvia la finestra principale dell'applicazione guest.
     * @param primaryStage Stage principale
     */
    @Override
    public void start(Stage primaryStage) {
        // Se la nazione è già stata impostata (login o fetch) saltiamo la finestra di scelta
        if (paeseSelezionato != null && !paeseSelezionato.isBlank()) {
            showRestaurantTable(primaryStage);
            return;
        }

        // Altrimenti chiediamo il paese (guest anonimo)
        Stage countryStage = new Stage();
        countryStage.setTitle("Seleziona Residenza");
        countryStage.initModality(Modality.APPLICATION_MODAL);

        ComboBox<String> countryBox = new ComboBox<>();
        List<String> countries = Arrays.stream(Locale.getISOCountries())
                .map(code -> Locale.forLanguageTag("und-" + code).getDisplayCountry())
                .sorted()
                .collect(Collectors.toList());
        countryBox.setItems(FXCollections.observableArrayList(countries));
        countryBox.setPromptText("Seleziona un paese");

        Button btnConfirm = new Button("Conferma");
    btnConfirm.setOnAction(_ -> {
            String sel = countryBox.getValue();
            if (sel == null || sel.isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.WARNING, "Seleziona un Paese!", ButtonType.OK);
                alert.showAndWait();
                return;
            }
            paeseSelezionato = sel;
            countryStage.close();
            showRestaurantTable(primaryStage);
        });

    VBox vb = new VBox(10, new Label("Paese di residenza:"), countryBox, btnConfirm);
    vb.setPadding(new Insets(20));
    vb.setStyle("-fx-background-color: #f8fff8; -fx-border-color: #43a047; -fx-border-width: 3px; -fx-background-radius: 0; -fx-border-radius: 0;");
    Scene countryScene = new Scene(vb, 300, 150);
    vb.prefWidthProperty().bind(countryScene.widthProperty().multiply(0.98));
    vb.prefHeightProperty().bind(countryScene.heightProperty().multiply(0.98));
    countryStage.setScene(countryScene);
    countryStage.showAndWait();
    }

    /**
     * Mostra la tabella dei ristoranti con tutte le funzionalità di ricerca, paginazione e gestione.
     * <p>
     * Crea la UI principale con barra di ricerca avanzata, tabella dei ristoranti, controlli di paginazione e azioni per gestori.
     * La tabella è completamente responsiva e aggiorna i dati in base ai filtri e alla paginazione.
     * <p>
     * I gestori possono modificare, eliminare o aggiungere ristoranti tramite finestre di dialogo dedicate.
     * Gli utenti possono visualizzare recensioni, aggiungere preferiti e filtrare i risultati.
     * @param stage Stage corrente
     */
    @SuppressWarnings("unchecked")
    private void showRestaurantTable(Stage stage) {
    BorderPane root = new BorderPane();
    root.setPadding(new Insets(10));
    root.setStyle("-fx-background-color: #f8fff8; -fx-border-color: #43a047; -fx-border-width: 3px; -fx-background-radius: 0; -fx-border-radius: 0;");
    TableView<Ristorante> table = new TableView<>();
    table.setStyle("-fx-background-color: white; -fx-border-color: #43a047; -fx-border-width: 3px; -fx-table-cell-border-color: #e0f2e9; -fx-background-radius: 0; -fx-border-radius: 0;");

    // Responsive bindings
    root.prefWidthProperty().bind(stage.widthProperty());
    root.prefHeightProperty().bind(stage.heightProperty());
    table.prefWidthProperty().bind(root.widthProperty().multiply(0.98));
    table.prefHeightProperty().bind(root.heightProperty().multiply(0.7));

    // Pagination state is now at class level

    // Advanced search bar
    HBox searchBar = new HBox(8);
    searchBar.setPadding(new Insets(8));
    searchBar.setStyle("-fx-background-color: #e0f2e9; -fx-border-color: #43a047; -fx-border-width: 2px; -fx-background-radius: 0; -fx-border-radius: 0;");
    TextField nameField = new TextField(); nameField.setPromptText("Nome");
    TextField nationField = new TextField(); nationField.setPromptText("Nazione");
    TextField cityField = new TextField(); cityField.setPromptText("Città");
    TextField addressField = new TextField(); addressField.setPromptText("Indirizzo");
    TextField cuisineField = new TextField(); cuisineField.setPromptText("Tipo Cucina");
    TextField priceMinField = new TextField(); priceMinField.setPromptText("Prezzo min");
    TextField priceMaxField = new TextField(); priceMaxField.setPromptText("Prezzo max");
    TextField starMinField = new TextField(); starMinField.setPromptText("Stelle min");
    TextField starMaxField = new TextField(); starMaxField.setPromptText("Stelle max");
    TextField deliveryField = new TextField(); deliveryField.setPromptText("Delivery");
    TextField onlineField = new TextField(); onlineField.setPromptText("Online");
    Button searchBtn = new Button("Cerca");
    searchBtn.setStyle("-fx-background-color: #43a047; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-border-radius: 8;");
    searchBar.getChildren().addAll(nameField, nationField, cityField, addressField, cuisineField, priceMinField, priceMaxField, starMinField, starMaxField, deliveryField, onlineField, searchBtn);

    // Pagination controls
    HBox paginationBar = new HBox(8);
    paginationBar.setPadding(new Insets(8));
    paginationBar.setStyle("-fx-background-color: #e0f2e9; -fx-border-radius: 8; -fx-background-radius: 8; -fx-border-color: #43a047; -fx-border-width: 1px;");
    Button btnFirst = new Button("⏮ Prima");
    Button btnPrev = new Button("⬅ Indietro");
    Button btnNext = new Button("Avanti ➡");
    Button btnLast = new Button("Ultima ⏭");
    btnFirst.setStyle("-fx-background-color: #43a047; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-border-radius: 8;");
    btnPrev.setStyle("-fx-background-color: #43a047; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-border-radius: 8;");
    btnNext.setStyle("-fx-background-color: #43a047; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-border-radius: 8;");
    btnLast.setStyle("-fx-background-color: #43a047; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-border-radius: 8;");
    TextField pageInput = new TextField();
    pageInput.setPromptText("Pag.");
    pageInput.setPrefWidth(50);
    ComboBox<String> pageSizeBox = new ComboBox<>(FXCollections.observableArrayList(pageSizeLabels));
    pageSizeBox.setValue("20");
    Label pageInfoLabel = new Label();
    Label countInfoLabel = new Label();
    paginationBar.getChildren().addAll(btnFirst, btnPrev, btnNext, btnLast, new Label("Vai a pagina:"), pageInput, new Label("Ristoranti per pagina:"), pageSizeBox, pageInfoLabel, countInfoLabel);

    /**
     * Funzione di utilità per aggiornare la tabella dei ristoranti in base alla pagina corrente e ai filtri applicati.
     */
    updateTable = () -> {
        int total = filteredRestaurants.size();
        int size = pageSize[0];
        totalPages[0] = (size == Integer.MAX_VALUE) ? 1 : Math.max(1, (int)Math.ceil((double)total / size));
        if (currentPage[0] < 1) currentPage[0] = totalPages[0];
        if (currentPage[0] > totalPages[0]) currentPage[0] = 1;
        int start = (size == Integer.MAX_VALUE) ? 0 : (currentPage[0] - 1) * size;
        int end = (size == Integer.MAX_VALUE) ? total : Math.min(start + size, total);
        java.util.List<Ristorante> pageList = filteredRestaurants.subList(start, end);
        ristorantiObs.setAll(pageList);
        pageInfoLabel.setText("Pagina " + currentPage[0] + "/" + totalPages[0]);
        countInfoLabel.setText("Mostrati " + pageList.size() + "/" + total + " ristoranti");
    };

    /**
     * Gestore evento per il pulsante di ricerca avanzata.
     * Costruisce il filtro in formato JSON e aggiorna la tabella dei ristoranti.
     */
    searchBtn.setOnAction(_ -> {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        if (!nameField.getText().isEmpty()) sb.append("\"name\":\"" + nameField.getText() + "\",");
        if (!nationField.getText().isEmpty()) sb.append("\"nation\":\"" + nationField.getText() + "\",");
        if (!cityField.getText().isEmpty()) sb.append("\"city\":\"" + cityField.getText() + "\",");
        if (!addressField.getText().isEmpty()) sb.append("\"address\":\"" + addressField.getText() + "\",");
        if (!cuisineField.getText().isEmpty()) sb.append("\"cuisine\":\"" + cuisineField.getText() + "\",");
        if (!priceMinField.getText().isEmpty()) sb.append("\"priceMin\":\"" + priceMinField.getText() + "\",");
        if (!priceMaxField.getText().isEmpty()) sb.append("\"priceMax\":\"" + priceMaxField.getText() + "\",");
        if (!starMinField.getText().isEmpty()) sb.append("\"starMin\":\"" + starMinField.getText() + "\",");
        if (!starMaxField.getText().isEmpty()) sb.append("\"starMax\":\"" + starMaxField.getText() + "\",");
        if (!deliveryField.getText().isEmpty()) sb.append("\"delivery\":\"" + deliveryField.getText() + "\",");
        if (!onlineField.getText().isEmpty()) sb.append("\"online\":\"" + onlineField.getText() + "\",");
        if (sb.charAt(sb.length()-1) == ',') sb.deleteCharAt(sb.length()-1);
        sb.append("}");
        loadRestaurants(sb.toString());
    });

    /**
     * Gestori evento per i pulsanti di paginazione (prima, indietro, avanti, ultima) e input pagina.
     * Permettono la navigazione tra le pagine dei risultati.
     */
    btnFirst.setOnAction(_ -> { currentPage[0] = 1; updateTable.run(); });
    btnLast.setOnAction(_ -> { currentPage[0] = totalPages[0]; updateTable.run(); });
    btnNext.setOnAction(_ -> { currentPage[0] = (currentPage[0] == totalPages[0]) ? 1 : currentPage[0] + 1; updateTable.run(); });
    btnPrev.setOnAction(_ -> { currentPage[0] = (currentPage[0] == 1) ? totalPages[0] : currentPage[0] - 1; updateTable.run(); });
    pageInput.setOnAction(_ -> {
        try {
            int p = Integer.parseInt(pageInput.getText());
            if (p >= 1 && p <= totalPages[0]) {
                currentPage[0] = p;
                updateTable.run();
            }
        } catch (Exception ignored) {}
    });
    /**
     * Gestore evento per la selezione della dimensione pagina.
     * Aggiorna la tabella in base al numero di ristoranti per pagina scelto.
     */
    pageSizeBox.setOnAction(_ -> {
        int idx = pageSizeBox.getSelectionModel().getSelectedIndex();
        pageSize[0] = pageSizeOptions[idx];
        currentPage[0] = 1;
        updateTable.run();
    });

    // ...existing code...
    /**
     * Colonna delle azioni per il gestore (modifica/elimina ristorante).
     * Visibile solo se l'utente ha ruolo gestore e possiede il ristorante.
     */
    TableColumn<Ristorante, Void> ownerActionsCol = new TableColumn<>("Azioni");
    ownerActionsCol.setCellFactory(_ -> new TableCell<Ristorante, Void>() {
        private final Button editBtn = new Button("Modifica");
        private final Button deleteBtn = new Button("Elimina");
        private final HBox btnBox = new HBox(5, editBtn, deleteBtn);
        {
            editBtn.setOnAction(_ -> {
                Ristorante r = getTableView().getItems().get(getIndex());
                // Show dialog to edit restaurant fields
                Stage dialog = new Stage();
                dialog.initModality(Modality.APPLICATION_MODAL);
                dialog.setTitle("Modifica Ristorante");
                VBox dialogRoot = new VBox(10);
                dialogRoot.setPadding(new Insets(20));
                TextField nomeField = new TextField(r.getNome());
                TextField cittaField = new TextField(r.getCitta());
                TextField indirizzoField = new TextField(r.getIndirizzo());
                TextField tipoCucinaField = new TextField(r.getTipoCucina());
                TextField prezzoField = new TextField(r.getFasciaPrezzo() != null ? r.getFasciaPrezzo().toPlainString() : "");
                TextField deliveryField = new TextField(r.getDelivery());
                TextField onlineField = new TextField(r.getOnline());
                Button btnConfirm = new Button("Conferma");
                btnConfirm.setOnAction(_ -> {
                    String oldNome = r.getNome();
                    String nome = nomeField.getText();
                    String citta = cittaField.getText();
                    String indirizzo = indirizzoField.getText();
                    String tipoCucina = tipoCucinaField.getText();
                    BigDecimal fasciaPrezzo = null;
                    try { fasciaPrezzo = new BigDecimal(prezzoField.getText()); } catch (Exception ignored) {}
                    String delivery = deliveryField.getText();
                    String online = onlineField.getText();
                    try {
                        boolean success = ClientService.getInstance().updateRestaurant(
                            oldNome, nome, citta, indirizzo, tipoCucina, 
                            fasciaPrezzo != null ? fasciaPrezzo.toString() : "", 
                            delivery, online
                        );
                        if (success) {
                            // Update in-memory owner map if name changed
                            if (!oldNome.equals(nome)) {
                                String owner = restaurantOwners.remove(oldNome);
                                if (owner != null) restaurantOwners.put(nome, owner);
                                saveOwnersToFile();
                            }
                            showAlert(Alert.AlertType.INFORMATION, "Ristorante modificato!");
                            dialog.close();
                            loadRestaurants(null);
                        } else {
                            showAlert(Alert.AlertType.ERROR, "Errore nella modifica del ristorante");
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        showAlert(Alert.AlertType.ERROR, "Errore nella modifica: " + ex.getMessage());
                    }
                });
                dialogRoot.getChildren().addAll(
                    new Label("Nome:"), nomeField,
                    new Label("Città:"), cittaField,
                    new Label("Indirizzo:"), indirizzoField,
                    new Label("Tipo Cucina:"), tipoCucinaField,
                    new Label("Prezzo:"), prezzoField,
                    new Label("Delivery:"), deliveryField,
                    new Label("Online:"), onlineField,
                    btnConfirm
                );
                dialog.setScene(new Scene(dialogRoot, 400, 400));
                dialog.showAndWait();
            });
            deleteBtn.setOnAction(_ -> {
                Ristorante r = getTableView().getItems().get(getIndex());
                try {
                    boolean success = ClientService.getInstance().deleteRestaurant(r.getNome());
                    if (success) {
                        restaurantOwners.remove(r.getNome());
                        saveOwnersToFile();
                        showAlert(Alert.AlertType.INFORMATION, "Ristorante eliminato!");
                    } else {
                        showAlert(Alert.AlertType.ERROR, "Errore nell'eliminazione del ristorante");
                    }
                    loadRestaurants(null);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    showAlert(Alert.AlertType.ERROR, "Errore nell'eliminazione: " + ex.getMessage());
                }
            });
        }
        @Override
        protected void updateItem(Void item, boolean empty) {
            super.updateItem(item, empty);
            if (empty) {
                setGraphic(null);
            } else {
                Ristorante r = getTableView().getItems().get(getIndex());
                String owner = restaurantOwners.get(r.getNome());
                if ("gestore".equals(ruolo) && username != null && owner != null && owner.equals(username)) {
                    setGraphic(btnBox);
                } else {
                    setGraphic(null);
                }
            }
        }
    });
    table.setItems(ristorantiObs);
    /**
     * Row factory personalizzata per aggiungere una linea di separazione tra ristoranti posseduti e altri.
     * Utile per distinguere visivamente i ristoranti gestiti dall'utente.
     */
    table.setRowFactory(_ -> new TableRow<Ristorante>() {
        @Override
        protected void updateItem(Ristorante item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setStyle("");
            } else {
                // Only show separator for gestore owner
                if ("gestore".equals(ruolo) && username != null) {
                    int idx = getIndex();
                    int ownedCount = 0;
                    for (Ristorante r : ristorantiObs) {
                        String owner = restaurantOwners.get(r.getNome());
                        if (owner != null && owner.equals(username)) {
                            ownedCount++;
                        } else {
                            break;
                        }
                    }
                    // If this is the last owned restaurant, add a bottom border
                    if (idx == ownedCount - 1 && ownedCount > 0 && idx < ristorantiObs.size() - 1) {
                        setStyle("-fx-border-color: black; -fx-border-width: 0 0 5px 0;");
                    } else {
                        setStyle("");
                    }
                } else {
                    setStyle("");
                }
            }
        }
    });
    /**
     * Colonna per la media delle stelle delle recensioni (visibile solo al gestore proprietario).
     */
    TableColumn<Ristorante, String> avgScoreCol = new TableColumn<Ristorante, String>("Media Stelle");
        avgScoreCol.setCellFactory(_ -> new TableCell<Ristorante, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                } else {
                    Ristorante r = getTableView().getItems().get(getIndex());
                    Double avg = null;
                    int count = 0;
                    // Use the same map as above
                    if (r != null) {
                        avg = avgStarsMap.get(r.getNome());
                        count = reviewCountMap.getOrDefault(r.getNome(), 0);
                    }
                    if (count > 0 && avg != null) {
                        setText(String.format("%.2f★", avg));
                    } else {
                        setText("Nessuna recensione");
                    }
                }
            }
        });
    /**
     * Colonna per il nome del ristorante.
     */
    TableColumn<Ristorante, String> nameCol = new TableColumn<>("Nome");
        nameCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getNome()));
    /**
     * Colonna per la nazione del ristorante.
     */
    TableColumn<Ristorante, String> nationCol = new TableColumn<>("Nazione");
        nationCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getNazione()));
    /**
     * Colonna per la città del ristorante.
     */
    TableColumn<Ristorante, String> cityCol = new TableColumn<>("Città");
        cityCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getCitta()));
    /**
     * Colonna per l'indirizzo del ristorante.
     */
    TableColumn<Ristorante, String> addressCol = new TableColumn<>("Indirizzo");
        addressCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getIndirizzo()));
    /**
     * Colonna per il tipo di cucina del ristorante.
     */
    TableColumn<Ristorante, String> cucinaCol = new TableColumn<>("Tipo Cucina");
        cucinaCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getTipoCucina()));
    /**
     * Colonna per la fascia di prezzo del ristorante.
     */
    TableColumn<Ristorante, String> prezzoCol = new TableColumn<>("Prezzo");
        prezzoCol.setCellValueFactory(cellData -> {
            BigDecimal fp = cellData.getValue().getFasciaPrezzo();
            String text = (fp != null) ? fp.toPlainString() + "€" : "-";
            return new javafx.beans.property.SimpleStringProperty(text);
        });
    /**
     * Colonna per la disponibilità di consegna a domicilio.
     */
    TableColumn<Ristorante, String> deliveryCol = new TableColumn<>("Delivery");
        deliveryCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getDelivery()));
    /**
     * Colonna per la disponibilità online del ristorante.
     */
    TableColumn<Ristorante, String> onlineCol = new TableColumn<>("Online");
        onlineCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getOnline()));

    /**
     * Colonna per la gestione dei ristoranti preferiti dell'utente.
     */
    TableColumn<Ristorante, Void> favoriteCol = new TableColumn<>("Preferito");
        favoriteCol.setCellFactory(_ -> new TableCell<Ristorante, Void>() {
            private final Button starBtn = new Button();
            private boolean isFavorite = false;
            private Ristorante currentRistorante;

            private void updateStar() {
                if (isFavorite) {
                    starBtn.setText("★");
                    starBtn.setStyle("-fx-text-fill: gold; -fx-background-color: transparent;");
                } else {
                    starBtn.setText("☆");
                    starBtn.setStyle("-fx-text-fill: gray; -fx-background-color: transparent;");
                }
            }

            {
                starBtn.setOnAction(_ -> {
                    currentRistorante = getTableView().getItems().get(getIndex());
                    if (isFavorite) {
                        // Remove from Preferiti
                        try {
                            boolean success = ClientService.getInstance().removeFavorite(
                                username, currentRistorante.getNome(), 
                                currentRistorante.getCitta(), currentRistorante.getIndirizzo()
                            );
                            if (success) {
                                isFavorite = false;
                            }
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    } else {
                        // Add to Preferiti
                        try {
                            boolean success = ClientService.getInstance().addFavorite(
                                username, currentRistorante.getNome(), 
                                currentRistorante.getCitta(), currentRistorante.getIndirizzo()
                            );
                            if (success) {
                                isFavorite = true;
                            }
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                    updateStar();
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    if (ruolo != null && (ruolo.equals("utente") || ruolo.equals("gestore"))) {
                        currentRistorante = getTableView().getItems().get(getIndex());
                        isFavorite = false;
                        try {
                            isFavorite = ClientService.getInstance().checkFavorite(
                                username, currentRistorante.getNome(), 
                                currentRistorante.getCitta(), currentRistorante.getIndirizzo()
                            );
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                        updateStar();
                        setGraphic(starBtn);
                    } else {
                        setGraphic(null);
                    }
                }
            }
        });

    /**
     * Colonna per la gestione delle recensioni (mie/tutte) per ogni ristorante.
     */
    TableColumn<Ristorante, Void> reviewCol = new TableColumn<>("Recensioni");
        reviewCol.setCellFactory(_ -> new TableCell<Ristorante, Void>() {
            private final Button myReviewsBtn = new Button("Le mie recensioni");
            private final Button allReviewsBtn = new Button("Tutte le recensioni");
            private final HBox btnBox = new HBox(5);
            {
                myReviewsBtn.setOnAction(_ -> {
                    Ristorante r = getTableView().getItems().get(getIndex());
                    openReviewDialogForUser(r, username); // Only user's reviews
                });
                allReviewsBtn.setOnAction(_ -> {
                    Ristorante r = getTableView().getItems().get(getIndex());
                    openReviewDialog(r); // All reviews
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    btnBox.getChildren().clear();
                    if (ruolo != null && !ruolo.equals("guest")) {
                        btnBox.getChildren().addAll(myReviewsBtn, allReviewsBtn);
                    } else {
                        btnBox.getChildren().add(allReviewsBtn);
                    }
                    setGraphic(btnBox);
                }
            }
        });

    // Suppress type safety warning for varargs
    table.getColumns().addAll(nameCol, nationCol, cityCol, addressCol, cucinaCol, prezzoCol, deliveryCol, onlineCol, favoriteCol, reviewCol);
    table.getColumns().add(avgScoreCol);
    table.getColumns().add(ownerActionsCol);

    /**
     * Barra superiore con pulsanti di navigazione, mostra tutti e azioni gestore.
     */
        HBox topBar = new HBox(10);
    topBar.setPadding(new Insets(10));
    topBar.setStyle("-fx-background-color: #43a047; -fx-background-radius: 8; -fx-border-radius: 8;");

        Button btnBack = new Button("Indietro");
    btnBack.setStyle("-fx-background-color: white; -fx-text-fill: #43a047; -fx-font-weight: bold; -fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #43a047; -fx-border-width: 2px;");
    btnBack.setOnAction(_ -> NavigationManager.getInstance().goBack());
    topBar.getChildren().add(btnBack);

    Button btnShowAll = new Button("Mostra tutti");
    btnShowAll.setStyle("-fx-background-color: white; -fx-text-fill: #43a047; -fx-font-weight: bold; -fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #43a047; -fx-border-width: 2px;");
    btnShowAll.setOnAction(_ -> loadRestaurants("__SHOW_ALL__"));
    topBar.getChildren().add(btnShowAll);

        System.out.println("DEBUG: Checking gestore role for button creation. Current role: '" + ruolo + "'");
        if ("gestore".equals(ruolo)) {
            System.out.println("DEBUG: Creating 'Aggiungi Ristorante' button for gestore user");
            Button btnAddRistorante = new Button("Aggiungi Ristorante");
            btnAddRistorante.setStyle("-fx-background-color: white; -fx-text-fill: #43a047; -fx-font-weight: bold; -fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #43a047; -fx-border-width: 2px;");
            btnAddRistorante.setOnAction(_ -> {
                Stage dialog = new Stage();
                dialog.initModality(Modality.APPLICATION_MODAL);
                dialog.setTitle("Aggiungi Ristorante");
                VBox dialogRoot = new VBox(10);
                dialogRoot.setPadding(new Insets(20));
                TextField nomeField = new TextField();
                nomeField.setPromptText("Nome");
                TextField nazioneField = new TextField();
                nazioneField.setPromptText("Nazione");
                TextField cittaField = new TextField();
                cittaField.setPromptText("Città");
                TextField indirizzoField = new TextField();
                indirizzoField.setPromptText("Indirizzo");
                TextField tipoCucinaField = new TextField();
                tipoCucinaField.setPromptText("Tipo Cucina");
                TextField prezzoField = new TextField();
                prezzoField.setPromptText("Prezzo");
                TextField deliveryFieldAdd = new TextField();
                deliveryFieldAdd.setPromptText("Delivery");
                TextField onlineFieldAdd = new TextField();
                onlineFieldAdd.setPromptText("Online");
                Button btnConfirm = new Button("Conferma");
                btnConfirm.setOnAction(_ -> {
                    String nome = nomeField.getText();
                    String nazione = nazioneField.getText();
                    String citta = cittaField.getText();
                    String indirizzo = indirizzoField.getText();
                    String tipoCucina = tipoCucinaField.getText();
                    BigDecimal fasciaPrezzo = null;
                    try { fasciaPrezzo = new BigDecimal(prezzoField.getText()); } catch (Exception ignored) {}
                    String delivery = deliveryFieldAdd.getText();
                    String online = onlineFieldAdd.getText();
                    try {
                        // Use ClientService for client-server restaurant creation
                        boolean success = ClientService.getInstance().addRestaurant(
                            nome, nazione, citta, indirizzo, fasciaPrezzo, delivery, online, tipoCucina, username);
                        
                        if (success) {
                            // Track owner in memory
                            if (username != null && !nome.isEmpty()) {
                                restaurantOwners.put(nome, username);
                                saveOwnersToFile();
                            }
                            showAlert(Alert.AlertType.INFORMATION, "Ristorante aggiunto!");
                            dialog.close();
                            loadRestaurants(null);
                        } else {
                            showAlert(Alert.AlertType.ERROR, "Errore nell'aggiunta del ristorante!");
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        showAlert(Alert.AlertType.ERROR, "Errore nell'aggiunta: " + ex.getMessage());
                    }
                });
                dialogRoot.getChildren().addAll(
                    new Label("Nome:"), nomeField,
                    new Label("Nazione:"), nazioneField,
                    new Label("Città:"), cittaField,
                    new Label("Indirizzo:"), indirizzoField,
                    new Label("Tipo Cucina:"), tipoCucinaField,
                    new Label("Prezzo:"), prezzoField,
                    new Label("Delivery:"), deliveryFieldAdd,
                    new Label("Online:"), onlineFieldAdd,
                    btnConfirm
                );
                dialog.setScene(new Scene(dialogRoot, 400, 500));
                dialog.showAndWait();
            });
            topBar.getChildren().add(btnAddRistorante);
        }

        root.setTop(topBar);
    /**
     * VBox principale che contiene la barra di ricerca, la tabella e la paginazione.
     */
    VBox mainVBox = new VBox(8, searchBar, table, paginationBar);
    mainVBox.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-border-radius: 12; -fx-border-color: #43a047; -fx-border-width: 2px; -fx-padding: 16;");
    root.setCenter(mainVBox);
    Scene scene = new Scene(root, 1000, 600);
    /**
     * Naviga alla scena principale e carica i ristoranti iniziali.
     */
    NavigationManager.getInstance().navigateTo(scene);
    loadRestaurants(null);
    }

    // Dialog for showing, creating, editing, deleting only the current user's reviews for a restaurant
    /**
     * Mostra la finestra di dialogo per gestire le recensioni dell'utente corrente su un ristorante.
     * @param ristorante Ristorante selezionato
     * @param username Username dell'utente
     */
    private void openReviewDialogForUser(Ristorante ristorante, String username) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Le mie recensioni per " + ristorante.getNome());

        VBox root = new VBox(10);
        root.setPadding(new Insets(20));

        // Section: List only current user's reviews for this restaurant
        Label myReviewsLabel = new Label("Le tue recensioni per " + ristorante.getNome() + ":");
        ListView<String> reviewsList = new ListView<>();
        ObservableList<String> reviewsObs = FXCollections.observableArrayList();
        class ReviewInfo {
            int stelle;
            String testo;
            String username;
            String data;
            String ora;
        }
        List<ReviewInfo> reviewInfos = new java.util.ArrayList<>();
        try {
            List<Recensione> userReviews = ClientService.getInstance().getUserReviews(ristorante.getNome(), username);
            for (Recensione review : userReviews) {
                ReviewInfo info = new ReviewInfo();
                info.stelle = review.getStelle();
                info.testo = review.getTesto();
                info.username = review.getUsernameScrittore();
                info.data = review.getData();
                info.ora = review.getOra();
                reviewInfos.add(info);
                String display = String.format("[%s] %s - %d★\n%s\n%s %s",
                    info.username,
                    info.testo,
                    info.stelle,
                    info.data,
                    info.ora,
                    "(tu)");
                reviewsObs.add(display);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        reviewsList.setItems(reviewsObs);
        reviewsList.setPrefHeight(150);

        // Section: Review editor (same as openReviewDialog)
        Label starsLabel = new Label("Valutazione:");
        HBox starsBox = new HBox(5);
        Button[] starBtns = new Button[5];
        int[] selectedStars = {0};
        for (int i = 0; i < 5; i++) {
            Button star = new Button("☆");
            star.setStyle("-fx-font-size: 20; -fx-text-fill: gray; -fx-background-color: transparent;");
            final int starIndex = i;
            star.setOnMouseEntered(_ -> {
                for (int j = 0; j <= starIndex; j++) {
                    starBtns[j].setText("★");
                    starBtns[j].setStyle("-fx-font-size: 20; -fx-text-fill: gold; -fx-background-color: transparent;");
                }
                for (int j = starIndex + 1; j < 5; j++) {
                    starBtns[j].setText("☆");
                    starBtns[j].setStyle("-fx-font-size: 20; -fx-text-fill: gray; -fx-background-color: transparent;");
                }
            });
            star.setOnMouseExited(_ -> {
                for (int j = 0; j < 5; j++) {
                    if (j < selectedStars[0]) {
                        starBtns[j].setText("★");
                        starBtns[j].setStyle("-fx-font-size: 20; -fx-text-fill: gold; -fx-background-color: transparent;");
                    } else {
                        starBtns[j].setText("☆");
                        starBtns[j].setStyle("-fx-font-size: 20; -fx-text-fill: gray; -fx-background-color: transparent;");
                    }
                }
            });
            star.setOnAction(_ -> {
                selectedStars[0] = starIndex + 1;
                for (int j = 0; j < 5; j++) {
                    if (j < selectedStars[0]) {
                        starBtns[j].setText("★");
                        starBtns[j].setStyle("-fx-font-size: 20; -fx-text-fill: gold; -fx-background-color: transparent;");
                    } else {
                        starBtns[j].setText("☆");
                        starBtns[j].setStyle("-fx-font-size: 20; -fx-text-fill: gray; -fx-background-color: transparent;");
                    }
                }
            });
            starBtns[i] = star;
            starsBox.getChildren().add(star);
        }

        TextArea reviewText = new TextArea();
        reviewText.setPromptText("Scrivi la tua recensione...");
        reviewText.setPrefRowCount(5);

        Button saveBtn = new Button("Salva");
        Button deleteBtn = new Button("Elimina");
        Button closeBtn = new Button("Chiudi");
        HBox btnBox = new HBox(10, saveBtn, deleteBtn, closeBtn);

        // Reset stars and text for new review
        selectedStars[0] = 0;
        reviewText.setText("");

        // When user selects a review, load it for edit/delete
    reviewsList.getSelectionModel().selectedIndexProperty().addListener((_, _, newValue) -> {
            int idx = newValue.intValue();
            if (idx >= 0 && idx < reviewInfos.size()) {
                ReviewInfo info = reviewInfos.get(idx);
                selectedStars[0] = info.stelle;
                for (int j = 0; j < 5; j++) {
                    if (j < info.stelle) {
                        starBtns[j].setText("★");
                        starBtns[j].setStyle("-fx-font-size: 20; -fx-text-fill: gold; -fx-background-color: transparent;");
                    } else {
                        starBtns[j].setText("☆");
                        starBtns[j].setStyle("-fx-font-size: 20; -fx-text-fill: gray; -fx-background-color: transparent;");
                    }
                }
                reviewText.setText(info.testo);
                reviewText.setUserData(info);
            }
        });

    saveBtn.setOnAction(_ -> {
            int stelle = selectedStars[0];
            String testo = reviewText.getText();
            Object userData = reviewText.getUserData();
            try {
                if (userData instanceof ReviewInfo) {
                    ReviewInfo info = (ReviewInfo) userData;
                    ClientService.getInstance().updateReview(username, ristorante.getNome(),
                        info.data, info.ora, stelle, testo);
                } else {
                    // Insert new review
                    ClientService.getInstance().aggiungiRecensione(username, ristorante.getNome(),
                        stelle, testo);
                }
                showAlert(Alert.AlertType.INFORMATION, "Recensione salvata!");
                dialog.close();
            } catch (Exception ex) {
                ex.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Errore nel salvataggio della recensione: " + ex.getMessage());
            }
        });

    deleteBtn.setOnAction(_ -> {
            Object userData = reviewText.getUserData();
            if (userData instanceof ReviewInfo) {
                ReviewInfo info = (ReviewInfo) userData;
                try {
                    ClientService.getInstance().deleteReview(username, ristorante.getNome(),
                        info.data, info.ora);
                    showAlert(Alert.AlertType.INFORMATION, "Recensione eliminata!");
                    dialog.close();
                } catch (Exception ex) {
                    ex.printStackTrace();
                    showAlert(Alert.AlertType.ERROR, "Errore nell'eliminazione della recensione: " + ex.getMessage());
                }
            } else {
                showAlert(Alert.AlertType.WARNING, "Seleziona una tua recensione da eliminare.");
            }
        });

    closeBtn.setOnAction(_ -> dialog.close());

        root.getChildren().addAll(myReviewsLabel, reviewsList, starsLabel, starsBox, new Label("Testo:"), reviewText, btnBox);
        dialog.setScene(new Scene(root, 500, 500));
        dialog.showAndWait();
    } // end showRestaurantTable
    // Dialog for gestore to add a new restaurant

    // Dialog for creating/editing/deleting a review
    /**
     * Mostra la finestra di dialogo con tutte le recensioni di un ristorante.
     * @param ristorante Ristorante selezionato
     * @throws SQLException Eccezione SQL
     */
    private void openReviewDialog(Ristorante ristorante) {
    Stage dialog = new Stage();
    dialog.initModality(Modality.APPLICATION_MODAL);
    dialog.setTitle("Tutte le recensioni per " + ristorante.getNome());
    VBox root = new VBox(10);
    root.setPadding(new Insets(20));
    boolean isGestore = "gestore".equals(ruolo);
    boolean isOwner = false;
    // Check owner from in-memory map
    if (isGestore && username != null && ristorante != null) {
        String owner = restaurantOwners.get(ristorante.getNome());
        if (owner != null && owner.equals(username)) {
            isOwner = true;
        }
    }
    VBox reviewsBox = new VBox(10);
    Label allReviewsLabel = new Label("Tutte le recensioni per " + ristorante.getNome() + ":");
    try {
        List<Recensione> allReviews = ClientService.getInstance().getRecensioniPerRistorante(ristorante.getNome());
        for (Recensione review : allReviews) {
                int stelle = review.getStelle();
                String testo = review.getTesto();
                String reviewUser = review.getUsernameScrittore();
                String data = review.getData();
                String ora = review.getOra();
                String rispostaGestore = review.getRispostaGestore();

                VBox reviewPane = new VBox(5);
                reviewPane.setStyle("-fx-border-color: #ccc; -fx-padding: 8;");
                Label reviewLabel = new Label(String.format("[%s] %s - %d★\n%s\n%s", reviewUser, testo, stelle, data, ora));
                reviewPane.getChildren().add(reviewLabel);

                if (rispostaGestore != null && !rispostaGestore.isEmpty()) {
                    Label rispostaLabel = new Label("Risposta gestore: " + rispostaGestore);
                    reviewPane.getChildren().add(rispostaLabel);
                    if (isGestore && isOwner) {
                        Button editBtn = new Button("Modifica");
                        Button deleteBtn = new Button("Elimina");
                        HBox btnBox = new HBox(8, editBtn, deleteBtn);
                        reviewPane.getChildren().add(btnBox);

                        editBtn.setOnAction(_ -> {
                            reviewPane.getChildren().removeAll(rispostaLabel, btnBox);
                            TextArea rispostaArea2 = new TextArea(rispostaGestore);
                            rispostaArea2.setPrefRowCount(2);
                            Button saveBtn2 = new Button("Salva modifica");
                            VBox rispostaBox2 = new VBox(5, rispostaArea2, saveBtn2);
                            reviewPane.getChildren().add(rispostaBox2);
                            saveBtn2.setOnAction(_ -> {
                                String nuovaRisposta = rispostaArea2.getText();
                                if (nuovaRisposta.trim().isEmpty()) {
                                    showAlert(Alert.AlertType.WARNING, "Inserisci una risposta.");
                                    return;
                                }
                                try {
                                    ClientService.getInstance().updateManagerResponse(reviewUser, ristorante.getNome(), data, ora, nuovaRisposta);
                                    showAlert(Alert.AlertType.INFORMATION, "Risposta modificata!");
                                    reviewPane.getChildren().remove(rispostaBox2);
                                    rispostaLabel.setText("Risposta gestore: " + nuovaRisposta);
                                    reviewPane.getChildren().add(rispostaLabel);
                                    reviewPane.getChildren().add(btnBox);
                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                    showAlert(Alert.AlertType.ERROR, "Errore nella modifica della risposta: " + ex.getMessage());
                                }
                            });
                        });
                        deleteBtn.setOnAction(_ -> {
                            try {
                                ClientService.getInstance().deleteManagerResponse(reviewUser, ristorante.getNome(), data, ora);
                                showAlert(Alert.AlertType.INFORMATION, "Risposta eliminata!");
                                reviewPane.getChildren().removeAll(rispostaLabel, btnBox);
                            } catch (Exception ex) {
                                ex.printStackTrace();
                                showAlert(Alert.AlertType.ERROR, "Errore nell'eliminazione della risposta: " + ex.getMessage());
                            }
                        });
                    }
                } else if (isGestore && isOwner) {
                    // Show '+' button for gestore owner to reply if no response exists
                    Button replyBtn = new Button("+");
                    reviewPane.getChildren().add(replyBtn);
                    replyBtn.setOnAction(_ -> {
                        reviewPane.getChildren().remove(replyBtn);
                        TextArea rispostaArea = new TextArea();
                        rispostaArea.setPromptText("Rispondi come gestore...");
                        rispostaArea.setPrefRowCount(2);
                        Button saveBtn = new Button("Salva risposta");
                        VBox rispostaBox = new VBox(5, rispostaArea, saveBtn);
                        reviewPane.getChildren().add(rispostaBox);
                        saveBtn.setOnAction(_ -> {
                            String risposta = rispostaArea.getText();
                            if (risposta.trim().isEmpty()) {
                                showAlert(Alert.AlertType.WARNING, "Inserisci una risposta.");
                                return;
                            }
                            try {
                                ClientService.getInstance().updateManagerResponse(reviewUser, ristorante.getNome(), data, ora, risposta);
                                showAlert(Alert.AlertType.INFORMATION, "Risposta salvata!");
                                reviewPane.getChildren().remove(rispostaBox);
                                Label rispostaLabel = new Label("Risposta gestore: " + risposta);
                                reviewPane.getChildren().add(rispostaLabel);
                                // Add modify/delete buttons after reply
                                Button editBtn = new Button("Modifica");
                                Button deleteBtn = new Button("Elimina");
                                HBox btnBox = new HBox(8, editBtn, deleteBtn);
                                reviewPane.getChildren().add(btnBox);
                                editBtn.setOnAction(_ -> {
                                    reviewPane.getChildren().removeAll(rispostaLabel, btnBox);
                                    TextArea rispostaArea2 = new TextArea(risposta);
                                    rispostaArea2.setPrefRowCount(2);
                                    Button saveBtn2 = new Button("Salva modifica");
                                    VBox rispostaBox2 = new VBox(5, rispostaArea2, saveBtn2);
                                    reviewPane.getChildren().add(rispostaBox2);
                                    saveBtn2.setOnAction(_ -> {
                                        String nuovaRisposta = rispostaArea2.getText();
                                        if (nuovaRisposta.trim().isEmpty()) {
                                            showAlert(Alert.AlertType.WARNING, "Inserisci una risposta.");
                                            return;
                                        }
                                        try {
                                            ClientService.getInstance().updateManagerResponse(reviewUser, ristorante.getNome(), data, ora, nuovaRisposta);
                                            showAlert(Alert.AlertType.INFORMATION, "Risposta modificata!");
                                            reviewPane.getChildren().remove(rispostaBox2);
                                            rispostaLabel.setText("Risposta gestore: " + nuovaRisposta);
                                            reviewPane.getChildren().add(rispostaLabel);
                                            reviewPane.getChildren().add(btnBox);
                                        } catch (Exception ex) {
                                            ex.printStackTrace();
                                            showAlert(Alert.AlertType.ERROR, "Errore nella modifica della risposta: " + ex.getMessage());
                                        }
                                    });
                                });
                                deleteBtn.setOnAction(_ -> {
                                    try {
                                        ClientService.getInstance().deleteManagerResponse(reviewUser, ristorante.getNome(), data, ora);
                                        showAlert(Alert.AlertType.INFORMATION, "Risposta eliminata!");
                                        reviewPane.getChildren().removeAll(rispostaLabel, btnBox);
                                    } catch (Exception ex) {
                                        ex.printStackTrace();
                                        showAlert(Alert.AlertType.ERROR, "Errore nell'eliminazione della risposta: " + ex.getMessage());
                                    }
                                });
                            } catch (Exception ex) {
                                ex.printStackTrace();
                                showAlert(Alert.AlertType.ERROR, "Errore nel salvataggio della risposta: " + ex.getMessage());
                            }
                        });
                    });
                }
                reviewsBox.getChildren().add(reviewPane);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        Button closeBtn = new Button("Chiudi");
    closeBtn.setOnAction(_ -> dialog.close());
        ScrollPane scroll = new ScrollPane(reviewsBox);
        scroll.setFitToWidth(true);
        root.getChildren().addAll(allReviewsLabel, scroll, closeBtn);
        dialog.setScene(new Scene(root, 600, 600));
        dialog.showAndWait();
    }

    /**
     * Carica e filtra la lista dei ristoranti in base ai criteri di ricerca.
     * @param filtro Filtro di ricerca in formato stringa
     */
    private void loadRestaurants(String filtro) {
        filteredRestaurants.clear();

        // Clear ratings maps - in client-server mode, reviews are loaded per restaurant
        avgStarsMap.clear();
        reviewCountMap.clear();

        boolean showAll = "__SHOW_ALL__".equals(filtro);
        boolean isSearch = false;
        if (filtro != null && !filtro.isEmpty() && !showAll) {
            String f = filtro.replaceAll("[{}\"]", "").trim();
            if (!f.isEmpty()) isSearch = true;
        }
        String nationEnglish = paeseSelezionato;
        for (String code : java.util.Locale.getISOCountries()) {
            java.util.Locale itLocale = java.util.Locale.forLanguageTag("it");
            java.util.Locale enLocale = java.util.Locale.forLanguageTag("en");
            java.util.Locale countryLocale = java.util.Locale.forLanguageTag("und-" + code);
            if (countryLocale.getDisplayCountry(itLocale).equalsIgnoreCase(paeseSelezionato)) {
                nationEnglish = countryLocale.getDisplayCountry(enLocale);
                break;
            }
        }
        String mapped = nationEnglish;
        System.out.println("DEBUG: User nation: '" + paeseSelezionato + "', Query: '" + mapped + "', showAll: " + showAll);
        System.out.println("DEBUG: Looking for restaurants in nation: '" + nationEnglish + "'");

        java.util.Map<String, String> filterMap = new java.util.HashMap<>();
        BigDecimal priceMin = null, priceMax = null;
        Double starMin = null, starMax = null;
        boolean starMinValid = true, starMaxValid = true;
        if (filtro != null && !filtro.isEmpty()) {
            filtro = filtro.trim();
            if (filtro.startsWith("{") && filtro.endsWith("}")) {
                filtro = filtro.substring(1, filtro.length() - 1);
                String[] pairs = filtro.split(",");
                for (String pair : pairs) {
                    String[] kv = pair.split(":", 2);
                    if (kv.length == 2) {
                        String key = kv[0].replaceAll("[\"{}]", "").trim();
                        String value = kv[1].replaceAll("[\"{}]", "").trim();
                        filterMap.put(key, value);
                        if (key.equals("priceMin")) try { priceMin = new BigDecimal(value); } catch (Exception ignored) {}
                        if (key.equals("priceMax")) try { priceMax = new BigDecimal(value); } catch (Exception ignored) {}
                        if (key.equals("starMin")) {
                            if (!value.isEmpty()) {
                                String normalized = value.replace(',', '.');
                                try { starMin = Double.parseDouble(normalized); }
                                catch (Exception ex1) {
                                    try { starMin = (double) Integer.parseInt(normalized); } catch (Exception ex2) { starMinValid = false; }
                                }
                            }
                        }
                        if (key.equals("starMax")) {
                            if (!value.isEmpty()) {
                                String normalized = value.replace(',', '.');
                                try { starMax = Double.parseDouble(normalized); }
                                catch (Exception ex1) {
                                    try { starMax = (double) Integer.parseInt(normalized); } catch (Exception ex2) { starMaxValid = false; }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Use direct database access through DAO
        java.util.List<Ristorante> owned = new java.util.ArrayList<>();
        java.util.List<Ristorante> dbOnly = new java.util.ArrayList<>();
        
        try {
            // Create DAO instances for direct database access
            RistoranteDAO ristoranteDAO = new RistoranteDAO();
            RecensioneDAO recensioneDAO = new RecensioneDAO();
            
            // Get ALL restaurants from database (no server-side filtering)
            java.util.List<Ristorante> allRestaurants = ristoranteDAO.cercaRistoranti(
                null, null, null, null, null, null);
            
            // Populate rating maps from database
            avgStarsMap.putAll(recensioneDAO.getAllRestaurantRatings());
            reviewCountMap.putAll(recensioneDAO.getAllRestaurantReviewCounts());
            System.out.println("DEBUG: Loaded " + avgStarsMap.size() + " restaurant ratings from database");
            
            // Apply client-side filtering with all the original logic
            for (Ristorante r : allRestaurants) {
                String nome = r.getNome();
                String nazione = r.getNazione();
                String citta = r.getCitta();
                String indirizzo = r.getIndirizzo();
                BigDecimal fasciaPrezzo = r.getFasciaPrezzo();
                String delivery = r.getDelivery();
                String online = r.getOnline();
                String tipoCucina = r.getTipoCucina();

                if (!showAll && !isSearch && (nazione == null || !nazione.equalsIgnoreCase(nationEnglish))) {
                    continue;
                }

                boolean matches = true;
                for (var entry : filterMap.entrySet()) {
                    String key = entry.getKey();
                    String val = entry.getValue();
                    if (val == null || val.trim().isEmpty()) continue;
                    if (key.equals("priceMin") || key.equals("priceMax") || key.equals("starMin") || key.equals("starMax")) continue;
                    val = val.toLowerCase();
                    String field = "";
                    switch(key) {
                        case "name": field = nome != null ? nome : ""; break;
                        case "nation": field = nazione != null ? nazione : ""; break;
                        case "city": field = citta != null ? citta : ""; break;
                        case "address": field = indirizzo != null ? indirizzo : ""; break;
                        case "cuisine": field = tipoCucina != null ? tipoCucina : ""; break;
                        case "delivery": field = delivery != null ? delivery : ""; break;
                        case "online": field = online != null ? online : ""; break;
                    }
                    if (!field.toLowerCase().contains(val)) { matches = false; break; }
                }
                if (priceMin != null) {
                    if (fasciaPrezzo == null || fasciaPrezzo.compareTo(priceMin) < 0) matches = false;
                }
                if (priceMax != null) {
                    if (fasciaPrezzo == null || fasciaPrezzo.compareTo(priceMax) > 0) matches = false;
                }
                if ((starMin != null && starMinValid) || (starMax != null && starMaxValid)) {
                    Double avg = avgStarsMap.get(nome);
                    int count = reviewCountMap.getOrDefault(nome, 0);
                    if (count == 0 || avg == null) {
                        if ((starMin != null && starMinValid) || (starMax != null && starMaxValid)) {
                            matches = false;
                        }
                    } else {
                        if (starMin != null && starMinValid && avg < starMin) matches = false;
                        if (starMax != null && starMaxValid && avg > starMax) matches = false;
                    }
                }
                if (matches) {
                    String owner = restaurantOwners.get(nome);
                    if (owner != null && username != null && owner.equals(username)) owned.add(r);
                    else dbOnly.add(r);
                }
            }
        } catch (Exception ex) { 
            ex.printStackTrace();
            System.err.println("Error loading restaurants from server: " + ex.getMessage());
        }

        filteredRestaurants.addAll(owned);
        filteredRestaurants.addAll(dbOnly);
        // After filtering, update the table for the current page
        if (pageSize != null && currentPage != null && updateTable != null) {
            currentPage[0] = 1;
            updateTable.run();
        }
    }

    // End of GuestPage class
}
