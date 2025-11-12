/**
 * Modulo principale dell'applicazione TheKnife - Sistema di recensioni ristoranti.
 * 
 * <p>Questo modulo implementa un'applicazione client-server completa per la gestione
 * e consultazione di recensioni di ristoranti. Il sistema è progettato con architettura
 * modulare per garantire sicurezza, manutenibilità e scalabilità.
 * 
 * <h2>Componenti Principali</h2>
 * <ul>
 *   <li><strong>Client JavaFX:</strong> Interfaccia utente desktop moderna</li>
 *   <li><strong>Server TCP:</strong> Servizio backend per logica di business</li>
 *   <li><strong>Database Layer:</strong> Accesso dati PostgreSQL sicuro</li>
 *   <li><strong>Security Layer:</strong> Autenticazione e crittografia password</li>
 * </ul>
 * 
 * <h2>Dipendenze del Modulo</h2>
 * <ul>
 *   <li><code>javafx.controls</code> - Componenti UI per interfaccia utente</li>
 *   <li><code>javafx.fxml</code> - Supporto per file FXML di layout</li>
 *   <li><code>javafx.graphics</code> - Rendering grafico e gestione scene</li>
 *   <li><code>java.sql</code> - Connettività database JDBC</li>
 * </ul>
 * 
 * <h2>Esportazioni</h2>
 * <p>Il modulo esporta il package {@code com.example} per consentire
 * l'accesso alle classi pubbliche dell'applicazione da parte di moduli esterni
 * e del runtime JavaFX.
 * 
 * <h2>Riflessione</h2>
 * <p>Il package {@code com.example} è aperto al modulo {@code javafx.fxml}
 * per permettere l'iniezione di dipendenze FXML e l'accesso via riflessione
 * ai controller delle scene.
 * 
 * @author Andrea De Nisco, Antonio De Nisco
 * @version 2.0
 * @since 1.0
 */
module com.example {
    requires javafx.controls;
    requires javafx.fxml;
    requires transitive javafx.graphics;
    requires transitive java.sql;
    opens com.example to javafx.fxml;
    exports com.example;
}