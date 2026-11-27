package com.reservation.client;

import com.reservation.network.Request;
import com.reservation.network.RequestType;
import com.reservation.network.Response;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * Client TCP communiquant avec ReservationServer via ObjectInputStream/ObjectOutputStream.
 */
public class ReservationClient {
    private final String host;
    private final int port;
    private Socket socket;
    private ObjectOutputStream oos;
    private ObjectInputStream ois;
    private String userEmail;

    public ReservationClient() {
        this("localhost", 5000);
    }

    public ReservationClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public synchronized void connect() throws IOException {
        if (isConnected()) return;
        this.socket = new Socket(host, port);
        this.oos = new ObjectOutputStream(socket.getOutputStream());
        this.oos.flush();
        this.ois = new ObjectInputStream(socket.getInputStream());
    }

    public synchronized boolean isConnected() {
        return socket != null && !socket.isClosed() && socket.isConnected();
    }

    public synchronized Response sendRequest(Request request) throws IOException, ClassNotFoundException {
        if (!isConnected()) {
            throw new IOException("Connexion au serveur perdue ou non établie.");
        }
        if (request.getUserEmail() == null && userEmail != null) {
            request.setUserEmail(userEmail);
        }
        try {
            oos.writeObject(request);
            oos.flush();
            oos.reset();
            Object resp = ois.readObject();
            if (resp instanceof Response) {
                return (Response) resp;
            }
            throw new IOException("Réponse inattendue du serveur.");
        } catch (IOException e) {
            closeQuietly();
            throw new IOException("Perte de connexion avec le serveur : " + e.getMessage(), e);
        }
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public synchronized void disconnect() {
        if (isConnected()) {
            try {
                oos.writeObject(new Request(RequestType.LOGOUT, null, userEmail));
                oos.flush();
            } catch (IOException ignored) {}
        }
        closeQuietly();
    }

    private void closeQuietly() {
        try { if (ois != null) ois.close(); } catch (IOException ignored) {}
        try { if (oos != null) oos.close(); } catch (IOException ignored) {}
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
        socket = null;
        ois = null;
        oos = null;
    }
}
