package com.reservation.dao;

import com.reservation.model.Utilisateur;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Opérations JDBC sur la table Utilisateur.
 */
public class UtilisateurDAO {

    public Utilisateur authenticate(String email, String motDePasse) throws SQLException {
        String sql = "SELECT id, nom, email, motDePasse, role FROM Utilisateur WHERE email = ? AND motDePasse = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, motDePasse);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    public List<Utilisateur> findAll() throws SQLException {
        List<Utilisateur> list = new ArrayList<>();
        String sql = "SELECT id, nom, email, motDePasse, role FROM Utilisateur ORDER BY id";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    private Utilisateur mapRow(ResultSet rs) throws SQLException {
        return new Utilisateur(
            rs.getInt("id"),
            rs.getString("nom"),
            rs.getString("email"),
            rs.getString("motDePasse"),
            rs.getString("role")
        );
    }
}
