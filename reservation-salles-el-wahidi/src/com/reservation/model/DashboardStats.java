package com.reservation.model;

import java.io.Serializable;

/**
 * Statistiques affichées sur le tableau de bord.
 */
public class DashboardStats implements Serializable {
    private static final long serialVersionUID = 1L;

    private int totalSalles;
    private int sallesDisponibles;
    private int totalReservations;
    private int reservationsConfirmees;
    private int reservationsAnnulees;
    private int reservationsAujourdhui;

    public int getTotalSalles() { return totalSalles; }
    public void setTotalSalles(int totalSalles) { this.totalSalles = totalSalles; }

    public int getSallesDisponibles() { return sallesDisponibles; }
    public void setSallesDisponibles(int sallesDisponibles) { this.sallesDisponibles = sallesDisponibles; }

    public int getTotalReservations() { return totalReservations; }
    public void setTotalReservations(int totalReservations) { this.totalReservations = totalReservations; }

    public int getReservationsConfirmees() { return reservationsConfirmees; }
    public void setReservationsConfirmees(int reservationsConfirmees) { this.reservationsConfirmees = reservationsConfirmees; }

    public int getReservationsAnnulees() { return reservationsAnnulees; }
    public void setReservationsAnnulees(int reservationsAnnulees) { this.reservationsAnnulees = reservationsAnnulees; }

    public int getReservationsAujourdhui() { return reservationsAujourdhui; }
    public void setReservationsAujourdhui(int reservationsAujourdhui) { this.reservationsAujourdhui = reservationsAujourdhui; }
}
