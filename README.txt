================================================================================
                            THE KNIFE APPLICATION
                         Guida all'Installazione e Utilizzo
================================================================================

DESCRIZIONE
-----------
TheKnife è un'applicazione client-server per la gestione e recensione di 
ristoranti, sviluppata in Java con interfaccia JavaFX. L'applicazione 
implementa un'architettura distribuita con separazione netta tra client 
(interfaccia utente) e server (logica di business e accesso dati).

AUTORI
------
- Andrea De Nisco (Matricola: 752452 CO)
- Antonio De Nisco (Matricola: 752445 CO)

VERSIONE
--------
1.0 - CLIENT

================================================================================
                              REQUISITI DI SISTEMA
================================================================================

REQUISITI MINIMI
----------------
- Java Development Kit (JDK) 21 o superiore
- Apache Maven 3.6.0 o superiore
- PostgreSQL 12 o superiore (per il database)
- Sistema operativo: Windows 10/11, macOS 10.14+, Linux (Ubuntu 18.04+)
- RAM: minimo 2GB, raccomandati 4GB
- Spazio disco: 500MB per il progetto + dipendenze

VERIFICA INSTALLAZIONE JAVA
---------------------------
Per verificare la versione di Java installata:
    java -version
    javac -version

Entrambi i comandi devono restituire Java 21 o superiore.

VERIFICA INSTALLAZIONE MAVEN
-----------------------------
Per verificare Maven:
    mvn -version

Deve restituire Maven 3.6.0 o superiore.

================================================================================
                              CONFIGURAZIONE DATABASE
================================================================================

INSTALLAZIONE POSTGRESQL
-------------------------
1. Scaricare e installare PostgreSQL da: https://www.postgresql.org/download/
2. Durante l'installazione, annotare username, password e porta (default 6028)
3. Avviare il servizio PostgreSQL

CONFIGURAZIONE DATABASE
-----------------------
1. Creare un database per l'applicazione:
    createdb TheKnife

2. Configurare le credenziali nel file:
    src/main/resources/restaurant_owners.properties

Esempio di configurazione:
    db.url=jdbc:postgresql://localhost:6028/TheKnife
    db.username=postgres
    db.password=andrea
    db.driver=org.postgresql.Driver

================================================================================
                              LIBRERIE E DIPENDENZE
================================================================================

LIBRERIE PRINCIPALI
-------------------
Il progetto utilizza le seguenti librerie con configurazioni specifiche:

1. JAVAFX 24.0.2
   - Utilizzata per l'interfaccia utente grafica
   - Include moduli: base, controls, fxml, graphics
   - Configurazione speciale: librerie platform-specific per Windows
   - File: javafx-*-24.0.2.jar e javafx-*-24.0.2-win.jar

2. POSTGRESQL JDBC DRIVER 42.7.8
   - Driver per connessione al database PostgreSQL
   - Ultima versione stabile con supporto per PostgreSQL 16
   - File: postgresql-42.7.8.jar

3. CHECKER FRAMEWORK 3.51.0
   - Framework per analisi statica del codice
   - Utilizzato per annotazioni di nullability e type checking
   - File: checker-qual-3.51.0.jar

4. SLF4J 2.0.16 + LOGBACK 1.5.12
   - Sistema di logging professionale
   - SLF4J come facade, Logback come implementazione
   - File: slf4j-api-2.0.16.jar, logback-classic-1.5.12.jar, logback-core-1.5.12.jar

CONFIGURAZIONI SPECIALI
-----------------------

JAVAFX MODULE PATH:
Il progetto utilizza il sistema dei moduli Java (Project Jigsaw). Per JavaFX
è necessario configurare il module path correttamente:

    --module-path "lib/javafx-base-24.0.2.jar;lib/javafx-controls-24.0.2.jar;lib/javafx-fxml-24.0.2.jar;lib/javafx-graphics-24.0.2.jar"
    --add-modules javafx.controls,javafx.fxml

LIBRERIE PLATFORM-SPECIFIC:
JavaFX richiede librerie native specifiche per il sistema operativo.
Il progetto include librerie per Windows (-win.jar). Per altri sistemi:
- macOS: scaricare javafx-*-mac.jar
- Linux: scaricare javafx-*-linux.jar

