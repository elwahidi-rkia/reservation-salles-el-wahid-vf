# ReservationDB — Système de Gestion des Réservations de Salles

Application client-serveur en **Java** (Swing, Sockets TCP, JDBC avec H2 / MySQL) permettant de gérer la réservation de salles au sein d'un établissement.

---

## Comptes pour consulter et tester l'application

> Les identifiants ne sont plus affichés ni pré-remplis dans l'interface de connexion pour des raisons de sécurité. Utilisez les comptes ci-dessous pour vous connecter :

| Rôle | Nom | Email | Mot de passe | Droits d'accès |
| :--- | :--- | :--- | :--- | :--- |
| **ADMIN** | EL wahidi rkia | `r.elwahidi8548@uca.ac.ma` | `admin123` | Tableau de bord, Réservations, Consultation des salles, **Administration des salles** (ajout / modification / suppression) |
| **USER** | Imane Alaoui | `imane@ecole.ma` | `pass123` | Tableau de bord, Réservations, Consultation des salles |
| **USER** | Salma Bennani | `salma@ecole.ma` | `pass123` | Tableau de bord, Réservations, Consultation des salles |
| **USER** | Youssef Amrani | `youssef@ecole.ma` | `pass123` | Tableau de bord, Réservations, Consultation des salles |

---

## Architecture du projet

* `src/com/reservation/client/` — Client TCP (`ReservationClient`)
* `src/com/reservation/server/` — Serveur TCP multi-thread (`ReservationServer`, `ClientHandler`)
* `src/com/reservation/dao/` — Couche d'accès aux données JDBC (`DatabaseConnection`, `UtilisateurDAO`, `SalleDAO`, `ReservationDAO`)
* `src/com/reservation/model/` — Modèles sérialisables (`Utilisateur`, `Salle`, `Reservation`, `DashboardStats`)
* `src/com/reservation/network/` — Protocole réseau (`Request`, `RequestType`, `Response`)
* `src/com/reservation/ui/` — Interface graphique Swing (`ClientApp`, `LoginFrame`, `MainFrame`)
* `src/com/reservation/util/` — Utilitaires (`FileManager` pour logs, export CSV et sérialisation binaire)
* `database/` — Scripts SQL (`ReservationDB.sql`)

---

## Lancement rapide (Windows)

1. **Démarrer le serveur** : double-cliquez sur `start-server.bat` (écoute par défaut sur `localhost:5000`).
2. **Démarrer le client** : double-cliquez sur `start-client.bat`.
3. **Se connecter** avec l'un des comptes listés dans le tableau ci-dessus (par défaut serveur `localhost`, port `5000`).

---

## Fonctionnalités principales

* Authentification sécurisée par rôle (`ADMIN` / `USER`)
* Tableau de bord avec statistiques en temps réel
* Gestion complète des réservations (création, modification, annulation, recherche, détection de conflits de créneaux)
* Consultation des salles disponibles
* Administration des salles (réservée au rôle `ADMIN`)
* Export des réservations au format CSV et sauvegarde/chargement binaire
