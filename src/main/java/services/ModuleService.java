package services;

import entities.Module;
import utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ModuleService implements IModuleService {  // ← AJOUTER implements

    private Connection con = MyDatabase.getInstance().getConnection();


    @Override  // ← AJOUTER @Override
    public void ajouter(Module module) {
        validateModule(module);
        String sql = "INSERT INTO module (titre, description, ordre, cours_id) VALUES (?, ?, ?, ?)";

        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, module.getTitre().trim());
            ps.setString(2, module.getDescription().trim());
            ps.setInt(3, module.getOrdre());
            ps.setInt(4, module.getCoursId());
            int rows = ps.executeUpdate();
            if (rows > 0) {
                System.out.println("✅ Module ajouté: " + module.getTitre());
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur ajout module: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de l'ajout du module: " + e.getMessage(), e);
        }
    }

    @Override
    public void modifier(Module module) {
        validateModule(module);
        String sql = "UPDATE module SET titre=?, description=?, ordre=?, cours_id=? WHERE id=?";

        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, module.getTitre().trim());
            ps.setString(2, module.getDescription().trim());
            ps.setInt(3, module.getOrdre());
            ps.setInt(4, module.getCoursId());
            ps.setInt(5, module.getId());
            int rows = ps.executeUpdate();
            if (rows > 0) {
                System.out.println("✅ Module modifié: " + module.getId());
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur modification module: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de la modification du module: " + e.getMessage(), e);
        }
    }

    @Override
    public void supprimer(int id) {
        Connection conn = null;
        try {
            conn = MyDatabase.getInstance().getConnection();
            conn.setAutoCommit(false); // Transaction

            // 1. Supprimer les réponses des questions de quiz du module
            String deleteReponsesQuiz = "DELETE r FROM reponse r " +
                    "INNER JOIN question_quiz qq ON r.question_id = qq.id AND r.question_type = 'QUIZ' " +
                    "WHERE qq.module_id = ?";
            try (PreparedStatement ps = conn.prepareStatement(deleteReponsesQuiz)) {
                ps.setInt(1, id);
                int reponsesSupprimees = ps.executeUpdate();
                System.out.println("📝 Réponses de quiz supprimées du module " + id + ": " + reponsesSupprimees);
            }

            // 2. Supprimer les questions de quiz du module
            String deleteQuestionsQuiz = "DELETE FROM question_quiz WHERE module_id = ?";
            try (PreparedStatement ps = conn.prepareStatement(deleteQuestionsQuiz)) {
                ps.setInt(1, id);
                int questionsSupprimees = ps.executeUpdate();
                System.out.println("❓ Questions de quiz supprimées du module " + id + ": " + questionsSupprimees);
            }

            // 3. Supprimer les leçons du module
            String deleteLecons = "DELETE FROM lecon WHERE module_id=?";
            try (PreparedStatement ps1 = conn.prepareStatement(deleteLecons)) {
                ps1.setInt(1, id);
                int leconsSupprimees = ps1.executeUpdate();
                System.out.println("📚 Leçons supprimées du module " + id + ": " + leconsSupprimees);
            }

            // 4. Supprimer le module
            String deleteModule = "DELETE FROM module WHERE id=?";
            try (PreparedStatement ps2 = conn.prepareStatement(deleteModule)) {
                ps2.setInt(1, id);
                int rows = ps2.executeUpdate();
                if (rows > 0) {
                    System.out.println("✅ Module supprimé: " + id);
                }
            }

            conn.commit(); // Valider la transaction
            System.out.println("✅ Transaction validée");

        } catch (SQLException e) {
            System.err.println("❌ Erreur suppression module: " + e.getMessage());
            try {
                if (conn != null) conn.rollback(); // Annuler en cas d'erreur
                System.err.println("⏪ Transaction annulée");
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            e.printStackTrace();
        }
    }

    @Override
    public List<Module> getAll() {
        List<Module> list = new ArrayList<>();
        String sql = "SELECT * FROM module ORDER BY cours_id, ordre";

        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                list.add(new Module(
                        rs.getInt("id"),
                        rs.getString("titre"),
                        rs.getString("description"),
                        rs.getInt("ordre"),
                        rs.getInt("cours_id")
                ));
            }
            System.out.println("📚 Modules chargés: " + list.size());

        } catch (SQLException e) {
            System.err.println("❌ Erreur getAll modules: " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public List<Module> getModulesByCours(int coursId) {
        List<Module> list = new ArrayList<>();
        String sql = "SELECT * FROM module WHERE cours_id=? ORDER BY ordre";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, coursId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Module(
                            rs.getInt("id"),
                            rs.getString("titre"),
                            rs.getString("description"),
                            rs.getInt("ordre"),
                            rs.getInt("cours_id")
                    ));
                }
            }
            System.out.println("📚 Modules chargés pour cours " + coursId + ": " + list.size());

        } catch (SQLException e) {
            System.err.println("❌ Erreur getModulesByCours: " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }
    // Dans ModuleService.java, ajoutez cette méthode :

    public Module getModuleById(int id) {
        String sql = "SELECT * FROM module WHERE id = ?";
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new Module(
                        rs.getInt("id"),
                        rs.getString("titre"),
                        rs.getString("description"),
                        rs.getInt("ordre"),
                        rs.getInt("cours_id")
                );
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur getModuleById: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    // ============================================
    // VALIDATION
    // ============================================
    private void validateModule(Module module) {
        // Validation du titre
        if (module.getTitre() == null || module.getTitre().trim().isEmpty()) {
            throw new IllegalArgumentException("Le titre du module est obligatoire");
        }
        if (module.getTitre().trim().length() < 3) {
            throw new IllegalArgumentException("Le titre doit contenir au moins 3 caractères");
        }
        if (module.getTitre().length() > 200) {
            throw new IllegalArgumentException("Le titre ne peut pas dépasser 200 caractères");
        }

        // ✅ VALIDATION LOGIQUE: Le titre ne peut pas être composé uniquement de chiffres
        if (isOnlyDigits(module.getTitre().trim())) {
            throw new IllegalArgumentException("Le titre ne peut pas être composé uniquement de chiffres");
        }

        // ✅ VALIDATION D'UNICITÉ: Vérifier que le titre du module est unique dans le cours
        if (isTitreModuleExiste(module.getTitre().trim(), module.getCoursId(), module.getId())) {
            throw new IllegalArgumentException("Un module avec ce titre existe déjà dans ce cours !");
        }

        // Validation de la description
        if (module.getDescription() == null || module.getDescription().trim().isEmpty()) {
            throw new IllegalArgumentException("La description est obligatoire");
        }
        if (module.getDescription().trim().length() < 10) {
            throw new IllegalArgumentException("La description doit contenir au moins 10 caractères");
        }
        if (module.getDescription().length() > 1000) {
            throw new IllegalArgumentException("La description ne peut pas dépasser 1000 caractères");
        }

        // ✅ VALIDATION LOGIQUE: La description ne peut pas être composée uniquement de chiffres
        if (isOnlyDigits(module.getDescription().trim())) {
            throw new IllegalArgumentException("La description ne peut pas être composée uniquement de chiffres");
        }

        // ✅ VALIDATION LOGIQUE: La description ne doit pas contenir trop de chiffres (max 30%)
        if (hasTooManyDigits(module.getDescription().trim(), 30)) {
            throw new IllegalArgumentException("La description contient trop de chiffres (maximum 30% autorisé)");
        }

        // Validation de l'ordre
        if (module.getOrdre() <= 0) {
            throw new IllegalArgumentException("L'ordre doit être un nombre positif");
        }
        if (module.getOrdre() > 100) {
            throw new IllegalArgumentException("L'ordre ne peut pas dépasser 100");
        }

        // Validation du cours ID
        if (module.getCoursId() <= 0) {
            throw new IllegalArgumentException("Le module doit être associé à un cours valide");
        }
    }

    // ✅ MÉTHODE POUR VÉRIFIER SI UN TEXTE EST COMPOSÉ UNIQUEMENT DE CHIFFRES
    private boolean isOnlyDigits(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
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

    // ✅ MÉTHODE POUR VÉRIFIER L'UNICITÉ DU TITRE DU MODULE DANS UN COURS
    private boolean isTitreModuleExiste(String titre, int coursId, int moduleIdExclu) {
        String sql = "SELECT COUNT(*) FROM module WHERE LOWER(titre) = LOWER(?) AND cours_id = ? AND id != ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, titre);
            ps.setInt(2, coursId);
            ps.setInt(3, moduleIdExclu <= 0 ? -1 : moduleIdExclu);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur vérification unicité titre module: " + e.getMessage());
        }
        return false;
    }
}