================================================================================
                              COMPILAZIONE
================================================================================

PULIZIA E COMPILAZIONE COMPLETA
-------------------------------
Per compilare il progetto da zero:

    mvn clean compile

Questo comando:
- Pulisce tutti i file compilati precedenti (clean)
- Ricompila tutto il codice sorgente (compile)
- Scarica automaticamente le dipendenze mancanti
- Applica configurazioni avanzate del compilatore con -Xlint:all

COMPILAZIONE CON VERIFICA QUALITÀ
---------------------------------
Il progetto è configurato con controlli di qualità avanzati:

    mvn clean compile -X

Il flag -X abilita output dettagliato per diagnosticare eventuali problemi.

CONFIGURAZIONE MAVEN SPECIALE
-----------------------------
Il file pom.xml include configurazioni avanzate:

1. MAVEN COMPILER PLUGIN:
   - Target Java 21
   - Encoding UTF-8
   - Warnings dettagliati (-Xlint:all)
   - Deprecation warnings abilitati

2. MAVEN DEPENDENCY PLUGIN:
   - Copia automatica delle dipendenze in lib/
   - Gestione delle librerie platform-specific

3. MAVEN SHADE PLUGIN:
   - Creazione di JAR eseguibili completi
   - Manifest con Main-Class configurato

================================================================================
                              CREAZIONE ESEGUIBILI
================================================================================

CREAZIONE JAR ESEGUIBILI
-----------------------
Per creare i file JAR eseguibili:

    mvn clean package

Questo comando genera:
- target/TheKnife-client-1.0.jar (versione base)
- target/TheKnife-client-1.0-shaded.jar (con tutte le dipendenze)

