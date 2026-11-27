package com.reservation.test;

import com.reservation.client.ReservationClient;
import com.reservation.dao.DatabaseConnection;
import com.reservation.model.DashboardStats;
import com.reservation.model.Reservation;
import com.reservation.model.Salle;
import com.reservation.model.Utilisateur;
import com.reservation.network.Request;
import com.reservation.network.RequestType;
import com.reservation.network.Response;
import com.reservation.server.ReservationServer;
import com.reservation.util.FileManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.Date;
import java.sql.Statement;
import java.sql.Time;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Suite de tests automatisés couvrant les 6 scénarios de validation du devoir.
 */
public class ValidationTestSuite {
    private static final int TEST_PORT = 5000;

    public static void main(String[] args) throws Exception {
        System.out.println("=== DÉMARRAGE DES TESTS DE VALIDATION ===");

        ReservationServer server = new ReservationServer(TEST_PORT);
        server.startInBackground();
        Thread.sleep(300);

        try {
            test1AuthAndCrud();
            test2ThreeSimultaneousClients(server);
            test3ConcurrentOverlappingReservations();
            test4FileGenerationCsvAndBinary();
            test5ClientConnectionLoss(server);
            exportDatabaseSnapshot();
            System.out.println("\n=== TOUS LES TESTS DE VALIDATION ONT RÉUSSI ===");
        } finally {
            server.stop();
        }
    }

    private static void test1AuthAndCrud() throws Exception {
        System.out.println("\n[TEST 1] Authentification et opérations CRUD...");
        ReservationClient client = new ReservationClient("localhost", TEST_PORT);
        client.connect();

        // Échec auth
        Response badLogin = client.sendRequest(new Request(RequestType.LOGIN, new String[]{"r.elwahidi8548@uca.ac.ma", "mauvais"}, "r.elwahidi8548@uca.ac.ma"));
        assertTrue(!badLogin.isSuccess(), "Un mauvais mot de passe doit échouer.");

        // Succès auth
        Response okLogin = client.sendRequest(new Request(RequestType.LOGIN, new String[]{"r.elwahidi8548@uca.ac.ma", "admin123"}, "r.elwahidi8548@uca.ac.ma"));
        assertTrue(okLogin.isSuccess() && okLogin.getData() instanceof Utilisateur, "Authentification admin réussie.");
        Utilisateur admin = (Utilisateur) okLogin.getData();
        client.setUserEmail(admin.getEmail());

        // Consultation salles
        Response sallesResp = client.sendRequest(new Request(RequestType.GET_SALLES, null));
        assertTrue(sallesResp.isSuccess(), "Consultation des salles réussie.");
        @SuppressWarnings("unchecked")
        List<Salle> salles = (List<Salle>) sallesResp.getData();
        assertTrue(!salles.isEmpty(), "La liste des salles ne doit pas être vide.");

        // Ajout réservation
        Reservation r = new Reservation(0, admin.getId(), salles.get(0).getId(),
                Date.valueOf("2026-12-15"), Time.valueOf("08:00:00"), Time.valueOf("10:00:00"),
                "Test CRUD Soutenance", "CONFIRMEE");
        Response addResp = client.sendRequest(new Request(RequestType.ADD_RESERVATION, r));
        assertTrue(addResp.isSuccess(), "Ajout réservation réussi : " + addResp.getMessage());
        Reservation created = (Reservation) addResp.getData();

        // Modification réservation
        created.setMotif("Test CRUD Soutenance Modifié");
        Response updResp = client.sendRequest(new Request(RequestType.UPDATE_RESERVATION, created));
        assertTrue(updResp.isSuccess(), "Modification réservation réussie.");

        // Recherche réservation
        Response searchResp = client.sendRequest(new Request(RequestType.SEARCH_RESERVATIONS, "Modifié"));
        @SuppressWarnings("unchecked")
        List<Reservation> found = (List<Reservation>) searchResp.getData();
        assertTrue(found.stream().anyMatch(item -> item.getId() == created.getId()), "Recherche réservation trouvée.");

        // Annulation réservation
        Response cancelResp = client.sendRequest(new Request(RequestType.CANCEL_RESERVATION, created.getId()));
        assertTrue(cancelResp.isSuccess(), "Annulation réservation réussie.");

        // Suppression réservation de test
        Response delResp = client.sendRequest(new Request(RequestType.DELETE_RESERVATION, created.getId()));
        assertTrue(delResp.isSuccess(), "Suppression réservation réussie.");

        // Statistiques
        Response statsResp = client.sendRequest(new Request(RequestType.GET_STATS, null));
        assertTrue(statsResp.isSuccess() && statsResp.getData() instanceof DashboardStats, "Statistiques récupérées.");

        client.disconnect();
        System.out.println("[TEST 1] OK");
    }

