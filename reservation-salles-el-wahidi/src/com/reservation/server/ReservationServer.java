package com.reservation.server;

import com.reservation.dao.DatabaseConnection;
import com.reservation.util.FileManager;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Serveur TCP multi-thread écoutant sur le port 5000.
 */
public class ReservationServer {
    public static final int DEFAULT_PORT = 5000;

    private final int port;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicInteger activeClients = new AtomicInteger(0);
    private final Set<Socket> clientSockets = ConcurrentHashMap.newKeySet();
    private ServerSocket serverSocket;

    public ReservationServer() {
        this(DEFAULT_PORT);
    }

    public ReservationServer(int port) {
        this.port = port;
    }

    public void start() throws IOException {
        try (Connection ignored = DatabaseConnection.getConnection()) {
            System.out.println("[SERVEUR] Base de données prête (" + DatabaseConnection.getActiveMode() + ").");
        } catch (Exception e) {
            System.err.println("[SERVEUR] Avertissement DB : " + e.getMessage());
        }

        serverSocket = new ServerSocket(port);
        running.set(true);
        FileManager.log("SERVEUR", "DEMARRAGE", "Écoute sur le port " + port + " (" + DatabaseConnection.getActiveMode() + ")");
        System.out.println("[SERVEUR] ReservationServer démarré sur le port " + port + "...");

        while (running.get()) {
            try {
                Socket clientSocket = serverSocket.accept();
                clientSockets.add(clientSocket);
                Thread t = new Thread(new ClientHandler(clientSocket, this));
                t.setDaemon(true);
                t.start();
            } catch (IOException e) {
                if (running.get()) {
                    System.err.println("[SERVEUR] Erreur accept : " + e.getMessage());
                }
            }
        }
    }

    public void startInBackground() throws IOException {
        try (Connection ignored = DatabaseConnection.getConnection()) {
            System.out.println("[SERVEUR] Base de données prête (" + DatabaseConnection.getActiveMode() + ").");
        } catch (Exception e) {
            System.err.println("[SERVEUR] Avertissement DB : " + e.getMessage());
        }

        serverSocket = new ServerSocket(port);
        running.set(true);
        Thread acceptThread = new Thread(() -> {
            while (running.get()) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    clientSockets.add(clientSocket);
                    Thread t = new Thread(new ClientHandler(clientSocket, this));
                    t.setDaemon(true);
                    t.start();
                } catch (IOException e) {
                    if (running.get()) {
                        System.err.println("[SERVEUR] Erreur accept : " + e.getMessage());
                    }
                }
            }
        }, "ReservationServer-Acceptor");
        acceptThread.setDaemon(true);
        acceptThread.start();
    }

    public void stop() {
        running.set(false);
        for (Socket s : clientSockets) {
            try {
                s.close();
            } catch (IOException ignored) {}
        }
        clientSockets.clear();
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException ignored) {}
        }
        FileManager.log("SERVEUR", "ARRET", "Serveur arrêté");
    }

    public void onClientConnected(String addr) {
        int count = activeClients.incrementAndGet();
        System.out.println("[SERVEUR] Client connecté : " + addr + " | Clients actifs : " + count);
    }

    public void onClientDisconnected(String addr, Socket socket) {
        clientSockets.remove(socket);
        int count = activeClients.decrementAndGet();
        System.out.println("[SERVEUR] Client déconnecté : " + addr + " | Clients actifs : " + count);
    }

    public int getActiveClientsCount() {
        return activeClients.get();
    }

    public int getPort() {
        return port;
    }

    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            port = Integer.parseInt(args[0]);
        }
        ReservationServer server = new ReservationServer(port);
        try {
            server.start();
        } catch (IOException e) {
            System.err.println("[SERVEUR] Impossible de démarrer sur le port " + port + " : " + e.getMessage());
        }
    }
}
