package com.reservation.network;

import java.io.Serializable;

/**
 * Requête sérialisable envoyée du client vers le serveur via ObjectOutputStream.
 */
public class Request implements Serializable {
    private static final long serialVersionUID = 1L;

    private RequestType type;
    private Object payload;
    private String userEmail;

    public Request() {}

    public Request(RequestType type, Object payload) {
        this.type = type;
        this.payload = payload;
    }

    public Request(RequestType type, Object payload, String userEmail) {
        this.type = type;
        this.payload = payload;
        this.userEmail = userEmail;
    }

    public RequestType getType() { return type; }
    public void setType(RequestType type) { this.type = type; }

    public Object getPayload() { return payload; }
    public void setPayload(Object payload) { this.payload = payload; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
}
