package com.example;

/**
 * Classe launcher per avviare l'applicazione client JavaFX di TheKnife.
 * 
 * <p>Questa classe funge da punto di ingresso principale che risolve problemi comuni
 * del runtime JavaFX evitando conflitti con il sistema di moduli Java. Implementa
 * una strategia di avvio robusta con gestione degli errori e diagnostica automatica.
 * 
 * <h3>Problematiche Risolte</h3>
 * <ul>
 *   <li><strong>Conflitti Moduli:</strong> Evita estensione diretta di Application</li>
 *   <li><strong>Runtime Missing:</strong> Diagnostica automatica componenti JavaFX mancanti</li>
 *   <li><strong>Headless Mode:</strong> Configura proprietà sistema per GUI</li>
 *   <li><strong>Error Reporting:</strong> Messaggi di errore utente-friendly</li>
 * </ul>
 * 
 * <h3>Requisiti Runtime</h3>
 * <p>L'applicazione richiede:
 * <ul>
 *   <li>Java 21+ con moduli JavaFX (controls, fxml)</li>
 *   <li>Accesso di rete per connessione server (localhost:8080)</li>
 *   <li>Sistema operativo con supporto GUI (non headless)</li>
 * </ul>
 * 
 * <h3>Modalità di Avvio</h3>
 * <pre>{@code
 * // Avvio diretto (se JavaFX è nel classpath)
 * java -jar TheKnife-client.jar
 * 
 * // Avvio con module path esplicito
 * java --module-path /path/to/javafx/lib 
 *      --add-modules javafx.controls,javafx.fxml 
 *      -jar TheKnife-client.jar
 * }</pre>
 * 
 * <h3>Gestione Errori</h3>
 * <p>Il launcher fornisce diagnostica automatica per:
 * <ul>
 *   <li>Componenti JavaFX mancanti con istruzioni correzione</li>
 *   <li>Problemi di configurazione moduli</li>
 *   <li>Errori generici di avvio con stack trace</li>
 * </ul>
 * 
 * @author Andrea De Nisco, Antonio De Nisco
 * @version 2.0 - Ottimizzato per deployment client-server
 * @since 1.5
 * @see ClientApp
 * @see MainApp
 */
public class ClientLauncher {
    
    /**
     * Costruttore privato per prevenire istanziazione.
     * Questa è una classe di utilità con solo metodi statici.
     */
    private ClientLauncher() {
        // Utility class - no instantiation
    }
    
    /**
     * Metodo principale che avvia l'applicazione client TheKnife.
     * 
     * <p>Configura l'ambiente di esecuzione per JavaFX e avvia {@link ClientApp}
     * utilizzando il framework Application. Implementa gestione errori robusta
     * con diagnostica automatica per problemi comuni di runtime.
     * 
     * <p><strong>Processo di Avvio:</strong>
     * <ol>
     *   <li>Configura proprietà sistema per supporto GUI</li>
     *   <li>Avvia ClientApp tramite Application.launch()</li>
     *   <li>Gestisce errori di runtime JavaFX con messaggi utili</li>
     *   <li>Fornisce istruzioni di correzione per errori comuni</li>
     * </ol>
     * 
     * @param args argomenti da linea di comando (attualmente non utilizzati)
     * @throws RuntimeException se JavaFX runtime non è disponibile
     * @see ClientApp#main(String[])
     * @since 1.5
     */
    public static void main(String[] args) {
        // Set system properties to help with JavaFX runtime
        System.setProperty("java.awt.headless", "false");
        
        try {
            // Avvia l'applicazione JavaFX utilizzando il metodo Application.launch
            javafx.application.Application.launch(ClientApp.class, args);
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("JavaFX runtime components are missing")) {
                System.err.println("=== Problema Runtime JavaFX ===");
                System.err.println("Il runtime JavaFX non è disponibile con questa installazione Java.");
                System.err.println("Eseguire con: java --module-path /path/to/javafx/lib --add-modules javafx.controls,javafx.fxml -jar TheKnife-client.jar");
                System.err.println("Oppure installare OpenJDK con JavaFX incluso.");
                System.exit(1);
            } else {
                throw e;
            }
        } catch (Exception e) {
            System.err.println("Errore nell'avvio dell'applicazione client: " + e.getMessage());
            e.printStackTrace();
        }
    }
}