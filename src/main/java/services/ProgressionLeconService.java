package services;

import entities.Lecon;
import utils.MyDatabase;
import java.sql.*;
import java.util.List;
import entities.Module;

public class ProgressionLeconService implements IProgressionLeconService {

    Connection con = MyDatabase.getInstance().getConnection();

    @Override
    public void marquerTerminee(int candidatId, int leconId) {
        // Validation
        if (candidatId <= 0) {
            throw new IllegalArgumentException("ID candidat invalide");
        }
        if (leconId <= 0) {
            throw new IllegalArgumentException("ID leçon invalide");
        }

        System.out.println("🔵 SERVICE: marquerTerminee - candidat=" + candidatId + ", leçon=" + leconId);

        String sql = "INSERT INTO progression_lecon (candidat_id, lecon_id, termine) " +
                "VALUES (?, ?, 1) " +
                "ON DUPLICATE KEY UPDATE termine = 1, date_validation = CURRENT_TIMESTAMP";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, candidatId);
            ps.setInt(2, leconId);
            int rows = ps.executeUpdate();
            System.out.println("🔵 SERVICE: " + rows + " ligne(s) affectée(s)");

            if (rows > 0) {
                System.out.println("✅ SERVICE: Leçon " + leconId + " marquée terminée avec succès");
            } else {
                System.out.println("⚠️ SERVICE: Aucune ligne affectée");
            }
        } catch (SQLException e) {
            System.err.println("❌ SERVICE ERROR marquerTerminee: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public boolean isLeconTerminee(int candidatId, int leconId) {
        String sql = "SELECT termine FROM progression_lecon WHERE candidat_id = ? AND lecon_id = ?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, candidatId);
            ps.setInt(2, leconId);

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                boolean termine = rs.getBoolean("termine");
                System.out.println("🔵 SERVICE: Leçon " + leconId + " termine = " + termine);
                return termine;
            } else {
                System.out.println("🔵 SERVICE: Leçon " + leconId + " non trouvée dans progression_lecon");
            }
        } catch (SQLException e) {
            System.err.println("❌ SERVICE ERROR isLeconTerminee: " + e.getMessage());
        }
        return false;
    }

    @Override
    public double getProgressionCours(int candidatId, int coursId) {
        System.out.println("SERVICE: Calcul progression COMPLETE cours " + coursId + " pour candidat " + candidatId);

        List<Module> modules = new ModuleService().getModulesByCours(coursId);

        int totalElements = 0;
        int elementsTermines = 0;

        for (Module m : modules) {
            // ✅ 1. Compter les leçons
            List<Lecon> lecons = new LeconService().getLeconsByModule(m.getId());
            totalElements += lecons.size();

            for (Lecon l : lecons) {
                if (isLeconTerminee(candidatId, l.getId())) {
                    elementsTermines++;
                }
            }

            // ✅ 2. Compter le quiz du module (1 élément)
            if (aDesQuestionsQuiz(m.getId())) {
                totalElements += 1; // Le quiz compte comme 1 élément
                if (isModuleReussi(candidatId, m.getId())) {
                    elementsTermines++;
                }
            }
        }

        // ✅ 3. Compter le test final du cours (1 élément)
        if (aDesQuestionsTest(coursId)) {
            totalElements += 1;
            if (isTestFinalReussi(candidatId, coursId)) {
                elementsTermines++;
            }
        }

        System.out.println("   Total éléments: " + totalElements + " | Terminés: " + elementsTermines);

        if (totalElements == 0) return 0;

        double progression = ((double) elementsTermines / totalElements) * 100;
        System.out.println("   Progression COMPLETE: " + progression + "%");

        return progression;
    }
    // ✅ Vérifier si un module a des questions de quiz
    private boolean aDesQuestionsQuiz(int moduleId) {
        String sql = "SELECT COUNT(*) FROM question_quiz WHERE module_id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, moduleId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // ✅ Vérifier si un cours a des questions de test
    private boolean aDesQuestionsTest(int coursId) {
        String sql = "SELECT COUNT(*) FROM question_test WHERE cours_id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, coursId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // ✅ Vérifier si un module est réussi (quiz)
    private boolean isModuleReussi(int candidatId, int moduleId) {
        QuizModuleService qms = new QuizModuleService();
        return qms.isModuleReussi(candidatId, moduleId);
    }

    // ✅ Vérifier si le test final est réussi
    private boolean isTestFinalReussi(int candidatId, int coursId) {
        TestCoursService tcs = new TestCoursService();
        return tcs.isCoursReussi(candidatId, coursId);
    }
}