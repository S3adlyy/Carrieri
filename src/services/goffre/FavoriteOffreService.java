package services.goffre;

import entities.goffre.FavoriteOffre;
import entities.goffre.OffreEmploi;
import utils.MyDatabase;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FavoriteOffreService {
    private final Connection connection;
    private final OffreEmploiService offreService;

    public FavoriteOffreService() {
        this.connection = MyDatabase.getInstance().getConnection();
        this.offreService = new OffreEmploiService();
        createTableIfNotExists();
    }

    /**
     * Créer la table favorites_offres si elle n'existe pas
     */
    private void createTableIfNotExists() {
        String sql = "CREATE TABLE IF NOT EXISTS favorites_offres (" +
                "id INT PRIMARY KEY AUTO_INCREMENT, " +
                "candidat_id INT NOT NULL, " +
                "offre_id INT NOT NULL, " +
                "date_ajout DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                "UNIQUE KEY unique_favorite (candidat_id, offre_id), " +
                "FOREIGN KEY (offre_id) REFERENCES offre_emploi(id) ON DELETE CASCADE" +
                ")";

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
            System.out.println("✅ Table favorites_offres vérifiée/créée");
        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la création de la table favorites_offres: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Ajouter une offre aux favoris
     */
    public boolean ajouterFavori(int candidatId, int offreId) {
        // Vérifier si le favori existe déjà
        if (isFavorite(candidatId, offreId)) {
            System.out.println("⚠️ Cette offre est déjà dans les favoris");
            return false;
        }

        String sql = "INSERT INTO favorites_offres (candidat_id, offre_id, date_ajout) VALUES (?, ?, ?)";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, candidatId);
            ps.setInt(2, offreId);
            ps.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));

            int result = ps.executeUpdate();
            if (result > 0) {
                System.out.println("✅ Offre ajoutée aux favoris");
                return true;
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de l'ajout du favori: " + e.getMessage());
        }
        return false;
    }

    /**
     * Retirer une offre des favoris
     */
    public boolean retirerFavori(int candidatId, int offreId) {
        String sql = "DELETE FROM favorites_offres WHERE candidat_id = ? AND offre_id = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, candidatId);
            ps.setInt(2, offreId);

            int result = ps.executeUpdate();
            if (result > 0) {
                System.out.println("✅ Offre retirée des favoris");
                return true;
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la suppression du favori: " + e.getMessage());
        }
        return false;
    }

    /**
     * Basculer l'état favori (ajouter ou retirer)
     */
    public boolean toggleFavori(int candidatId, int offreId) {
        if (isFavorite(candidatId, offreId)) {
            return retirerFavori(candidatId, offreId);
        } else {
            return ajouterFavori(candidatId, offreId);
        }
    }

    /**
     * Vérifier si une offre est dans les favoris
     */
    public boolean isFavorite(int candidatId, int offreId) {
        String sql = "SELECT COUNT(*) FROM favorites_offres WHERE candidat_id = ? AND offre_id = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, candidatId);
            ps.setInt(2, offreId);

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la vérification du favori: " + e.getMessage());
        }
        return false;
    }

    /**
     * Obtenir tous les favoris d'un candidat
     */
    public List<FavoriteOffre> getFavoritesByCandidat(int candidatId) {
        List<FavoriteOffre> favorites = new ArrayList<>();
        String sql = "SELECT * FROM favorites_offres WHERE candidat_id = ? ORDER BY date_ajout DESC";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, candidatId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                FavoriteOffre favorite = new FavoriteOffre(
                    rs.getInt("id"),
                    rs.getInt("candidat_id"),
                    rs.getInt("offre_id"),
                    rs.getTimestamp("date_ajout").toLocalDateTime()
                );

                // Charger l'offre associée
                try {
                    OffreEmploi offre = offreService.findById(favorite.getOffreId());
                    favorite.setOffre(offre);
                } catch (SQLException ex) {
                    System.err.println("⚠️ Impossible de charger l'offre " + favorite.getOffreId() + ": " + ex.getMessage());
                }

                favorites.add(favorite);
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la récupération des favoris: " + e.getMessage());
        }

        return favorites;
    }

    /**
     * Obtenir les IDs de toutes les offres favorites d'un candidat (pour optimisation)
     */
    public Set<Integer> getFavoriteOffreIds(int candidatId) {
        Set<Integer> favoriteIds = new HashSet<>();
        String sql = "SELECT offre_id FROM favorites_offres WHERE candidat_id = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, candidatId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                favoriteIds.add(rs.getInt("offre_id"));
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la récupération des IDs favoris: " + e.getMessage());
        }

        return favoriteIds;
    }

    /**
     * Obtenir le nombre de favoris d'un candidat
     */
    public int countFavorites(int candidatId) {
        String sql = "SELECT COUNT(*) FROM favorites_offres WHERE candidat_id = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, candidatId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur lors du comptage des favoris: " + e.getMessage());
        }
        return 0;
    }

    /**
     * Supprimer tous les favoris d'un candidat
     */
    public boolean clearFavorites(int candidatId) {
        String sql = "DELETE FROM favorites_offres WHERE candidat_id = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, candidatId);
            ps.executeUpdate();
            System.out.println("✅ Tous les favoris supprimés");
            return true;
        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la suppression de tous les favoris: " + e.getMessage());
            return false;
        }
    }
}

