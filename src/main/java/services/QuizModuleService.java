package services;

import entities.QuestionQuiz;
import entities.Reponse;
import entities.ResultatQuizModule;
import utils.MyDatabase;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class QuizModuleService implements IQuizModuleService {
    private Connection con = MyDatabase.getInstance().getConnection();

    // Ajouter une question de quiz pour un module
    public void ajouterQuestion(int moduleId, String questionText, int points, int ordre, List<Reponse> reponses) {
        Connection conn = null;
        try {
            conn = MyDatabase.getInstance().getConnection();
            conn.setAutoCommit(false);

            String sql = "INSERT INTO question_quiz (module_id, question_text, points, ordre) VALUES (?, ?, ?, ?)";
            PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, moduleId);
            ps.setString(2, questionText);
            ps.setInt(3, points);
            ps.setInt(4, ordre);
            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();
            int questionId = 0;
            if (rs.next()) {
                questionId = rs.getInt(1);
            }

            String sqlRep = "INSERT INTO reponse (question_id, question_type, reponse_text, est_correcte, ordre) VALUES (?, 'QUIZ', ?, ?, ?)";
            PreparedStatement psRep = conn.prepareStatement(sqlRep);

            for (Reponse r : reponses) {
                psRep.setInt(1, questionId);
                psRep.setString(2, r.getReponseText());
                psRep.setBoolean(3, r.isEstCorrecte());
                psRep.setInt(4, r.getOrdre());
                psRep.addBatch();
            }
            psRep.executeBatch();

            conn.commit();
            System.out.println("✅ Question de quiz ajoutée au module " + moduleId);

        } catch (SQLException e) {
            e.printStackTrace();
            try { if (conn != null) conn.rollback(); } catch (SQLException ex) {}
        }
    }

    // ✅ RÉCUPÉRER LES QUESTIONS D'UN MODULE - CORRIGÉ
    public List<QuestionQuiz> getQuestionsByModule(int moduleId) {
        List<QuestionQuiz> list = new ArrayList<>();
        String sql = "SELECT * FROM question_quiz WHERE module_id = ? ORDER BY ordre";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, moduleId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                QuestionQuiz q = new QuestionQuiz(
                        rs.getInt("module_id"),
                        rs.getString("question_text"),
                        rs.getInt("points"),
                        rs.getInt("ordre")
                );
                q.setId(rs.getInt("id"));

                // ✅ FORCER LE CHARGEMENT DES RÉPONSES
                List<Reponse> reponses = getReponsesByQuestion(q.getId());
                q.setReponses(reponses);

                System.out.println("📌 Question " + q.getId() + " chargée avec " + reponses.size() + " réponses");
                list.add(q);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        System.out.println("📚 Total: " + list.size() + " questions pour module " + moduleId);
        return list;
    }

    // ✅ RÉCUPÉRER LES RÉPONSES D'UNE QUESTION - CORRIGÉ
    private List<Reponse> getReponsesByQuestion(int questionId) {
        List<Reponse> list = new ArrayList<>();
        String sql = "SELECT * FROM reponse WHERE question_id = ? AND question_type = 'QUIZ' ORDER BY ordre";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, questionId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Reponse r = new Reponse(
                        rs.getInt("question_id"),
                        rs.getString("question_type"),
                        rs.getString("reponse_text"),
                        rs.getBoolean("est_correcte"),
                        rs.getInt("ordre")
                );
                r.setId(rs.getInt("id"));
                list.add(r);
                System.out.println("   ➤ Réponse: " + r.getReponseText() + " (correcte: " + r.isEstCorrecte() + ")");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // ✅ VÉRIFIER SI UN MODULE EST RÉUSSI
    public boolean isModuleReussi(int candidatId, int moduleId) {
        String sql = "SELECT reussite FROM resultat_quiz_module WHERE candidat_id = ? AND module_id = ? ORDER BY date_completion DESC LIMIT 1";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, candidatId);
            ps.setInt(2, moduleId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                boolean reussite = rs.getBoolean("reussite");
                System.out.println("🔍 Vérification module " + moduleId +
                        " pour candidat " + candidatId + " = " + reussite);
                return reussite;
            } else {
                System.out.println("🔍 Aucun résultat pour module " + moduleId);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // ✅ SAUVEGARDER LE RÉSULTAT D'UN QUIZ
    public void sauvegarderResultat(ResultatQuizModule resultat) {
        String sql = "INSERT INTO resultat_quiz_module (candidat_id, module_id, score, total_points, reussite) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, resultat.getCandidatId());
            ps.setInt(2, resultat.getModuleId());
            ps.setInt(3, resultat.getScore());
            ps.setInt(4, resultat.getTotalPoints());
            ps.setBoolean(5, resultat.isReussite());
            ps.executeUpdate();
            System.out.println("✅ Résultat sauvegardé - Module " + resultat.getModuleId() +
                    " Score: " + resultat.getScore() + "/" + resultat.getTotalPoints() +
                    " Réussite: " + resultat.isReussite());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}