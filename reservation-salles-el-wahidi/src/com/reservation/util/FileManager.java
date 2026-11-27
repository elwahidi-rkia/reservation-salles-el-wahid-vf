package com.reservation.util;

import com.reservation.model.Reservation;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Gestionnaire des fichiers :
 * 1. Journalisation dans logs.txt
 * 2. Exportation CSV des réservations
 * 3. Sauvegarde et lecture binaire par sérialisation Java
 */
public class FileManager {
    public static final String LOG_FILE = "logs.txt";
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Object LOG_LOCK = new Object();

    private FileManager() {}

    /**
     * Enregistre une ligne de log dans logs.txt : date | utilisateur | opération | résultat.
     */
    public static void log(String utilisateur, String operation, String resultat) {
        String user = (utilisateur == null || utilisateur.isBlank()) ? "ANONYME" : utilisateur;
        String line = String.format("%s | %s | %s | %s",
                LocalDateTime.now().format(FMT), user, operation, resultat);
        synchronized (LOG_LOCK) {
            try (PrintWriter out = new PrintWriter(
                    new BufferedWriter(new OutputStreamWriter(new FileOutputStream(LOG_FILE, true), StandardCharsets.UTF_8)))) {
                out.println(line);
            } catch (IOException e) {
                System.err.println("[LOG] Erreur d'écriture dans " + LOG_FILE + " : " + e.getMessage());
            }
        }
    }

    /**
     * Exporte la liste des réservations dans un fichier CSV UTF-8.
     */
    public static void exportReservationsToCSV(List<Reservation> reservations, File file) throws IOException {
        try (PrintWriter pw = new PrintWriter(
                new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)))) {
            pw.println("ID,IdUtilisateur,Utilisateur,IdSalle,Salle,Date,HeureDebut,HeureFin,Motif,Statut");
            for (Reservation r : reservations) {
                pw.printf("%d,%d,%s,%d,%s,%s,%s,%s,%s,%s%n",
                        r.getId(),
                        r.getIdUtilisateur(),
                        escapeCsv(r.getNomUtilisateur()),
                        r.getIdSalle(),
                        escapeCsv(r.getNomSalle()),
                        r.getDate(),
                        r.getHeureDebut(),
                        r.getHeureFin(),
                        escapeCsv(r.getMotif()),
                        escapeCsv(r.getStatut()));
            }
        }
    }

    /**
     * Sauvegarde la liste des réservations dans un fichier binaire via ObjectOutputStream.
     */
    public static void saveReservationsBinary(List<Reservation> reservations, File file) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
            oos.writeObject(new ArrayList<>(reservations));
        }
    }

    /**
     * Lit la liste des réservations depuis un fichier binaire via ObjectInputStream.
     */
    @SuppressWarnings("unchecked")
    public static List<Reservation> loadReservationsBinary(File file) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            Object obj = ois.readObject();
            if (obj instanceof List<?>) {
                return (List<Reservation>) obj;
            }
            throw new IOException("Format de fichier binaire invalide.");
        }
    }

    private static String escapeCsv(String value) {
        if (value == null) return "";
        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }
}
