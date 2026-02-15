package services;

import entities.Cours;
import utils.MyDatabase;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CoursService implements ICoursService {
    private final Connection connection;

    public CoursService() {
        this.connection = MyDatabase.getInstance().getConnection();
    }

    @Override
    public void ajouter(Cours cours) throws SQLException {
        validateCours(cours);
        String sql = "INSERT INTO cours (titre, description, duree, niveau, competences_visees, est_obligatoire, created_by, image_couverture) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, cours.getTitre().trim());
            ps.setString(2, cours.getDescription().trim());
            ps.setInt(3, cours.getDuree());
            ps.setString(4, cours.getNiveau().trim());
            ps.setString(5, cours.getCompetences_visees().trim());
            ps.setBoolean(6, cours.isEst_obligatoire());
            ps.setInt(7, cours.getCreatedBy());
            ps.setBytes(8, cours.getImageCouverture());
            ps.executeUpdate();
        }
    }

    @Override
    public void update(Cours cours) throws SQLException {
        validateCours(cours);
        String sql = "UPDATE cours SET titre=?, description=?, duree=?, niveau=?, competences_visees=?, est_obligatoire=?, image_couverture=? WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, cours.getTitre());
            ps.setString(2, cours.getDescription());
            ps.setInt(3, cours.getDuree());
            ps.setString(4, cours.getNiveau());
            ps.setString(5, cours.getCompetences_visees());
            ps.setBoolean(6, cours.isEst_obligatoire());
            ps.setBytes(7, cours.getImageCouverture());
            ps.setInt(8, cours.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public void modifier(Cours cours) throws SQLException {
        update(cours);
    }

    @Override
    public void supprimer(int id) throws SQLException {
        connection.setAutoCommit(false);
        try {
            // 1. Supprimer les réponses des questions de quiz
            try (PreparedStatement ps = connection.prepareStatement(
                    "DELETE r FROM reponse r " +
                    "INNER JOIN question_quiz qq ON r.question_id = qq.id AND r.question_type = 'QUIZ' " +
                    "INNER JOIN module m ON qq.module_id = m.id " +
                    "WHERE m.cours_id = ?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }

            // 2. Supprimer les réponses des questions de test
            try (PreparedStatement ps = connection.prepareStatement(
                    "DELETE r FROM reponse r " +
                    "INNER JOIN question_test qt ON r.question_id = qt.id AND r.question_type = 'TEST' " +
                    "WHERE qt.cours_id = ?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }

            // 3. Supprimer les questions de quiz
            try (PreparedStatement ps = connection.prepareStatement(
                    "DELETE qq FROM question_quiz qq " +
                    "INNER JOIN module m ON qq.module_id = m.id " +
                    "WHERE m.cours_id = ?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }

            // 4. Supprimer les questions de test
            try (PreparedStatement ps = connection.prepareStatement(
                    "DELETE FROM question_test WHERE cours_id = ?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }

            // 5. Supprimer les résultats de quiz
            try (PreparedStatement ps = connection.prepareStatement(
                    "DELETE rq FROM resultat_quiz_module rq " +
                    "INNER JOIN module m ON rq.module_id = m.id " +
                    "WHERE m.cours_id = ?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }

            // 6. Supprimer les résultats de test final
            try (PreparedStatement ps = connection.prepareStatement(
                    "DELETE FROM resultat_test_cours WHERE cours_id = ?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }

            // 7. Supprimer les progressions de leçons
            try (PreparedStatement ps = connection.prepareStatement(
                    "DELETE pl FROM progression_lecon pl " +
                    "INNER JOIN lecon l ON pl.lecon_id = l.id " +
                    "INNER JOIN module m ON l.module_id = m.id " +
                    "WHERE m.cours_id = ?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }

            // 8. Supprimer les leçons
            try (PreparedStatement ps = connection.prepareStatement(
                    "DELETE l FROM lecon l " +
                    "INNER JOIN module m ON l.module_id = m.id " +
                    "WHERE m.cours_id = ?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }

            // 9. Supprimer les modules
            try (PreparedStatement ps = connection.prepareStatement(
                    "DELETE FROM module WHERE cours_id = ?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }

            // 10. Supprimer les progressions de cours
            try (PreparedStatement ps = connection.prepareStatement(
                    "DELETE FROM progression_cours WHERE cours_id = ?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }

            // 11. Supprimer les certifications
            try (PreparedStatement ps = connection.prepareStatement(
                    "DELETE FROM certification WHERE cours_id = ?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }

            // 12. Enfin supprimer le cours lui-même
            try (PreparedStatement ps = connection.prepareStatement("DELETE FROM cours WHERE id=?")) {
                ps.setInt(1, id);
                int rowsAffected = ps.executeUpdate();
                System.out.println("✅ Cours supprimé: " + rowsAffected + " ligne(s) affectée(s)");
            }

            connection.commit();
            System.out.println("✅ Transaction validée");
        } catch (SQLException e) {
            connection.rollback();
            System.err.println("❌ Erreur lors de la suppression, rollback effectué: " + e.getMessage());
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    @Override
    public List<Cours> readAll() throws SQLException {
        List<Cours> list = new ArrayList<>();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM cours")) {
            while (rs.next()) {
                list.add(mapResultSetToCours(rs));
            }
        }
        return list;
    }

    @Override
    public List<Cours> getAll() throws SQLException {
        return readAll();
    }

    @Override
    public List<Cours> readByAdmin(int userId) throws SQLException {
        List<Cours> list = new ArrayList<>();
        String sql = "SELECT * FROM cours WHERE created_by = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToCours(rs));
                }
            }
        }
        return list;
    }

    private Cours mapResultSetToCours(ResultSet rs) throws SQLException {
        return new Cours(
                rs.getInt("id"),
                rs.getString("titre"),
                rs.getString("description"),
                rs.getInt("duree"),
                rs.getString("niveau"),
                rs.getString("competences_visees"),
                rs.getBoolean("est_obligatoire"),
                rs.getInt("created_by"),
                rs.getBytes("image_couverture")
        );
    }

    private void validateCours(Cours cours) {
        // Validation du titre
        if (cours.getTitre() == null || cours.getTitre().trim().isEmpty()) {
            throw new IllegalArgumentException("Le titre est obligatoire");
        }
        if (cours.getTitre().trim().length() < 3) {
            throw new IllegalArgumentException("Le titre doit contenir au moins 3 caractères");
        }
        if (cours.getTitre().length() > 200) {
            throw new IllegalArgumentException("Le titre ne peut pas dépasser 200 caractères");
        }

        // ✅ VALIDATION LOGIQUE: Le titre ne peut pas être composé uniquement de chiffres
        if (isOnlyDigits(cours.getTitre().trim())) {
            throw new IllegalArgumentException("Le titre ne peut pas être composé uniquement de chiffres");
        }

        // ✅ VALIDATION D'UNICITÉ: Vérifier que le titre du cours est unique
        if (isTitreCourExiste(cours.getTitre().trim(), cours.getId())) {
            throw new IllegalArgumentException("Un cours avec ce titre existe déjà !");
        }

        // Validation du niveau
        if (cours.getNiveau() == null || cours.getNiveau().trim().isEmpty()) {
            throw new IllegalArgumentException("Le niveau est obligatoire");
        }

        // Validation de la durée
        if (cours.getDuree() <= 0) {
            throw new IllegalArgumentException("La durée doit être un nombre positif");
        }
        if (cours.getDuree() > 1000) {
            throw new IllegalArgumentException("La durée ne peut pas dépasser 1000 heures");
        }

        // Validation de la description
        if (cours.getDescription() == null || cours.getDescription().trim().isEmpty()) {
            throw new IllegalArgumentException("La description est obligatoire");
        }
        if (cours.getDescription().trim().length() < 10) {
            throw new IllegalArgumentException("La description doit contenir au moins 10 caractères");
        }
        if (cours.getDescription().length() > 1000) {
            throw new IllegalArgumentException("La description ne peut pas dépasser 1000 caractères");
        }

        // ✅ VALIDATION LOGIQUE: La description ne peut pas être composée uniquement de chiffres
        if (isOnlyDigits(cours.getDescription().trim())) {
            throw new IllegalArgumentException("La description ne peut pas être composée uniquement de chiffres");
        }

        // ✅ VALIDATION LOGIQUE: La description ne doit pas contenir trop de chiffres (max 30%)
        if (hasTooManyDigits(cours.getDescription().trim(), 30)) {
            throw new IllegalArgumentException("La description contient trop de chiffres (maximum 30% autorisé)");
        }

        // Validation des compétences
        if (cours.getCompetences_visees() != null && cours.getCompetences_visees().length() > 500) {
            throw new IllegalArgumentException("Les compétences ne peuvent pas dépasser 500 caractères");
        }

        // ✅ VALIDATION LOGIQUE: Les compétences ne peuvent pas être composées uniquement de chiffres
        if (cours.getCompetences_visees() != null && !cours.getCompetences_visees().trim().isEmpty()) {
            if (isOnlyDigits(cours.getCompetences_visees().trim())) {
                throw new IllegalArgumentException("Les compétences ne peuvent pas être composées uniquement de chiffres");
            }
            if (hasTooManyDigits(cours.getCompetences_visees().trim(), 30)) {
                throw new IllegalArgumentException("Les compétences contiennent trop de chiffres (maximum 30% autorisé)");
            }
        }
    }

    // ✅ MÉTHODE POUR VÉRIFIER SI UN TEXTE EST COMPOSÉ UNIQUEMENT DE CHIFFRES
    private boolean isOnlyDigits(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        // Retirer les espaces pour la vérification
        String textWithoutSpaces = text.replaceAll("\\s+", "");
        return textWithoutSpaces.matches("\\d+");
    }

    // ✅ MÉTHODE POUR VÉRIFIER SI UN TEXTE CONTIENT TROP DE CHIFFRES
    private boolean hasTooManyDigits(String text, int maxPercentage) {
        if (text == null || text.isEmpty()) {
            return false;
        }

        int totalChars = 0;
        int digitCount = 0;

        for (char c : text.toCharArray()) {
            if (!Character.isWhitespace(c)) {
                totalChars++;
                if (Character.isDigit(c)) {
                    digitCount++;
                }
            }
        }

        if (totalChars == 0) {
            return false;
        }

        double digitPercentage = (digitCount * 100.0) / totalChars;
        return digitPercentage > maxPercentage;
    }

    // ✅ MÉTHODE POUR VÉRIFIER L'UNICITÉ DU TITRE DU COURS
    private boolean isTitreCourExiste(String titre, int coursIdExclu) {
        String sql = "SELECT COUNT(*) FROM cours WHERE LOWER(titre) = LOWER(?) AND id != ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, titre);
            ps.setInt(2, coursIdExclu <= 0 ? -1 : coursIdExclu);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur vérification unicité titre cours: " + e.getMessage());
        }
        return false;
    }
}