package com.reservation.ui;

import com.reservation.client.ReservationClient;
import com.reservation.model.Utilisateur;
import com.reservation.network.Request;
import com.reservation.network.RequestType;
import com.reservation.network.Response;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/**
 * Fenêtre d'authentification Swing (email, mot de passe) utilisant SwingWorker.
 */
public class LoginFrame extends JFrame {
    private static final Color BG_PAGE = new Color(240, 253, 250);
    private static final Color BG_HEADER = new Color(19, 78, 74);
    private static final Color BG_CARD = Color.WHITE;
    private static final Color BORDER_SUBTLE = new Color(203, 213, 225);
    private static final Color TEXT_PRIMARY = new Color(15, 23, 42);
    private static final Color TEXT_MUTED = new Color(100, 116, 139);
    private static final Color PRIMARY_COLOR = new Color(13, 148, 136);
    private static final Color ERROR_RED = new Color(220, 38, 38);

    private final JTextField hostField = new JTextField("localhost");
    private final JTextField portField = new JTextField("5000");
    private final JTextField emailField = new JTextField();
    private final JPasswordField passwordField = new JPasswordField();
    private final JCheckBox showPasswordCheck = new JCheckBox("Afficher le mot de passe");
    private final JButton loginButton = new JButton("Se connecter");
    private final JLabel statusLabel = new JLabel("Prêt à se connecter au serveur", SwingConstants.CENTER);

    public LoginFrame() {
        super("Gestion des Réservations - Authentification");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(450, 500);
        setMinimumSize(new Dimension(420, 480));
        setLocationRelativeTo(null);
        setResizable(false);
        initUI();
    }

