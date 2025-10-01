package com.example;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object (DAO) per la gestione dei ristoranti nel database PostgreSQL.
 * <p>
 * Questa classe implementa il pattern DAO per fornire un'interfaccia unificata
 * per tutte le operazioni sui dati dei ristoranti. Supporta ricerche avanzate
 * con filtri multipli e costruzione dinamica delle query SQL per ottimizzare
 * le performance del database.
 * <p>
 * La classe gestisce la mappatura tra oggetti Java {@link Ristorante} e le righe
 * della tabella database, implementando metodi per:
 * <ul>
 *   <li>Ricerca per criteri multipli (nazione, città, cucina, prezzo)</li>
 *   <li>Filtri per servizi speciali (delivery, online)</li>
 *   <li>Costruzione dinamica delle query con PreparedStatement</li>
 *   <li>Mapping sicuro dei ResultSet agli oggetti modello</li>
 * </ul>
 * <p>
 * <strong>Sicurezza:</strong> Tutte le query utilizzano PreparedStatement per
 * prevenire attacchi di SQL injection. I parametri vengono validati e sanitizzati
 * prima dell'esecuzione delle query.
 * <p>
 * <strong>Architettura:</strong> Utilizzato dal ServerService nell'architettura
 * client-server per servire le richieste di ricerca ristoranti dai client.
 * 
 * @author Andrea De Nisco, Antonio De Nisco
 * @version 2.0 - Ottimizzato per architettura client-server
 * @since 1.0
 * @see Ristorante
 * @see DBConnection
 * @see ServerService
 */
public class RistoranteDAO {
    /**
     * Costruttore di default.
     */
    public RistoranteDAO() {}

    /**
     * Esegue una ricerca di ristoranti nel database in base ai parametri specificati.
    *
     * I parametri possono essere nulli o vuoti per ignorare il filtro corrispondente. La fascia di prezzo viene gestita tramite intervalli predefiniti.
     * Se delivery o online sono true, vengono filtrati solo i ristoranti che offrono questi servizi.
    *
     * La query viene costruita dinamicamente in base ai parametri forniti e i risultati vengono convertiti in oggetti {@link Ristorante}.
     * 
     * @param nazione la nazione del ristorante da cercare (può essere null)
     * @param citta la città del ristorante da cercare (può essere null)
     * @param tipoCucina il tipo di cucina da cercare (può essere null)
    * @param fasciaPrezzo la fascia di prezzo ("&lt;10", "10-15", "&gt;20")
     * @param delivery true se si vuole filtrare solo ristoranti con consegna a domicilio
     * @param online true se si vuole filtrare solo ristoranti disponibili online
     * @return lista di ristoranti che corrispondono ai criteri di ricerca
     */
    public List<Ristorante> cercaRistoranti(String nazione, String citta, 
                                            String tipoCucina, String fasciaPrezzo, 
                                            Boolean delivery, Boolean online) {
    /**
     * Lista che conterrà i ristoranti trovati.
     */
    List<Ristorante> lista = new ArrayList<>();
    /**
     * StringBuilder per costruire la query SQL dinamicamente in base ai parametri.
     */
    StringBuilder query = new StringBuilder("SELECT * FROM \"ristorantitheknife\" WHERE 1=1");

        // Aggiunta dei filtri alla query in base ai parametri
        if (nazione != null && !nazione.isEmpty()) query.append(" AND \"Nazione\" ILIKE ?");
        if (citta != null && !citta.isEmpty()) query.append(" AND \"Citta\" ILIKE ?");
        if (tipoCucina != null && !tipoCucina.isEmpty()) query.append(" AND \"Tipo_di_cucina\" ILIKE ?");
        if (fasciaPrezzo != null) {
            switch (fasciaPrezzo) {
                case "<10": query.append(" AND \"Fascia_di_prezzo\" < 10"); break;
                case "10-15": query.append(" AND \"Fascia_di_prezzo\" BETWEEN 10 AND 15"); break;
                case ">20": query.append(" AND \"Fascia_di_prezzo\" > 20"); break;
            }
        }
        if (delivery != null) query.append(" AND \"Delivery\" = 'SI'");
        if (online != null) query.append(" AND \"Online\" = 'SI'");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(query.toString())) {

            /**
             * Indice per i parametri della query SQL.
             */
            int index = 1;
            // Impostazione dei parametri della query in base ai filtri
            if (nazione != null && !nazione.isEmpty()) ps.setString(index++, "%" + nazione + "%");
            if (citta != null && !citta.isEmpty()) ps.setString(index++, "%" + citta + "%");
            if (tipoCucina != null && !tipoCucina.isEmpty()) ps.setString(index++, "%" + tipoCucina + "%");

            /**
             * Esecuzione della query e popolamento della lista dei ristoranti.
             */
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                // Creazione dell'oggetto Ristorante dai dati del ResultSet
                Ristorante r = new Ristorante(
                        rs.getString("Nome"),
                        rs.getString("Nazione"),
                        rs.getString("Citta"),
                        rs.getString("Indirizzo"),
                        rs.getBigDecimal("Fascia_di_prezzo"),
                        rs.getString("Delivery"),
                        rs.getString("Online"),
                        rs.getString("Tipo_di_cucina")
                );
                lista.add(r);
            }

        } catch (SQLException e) {
            // Gestione delle eccezioni SQL
            e.printStackTrace();
        }
        // Restituisce la lista dei ristoranti trovati
        return lista;
    }

}
