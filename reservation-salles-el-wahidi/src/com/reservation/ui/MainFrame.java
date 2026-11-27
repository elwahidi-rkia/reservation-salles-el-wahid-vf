package com.reservation.ui;

import com.reservation.client.ReservationClient;
import com.reservation.model.DashboardStats;
import com.reservation.model.Reservation;
import com.reservation.model.Salle;
import com.reservation.model.Utilisateur;
import com.reservation.network.Request;
import com.reservation.network.RequestType;
import com.reservation.network.Response;
import com.reservation.util.FileManager;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.sql.Date;
import java.sql.Time;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Interface graphique principale :
 * - Tableau de bord statistiques
 * - Gestion des réservations (CRUD, recherche, CSV, sérialisation binaire)
 * - Consultation des salles
 * - Administration des salles (pour rôle ADMIN)
 */
public class MainFrame extends JFrame {
    private final ReservationClient client;
    private final Utilisateur currentUser;

    private final JLabel connectionBadge = new JLabel("● Connecté");

    // Dashboard labels
    private final JLabel lblTotalSalles = createStatValueLabel();
    private final JLabel lblSallesDisp = createStatValueLabel();
    private final JLabel lblTotalRes = createStatValueLabel();
    private final JLabel lblResConf = createStatValueLabel();
    private final JLabel lblResAnn = createStatValueLabel();
    private final JLabel lblResToday = createStatValueLabel();

