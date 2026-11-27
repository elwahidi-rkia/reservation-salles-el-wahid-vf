package com.reservation.dao;

import com.reservation.model.Salle;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Opérations JDBC CRUD sur la table Salle.
 */
public class SalleDAO {

    public List<Salle> findAll() throws SQLException {
        List<Salle> list = new ArrayList<>();
        String sql = "SELECT id, nom, type, capacite, disponible FROM Salle ORDER BY id";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    public Salle addSalle(Salle salle) throws SQLException {
        String sql = "INSERT INTO Salle (nom, type, capacite, disponible) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, salle.getNom());
            ps.setString(2, salle.getType());
            ps.setInt(3, salle.getCapacite());
            ps.setBoolean(4, salle.isDisponible());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    salle.setId(keys.getInt(1));
                }
            }
            return salle;
        }
    }

    public boolean updateSalle(Salle salle) throws SQLException {
        String sql = "UPDATE Salle SET nom = ?, type = ?, capacite = ?, disponible = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, salle.getNom());
            ps.setString(2, salle.getType());
            ps.setInt(3, salle.getCapacite());
            ps.setBoolean(4, salle.isDisponible());
            ps.setInt(5, salle.getId());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean deleteSalle(int id) throws SQLException {
        String sql = "DELETE FROM Salle WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    private Salle mapRow(ResultSet rs) throws SQLException {
        return new Salle(
            rs.getInt("id"),
            rs.getString("nom"),
            rs.getString("type"),
            rs.getInt("capacite"),
            rs.getBoolean("disponible")
        );
    }
}
