package com.reservation.server;

import com.reservation.dao.ReservationDAO;
import com.reservation.dao.SalleDAO;
import com.reservation.dao.UtilisateurDAO;
import com.reservation.model.Reservation;
import com.reservation.model.Salle;
import com.reservation.model.Utilisateur;
import com.reservation.network.Request;
import com.reservation.network.Response;
import com.reservation.util.FileManager;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestionnaire d'un client connecté dans un thread dédié.
 * Synchronise les opérations de réservation critiques pour éviter tout conflit concurrent.
 */
public class ClientHandler implements Runnable {
    private static final ConcurrentHashMap<String, Object> SLOT_LOCKS = new ConcurrentHashMap<>();

    private final Socket socket;
    private final ReservationServer server;
    private final UtilisateurDAO utilisateurDAO = new UtilisateurDAO();
    private final SalleDAO salleDAO = new SalleDAO();
    private final ReservationDAO reservationDAO = new ReservationDAO();

    private Utilisateur currentUser;

    public ClientHandler(Socket socket, ReservationServer server) {
        this.socket = socket;
        this.server = server;
    }

    @Override
    public void run() {
        String clientAddr = socket.getRemoteSocketAddress().toString();
        server.onClientConnected(clientAddr);
        try (ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())) {
            oos.flush();

            while (!socket.isClosed()) {
                Object obj = ois.readObject();
                if (!(obj instanceof Request)) {
                    oos.writeObject(Response.error("Requête invalide."));
                    oos.flush();
                    continue;
                }

                Request req = (Request) obj;
                Response resp = handleRequest(req);
                oos.writeObject(resp);
                oos.flush();

                if (req.getType() == com.reservation.network.RequestType.LOGOUT) {
                    break;
                }
            }
        } catch (EOFException e) {
            // Déconnexion normale du client
        } catch (IOException | ClassNotFoundException e) {
            FileManager.log(getActorName(null), "CONNEXION_CLIENT", "Déconnexion (" + clientAddr + "): " + e.getMessage());
        } finally {
            server.onClientDisconnected(clientAddr, socket);
            try {
                socket.close();
            } catch (IOException ignored) {}
        }
    }

    private Response handleRequest(Request req) {
        String actor = getActorName(req);
        try {
            switch (req.getType()) {
                case LOGIN:
                    return handleLogin(req);
                case LOGOUT:
                    FileManager.log(actor, "LOGOUT", "SUCCÈS");
                    return Response.ok("Déconnexion réussie.", null);
                case GET_SALLES:
                    List<Salle> salles = salleDAO.findAll();
                    FileManager.log(actor, "CONSULTATION_SALLES", "SUCCÈS (" + salles.size() + " salles)");
                    return Response.ok("Liste des salles récupérée.", salles);
                case ADD_SALLE:
                    Salle newSalle = salleDAO.addSalle((Salle) req.getPayload());
                    FileManager.log(actor, "AJOUT_SALLE", "SUCCÈS (" + newSalle.getNom() + ")");
                    return Response.ok("Salle ajoutée avec succès.", newSalle);
                case UPDATE_SALLE:
                    Salle updSalle = (Salle) req.getPayload();
                    boolean okUpdSalle = salleDAO.updateSalle(updSalle);
                    FileManager.log(actor, "MODIFICATION_SALLE", okUpdSalle ? "SUCCÈS" : "ÉCHEC");
                    return okUpdSalle
                            ? Response.ok("Salle mise à jour avec succès.", updSalle)
                            : Response.error("Salle introuvable.");
                case DELETE_SALLE:
                    int delSalleId = (Integer) req.getPayload();
                    boolean okDelSalle = salleDAO.deleteSalle(delSalleId);
                    FileManager.log(actor, "SUPPRESSION_SALLE", okDelSalle ? "SUCCÈS" : "ÉCHEC");
                    return okDelSalle
                            ? Response.ok("Salle supprimée avec succès.", null)
                            : Response.error("Salle introuvable.");
                case GET_RESERVATIONS:
                    List<Reservation> resList = reservationDAO.findAll();
                    FileManager.log(actor, "CONSULTATION_RESERVATIONS", "SUCCÈS (" + resList.size() + ")");
                    return Response.ok("Réservations récupérées.", resList);
                case SEARCH_RESERVATIONS:
                    String kw = (String) req.getPayload();
                    List<Reservation> found = reservationDAO.search(kw);
                    FileManager.log(actor, "RECHERCHE_RESERVATIONS", "SUCCÈS (" + found.size() + " résultats)");
                    return Response.ok("Recherche effectuée.", found);
                case ADD_RESERVATION:
                    return handleAddReservation((Reservation) req.getPayload(), actor);
                case UPDATE_RESERVATION:
                    return handleUpdateReservation((Reservation) req.getPayload(), actor);
                case CANCEL_RESERVATION:
                    int cancelId = (Integer) req.getPayload();
                    boolean cancelled = reservationDAO.cancelReservation(cancelId);
                    FileManager.log(actor, "ANNULATION_RESERVATION #" + cancelId, cancelled ? "SUCCÈS" : "ÉCHEC");
                    return cancelled
                            ? Response.ok("Réservation #" + cancelId + " annulée.", null)
                            : Response.error("Réservation introuvable.");
                case DELETE_RESERVATION:
                    int delId = (Integer) req.getPayload();
                    boolean deleted = reservationDAO.deleteReservation(delId);
                    FileManager.log(actor, "SUPPRESSION_RESERVATION #" + delId, deleted ? "SUCCÈS" : "ÉCHEC");
                    return deleted
                            ? Response.ok("Réservation #" + delId + " supprimée.", null)
                            : Response.error("Réservation introuvable.");
                case GET_STATS:
                    return Response.ok("Statistiques récupérées.", reservationDAO.getStats());
                default:
                    return Response.error("Opération non supportée.");
            }
        } catch (SQLException e) {
            FileManager.log(actor, req.getType().name(), "ERREUR SQL : " + e.getMessage());
            return Response.error("Erreur base de données : " + e.getMessage());
        } catch (Exception e) {
            FileManager.log(actor, req.getType().name(), "ERREUR : " + e.getMessage());
            return Response.error("Erreur serveur : " + e.getMessage());
        }
    }

    private Response handleLogin(Request req) throws SQLException {
        String[] creds = (String[]) req.getPayload();
        String email = creds[0];
        String mdp = creds[1];
        Utilisateur user = utilisateurDAO.authenticate(email, mdp);
        if (user != null) {
            this.currentUser = user;
            FileManager.log(user.getEmail(), "AUTHENTIFICATION", "SUCCÈS (" + user.getRole() + ")");
            return Response.ok("Authentification réussie.", user);
        }
        FileManager.log(email, "AUTHENTIFICATION", "ÉCHEC (identifiants invalides)");
        return Response.error("Email ou mot de passe incorrect.");
    }

    private Response handleAddReservation(Reservation r, String actor) throws SQLException {
        String lockKey = r.getIdSalle() + "_" + r.getDate();
        Object lock = SLOT_LOCKS.computeIfAbsent(lockKey, k -> new Object());
        synchronized (lock) {
            ReservationDAO.OperationResult result = reservationDAO.addReservation(r);
            FileManager.log(actor, "AJOUT_RESERVATION (Salle=" + r.getIdSalle() + ", Date=" + r.getDate() + ")",
                    result.success ? "SUCCÈS #" + r.getId() : "REFUSÉ : " + result.message);
            return result.success
                    ? Response.ok(result.message, result.reservation)
                    : Response.error(result.message);
        }
    }

    private Response handleUpdateReservation(Reservation r, String actor) throws SQLException {
        String lockKey = r.getIdSalle() + "_" + r.getDate();
        Object lock = SLOT_LOCKS.computeIfAbsent(lockKey, k -> new Object());
        synchronized (lock) {
            ReservationDAO.OperationResult result = reservationDAO.updateReservation(r);
            FileManager.log(actor, "MODIFICATION_RESERVATION #" + r.getId(),
                    result.success ? "SUCCÈS" : "REFUSÉ : " + result.message);
            return result.success
                    ? Response.ok(result.message, result.reservation)
                    : Response.error(result.message);
        }
    }

    private String getActorName(Request req) {
        if (currentUser != null) return currentUser.getEmail();
        if (req != null && req.getUserEmail() != null) return req.getUserEmail();
        return "ANONYME";
    }
}