    // Reservations UI
    private final DefaultTableModel resTableModel = new DefaultTableModel(
            new String[]{"ID", "Utilisateur", "Salle", "Date", "Début", "Fin", "Motif", "Statut"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable resTable = new JTable(resTableModel);
    private final JTextField searchField = new JTextField(20);
    private List<Reservation> currentReservations = new ArrayList<>();

    // Salles Consultation UI
    private final DefaultTableModel salleViewModel = new DefaultTableModel(
            new String[]{"ID", "Nom", "Type", "Capacité", "Disponible"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable salleViewTable = new JTable(salleViewModel);

    // Admin Salles UI
    private final DefaultTableModel adminSalleModel = new DefaultTableModel(
            new String[]{"ID", "Nom", "Type", "Capacité", "Disponible"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable adminSalleTable = new JTable(adminSalleModel);
    private List<Salle> currentSalles = new ArrayList<>();

    public MainFrame(ReservationClient client, Utilisateur user) {
        super("Gestion des Réservations de Salles - " + user.getNom() + " [" + user.getRole() + "]");
        this.client = client;
        this.currentUser = user;

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(1020, 660);
        setLocationRelativeTo(null);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                client.disconnect();
                dispose();
                System.exit(0);
            }
        });

        initUI();
        refreshAllData();
    }

    private void initUI() {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Top bar
        JPanel topBar = new JPanel(new BorderLayout());
        JLabel header = new JLabel("Utilisateur : " + currentUser.getNom() + " (" + currentUser.getEmail() + ") — Rôle : " + currentUser.getRole());
        header.setFont(new Font("SansSerif", Font.BOLD, 13));
        topBar.add(header, BorderLayout.WEST);

        JPanel rightTop = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        connectionBadge.setForeground(new Color(0, 140, 60));
        connectionBadge.setFont(new Font("SansSerif", Font.BOLD, 12));
        JButton btnLogout = new JButton("Déconnexion");
        btnLogout.addActionListener(e -> {
            client.disconnect();
            dispose();
            new LoginFrame().setVisible(true);
        });
        rightTop.add(connectionBadge);
        rightTop.add(btnLogout);
        topBar.add(rightTop, BorderLayout.EAST);
        root.add(topBar, BorderLayout.NORTH);

        // Tabs
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Tableau de bord", buildDashboardPanel());
        tabs.addTab("Gestion des réservations", buildReservationPanel());
        tabs.addTab("Consultation des salles", buildSallesViewPanel());
        if (currentUser.isAdmin()) {
            tabs.addTab("Administration des salles", buildAdminSallePanel());
        }

        root.add(tabs, BorderLayout.CENTER);
        setContentPane(root);
    }

    private JPanel buildDashboardPanel() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JPanel grid = new JPanel(new GridLayout(2, 3, 16, 16));
        grid.add(createStatCard("Total des Salles", lblTotalSalles, new Color(13, 148, 136)));
        grid.add(createStatCard("Salles Disponibles", lblSallesDisp, new Color(39, 174, 96)));
        grid.add(createStatCard("Total Réservations", lblTotalRes, new Color(190, 24, 93)));
        grid.add(createStatCard("Réservations Confirmées", lblResConf, new Color(101, 163, 13)));
        grid.add(createStatCard("Réservations Annulées", lblResAnn, new Color(192, 57, 43)));
        grid.add(createStatCard("Réservations Aujourd'hui", lblResToday, new Color(234, 88, 12)));

        panel.add(grid, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnRefresh = new JButton("Actualiser les statistiques");
        btnRefresh.addActionListener(e -> refreshStats());
        bottom.add(btnRefresh);
        panel.add(bottom, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildReservationPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Search Bar
        JPanel searchBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        searchBar.add(new JLabel("Rechercher (utilisateur, salle, motif, date) :"));
        searchBar.add(searchField);
        JButton btnSearch = new JButton("Rechercher");
        JButton btnShowAll = new JButton("Tout afficher");
        btnSearch.addActionListener(e -> searchReservations(searchField.getText()));
        btnShowAll.addActionListener(e -> {
            searchField.setText("");
            refreshReservations();
        });
        searchBar.add(btnSearch);
        searchBar.add(btnShowAll);
        panel.add(searchBar, BorderLayout.NORTH);

        resTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        resTable.setRowHeight(24);
        panel.add(new JScrollPane(resTable), BorderLayout.CENTER);

        // Action Buttons
        JPanel actions = new JPanel(new GridLayout(2, 1, 4, 4));
        JPanel crudRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        JButton btnAdd = new JButton("Ajouter");
        JButton btnEdit = new JButton("Modifier");
        JButton btnCancel = new JButton("Annuler");
        JButton btnDelete = new JButton("Supprimer");

        btnAdd.addActionListener(e -> openReservationDialog(null));
        btnEdit.addActionListener(e -> {
            Reservation sel = getSelectedReservation();
            if (sel != null) openReservationDialog(sel);
        });
        btnCancel.addActionListener(e -> {
            Reservation sel = getSelectedReservation();
            if (sel != null) cancelReservation(sel.getId());
        });
        btnDelete.addActionListener(e -> {
            Reservation sel = getSelectedReservation();
            if (sel != null) deleteReservation(sel.getId());
        });

        crudRow.add(btnAdd);
        crudRow.add(btnEdit);
        crudRow.add(btnCancel);
        crudRow.add(btnDelete);

        JPanel fileRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        JButton btnExportCsv = new JButton("Exporter CSV");
        JButton btnSaveBin = new JButton("Sauvegarder Binaire (.ser)");
        JButton btnLoadBin = new JButton("Lire Binaire (.ser)");

        btnExportCsv.addActionListener(e -> exportCsv());
        btnSaveBin.addActionListener(e -> saveBinary());
        btnLoadBin.addActionListener(e -> loadBinary());

        fileRow.add(btnExportCsv);
        fileRow.add(btnSaveBin);
        fileRow.add(btnLoadBin);

        actions.add(crudRow);
        actions.add(fileRow);
        panel.add(actions, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel buildSallesViewPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        salleViewTable.setRowHeight(24);
        panel.add(new JScrollPane(salleViewTable), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnRefresh = new JButton("Actualiser la liste des salles");
        btnRefresh.addActionListener(e -> refreshSalles());
        bottom.add(btnRefresh);
        panel.add(bottom, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildAdminSallePanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        adminSalleTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        adminSalleTable.setRowHeight(24);
        panel.add(new JScrollPane(adminSalleTable), BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        JButton btnAdd = new JButton("Ajouter Salle");
        JButton btnEdit = new JButton("Modifier Salle");
        JButton btnDelete = new JButton("Supprimer Salle");

        btnAdd.addActionListener(e -> openSalleDialog(null));
        btnEdit.addActionListener(e -> {
            Salle sel = getSelectedAdminSalle();
            if (sel != null) openSalleDialog(sel);
        });
        btnDelete.addActionListener(e -> {
            Salle sel = getSelectedAdminSalle();
            if (sel != null) deleteSalle(sel.getId());
        });

        buttons.add(btnAdd);
        buttons.add(btnEdit);
        buttons.add(btnDelete);
        panel.add(buttons, BorderLayout.SOUTH);
        return panel;
    }

    private void refreshAllData() {
        refreshStats();
        refreshSalles();
        refreshReservations();
    }

    private void refreshStats() {
        executeAsync(() -> client.sendRequest(new Request(RequestType.GET_STATS, null)), resp -> {
            if (resp.isSuccess() && resp.getData() instanceof DashboardStats stats) {
                lblTotalSalles.setText(String.valueOf(stats.getTotalSalles()));
                lblSallesDisp.setText(String.valueOf(stats.getSallesDisponibles()));
                lblTotalRes.setText(String.valueOf(stats.getTotalReservations()));
                lblResConf.setText(String.valueOf(stats.getReservationsConfirmees()));
                lblResAnn.setText(String.valueOf(stats.getReservationsAnnulees()));
                lblResToday.setText(String.valueOf(stats.getReservationsAujourdhui()));
            }
        });
    }

    @SuppressWarnings("unchecked")
    private void refreshSalles() {
        executeAsync(() -> client.sendRequest(new Request(RequestType.GET_SALLES, null)), resp -> {
            if (resp.isSuccess() && resp.getData() instanceof List<?> list) {
                currentSalles = (List<Salle>) list;
                salleViewModel.setRowCount(0);
                adminSalleModel.setRowCount(0);
                for (Salle s : currentSalles) {
                    Object[] row = {s.getId(), s.getNom(), s.getType(), s.getCapacite(), s.isDisponible() ? "Oui" : "Non"};
                    salleViewModel.addRow(row);
                    adminSalleModel.addRow(row);
                }
            }
        });
    }

    @SuppressWarnings("unchecked")
    private void refreshReservations() {
        executeAsync(() -> client.sendRequest(new Request(RequestType.GET_RESERVATIONS, null)), resp -> {
            if (resp.isSuccess() && resp.getData() instanceof List<?> list) {
                updateReservationTable((List<Reservation>) list);
            }
        });
    }

    @SuppressWarnings("unchecked")
    private void searchReservations(String keyword) {
        executeAsync(() -> client.sendRequest(new Request(RequestType.SEARCH_RESERVATIONS, keyword)), resp -> {
            if (resp.isSuccess() && resp.getData() instanceof List<?> list) {
                updateReservationTable((List<Reservation>) list);
            }
        });
    }

    private void updateReservationTable(List<Reservation> list) {
        currentReservations = list;
        resTableModel.setRowCount(0);
        for (Reservation r : list) {
            resTableModel.addRow(new Object[]{
                    r.getId(),
                    r.getNomUtilisateur() != null ? r.getNomUtilisateur() : ("User #" + r.getIdUtilisateur()),
                    r.getNomSalle() != null ? r.getNomSalle() : ("Salle #" + r.getIdSalle()),
                    r.getDate(),
                    r.getHeureDebut(),
                    r.getHeureFin(),
                    r.getMotif(),
                    r.getStatut()
            });
        }
    }

    private Reservation getSelectedReservation() {
        int row = resTable.getSelectedRow();
        if (row < 0 || row >= currentReservations.size()) {
            JOptionPane.showMessageDialog(this, "Veuillez sélectionner une réservation.", "Information", JOptionPane.INFORMATION_MESSAGE);
            return null;
        }
        return currentReservations.get(row);
    }

    private Salle getSelectedAdminSalle() {
        int row = adminSalleTable.getSelectedRow();
        if (row < 0 || row >= currentSalles.size()) {
            JOptionPane.showMessageDialog(this, "Veuillez sélectionner une salle.", "Information", JOptionPane.INFORMATION_MESSAGE);
            return null;
        }
        return currentSalles.get(row);
    }

    private void openReservationDialog(Reservation existing) {
        if (currentSalles.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Aucune salle chargée.", "Erreur", JOptionPane.WARNING_MESSAGE);
            return;
        }
        JComboBox<Salle> salleCombo = new JComboBox<>(currentSalles.toArray(new Salle[0]));
        JTextField dateField = new JTextField(existing != null ? existing.getDate().toString() : "2026-12-10", 12);
        JTextField debutField = new JTextField(existing != null ? existing.getHeureDebut().toString() : "09:00:00", 10);
        JTextField finField = new JTextField(existing != null ? existing.getHeureFin().toString() : "11:00:00", 10);
        JTextField motifField = new JTextField(existing != null ? existing.getMotif() : "", 20);
        JComboBox<String> statutCombo = new JComboBox<>(new String[]{"CONFIRMEE", "ANNULEE"});

        if (existing != null) {
            for (int i = 0; i < currentSalles.size(); i++) {
                if (currentSalles.get(i).getId() == existing.getIdSalle()) {
                    salleCombo.setSelectedIndex(i);
                    break;
                }
            }
            statutCombo.setSelectedItem(existing.getStatut());
        }

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.add(new JLabel("Salle :")); form.add(salleCombo);
        form.add(new JLabel("Date (AAAA-MM-JJ) :")); form.add(dateField);
        form.add(new JLabel("Heure début (HH:MM:SS) :")); form.add(debutField);
        form.add(new JLabel("Heure fin (HH:MM:SS) :")); form.add(finField);
        form.add(new JLabel("Motif :")); form.add(motifField);
        if (existing != null) {
            form.add(new JLabel("Statut :")); form.add(statutCombo);
        }

        int choice = JOptionPane.showConfirmDialog(this, form,
                existing == null ? "Nouvelle réservation" : "Modifier réservation #" + existing.getId(),
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (choice == JOptionPane.OK_OPTION) {
            try {
                Salle selectedSalle = (Salle) salleCombo.getSelectedItem();
                Date date = Date.valueOf(dateField.getText().trim());
                Time debut = Time.valueOf(normalizeTime(debutField.getText().trim()));
                Time fin = Time.valueOf(normalizeTime(finField.getText().trim()));
                String motif = motifField.getText().trim();
                if (motif.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Le motif est obligatoire.", "Validation", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                Reservation r = existing != null ? existing : new Reservation();
                r.setIdUtilisateur(existing != null ? existing.getIdUtilisateur() : currentUser.getId());
                r.setIdSalle(selectedSalle.getId());
                r.setDate(date);
                r.setHeureDebut(debut);
                r.setHeureFin(fin);
                r.setMotif(motif);
                r.setStatut(existing != null ? (String) statutCombo.getSelectedItem() : "CONFIRMEE");

                RequestType type = existing == null ? RequestType.ADD_RESERVATION : RequestType.UPDATE_RESERVATION;
                executeAsync(() -> client.sendRequest(new Request(type, r)), resp -> {
                    JOptionPane.showMessageDialog(this, resp.getMessage(),
                            resp.isSuccess() ? "Succès" : "Indisponibilité / Erreur",
                            resp.isSuccess() ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE);
                    refreshAllData();
                });
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(this, "Format date/heure invalide (ex: 2026-12-10 et 09:00:00).", "Erreur de format", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void cancelReservation(int id) {
        int ok = JOptionPane.showConfirmDialog(this, "Annuler la réservation #" + id + " ?", "Confirmation", JOptionPane.YES_NO_OPTION);
        if (ok == JOptionPane.YES_OPTION) {
            executeAsync(() -> client.sendRequest(new Request(RequestType.CANCEL_RESERVATION, id)), resp -> {
                JOptionPane.showMessageDialog(this, resp.getMessage());
                refreshAllData();
            });
        }
    }

    private void deleteReservation(int id) {
        int ok = JOptionPane.showConfirmDialog(this, "Supprimer définitivement la réservation #" + id + " ?", "Confirmation", JOptionPane.YES_NO_OPTION);
        if (ok == JOptionPane.YES_OPTION) {
            executeAsync(() -> client.sendRequest(new Request(RequestType.DELETE_RESERVATION, id)), resp -> {
                JOptionPane.showMessageDialog(this, resp.getMessage());
                refreshAllData();
            });
        }
    }

    private void openSalleDialog(Salle existing) {
        JTextField nomField = new JTextField(existing != null ? existing.getNom() : "", 18);
        JTextField typeField = new JTextField(existing != null ? existing.getType() : "", 18);
        JTextField capField = new JTextField(existing != null ? String.valueOf(existing.getCapacite()) : "30", 8);
        JCheckBox dispBox = new JCheckBox("Disponible", existing == null || existing.isDisponible());

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.add(new JLabel("Nom :")); form.add(nomField);
        form.add(new JLabel("Type :")); form.add(typeField);
        form.add(new JLabel("Capacité :")); form.add(capField);
        form.add(new JLabel("Statut :")); form.add(dispBox);

        int choice = JOptionPane.showConfirmDialog(this, form,
                existing == null ? "Ajouter une salle" : "Modifier salle #" + existing.getId(),
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (choice == JOptionPane.OK_OPTION) {
            try {
                int cap = Integer.parseInt(capField.getText().trim());
                Salle s = existing != null ? existing : new Salle();
                s.setNom(nomField.getText().trim());
                s.setType(typeField.getText().trim());
                s.setCapacite(cap);
                s.setDisponible(dispBox.isSelected());

                RequestType reqType = existing == null ? RequestType.ADD_SALLE : RequestType.UPDATE_SALLE;
                executeAsync(() -> client.sendRequest(new Request(reqType, s)), resp -> {
                    JOptionPane.showMessageDialog(this, resp.getMessage());
                    refreshAllData();
                });
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "La capacité doit être un nombre entier.", "Erreur", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void deleteSalle(int id) {
        int ok = JOptionPane.showConfirmDialog(this, "Supprimer la salle #" + id + " ?", "Confirmation", JOptionPane.YES_NO_OPTION);
        if (ok == JOptionPane.YES_OPTION) {
            executeAsync(() -> client.sendRequest(new Request(RequestType.DELETE_SALLE, id)), resp -> {
                JOptionPane.showMessageDialog(this, resp.getMessage());
                refreshAllData();
            });
        }
    }

    private void exportCsv() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("reservations_export.csv"));
        chooser.setFileFilter(new FileNameExtensionFilter("Fichier CSV (*.csv)", "csv"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            List<Reservation> snapshot = new ArrayList<>(currentReservations);
            new SwingWorker<Void, Void>() {
                @Override protected Void doInBackground() throws Exception {
                    FileManager.exportReservationsToCSV(snapshot, file);
                    FileManager.log(currentUser.getEmail(), "EXPORT_CSV", "SUCCÈS -> " + file.getName());
                    return null;
                }
                @Override protected void done() {
                    try {
                        get();
                        JOptionPane.showMessageDialog(MainFrame.this, "Export CSV réussi : " + file.getAbsolutePath());
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(MainFrame.this, "Erreur export CSV : " + ex.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }.execute();
        }
    }

    private void saveBinary() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("reservations.ser"));
        chooser.setFileFilter(new FileNameExtensionFilter("Fichier binaire (*.ser)", "ser"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            List<Reservation> snapshot = new ArrayList<>(currentReservations);
            new SwingWorker<Void, Void>() {
                @Override protected Void doInBackground() throws Exception {
                    FileManager.saveReservationsBinary(snapshot, file);
                    FileManager.log(currentUser.getEmail(), "SAUVEGARDE_BINAIRE", "SUCCÈS -> " + file.getName());
                    return null;
                }
                @Override protected void done() {
                    try {
                        get();
                        JOptionPane.showMessageDialog(MainFrame.this, "Sauvegarde binaire réussie : " + file.getAbsolutePath());
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(MainFrame.this, "Erreur sauvegarde binaire : " + ex.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }.execute();
        }
    }

    private void loadBinary() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Fichier binaire (*.ser)", "ser"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            new SwingWorker<List<Reservation>, Void>() {
                @Override protected List<Reservation> doInBackground() throws Exception {
                    List<Reservation> loaded = FileManager.loadReservationsBinary(file);
                    FileManager.log(currentUser.getEmail(), "LECTURE_BINAIRE", "SUCCÈS (" + loaded.size() + " enregistrements)");
                    return loaded;
                }
                @Override protected void done() {
                    try {
                        List<Reservation> loaded = get();
                        updateReservationTable(loaded);
                        JOptionPane.showMessageDialog(MainFrame.this, loaded.size() + " réservation(s) chargée(s) depuis le fichier binaire.");
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(MainFrame.this, "Erreur lecture binaire : " + ex.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }.execute();
        }
    }

    private void executeAsync(Callable<Response> task, java.util.function.Consumer<Response> onSuccess) {
        new SwingWorker<Response, Void>() {
            @Override
            protected Response doInBackground() throws Exception {
                return task.call();
            }

            @Override
            protected void done() {
                try {
                    Response resp = get();
                    onSuccess.accept(resp);
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    if (cause instanceof IOException) {
                        connectionBadge.setText("● Déconnecté");
                        connectionBadge.setForeground(Color.RED);
                        int opt = JOptionPane.showConfirmDialog(MainFrame.this,
                                "Perte de connexion avec le serveur !\nVoulez-vous revenir à l'écran de connexion ?",
                                "Connexion perdue", JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE);
                        if (opt == JOptionPane.YES_OPTION) {
                            dispose();
                            new LoginFrame().setVisible(true);
                        }
                    } else {
                        JOptionPane.showMessageDialog(MainFrame.this, "Erreur : " + cause.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        }.execute();
    }

    private static String normalizeTime(String t) {
        return t.length() == 5 ? t + ":00" : t;
    }

    private static JLabel createStatValueLabel() {
        JLabel lbl = new JLabel("0", SwingConstants.CENTER);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 32));
        return lbl;
    }

    private static JPanel createStatCard(String title, JLabel valueLabel, Color accent) {
        JPanel card = new JPanel(new BorderLayout(6, 6));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(accent, 2),
                BorderFactory.createEmptyBorder(16, 16, 16, 16)
        ));
        card.setBackground(Color.WHITE);
        JLabel titleLbl = new JLabel(title, SwingConstants.CENTER);
        titleLbl.setFont(new Font("SansSerif", Font.BOLD, 14));
        titleLbl.setForeground(accent);
        valueLabel.setForeground(new Color(40, 40, 40));
        card.add(titleLbl, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        return card;
    }
}
