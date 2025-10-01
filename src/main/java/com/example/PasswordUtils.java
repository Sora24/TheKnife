package com.example;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Classe di utilità per la gestione sicura delle password con supporto per salt.
 * 
 * <p>Questa classe fornisce metodi per l'hashing sicuro delle password utilizzando
 * algoritmi crittografici robusti con salt per prevenire attacchi rainbow table.
 * Mantiene la compatibilità con il sistema legacy MD5 per la migrazione graduale.
 * 
 * <h3>Funzionalità Principali</h3>
 * <ul>
 *   <li><strong>Salt Generation:</strong> Generazione sicura di salt casuali</li>
 *   <li><strong>SHA-256 Hashing:</strong> Algoritmo crittografico sicuro per nuove password</li>
 *   <li><strong>Backward Compatibility:</strong> Supporto per password MD5 esistenti</li>
 *   <li><strong>Password Verification:</strong> Verifica sicura delle password hashate</li>
 * </ul>
 * 
 * <h3>Formato Password Salted</h3>
 * <p>Le password hashate con salt vengono memorizzate nel formato:
 * <pre>{@code
 * $SALT${base64-encoded-salt}${sha256-hash}
 * 
 * Esempio:
 * $SALT$dGVzdHNhbHQ=$5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8
 * }</pre>
 * 
 * <h3>Compatibilità</h3>
 * <ul>
 *   <li><strong>Password Legacy MD5:</strong> 32 caratteri esadecimali (supportate)</li>
 *   <li><strong>Password Plain Text:</strong> Testo in chiaro (supportate con upgrade automatico)</li>
 *   <li><strong>Password Salted:</strong> Formato $SALT$... (nuove registrazioni)</li>
 * </ul>
 * 
 * @author Andrea De Nisco, Antonio De Nisco
 * @version 2.1
 * @since 2.1
 * @see MessageDigest
 * @see SecureRandom
 */
public class PasswordUtils {
    
    /**
     * Costruttore privato per prevenire istanziazione.
     * Questa è una classe di utilità con solo metodi statici.
     */
    private PasswordUtils() {
        // Utility class - no instantiation
    }
    
    /** Identificatore per password hashate con salt */
    private static final String SALT_PREFIX = "$SALT$";
    
    /** Separatore tra salt e hash */
    private static final String SALT_SEPARATOR = "$";
    
    /** Lunghezza del salt in bytes */
    private static final int SALT_LENGTH = 16;
    
    /** Algoritmo di hashing utilizzato per le nuove password */
    private static final String HASH_ALGORITHM = "SHA-256";
    
    /** Algoritmo legacy per compatibilità */
    private static final String LEGACY_ALGORITHM = "MD5";
    
    /**
     * Genera un salt casuale sicuro per l'hashing delle password.
     * 
     * <p>Utilizza {@link SecureRandom} per generare un salt crittograficamente
     * sicuro di {@value #SALT_LENGTH} bytes, che viene poi codificato in Base64
     * per la memorizzazione.
     * 
     * @return stringa Base64 contenente il salt generato
     * @throws RuntimeException se si verifica un errore nella generazione del salt
     * @since 2.1
     */
    public static String generateSalt() {
        try {
            SecureRandom random = new SecureRandom();
            byte[] salt = new byte[SALT_LENGTH];
            random.nextBytes(salt);
            return Base64.getEncoder().encodeToString(salt);
        } catch (Exception e) {
            throw new RuntimeException("Errore nella generazione del salt", e);
        }
    }
    
