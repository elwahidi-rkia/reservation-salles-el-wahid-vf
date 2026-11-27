package com.reservation.dao;

import com.reservation.model.DashboardStats;
import com.reservation.model.Reservation;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Opérations JDBC CRUD sur la table Reservation avec garantie transactionnelle
 * de non-chevauchement horaire au niveau de la base de données.
 */
public class ReservationDAO {

    /**
     * Résultat d'une opération de réservation avec indicateur de disponibilité.
     */
    public static class OperationResult {
        public final boolean success;
        public final String message;
        public final Reservation reservation;

        public OperationResult(boolean success, String message, Reservation reservation) {
            this.success = success;
            this.message = message;
            this.reservation = reservation;
        }
    }

    public List<Reservation> findAll() throws SQLException {
        String sql =
            "SELECT r.id, r.idUtilisateur, r.idSalle, r.date, r.heureDebut, r.heureFin, r.motif, r.statut, " +
            "u.nom AS nomUtilisateur, s.nom AS nomSalle " +
            "FROM Reservation r " +
            "JOIN Utilisateur u ON r.idUtilisateur = u.id " +
            "JOIN Salle s ON r.idSalle = s.id " +
            "ORDER BY r.date DESC, r.heureDebut ASC";
        List<Reservation> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    public List<Reservation> search(String keyword) throws SQLException {
        String like = "%" + (keyword == null ? "" : keyword.trim().toLowerCase()) + "%";
        String sql =
            "SELECT r.id, r.idUtilisateur, r.idSalle, r.date, r.heureDebut, r.heureFin, r.motif, r.statut, " +
            "u.nom AS nomUtilisateur, s.nom AS nomSalle " +
            "FROM Reservation r " +
            "JOIN Utilisateur u ON r.idUtilisateur = u.id " +
            "JOIN Salle s ON r.idSalle = s.id " +
            "WHERE LOWER(u.nom) LIKE ? OR LOWER(s.nom) LIKE ? OR LOWER(r.motif) LIKE ? " +
            "OR LOWER(r.statut) LIKE ? OR CAST(r.date AS CHAR) LIKE ? " +
            "ORDER BY r.date DESC, r.heureDebut ASC";
        List<Reservation> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 1; i <= 5; i++) {
                ps.setString(i, like);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    /**
     * Ajoute une réservation en vérifiant l'absence de conflit horaire
     * dans une transaction SERIALIZABLE avec verrouillage FOR UPDATE.
     */
    public OperationResult addReservation(Reservation r) throws SQLException {
        if (r.getHeureDebut() == null || r.getHeureFin() == null || !r.getHeureDebut().before(r.getHeureFin())) {
            return new OperationResult(false, "L'heure de début doit être strictement antérieure à l'heure de fin.", null);
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            conn.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            try {
                if (!isSalleDisponible(conn, r.getIdSalle())) {
                    conn.rollback();
                    return new OperationResult(false, "Salle indisponible : cette salle est actuellement désactivée.", null);
                }

                if (hasOverlap(conn, r.getIdSalle(), r.getDate(), r.getHeureDebut(), r.getHeureFin(), -1)) {
                    conn.rollback();
                    return new OperationResult(false, "Salle indisponible : un chevauchement horaire existe déjà pour cette date.", null);
                }

                String insertSql = "INSERT INTO Reservation (idUtilisateur, idSalle, date, heureDebut, heureFin, motif, statut) VALUES (?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, r.getIdUtilisateur());
                    ps.setInt(2, r.getIdSalle());
                    ps.setDate(3, r.getDate());
                    ps.setTime(4, r.getHeureDebut());
                    ps.setTime(5, r.getHeureFin());
                    ps.setString(6, r.getMotif());
                    ps.setString(7, r.getStatut() == null || r.getStatut().isBlank() ? "CONFIRMEE" : r.getStatut());
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (keys.next()) {
                            r.setId(keys.getInt(1));
                        }
                    }
                }
                conn.commit();
                return new OperationResult(true, "Réservation confirmée avec succès (ID #" + r.getId() + ").", r);
            } catch (SQLException e) {
                conn.rollback();
                return new OperationResult(false, "Salle indisponible (conflit concurrent en base de données) : " + e.getMessage(), null);
            }
        }
    }

