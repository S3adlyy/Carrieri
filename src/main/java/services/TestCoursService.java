package services;

import entities.QuestionTest;
import entities.Reponse;
import entities.ResultatTestCours;
import utils.MyDatabase;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TestCoursService {
    private Connection con = MyDatabase.getInstance().getConnection();

    // Ajouter une question de test pour un cours
    public void ajouterQuestion(int coursId, String questionText, int points, int ordre, List<Reponse> reponses) {
        Connection conn = null;
        try {
            conn = MyDatabase.getInstance().getConnection();
            conn.setAutoCommit(false);

            String sql = "INSERT INTO question_test (cours_id, question_text, points, ordre) VALUES (?, ?, ?, ?)";
            PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, coursId);
            ps.setString(2, questionText);
            ps.setInt(3, points);
            ps.setInt(4, ordre);
            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();
            int questionId = 0;
            if (rs.next()) {
                questionId = rs.getInt(1);
            }

            String sqlRep = "INSERT INTO reponse (question_id, question_type, reponse_text, est_correcte, ordre) VALUES (?, 'TEST', ?, ?, ?)";
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
            System.out.println("✅ Question de test ajoutée au cours " + coursId);

        } catch (SQLException e) {
            e.printStackTrace();
            try { if (conn != null) conn.rollback(); } catch (SQLException ex) {}
        }
    }

    // Récupérer les questions d'un cours
    public List<QuestionTest> getQuestionsByCours(int coursId) {
        List<QuestionTest> list = new ArrayList<>();
        String sql = "SELECT * FROM question_test WHERE cours_id = ? ORDER BY ordre";

        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, coursId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                QuestionTest q = new QuestionTest(
                        rs.getInt("cours_id"),
                        rs.getString("question_text"),
                        rs.getInt("points"),
                        rs.getInt("ordre")
                );
                q.setId(rs.getInt("id"));
                q.setReponses(getReponsesByQuestion(q.getId(), "TEST"));
                list.add(q);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // Sauvegarder le résultat du test final
    public void sauvegarderResultat(ResultatTestCours resultat) {
        String sql = "INSERT INTO resultat_test_cours (candidat_id, cours_id, score, total_points, reussite) VALUES (?, ?, ?, ?, ?)";

        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, resultat.getCandidatId());
            ps.setInt(2, resultat.getCoursId());
            ps.setInt(3, resultat.getScore());
            ps.setInt(4, resultat.getTotalPoints());
            ps.setBoolean(5, resultat.isReussite());
            ps.executeUpdate();
            System.out.println("✅ Résultat test cours sauvegardé");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Vérifier si un cours est réussi (test final validé)
    public boolean isCoursReussi(int candidatId, int coursId) {
        String sql = "SELECT reussite FROM resultat_test_cours WHERE candidat_id = ? AND cours_id = ? ORDER BY date_completion DESC LIMIT 1";
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, candidatId);
            ps.setInt(2, coursId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getBoolean("reussite");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private List<Reponse> getReponsesByQuestion(int questionId, String type) {
        List<Reponse> list = new ArrayList<>();
        String sql = "SELECT * FROM reponse WHERE question_id = ? AND question_type = ? ORDER BY ordre";

        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, questionId);
            ps.setString(2, type);
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
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
}