    /**
     * Calcola l'hash di una password utilizzando salt e SHA-256.
     * 
     * <p>Questo metodo combina la password con il salt fornito e calcola
     * l'hash SHA-256 risultante. Il salt deve essere decodificabile da Base64.
     * 
     * <p><strong>Processo di hashing:</strong>
     * <ol>
     *   <li>Decodifica il salt da Base64</li>
     *   <li>Combina password + salt</li>
     *   <li>Calcola SHA-256 hash</li>
     *   <li>Converte in formato esadecimale</li>
     * </ol>
     * 
     * @param password password in chiaro da hashare, non può essere null
     * @param salt salt codificato in Base64, non può essere null
     * @return hash SHA-256 in formato esadecimale (64 caratteri)
     * @throws IllegalArgumentException se password o salt sono null o vuoti
     * @throws RuntimeException se si verifica un errore nell'hashing
     * @since 2.1
     */
    public static String hashPasswordWithSalt(String password, String salt) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Password non può essere null o vuota");
        }
        if (salt == null || salt.isEmpty()) {
            throw new IllegalArgumentException("Salt non può essere null o vuoto");
        }
        
        try {
            byte[] saltBytes = Base64.getDecoder().decode(salt);
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            
            // Combina password e salt
            digest.update(password.getBytes("UTF-8"));
            digest.update(saltBytes);
            
            byte[] hashedBytes = digest.digest();
            
            // Converte in stringa esadecimale
            StringBuilder sb = new StringBuilder();
            for (byte b : hashedBytes) {
                sb.append(String.format("%02x", b));
            }
            
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Errore nell'hashing della password con salt", e);
        }
    }
    
    /**
     * Crea una password hashata con salt nel formato completo per la memorizzazione.
     * 
     * <p>Questo metodo esegue l'intero processo di hashing sicuro:
     * <ol>
     *   <li>Genera un salt casuale</li>
     *   <li>Calcola l'hash della password con il salt</li>
     *   <li>Formatta il risultato per la memorizzazione</li>
     * </ol>
     * 
     * <p><strong>Formato di output:</strong>
     * <pre>{@code $SALT${base64-salt}${sha256-hash}}</pre>
     * 
     * @param password password in chiaro da hashare, non può essere null
     * @return stringa formattata contenente salt e hash per la memorizzazione
     * @throws IllegalArgumentException se password è null o vuota
     * @throws RuntimeException se si verifica un errore nell'hashing
     * @since 2.1
     */
    public static String createSaltedPassword(String password) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Password non può essere null o vuota");
        }
        
        String salt = generateSalt();
        String hash = hashPasswordWithSalt(password, salt);
        
        return SALT_PREFIX + salt + SALT_SEPARATOR + hash;
    }
    
    /**
     * Verifica se una password corrisponde a un hash memorizzato.
     * 
     * <p>Questo metodo supporta tre formati di password memorizzate:
     * <ul>
     *   <li><strong>Salted:</strong> {@code $SALT$...} - verifica con salt</li>
     *   <li><strong>MD5 Legacy:</strong> 32 caratteri esadecimali - verifica MD5</li>
     *   <li><strong>Plain Text:</strong> qualsiasi altro formato - confronto diretto</li>
     * </ul>
     * 
     * <p><strong>Comportamento di sicurezza:</strong>
     * <ul>
     *   <li>Utilizza confronto constant-time per prevenire timing attacks</li>
     *   <li>Mantiene compatibilità con password legacy</li>
     *   <li>Supporta migrazione automatica durante il login</li>
     * </ul>
     * 
     * @param password password in chiaro da verificare
     * @param storedPassword password memorizzata nel database (qualsiasi formato)
     * @return {@code true} se la password corrisponde, {@code false} altrimenti
     * @throws IllegalArgumentException se i parametri sono null
     * @since 2.1
     */
    public static boolean verifyPassword(String password, String storedPassword) {
        if (password == null || storedPassword == null) {
            throw new IllegalArgumentException("Password e storedPassword non possono essere null");
        }
        
        try {
            if (isSaltedPassword(storedPassword)) {
                // Password con salt - estrae salt e hash
                return verifySaltedPassword(password, storedPassword);
            } else if (isLegacyMD5(storedPassword)) {
                // Password MD5 legacy - calcola MD5 della password
                String md5Hash = hashPasswordMD5(password);
                return constantTimeEquals(md5Hash, storedPassword);
            } else {
                // Password in plain text - confronto diretto
                return constantTimeEquals(password, storedPassword);
            }
        } catch (Exception e) {
            // In caso di errore, fallisce sempre per sicurezza
            return false;
        }
    }
    
    /**
     * Verifica se una stringa rappresenta una password hashata con salt.
     * 
     * @param password stringa da verificare
     * @return {@code true} se è una password salted, {@code false} altrimenti
     * @since 2.1
     */
    public static boolean isSaltedPassword(String password) {
        return password != null && password.startsWith(SALT_PREFIX) && 
               password.split("\\$").length == 4; // $SALT$ + salt + $ + hash
    }
    
    /**
     * Verifica se una stringa rappresenta un hash MD5 legacy.
     * 
     * @param password stringa da verificare
     * @return {@code true} se è un hash MD5 (32 caratteri esadecimali), {@code false} altrimenti
     * @since 2.1
     */
    public static boolean isLegacyMD5(String password) {
        return password != null && password.length() == 32 && 
               password.matches("[a-fA-F0-9]{32}");
    }
    
    /**
     * Estrae il salt da una password hashata con salt.
     * 
     * @param saltedPassword password nel formato $SALT$salt$hash
     * @return salt codificato in Base64
     * @throws IllegalArgumentException se il formato non è valido
     * @since 2.1
     */
    public static String extractSalt(String saltedPassword) {
        if (!isSaltedPassword(saltedPassword)) {
            throw new IllegalArgumentException("Password non è nel formato salted valido");
        }
        
        String[] parts = saltedPassword.split("\\$");
        return parts[2]; // $SALT$ + salt + $ + hash
    }
    
    /**
     * Verifica una password contro un hash salted memorizzato.
     * 
     * @param password password in chiaro
     * @param saltedPassword password salted memorizzata
     * @return true se corrispondono
     * @since 2.1
     */
    private static boolean verifySaltedPassword(String password, String saltedPassword) {
        try {
            String salt = extractSalt(saltedPassword);
            String calculatedHash = hashPasswordWithSalt(password, salt);
            String expectedHash = saltedPassword.substring(saltedPassword.lastIndexOf('$') + 1);
            
            return constantTimeEquals(calculatedHash, expectedHash);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Calcola l'hash MD5 di una password per compatibilità legacy.
     * 
     * @param password password da hashare
     * @return hash MD5 in formato esadecimale
     * @since 2.1
     */
    private static String hashPasswordMD5(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance(LEGACY_ALGORITHM);
            byte[] hashedBytes = md.digest(password.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashedBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Errore nell'hashing MD5", e);
        }
    }
    
    /**
     * Confronto constant-time per prevenire timing attacks.
     * 
     * @param a prima stringa
     * @param b seconda stringa
     * @return true se le stringhe sono uguali
     * @since 2.1
     */
    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        
        if (a.length() != b.length()) {
            return false;
        }
        
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        
        return result == 0;
    }
}