    /**
     * Modifie une réservation existante en vérifiant l'absence de conflit horaire.
     */
    public OperationResult updateReservation(Reservation r) throws SQLException {
        if (r.getHeureDebut() == null || r.getHeureFin() == null || !r.getHeureDebut().before(r.getHeureFin())) {
            return new OperationResult(false, "L'heure de début doit être strictement antérieure à l'heure de fin.", null);
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            conn.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            try {
                if (!isSalleDisponible(conn, r.getIdSalle())) {
                    conn.rollback();
                    return new OperationResult(false, "Salle indisponible : cette salle est actuellement désactivée.", null);
                }

                if (!"ANNULEE".equalsIgnoreCase(r.getStatut())
                        && hasOverlap(conn, r.getIdSalle(), r.getDate(), r.getHeureDebut(), r.getHeureFin(), r.getId())) {
                    conn.rollback();
                    return new OperationResult(false, "Salle indisponible : un chevauchement horaire existe déjà pour cette date.", null);
                }

                String sql = "UPDATE Reservation SET idSalle = ?, date = ?, heureDebut = ?, heureFin = ?, motif = ?, statut = ? WHERE id = ?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, r.getIdSalle());
                    ps.setDate(2, r.getDate());
                    ps.setTime(3, r.getHeureDebut());
                    ps.setTime(4, r.getHeureFin());
                    ps.setString(5, r.getMotif());
                    ps.setString(6, r.getStatut());
                    ps.setInt(7, r.getId());
                    int updated = ps.executeUpdate();
                    if (updated == 0) {
                        conn.rollback();
                        return new OperationResult(false, "Réservation introuvable (ID #" + r.getId() + ").", null);
                    }
                }
                conn.commit();
                return new OperationResult(true, "Réservation #" + r.getId() + " mise à jour.", r);
            } catch (SQLException e) {
                conn.rollback();
                return new OperationResult(false, "Erreur de mise à jour : " + e.getMessage(), null);
            }
        }
    }

    public boolean cancelReservation(int id) throws SQLException {
        String sql = "UPDATE Reservation SET statut = 'ANNULEE' WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean deleteReservation(int id) throws SQLException {
        String sql = "DELETE FROM Reservation WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    public DashboardStats getStats() throws SQLException {
        DashboardStats stats = new DashboardStats();
        try (Connection conn = DatabaseConnection.getConnection();
             Statement st = conn.createStatement()) {

            try (ResultSet rs = st.executeQuery("SELECT COUNT(*), SUM(CASE WHEN disponible = TRUE THEN 1 ELSE 0 END) FROM Salle")) {
                if (rs.next()) {
                    stats.setTotalSalles(rs.getInt(1));
                    stats.setSallesDisponibles(rs.getInt(2));
                }
            }

            try (ResultSet rs = st.executeQuery(
                "SELECT COUNT(*), " +
                "SUM(CASE WHEN statut = 'CONFIRMEE' THEN 1 ELSE 0 END), " +
                "SUM(CASE WHEN statut = 'ANNULEE' THEN 1 ELSE 0 END) " +
                "FROM Reservation")) {
                if (rs.next()) {
                    stats.setTotalReservations(rs.getInt(1));
                    stats.setReservationsConfirmees(rs.getInt(2));
                    stats.setReservationsAnnulees(rs.getInt(3));
                }
            }

            String todaySql = "SELECT COUNT(*) FROM Reservation WHERE date = ?";
            try (PreparedStatement ps = conn.prepareStatement(todaySql)) {
                ps.setDate(1, Date.valueOf(LocalDate.now()));
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        stats.setReservationsAujourdhui(rs.getInt(1));
                    }
                }
            }
        }
        return stats;
    }

    private boolean isSalleDisponible(Connection conn, int idSalle) throws SQLException {
        String sql = "SELECT disponible FROM Salle WHERE id = ? FOR UPDATE";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idSalle);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getBoolean("disponible");
            }
        }
    }

    private boolean hasOverlap(Connection conn, int idSalle, Date date, Time debut, Time fin, int excludeId) throws SQLException {
        String sql =
            "SELECT id FROM Reservation " +
            "WHERE idSalle = ? AND date = ? AND statut <> 'ANNULEE' " +
            "AND heureDebut < ? AND heureFin > ? AND id <> ? FOR UPDATE";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idSalle);
            ps.setDate(2, date);
            ps.setTime(3, fin);
            ps.setTime(4, debut);
            ps.setInt(5, excludeId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private Reservation mapRow(ResultSet rs) throws SQLException {
        Reservation r = new Reservation(
            rs.getInt("id"),
            rs.getInt("idUtilisateur"),
            rs.getInt("idSalle"),
            rs.getDate("date"),
            rs.getTime("heureDebut"),
            rs.getTime("heureFin"),
            rs.getString("motif"),
            rs.getString("statut")
        );
        r.setNomUtilisateur(rs.getString("nomUtilisateur"));
        r.setNomSalle(rs.getString("nomSalle"));
        return r;
    }
}
