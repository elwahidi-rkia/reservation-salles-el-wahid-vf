package com.reservation.network;

/**
 * Types d'opérations échangées entre ReservationClient et ReservationServer.
 */
public enum RequestType {
    LOGIN,
    LOGOUT,
    GET_SALLES,
    ADD_SALLE,
    UPDATE_SALLE,
    DELETE_SALLE,
    GET_RESERVATIONS,
    SEARCH_RESERVATIONS,
    ADD_RESERVATION,
    UPDATE_RESERVATION,
    CANCEL_RESERVATION,
    DELETE_RESERVATION,
    GET_STATS
}
