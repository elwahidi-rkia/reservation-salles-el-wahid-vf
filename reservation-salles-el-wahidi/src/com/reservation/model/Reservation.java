package com.reservation.model;

import java.io.Serializable;
import java.sql.Date;
import java.sql.Time;

/**
 * Représente une réservation de salle.
 */
public class Reservation implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private int idUtilisateur;
    private int idSalle;
    private Date date;
    private Time heureDebut;
    private Time heureFin;
    private String motif;
    private String statut;

    // Champs d'affichage (jointures)
    private String nomUtilisateur;
    private String nomSalle;

    public Reservation() {}

    public Reservation(int id, int idUtilisateur, int idSalle, Date date, Time heureDebut, Time heureFin, String motif, String statut) {
        this.id = id;
        this.idUtilisateur = idUtilisateur;
        this.idSalle = idSalle;
        this.date = date;
        this.heureDebut = heureDebut;
        this.heureFin = heureFin;
        this.motif = motif;
        this.statut = statut;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getIdUtilisateur() { return idUtilisateur; }
    public void setIdUtilisateur(int idUtilisateur) { this.idUtilisateur = idUtilisateur; }

    public int getIdSalle() { return idSalle; }
    public void setIdSalle(int idSalle) { this.idSalle = idSalle; }

    public Date getDate() { return date; }
    public void setDate(Date date) { this.date = date; }

    public Time getHeureDebut() { return heureDebut; }
    public void setHeureDebut(Time heureDebut) { this.heureDebut = heureDebut; }

    public Time getHeureFin() { return heureFin; }
    public void setHeureFin(Time heureFin) { this.heureFin = heureFin; }

    public String getMotif() { return motif; }
    public void setMotif(String motif) { this.motif = motif; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public String getNomUtilisateur() { return nomUtilisateur; }
    public void setNomUtilisateur(String nomUtilisateur) { this.nomUtilisateur = nomUtilisateur; }

    public String getNomSalle() { return nomSalle; }
    public void setNomSalle(String nomSalle) { this.nomSalle = nomSalle; }

    @Override
    public String toString() {
        return "Reservation #" + id + " [Salle=" + idSalle + ", Date=" + date + " " + heureDebut + "-" + heureFin + ", Statut=" + statut + "]";
    }
}
