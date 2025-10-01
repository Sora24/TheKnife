package com.example;

import javafx.scene.Scene;
import javafx.stage.Stage;
import java.util.Stack;

/**
 * Gestore centralizzato della navigazione tra le scene dell'applicazione JavaFX TheKnife.
 * <p>
 * Questa classe implementa il pattern Singleton per fornire un sistema di navigazione
 * unificato e coerente tra tutte le schermate dell'applicazione. Gestisce la transizione
 * tra le diverse interfacce utente (login, registrazione, pagina principale) mantenendo
 * uno storico delle scene per supportare la navigazione all'indietro.
 * <p>
 * Il sistema di navigazione supporta:
 * <ul>
 *   <li>Transizioni fluide tra scene diverse</li>
 *   <li>Stack-based navigation con possibilità di tornare indietro</li>
 *   <li>Gestione centralizzata dello Stage principale</li>
 *   <li>Controllo dell'lifecycle delle scene</li>
 * </ul>
 * <p>
 * <strong>Pattern Implementati:</strong>
 * <ul>
 *   <li>Singleton per istanza globale unica</li>
 *   <li>Stack per gestione dello storico di navigazione</li>
 * </ul>
 * <p>
 * <strong>Architettura:</strong> Componente fondamentale del client JavaFX che
 * coordina la user experience e l'integrazione tra le diverse interfacce utente.
 * 
 * @author Andrea De Nisco, Antonio De Nisco
 * @version 2.0 - Ottimizzato per architettura client-server
 * @since 1.0
 * @see MainApp
 * @see LoginForm
 * @see RegisterForm
 * @see GuestPage
 */
public class NavigationManager {
    /**
     * Istanza singleton del gestore di navigazione.
     */
    private static NavigationManager instance;
    /**
     * Stage principale dell'applicazione.
     */
    private Stage primaryStage;
    /**
     * Stack delle scene per la navigazione indietro.
     */
    private final Stack<Scene> sceneStack = new Stack<>();

    /**
     * Costruttore privato per il pattern singleton.
     * Utilizzato solo internamente per garantire l'unicità dell'istanza.
     */
    private NavigationManager() {}

    /**
     * Restituisce l'istanza singleton del gestore di navigazione.
     * @return NavigationManager istanza
     */
    public static NavigationManager getInstance() {
        if (instance == null) {
            instance = new NavigationManager();
        }
        return instance;
    }

    /**
     * Imposta lo stage principale dell'applicazione.
     * @param stage lo stage principale
     */
    public void setStage(Stage stage) {
        this.primaryStage = stage;
    }

    /**
     * Naviga verso una nuova scena, salvando la scena corrente nello stack.
     * @param newScene la nuova scena da visualizzare
     */
    public void navigateTo(Scene newScene) {
        if (primaryStage.getScene() != null) {
            sceneStack.push(primaryStage.getScene());
        }
        primaryStage.setScene(newScene);
    }

    /**
     * Torna alla scena precedente se disponibile.
     */
    public void goBack() {
        if (!sceneStack.isEmpty()) {
            Scene prev = sceneStack.pop();
            primaryStage.setScene(prev);
        }
    }
}