COPIA IN DIRECTORY BIN
---------------------
Per copiare i JAR nella directory bin/:

    mvn dependency:copy-dependencies -DoutputDirectory=lib
    cp target/*.jar bin/

VERIFICA ESEGUIBILI
------------------
Per verificare che i JAR siano stati creati correttamente:

Windows:
    dir bin\*.jar

Linux/macOS:
    ls -la bin/*.jar

================================================================================
                              ESECUZIONE
================================================================================

ESECUZIONE CLIENT
-----------------
Per avviare l'applicazione client:

Da directory bin/:
    java -jar TheKnife-client.jar

Con module path esplicito (se necessario):
    java --module-path "../lib" --add-modules javafx.controls,javafx.fxml -jar TheKnife-client.jar

ESECUZIONE SERVER
-----------------
Per avviare il server (se presente):
    java -jar TheKnife-server.jar

MODALITÀ DEBUG
--------------
Per eseguire con output di debug:
    java -Djava.util.logging.level=ALL -jar TheKnife-client.jar

RISOLUZIONE PROBLEMI JAVAFX
---------------------------
Se si verificano errori JavaFX, provare:

1. Verificare il module path:
    java --list-modules | grep javafx

2. Eseguire con parametri espliciti:
    java --module-path "lib" --add-modules javafx.controls,javafx.fxml,javafx.base,javafx.graphics -cp "lib/*" com.example.ClientApp

3. Per sistemi senza JavaFX preinstallato:
    java --module-path "lib/javafx-controls-24.0.2.jar;lib/javafx-fxml-24.0.2.jar;lib/javafx-base-24.0.2.jar;lib/javafx-graphics-24.0.2.jar" --add-modules javafx.controls,javafx.fxml -jar TheKnife-client.jar

================================================================================
                              STRUTTURA PROGETTO
================================================================================

DIRECTORY PRINCIPALI
--------------------
bin/                    - Eseguibili JAR
├── TheKnife-client.jar    - Applicazione client
└── TheKnife-server.jar    - Applicazione server

doc/                    - Documentazione
├── Manuale_Utente_TheKnife.html  - Manuale utente
└── File utilizzati per il progetto/  - Risorse del progetto

lib/                    - Librerie esterne
├── javafx-*.jar           - Librerie JavaFX
├── postgresql-*.jar       - Driver PostgreSQL
├── checker-qual-*.jar     - Checker Framework
├── slf4j-*.jar           - Logging SLF4J
└── logback-*.jar         - Logging Logback

src/main/java/          - Codice sorgente Java
├── module-info.java       - Definizione modulo Java
└── com/example/          - Package principale
    ├── ClientApp.java       - Applicazione client principale
    ├── ClientLauncher.java  - Launcher utility
    ├── LoginForm.java       - Form di login
    ├── RegisterForm.java    - Form di registrazione
    ├── GuestPage.java       - Pagina ospite
    ├── NavigationManager.java - Gestione navigazione
    ├── ServerService.java   - Servizi server
    ├── DBConnection.java    - Connessione database
    ├── PasswordUtils.java   - Utility password
    ├── Ristorante.java      - Modello ristorante
    ├── RistoranteDAO.java   - DAO ristorante
    ├── Recensione.java      - Modello recensione
    └── RecensioneDAO.java   - DAO recensione

src/main/resources/     - Risorse dell'applicazione
├── restaurant_owners.properties  - Configurazione database
└── com/example/          - Risorse JavaFX
    ├── primary.fxml         - Layout principale
    └── secondary.fxml       - Layout secondario

target/                 - File compilati (generati)
├── classes/               - Class files compilati
└── generated-sources/     - Sorgenti generati

================================================================================
                              RISOLUZIONE PROBLEMI
================================================================================

PROBLEMI COMUNI E SOLUZIONI
---------------------------

1. ERRORE: "Module javafx.controls not found"
   SOLUZIONE: Verificare che le librerie JavaFX siano in lib/ e configurare
   il module path correttamente.

2. ERRORE: "Could not create connection to database server"
   SOLUZIONE: 
   - Verificare che PostgreSQL sia avviato
   - Controllare le credenziali in restaurant_owners.properties
   - Verificare che il database esista

3. ERRORE: "Unsupported major.minor version"
   SOLUZIONE: Verificare di utilizzare Java 21 o superiore

4. ERRORE: "No main manifest attribute"
   SOLUZIONE: Ricompilare con mvn clean package per ricreare il manifest

5. ERRORE: "ClassNotFoundException"
   SOLUZIONE: Verificare che tutte le dipendenze siano in lib/

CONTROLLO DIPENDENZE
--------------------
Per verificare tutte le dipendenze Maven:
    mvn dependency:tree

Per verificare dipendenze mancanti:
    mvn dependency:analyze

PULIZIA COMPLETA
----------------
Per pulire completamente il progetto:
    mvn clean
    rmdir /s target (Windows)
    rm -rf target (Linux/macOS)

COMPILAZIONE VERBOSA
-------------------
Per output dettagliato durante la compilazione:
    mvn clean compile -X -e

================================================================================
                              NOTE TECNICHE
================================================================================

ARCHITETTURA
------------
L'applicazione implementa un'architettura client-server con:
- Client: Interfaccia utente JavaFX (questo progetto)
- Server: Logica di business e accesso dati
- Database: PostgreSQL per persistenza dati

PATTERN UTILIZZATI
-----------------
- MVC (Model-View-Controller)
- DAO (Data Access Object)
- Singleton (NavigationManager)
- Utility classes (PasswordUtils)

SICUREZZA
---------
- Password crittografate con salt
- Separazione client/server per sicurezza
- Validazione input utente
- Gestione sicura delle connessioni database

MODULI JAVA
-----------
Il progetto utilizza il sistema dei moduli Java 9+ con:
- module-info.java per definizione del modulo
- Exports e opens appropriati
- Dipendenze dei moduli esplicite

================================================================================
                              CONTATTI E SUPPORTO
================================================================================

Per problemi tecnici o domande:
- Andrea De Nisco: [adenisco@studenti.uninsubria.it]
- Antonio De Nisco: [adenisco1@studenti.uninsubria.it]

Repository: [https://github.com/Sora24/TheKnife]
Documentazione: doc/Manuale_Utente_TheKnife.html

Versione README: 1.0
Data ultimo aggiornamento: Ottobre 2025

================================================================================
                                    FINE
================================================================================