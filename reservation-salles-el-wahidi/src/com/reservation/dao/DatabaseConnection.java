package com.reservation.dao;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 * Gestionnaire de connexion JDBC vers ReservationDB.
 * Supporte MySQL 8.0 et H2 (en mode compatibilité MySQL) via db.properties.
 */
public class DatabaseConnection {
    private static final String DEFAULT_MYSQL_URL = "jdbc:mysql://localhost:3306/ReservationDB?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String DEFAULT_H2_URL = "jdbc:h2:file:./database/ReservationDB_h2;MODE=MySQL;DATABASE_TO_LOWER=TRUE;AUTO_SERVER=TRUE";

    private static String activeMode;
    private static String activeUrl;
    private static String activeUser;
    private static String activePassword;
    private static boolean initialized = false;

    private DatabaseConnection() {}

    public static synchronized Connection getConnection() throws SQLException {
        if (!initialized) {
            resolveConfiguration();
            initializeSchemaIfNeeded();
            initialized = true;
        }
        return DriverManager.getConnection(activeUrl, activeUser, activePassword);
    }

    public static String getActiveMode() {
        return activeMode == null ? "NON_INITIALISE" : activeMode;
    }

    private static void resolveConfiguration() throws SQLException {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream("db.properties")) {
            props.load(fis);
        } catch (IOException ignored) {
            // Utilise les valeurs par défaut si db.properties est absent
        }

        String mode = props.getProperty("db.mode", "auto").trim().toLowerCase();
        String mysqlUrl = props.getProperty("mysql.url", DEFAULT_MYSQL_URL);
        String mysqlUser = props.getProperty("mysql.user", "root");
        String mysqlPass = props.getProperty("mysql.password", "");

        String h2Url = props.getProperty("h2.url", DEFAULT_H2_URL);
        String h2User = props.getProperty("h2.user", "sa");
        String h2Pass = props.getProperty("h2.password", "");

        if ("mysql".equals(mode) || "auto".equals(mode)) {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
                try (Connection testConn = DriverManager.getConnection(mysqlUrl, mysqlUser, mysqlPass)) {
                    if (testConn.isValid(2)) {
                        activeMode = "MySQL";
                        activeUrl = mysqlUrl;
                        activeUser = mysqlUser;
                        activePassword = mysqlPass;
                        System.out.println("[DB] Connecté à MySQL : " + mysqlUrl);
                        return;
                    }
                }
            } catch (Exception e) {
                if ("mysql".equals(mode)) {
                    throw new SQLException("Impossible de se connecter à MySQL : " + e.getMessage(), e);
                }
                System.out.println("[DB] MySQL indisponible (" + e.getMessage() + "), bascule sur H2 (MODE=MySQL).");
            }
        }

        try {
            Class.forName("org.h2.Driver");
            activeMode = "H2 (MODE=MySQL)";
            activeUrl = h2Url;
            activeUser = h2User;
            activePassword = h2Pass;
            System.out.println("[DB] Connecté à H2 : " + h2Url);
        } catch (ClassNotFoundException e) {
            throw new SQLException("Pilote JDBC introuvable (MySQL / H2).", e);
        }
    }

    private static void initializeSchemaIfNeeded() throws SQLException {
        try (Connection conn = DriverManager.getConnection(activeUrl, activeUser, activePassword);
             Statement st = conn.createStatement()) {

            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS Utilisateur (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "nom VARCHAR(100) NOT NULL, " +
                "email VARCHAR(150) NOT NULL UNIQUE, " +
                "motDePasse VARCHAR(255) NOT NULL, " +
                "role VARCHAR(30) NOT NULL DEFAULT 'USER')"
            );

            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS Salle (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "nom VARCHAR(100) NOT NULL UNIQUE, " +
                "type VARCHAR(50) NOT NULL, " +
                "capacite INT NOT NULL, " +
                "disponible BOOLEAN NOT NULL DEFAULT TRUE)"
            );

            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS Reservation (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "idUtilisateur INT NOT NULL, " +
                "idSalle INT NOT NULL, " +
                "date DATE NOT NULL, " +
                "heureDebut TIME NOT NULL, " +
                "heureFin TIME NOT NULL, " +
                "motif VARCHAR(255) NOT NULL, " +
                "statut VARCHAR(30) NOT NULL DEFAULT 'CONFIRMEE', " +
                "CONSTRAINT fk_reservation_utilisateur FOREIGN KEY (idUtilisateur) REFERENCES Utilisateur(id) ON DELETE CASCADE ON UPDATE CASCADE, " +
                "CONSTRAINT fk_reservation_salle FOREIGN KEY (idSalle) REFERENCES Salle(id) ON DELETE CASCADE ON UPDATE CASCADE)"
            );

            try (ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM Utilisateur")) {
                if (rs.next() && rs.getInt(1) == 0) {
                    st.executeUpdate(
                        "INSERT INTO Utilisateur (nom, email, motDePasse, role) VALUES " +
                        "('EL wahidi rkia', 'r.elwahidi8548@uca.ac.ma', 'admin123', 'ADMIN'), " +
                        "('Imane Alaoui', 'imane@ecole.ma', 'pass123', 'USER'), " +
                        "('Salma Bennani', 'salma@ecole.ma', 'pass123', 'USER'), " +
                        "('Youssef Amrani', 'youssef@ecole.ma', 'pass123', 'USER')"
                    );
                    st.executeUpdate(
                        "INSERT INTO Salle (nom, type, capacite, disponible) VALUES " +
                        "('Amphi A', 'Amphithéâtre', 180, TRUE), " +
                        "('Salle Info 1', 'Laboratoire Informatique', 35, TRUE), " +
                        "('Salle Réunion B', 'Salle de Réunion', 16, TRUE), " +
                        "('Labo Réseaux', 'Laboratoire Technique', 24, TRUE), " +
                        "('Salle Conférence C', 'Conférence', 60, FALSE)"
                    );
                    st.executeUpdate(
                        "INSERT INTO Reservation (idUtilisateur, idSalle, date, heureDebut, heureFin, motif, statut) VALUES " +
                        "(2, 1, '2026-12-01', '09:00:00', '11:00:00', 'Cours Java Avancé', 'CONFIRMEE'), " +
                        "(3, 2, '2026-12-01', '14:00:00', '16:00:00', 'TP Réseaux & Sockets', 'CONFIRMEE'), " +
                        "(4, 3, '2026-12-02', '10:00:00', '12:00:00', 'Réunion Projet PFE', 'CONFIRMEE')"
                    );
                    System.out.println("[DB] Données initiales insérées dans ReservationDB.");
                }
            }
        }
    }
}