    private void initUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG_PAGE);

        // En-tête
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBackground(BG_HEADER);
        header.setBorder(new EmptyBorder(22, 24, 22, 24));

        JLabel title = new JLabel("ReservationDB");
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        title.setForeground(Color.WHITE);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitle = new JLabel("Système de Gestion des Réservations de Salles");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 12));
        subtitle.setForeground(new Color(153, 246, 228));
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        header.add(title);
        header.add(Box.createVerticalStrut(4));
        header.add(subtitle);
        root.add(header, BorderLayout.NORTH);

        // Conteneur central avec carte blanche
        JPanel centerWrapper = new JPanel(new GridBagLayout());
        centerWrapper.setBackground(BG_PAGE);
        centerWrapper.setBorder(new EmptyBorder(20, 24, 16, 24));

        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(BG_CARD);
        card.setBorder(new CompoundBorder(
                new LineBorder(BORDER_SUBTLE, 1, true),
                new EmptyBorder(20, 22, 20, 22)
        ));

        styleInput(hostField);
        styleInput(portField);
        styleInput(emailField);
        styleInput(passwordField);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        // Serveur & Port
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 4, 0);
        card.add(createFieldLabel("Serveur & Port"), gbc);

        JPanel serverRow = new JPanel(new GridBagLayout());
        serverRow.setOpaque(false);
        GridBagConstraints sg = new GridBagConstraints();
        sg.gridy = 0;
        sg.fill = GridBagConstraints.HORIZONTAL;
        sg.gridx = 0;
        sg.weightx = 0.72;
        sg.insets = new Insets(0, 0, 0, 8);
        serverRow.add(hostField, sg);

        sg.gridx = 1;
        sg.weightx = 0.28;
        sg.insets = new Insets(0, 0, 0, 0);
        serverRow.add(portField, sg);

        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 14, 0);
        card.add(serverRow, gbc);

        // Email
        gbc.gridy = 2;
        gbc.insets = new Insets(0, 0, 4, 0);
        card.add(createFieldLabel("Adresse email"), gbc);

        gbc.gridy = 3;
        gbc.insets = new Insets(0, 0, 14, 0);
        card.add(emailField, gbc);

        // Mot de passe
        gbc.gridy = 4;
        gbc.insets = new Insets(0, 0, 4, 0);
        card.add(createFieldLabel("Mot de passe"), gbc);

        gbc.gridy = 5;
        gbc.insets = new Insets(0, 0, 6, 0);
        card.add(passwordField, gbc);

        // Option afficher mot de passe
        char defaultEcho = passwordField.getEchoChar();
        showPasswordCheck.setOpaque(false);
        showPasswordCheck.setFont(new Font("SansSerif", Font.PLAIN, 12));
        showPasswordCheck.setForeground(TEXT_MUTED);
        showPasswordCheck.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        showPasswordCheck.addActionListener(e ->
                passwordField.setEchoChar(showPasswordCheck.isSelected() ? (char) 0 : defaultEcho)
        );
        gbc.gridy = 6;
        gbc.insets = new Insets(0, 0, 16, 0);
        card.add(showPasswordCheck, gbc);

        // Bouton de connexion
        loginButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        loginButton.setBackground(PRIMARY_COLOR);
        loginButton.setForeground(Color.WHITE);
        loginButton.setFocusPainted(false);
        loginButton.setOpaque(true);
        loginButton.setBorderPainted(false);
        loginButton.setPreferredSize(new Dimension(0, 40));
        loginButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        loginButton.addActionListener(e -> performLogin());
        getRootPane().setDefaultButton(loginButton);

        gbc.gridy = 7;
        gbc.insets = new Insets(0, 0, 0, 0);
        card.add(loginButton, gbc);

        GridBagConstraints cardGbc = new GridBagConstraints();
        cardGbc.fill = GridBagConstraints.BOTH;
        cardGbc.weightx = 1.0;
        cardGbc.weighty = 1.0;
        centerWrapper.add(card, cardGbc);
        root.add(centerWrapper, BorderLayout.CENTER);

        // Pied de page statut
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        statusLabel.setForeground(TEXT_MUTED);
        statusLabel.setBorder(new EmptyBorder(0, 20, 14, 20));
        root.add(statusLabel, BorderLayout.SOUTH);

        setContentPane(root);
    }

    private JLabel createFieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", Font.BOLD, 12));
        label.setForeground(TEXT_PRIMARY);
        return label;
    }

    private void styleInput(JTextField field) {
        field.setFont(new Font("SansSerif", Font.PLAIN, 13));
        field.setForeground(TEXT_PRIMARY);
        field.setPreferredSize(new Dimension(0, 34));
        field.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER_SUBTLE, 1, true),
                new EmptyBorder(4, 10, 4, 10)
        ));
    }

    private void performLogin() {
        String host = hostField.getText().trim();
        int port;
        try {
            port = Integer.parseInt(portField.getText().trim());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Port invalide.", "Erreur", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String email = emailField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (email.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Veuillez saisir l'email et le mot de passe.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        loginButton.setEnabled(false);
        statusLabel.setText("Connexion en cours...");
        statusLabel.setForeground(PRIMARY_COLOR);

        SwingWorker<Response, Void> worker = new SwingWorker<>() {
            private ReservationClient client;

            @Override
            protected Response doInBackground() throws Exception {
                client = new ReservationClient(host, port);
                client.connect();
                Request req = new Request(RequestType.LOGIN, new String[]{email, password}, email);
                return client.sendRequest(req);
            }

            @Override
            protected void done() {
                loginButton.setEnabled(true);
                try {
                    Response resp = get();
                    if (resp.isSuccess() && resp.getData() instanceof Utilisateur user) {
                        client.setUserEmail(user.getEmail());
                        statusLabel.setText("Connecté !");
                        MainFrame mainFrame = new MainFrame(client, user);
                        mainFrame.setVisible(true);
                        dispose();
                    } else {
                        client.disconnect();
                        statusLabel.setText(resp.getMessage());
                        statusLabel.setForeground(ERROR_RED);
                        JOptionPane.showMessageDialog(LoginFrame.this, resp.getMessage(), "Échec d'authentification", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    if (client != null) client.disconnect();
                    String msg = "Impossible de joindre le serveur (" + host + ":" + port + ") : " + ex.getMessage();
                    statusLabel.setText("Serveur injoignable");
                    statusLabel.setForeground(ERROR_RED);
                    JOptionPane.showMessageDialog(LoginFrame.this, msg, "Erreur réseau", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }
}
