-- ==========================================================
-- Base de données : ReservationDB
-- Projet : Gestion des Réservations de Salles (Java Avancé)
-- ==========================================================

CREATE DATABASE IF NOT EXISTS ReservationDB
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE ReservationDB;

DROP TABLE IF EXISTS Reservation;
DROP TABLE IF EXISTS Salle;
DROP TABLE IF EXISTS Utilisateur;

-- 1. Table Utilisateur
CREATE TABLE Utilisateur (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nom VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    motDePasse VARCHAR(255) NOT NULL,
    role VARCHAR(30) NOT NULL DEFAULT 'USER'
);

-- 2. Table Salle
CREATE TABLE Salle (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nom VARCHAR(100) NOT NULL UNIQUE,
    type VARCHAR(50) NOT NULL,
    capacite INT NOT NULL,
    disponible BOOLEAN NOT NULL DEFAULT TRUE
);

-- 3. Table Reservation
CREATE TABLE Reservation (
    id INT AUTO_INCREMENT PRIMARY KEY,
    idUtilisateur INT NOT NULL,
    idSalle INT NOT NULL,
    date DATE NOT NULL,
    heureDebut TIME NOT NULL,
    heureFin TIME NOT NULL,
    motif VARCHAR(255) NOT NULL,
    statut VARCHAR(30) NOT NULL DEFAULT 'CONFIRMEE',
    CONSTRAINT fk_reservation_utilisateur
        FOREIGN KEY (idUtilisateur) REFERENCES Utilisateur(id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_reservation_salle
        FOREIGN KEY (idSalle) REFERENCES Salle(id)
        ON DELETE CASCADE ON UPDATE CASCADE
);

-- Index pour optimiser la vérification des chevauchements horaires
CREATE INDEX idx_reservation_salle_date ON Reservation (idSalle, date, statut);

-- ==========================================================
-- Données initiales (Utilisateurs et Salles)
-- ==========================================================

INSERT INTO Utilisateur (nom, email, motDePasse, role) VALUES
('EL wahidi rkia', 'r.elwahidi8548@uca.ac.ma', 'admin123', 'ADMIN'),
('Imane Alaoui', 'imane@ecole.ma', 'pass123', 'USER'),
('Salma Bennani', 'salma@ecole.ma', 'pass123', 'USER'),
('Youssef Amrani', 'youssef@ecole.ma', 'pass123', 'USER');

INSERT INTO Salle (nom, type, capacite, disponible) VALUES
('Amphi A', 'Amphithéâtre', 180, TRUE),
('Salle Info 1', 'Laboratoire Informatique', 35, TRUE),
('Salle Réunion B', 'Salle de Réunion', 16, TRUE),
('Labo Réseaux', 'Laboratoire Technique', 24, TRUE),
('Salle Conférence C', 'Conférence', 60, FALSE);

INSERT INTO Reservation (idUtilisateur, idSalle, date, heureDebut, heureFin, motif, statut) VALUES
(2, 1, '2026-12-01', '09:00:00', '11:00:00', 'Cours Java Avancé', 'CONFIRMEE'),
(3, 2, '2026-12-01', '14:00:00', '16:00:00', 'TP Réseaux & Sockets', 'CONFIRMEE'),
(4, 3, '2026-12-02', '10:00:00', '12:00:00', 'Réunion Projet PFE', 'CONFIRMEE');
