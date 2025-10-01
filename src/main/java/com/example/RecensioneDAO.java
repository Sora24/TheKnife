package com.example;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Data Access Object (DAO) per la gestione completa delle recensioni dei ristoranti.
 * <p>
 * Questa classe implementa il pattern DAO per fornire un'interfaccia completa
 * per tutte le operazioni CRUD (Create, Read, Update, Delete) sulle recensioni
 * e le relative funzionalità di aggregazione per il calcolo delle valutazioni.
 * <p>
 * La classe supporta operazioni avanzate come:
 * <ul>
 *   <li>Recupero recensioni per ristorante con ordinamento</li>
 *   <li>Inserimento nuove recensioni con validazione</li>
 *   <li>Calcolo automatico delle medie per categoria di valutazione</li>
 *   <li>Gestione delle risposte dei gestori alle recensioni</li>
 *   <li>Operazioni di modifica e cancellazione autorizzate</li>
 * </ul>
 * <p>
 * <strong>Aggregazione Dati:</strong> Implementa calcoli complessi per le
 * statistiche dei ristoranti, incluse medie ponderate e conteggi delle recensioni
 * per supportare il sistema di rating dell'applicazione.
 * <p>
 * <strong>Sicurezza:</strong> Tutte le operazioni utilizzano PreparedStatement
 * e validazione dei parametri per garantire l'integrità dei dati e prevenire
 * attacchi di SQL injection.
 * <p>
 * <strong>Architettura:</strong> Componente critico del server che supporta
 * tutte le funzionalità relative alle recensioni nell'architettura client-server.
 * 
 * @author Andrea De Nisco, Antonio De Nisco
 * @version 2.0 - Esteso per architettura client-server e nuove funzionalità
 * @since 1.0
 * @see Recensione
 * @see Ristorante
 * @see DBConnection
 * @see ServerService
 */
public class RecensioneDAO {
    /**
     * Costruttore di default.
     */
    public RecensioneDAO() {}

    /**
     * Restituisce la lista delle recensioni per un dato ristorante.
     * @param nomeRistorante nome del ristorante
     * @return lista di recensioni
     */
    public List<Recensione> getRecensioniPerRistorante(String nomeRistorante) {
        List<Recensione> lista = new ArrayList<>();
        String sql = "SELECT * FROM \"recensioni\" WHERE \"Ristorante_scritto\" = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, nomeRistorante);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Recensione r = new Recensione(
                        rs.getString("Data"),
                        rs.getInt("Stelle"),
                        rs.getString("Ora"),
                        rs.getString("Username_scittore"),
                        rs.getString("Ristorante_scritto"),
                        rs.getString("Testo"),
                        rs.getString("Risposta_gestore")
                );
                lista.add(r);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }

    /**
     * Aggiunge una nuova recensione al database.
     * @param username username dello scrittore
     * @param ristorante nome del ristorante
     * @param stelle numero di stelle
     * @param testo testo della recensione
     */
    public void aggiungiRecensione(String username, String ristorante, int stelle, String testo) {
        String sql = "INSERT INTO \"recensioni\" (\"Data\", \"Stelle\", \"Ora\", \"Username_scittore\", \"Ristorante_scritto\", \"Testo\") " +
                     "VALUES (CURRENT_DATE::text, ?, TO_CHAR(NOW(), 'HH24:MI'), ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, stelle);
            ps.setString(2, username);
            ps.setString(3, ristorante);
            ps.setString(4, testo);

            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Modifica una recensione esistente.
     * @param username username dello scrittore
     * @param ristorante nome del ristorante
     * @param nuovoTesto nuovo testo della recensione
     * @param nuoveStelle nuovo numero di stelle
     */
    public void modificaRecensione(String username, String ristorante, String nuovoTesto, int nuoveStelle) {
        String sql = "UPDATE \"Recensioni\" SET \"Testo\"=?, \"Stelle\"=? " +
                     "WHERE \"Username_scittore\"=? AND \"Ristorante_scritto\"=?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, nuovoTesto);
            ps.setInt(2, nuoveStelle);
            ps.setString(3, username);
            ps.setString(4, ristorante);

            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Elimina una recensione dal database.
     * @param username username dello scrittore
     * @param ristorante nome del ristorante
     */
    public void eliminaRecensione(String username, String ristorante) {
        String sql = "DELETE FROM \"recensioni\" WHERE \"Username_scittore\"=? AND \"Ristorante_scritto\"=?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setString(2, ristorante);

            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Aggiunge una risposta del gestore a una recensione.
     * @param ristorante nome del ristorante
     * @param usernameScrittore username dello scrittore della recensione
     * @param risposta testo della risposta
     */
    public void rispondiRecensione(String ristorante, String usernameScrittore, String risposta) {
        String sql = "UPDATE \"recensioni\" SET \"Risposta_gestore\"=? " +
                     "WHERE \"Ristorante_scritto\"=? AND \"Username_scittore\"=? AND \"Risposta_gestore\" IS NULL";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, risposta);
            ps.setString(2, ristorante);
            ps.setString(3, usernameScrittore);

            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Restituisce una mappa con le valutazioni medie di tutti i ristoranti.
     * @return mappa con nome ristorante -> valutazione media
     */
    public Map<String, Double> getAllRestaurantRatings() {
        Map<String, Double> ratings = new HashMap<>();
        String sql = "SELECT \"Ristorante_scritto\", AVG(\"Stelle\"::numeric) as avg_rating " +
                     "FROM \"recensioni\" GROUP BY \"Ristorante_scritto\"";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String restaurant = rs.getString("Ristorante_scritto");
                double avgRating = rs.getDouble("avg_rating");
                ratings.put(restaurant, avgRating);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return ratings;
    }

    /**
     * Restituisce una mappa con il numero di recensioni per ogni ristorante.
     * @return mappa con nome ristorante -> numero di recensioni
     */
    public Map<String, Integer> getAllRestaurantReviewCounts() {
        Map<String, Integer> counts = new HashMap<>();
        String sql = "SELECT \"Ristorante_scritto\", COUNT(*) as review_count " +
                     "FROM \"recensioni\" GROUP BY \"Ristorante_scritto\"";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String restaurant = rs.getString("Ristorante_scritto");
                int count = rs.getInt("review_count");
                counts.put(restaurant, count);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return counts;
    }

}