    private static void test2ThreeSimultaneousClients(ReservationServer server) throws Exception {
        System.out.println("\n[TEST 2] Connexion simultanée de 3 clients...");
        ReservationClient c1 = new ReservationClient("localhost", TEST_PORT);
        ReservationClient c2 = new ReservationClient("localhost", TEST_PORT);
        ReservationClient c3 = new ReservationClient("localhost", TEST_PORT);

        c1.connect();
        c2.connect();
        c3.connect();
        Thread.sleep(200);

        int active = server.getActiveClientsCount();
        System.out.println("  Clients actifs simultanés sur le serveur : " + active);
        assertTrue(active >= 3, "Au moins 3 clients doivent être connectés simultanément.");

        Response r1 = c1.sendRequest(new Request(RequestType.LOGIN, new String[]{"imane@ecole.ma", "pass123"}, "imane@ecole.ma"));
        Response r2 = c2.sendRequest(new Request(RequestType.LOGIN, new String[]{"salma@ecole.ma", "pass123"}, "salma@ecole.ma"));
        Response r3 = c3.sendRequest(new Request(RequestType.LOGIN, new String[]{"youssef@ecole.ma", "pass123"}, "youssef@ecole.ma"));
        assertTrue(r1.isSuccess() && r2.isSuccess() && r3.isSuccess(), "Les 3 clients ont été authentifiés simultanément.");

        c1.disconnect();
        c2.disconnect();
        c3.disconnect();
        System.out.println("[TEST 2] OK");
    }

    private static void test3ConcurrentOverlappingReservations() throws Exception {
        System.out.println("\n[TEST 3] Test de réservations concurrentes sur créneaux qui se chevauchent...");
        ReservationClient c1 = new ReservationClient("localhost", TEST_PORT);
        ReservationClient c2 = new ReservationClient("localhost", TEST_PORT);
        c1.connect();
        c2.connect();

        Utilisateur u1 = (Utilisateur) c1.sendRequest(new Request(RequestType.LOGIN, new String[]{"imane@ecole.ma", "pass123"}, "imane@ecole.ma")).getData();
        Utilisateur u2 = (Utilisateur) c2.sendRequest(new Request(RequestType.LOGIN, new String[]{"salma@ecole.ma", "pass123"}, "salma@ecole.ma")).getData();
        c1.setUserEmail(u1.getEmail());
        c2.setUserEmail(u2.getEmail());

        Date dateTest = Date.valueOf("2026-12-20");
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger rejectedCount = new AtomicInteger(0);
        AtomicInteger createdId = new AtomicInteger(-1);

        Runnable task1 = () -> {
            try {
                ready.countDown();
                start.await();
                Reservation res = new Reservation(0, u1.getId(), 1, dateTest, Time.valueOf("10:00:00"), Time.valueOf("12:00:00"), "Concurrence Client 1", "CONFIRMEE");
                Response resp = c1.sendRequest(new Request(RequestType.ADD_RESERVATION, res));
                System.out.println("  Client 1 -> " + resp.isSuccess() + " : " + resp.getMessage());
                if (resp.isSuccess()) {
                    successCount.incrementAndGet();
                    createdId.set(((Reservation) resp.getData()).getId());
                } else {
                    rejectedCount.incrementAndGet();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        };

        Runnable task2 = () -> {
            try {
                ready.countDown();
                start.await();
                Reservation res = new Reservation(0, u2.getId(), 1, dateTest, Time.valueOf("11:00:00"), Time.valueOf("13:00:00"), "Concurrence Client 2", "CONFIRMEE");
                Response resp = c2.sendRequest(new Request(RequestType.ADD_RESERVATION, res));
                System.out.println("  Client 2 -> " + resp.isSuccess() + " : " + resp.getMessage());
                if (resp.isSuccess()) {
                    successCount.incrementAndGet();
                    createdId.set(((Reservation) resp.getData()).getId());
                } else {
                    rejectedCount.incrementAndGet();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        };

        Thread t1 = new Thread(task1);
        Thread t2 = new Thread(task2);
        t1.start();
        t2.start();
        ready.await();
        start.countDown();
        t1.join();
        t2.join();

        assertTrue(successCount.get() == 1 && rejectedCount.get() == 1,
                "Exactement 1 client doit réussir et 1 doit être refusé (succès=" + successCount.get() + ", refus=" + rejectedCount.get() + ")");

        if (createdId.get() > 0) {
            c1.sendRequest(new Request(RequestType.DELETE_RESERVATION, createdId.get()));
        }

        c1.disconnect();
        c2.disconnect();
        System.out.println("[TEST 3] OK");
    }

    private static void test4FileGenerationCsvAndBinary() throws Exception {
        System.out.println("\n[TEST 4] Vérification logs.txt, export CSV et sérialisation binaire...");
        ReservationClient client = new ReservationClient("localhost", TEST_PORT);
        client.connect();
        client.sendRequest(new Request(RequestType.LOGIN, new String[]{"r.elwahidi8548@uca.ac.ma", "admin123"}, "r.elwahidi8548@uca.ac.ma"));
        @SuppressWarnings("unchecked")
        List<Reservation> reservations = (List<Reservation>) client.sendRequest(new Request(RequestType.GET_RESERVATIONS, null)).getData();
        client.disconnect();

        File logFile = new File(FileManager.LOG_FILE);
        assertTrue(logFile.exists() && logFile.length() > 0, "logs.txt doit exister et contenir des entrées.");

        File csvFile = new File("data/reservations_export.csv");
        FileManager.exportReservationsToCSV(reservations, csvFile);
        assertTrue(csvFile.exists() && Files.readAllLines(csvFile.toPath()).size() >= reservations.size() + 1,
                "Export CSV valide.");

        File binFile = new File("data/reservations.ser");
        FileManager.saveReservationsBinary(reservations, binFile);
        List<Reservation> loaded = FileManager.loadReservationsBinary(binFile);
        assertTrue(loaded.size() == reservations.size(), "Lecture binaire fidèle (" + loaded.size() + " éléments).");

        System.out.println("[TEST 4] OK");
    }

    private static void test5ClientConnectionLoss(ReservationServer server) throws Exception {
        System.out.println("\n[TEST 5] Comportement du client en cas de perte de connexion serveur...");
        ReservationClient client = new ReservationClient("localhost", TEST_PORT);
        client.connect();
        assertTrue(client.isConnected(), "Client initialement connecté.");

        server.stop();
        Thread.sleep(200);

        boolean ioCaught = false;
        try {
            client.sendRequest(new Request(RequestType.GET_SALLES, null));
        } catch (IOException e) {
            ioCaught = true;
            System.out.println("  Exception interceptée proprement : " + e.getMessage());
        }
        assertTrue(ioCaught && !client.isConnected(), "Le client doit détecter la perte de connexion proprement.");
        System.out.println("[TEST 5] OK");
    }

    private static void exportDatabaseSnapshot() {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement st = conn.createStatement()) {
            if (DatabaseConnection.getActiveMode().startsWith("H2")) {
                st.execute("SCRIPT TO 'database/ReservationDB_h2_export.sql'");
                System.out.println("[EXPORT DB] Snapshot H2 exporté vers database/ReservationDB_h2_export.sql");
            }
        } catch (Exception e) {
            System.err.println("[EXPORT DB] Note : " + e.getMessage());
        }
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError("ÉCHEC ASSERTION : " + message);
        }
    }
}
