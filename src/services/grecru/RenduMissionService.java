package services.grecru;

import entities.grecru.Mission;
import entities.grecru.RenduMission;
import utils.MyDatabase;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RenduMissionService implements IRenduMissionService {

    private Connection connection;
    private AIClientService aiClient;
    public RenduMissionService() {
        this.connection = MyDatabase.getInstance().getConnection();
        this.aiClient = new AIClientService();
    }


    @Override
    public void ajouterRenduMission(RenduMission renduMission) {
        String query = "INSERT INTO rendu_mission (code_solution, date_rendu, score, resultat, mission_id, candidat_id, feedback, langue) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pst = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            pst.setString(1, renduMission.getCodeSolution());
            pst.setDate(2, renduMission.getDateRendu());
            pst.setInt(3, renduMission.getScore());
            pst.setString(4, renduMission.getResultat());
            pst.setInt(5, renduMission.getMissionId());
            pst.setInt(6, renduMission.getCandidatId());
            pst.setString(7, renduMission.getFeedback());
            pst.setString(8, renduMission.getLangue());

            pst.executeUpdate();

            ResultSet generatedKeys = pst.getGeneratedKeys();
            if (generatedKeys.next()) {
                renduMission.setId(generatedKeys.getInt(1));
            }

            System.out.println("✅ RenduMission saved with ID: " + renduMission.getId());

        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de l'ajout: " + e.getMessage());
        }
    }

    @Override
    public void modifierRenduMission(RenduMission renduMission) {
        String query = "UPDATE rendu_mission SET code_solution = ?, score = ?, resultat = ?, feedback = ? WHERE id = ?";

        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setString(1, renduMission.getCodeSolution());
            pst.setInt(2, renduMission.getScore());
            pst.setString(3, renduMission.getResultat());
            pst.setString(4, renduMission.getFeedback());
            pst.setInt(5, renduMission.getId());

            pst.executeUpdate();
            System.out.println("✅ RenduMission modifiée avec succès!");

        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la modification: " + e.getMessage());
        }
    }

    @Override
    public void supprimerRenduMission(int id) {
        String query = "DELETE FROM rendu_mission WHERE id = ?";

        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setInt(1, id);
            pst.executeUpdate();
            System.out.println("✅ RenduMission supprimée avec succès!");

        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la suppression: " + e.getMessage());
        }
    }

    @Override
    public List<RenduMission> afficherRenduMissions() {
        List<RenduMission> rendus = new ArrayList<>();
        String query = "SELECT * FROM rendu_mission ORDER BY date_rendu DESC";

        System.out.println("🔍 Executing query: " + query);

        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(query)) {

            System.out.println("✅ Query executed successfully");

            while (rs.next()) {
                RenduMission rm = createRenduMissionFromResultSet(rs);
                rendus.add(rm);
                System.out.println("  Found: id=" + rm.getId() +
                        ", score=" + rm.getScore() +
                        ", resultat=" + rm.getResultat());
            }

            System.out.println("✅ " + rendus.size() + " rendus chargés depuis la base de données");

        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la récupération: " + e.getMessage());
            e.printStackTrace();
        }

        return rendus;
    }

    @Override
    public RenduMission getRenduMissionById(int id) {
        String query = "SELECT * FROM rendu_mission WHERE id = ?";

        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setInt(1, id);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                return createRenduMissionFromResultSet(rs);
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur getRenduMissionById: " + e.getMessage());
        }

        return null;
    }

    @Override
    public List<RenduMission> getRenduMissionsByCandidat(int candidatId) {
        List<RenduMission> rendus = new ArrayList<>();
        String query = "SELECT * FROM rendu_mission WHERE candidat_id = ? ORDER BY date_rendu DESC";

        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setInt(1, candidatId);
            ResultSet rs = pst.executeQuery();

            while (rs.next()) {
                RenduMission rm = createRenduMissionFromResultSet(rs);
                rendus.add(rm);
            }

            System.out.println("✅ " + rendus.size() + " rendus trouvés pour le candidat " + candidatId);

        } catch (SQLException e) {
            System.err.println("❌ Erreur getRenduMissionsByCandidat: " + e.getMessage());
        }

        return rendus;
    }

    @Override
    public List<RenduMission> getRenduMissionsByMission(int missionId) {
        List<RenduMission> rendus = new ArrayList<>();
        String query = "SELECT * FROM rendu_mission WHERE mission_id = ? ORDER BY score DESC";

        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setInt(1, missionId);
            ResultSet rs = pst.executeQuery();

            while (rs.next()) {
                RenduMission rm = createRenduMissionFromResultSet(rs);
                rendus.add(rm);
            }

            System.out.println("✅ " + rendus.size() + " rendus trouvés pour la mission " + missionId);

        } catch (SQLException e) {
            System.err.println("❌ Erreur getRenduMissionsByMission: " + e.getMessage());
        }

        return rendus;
    }

    @Override
    public RenduMission evaluerCodePython(String code, int missionId, int candidatId) throws Exception {
        System.out.println("=".repeat(50));
        System.out.println("🧪 DÉBUT ÉVALUATION AI");
        System.out.println("=".repeat(50));

        // Get mission details
        MissionService missionService = new MissionService();
        Mission mission = missionService.getById(missionId);

        if (mission == null) {
            throw new Exception("Mission not found with ID: " + missionId);
        }

        System.out.println("📋 Mission ID: " + missionId + ", Type: " + mission.getType());
        System.out.println("📊 Minimum Score Required: " + mission.getScore_min() + "%");

        // Create RenduMission object
        RenduMission rendu = new RenduMission();
        rendu.setCandidatId(candidatId);
        rendu.setMissionId(missionId);
        rendu.setCodeSolution(code);
        rendu.setLangue("python");
        rendu.setDateRendu(new Date(System.currentTimeMillis()));

        try {
            // Call AI service
            System.out.println("🌐 Calling AI server: " + AIClientService.AI_SERVER_URL);
            long startTime = System.currentTimeMillis();

            EvaluationResponse result = aiClient.evaluatePythonCode(code, mission.getType());

            long endTime = System.currentTimeMillis();
            System.out.println("📥 Response received in " + (endTime - startTime) + "ms");
            System.out.println("✅ Score from AI: " + result.getScore() + "%");
            System.out.println("✅ Result: " + result.getResult());

            // Update rendu with AI results
            rendu.setScore(result.getScore());
            rendu.setResultat(result.getResult());

            // Check if accepted based on mission's minimum score
            boolean accepted = result.getScore() >= mission.getScore_min();

            if (accepted) {
                rendu.setFeedback("✅ Code accepted! Score: " + result.getScore() + "% meets minimum requirement of " + mission.getScore_min() + "%");
            } else {
                rendu.setFeedback("❌ Code rejected. Score: " + result.getScore() + "% below minimum requirement of " + mission.getScore_min() + "%");
            }

            // Save to database
            System.out.println("💾 Saving to database...");
            ajouterRenduMission(rendu);

            // Update candidate status if accepted
            if (accepted) {
                updateCandidateStatus(candidatId, missionId, result.getScore());
            }

            System.out.println("🎉 ÉVALUATION TERMINÉE");
            System.out.println("=".repeat(50));

        } catch (Exception e) {
            System.err.println("❌ ERREUR: " + e.getMessage());
            e.printStackTrace();

            // Don't use fallback score - throw the exception to the controller
            throw new Exception("AI Evaluation failed: " + e.getMessage());
        }

        return rendu;
    }

    private String getMissionType(int missionId) {
        // First try to get type from database
        String query = "SELECT type FROM mission WHERE id = ?";

        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setInt(1, missionId);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                String type = rs.getString("type");
                if (type != null && !type.trim().isEmpty()) {
                    return type;
                }
            }

        } catch (SQLException e) {
            System.out.println("ℹ️ Colonne 'type' non trouvée dans la table mission, utilisation du mapping par défaut");
        }

        // If type column doesn't exist or is empty, use mapping based on mission ID
        return mapMissionIdToType(missionId);
    }

    private String mapMissionIdToType(int missionId) {
        // Map mission IDs to types
        switch (missionId) {
            case 1: return "ADDITION";
            case 2: return "FACTORIAL";
            case 3: return "FIBONACCI";
            case 4: return "PRIME_CHECK";
            default: return "ADDITION"; // default
        }
    }

    private void updateCandidateStatus(int candidatId, int missionId, int score) {
        // Check if table exists
        if (!tableExists("candidat_mission")) {
            createCandidatMissionTable();
        }

        String query = "INSERT INTO candidat_mission (candidat_id, mission_id, status, score) " +
                "VALUES (?, ?, 'ACCEPTED', ?) " +
                "ON DUPLICATE KEY UPDATE status = 'ACCEPTED', score = ?";

        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setInt(1, candidatId);
            pst.setInt(2, missionId);
            pst.setInt(3, score);
            pst.setInt(4, score);

            pst.executeUpdate();
            System.out.println("✅ Candidate status updated for mission " + missionId);

        } catch (SQLException e) {
            System.err.println("❌ Error updating candidate status: " + e.getMessage());
        }
    }

    private void tryAlternativeUpdate(int candidatId, int missionId, int score) {
        try {
            // First try to update
            String updateQuery = "UPDATE candidat_mission SET status = 'ACCEPTED', score = ? " +
                    "WHERE candidat_id = ? AND mission_id = ?";
            PreparedStatement pst = connection.prepareStatement(updateQuery);
            pst.setInt(1, score);
            pst.setInt(2, candidatId);
            pst.setInt(3, missionId);

            int rows = pst.executeUpdate();

            if (rows == 0) {
                // If no rows updated, try to insert
                String insertQuery = "INSERT INTO candidat_mission (candidat_id, mission_id, status, score) " +
                        "VALUES (?, ?, 'ACCEPTED', ?)";
                pst = connection.prepareStatement(insertQuery);
                pst.setInt(1, candidatId);
                pst.setInt(2, missionId);
                pst.setInt(3, score);
                pst.executeUpdate();
            }

            System.out.println("✅ Mise à jour alternative réussie");

        } catch (SQLException ex) {
            System.err.println("❌ Échec de la mise à jour alternative: " + ex.getMessage());
        }
    }

    private boolean tableExists(String tableName) {
        String query = "SELECT COUNT(*) FROM information_schema.tables " +
                "WHERE table_schema = DATABASE() AND table_name = ?";

        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setString(1, tableName);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("❌ Error checking table existence: " + e.getMessage());
        }
        return false;
    }


    private void createCandidatMissionTable() {
        String query = "CREATE TABLE IF NOT EXISTS candidat_mission (" +
                "id INT PRIMARY KEY AUTO_INCREMENT, " +
                "candidat_id INT NOT NULL, " +
                "mission_id INT NOT NULL, " +
                "status VARCHAR(20) DEFAULT 'PENDING', " +
                "score INT, " +
                "date_affectation TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "UNIQUE KEY unique_candidat_mission (candidat_id, mission_id)" +
                ")";

        try (Statement st = connection.createStatement()) {
            st.executeUpdate(query);
            System.out.println("✅ Table candidat_mission created");
        } catch (SQLException e) {
            System.err.println("❌ Error creating table: " + e.getMessage());
        }
    }

    private RenduMission createRenduMissionFromResultSet(ResultSet rs) throws SQLException {
        RenduMission rm = new RenduMission();
        rm.setId(rs.getInt("id"));
        rm.setCodeSolution(rs.getString("code_solution"));
        rm.setDateRendu(rs.getDate("date_rendu"));
        rm.setScore(rs.getInt("score"));
        rm.setResultat(rs.getString("resultat"));
        rm.setMissionId(rs.getInt("mission_id"));
        rm.setCandidatId(rs.getInt("candidat_id"));

        // Handle nullable columns
        try {
            rm.setFeedback(rs.getString("feedback"));
        } catch (SQLException e) {
            rm.setFeedback(null);
        }

        try {
            rm.setLangue(rs.getString("langue"));
        } catch (SQLException e) {
            rm.setLangue("python");
        }

        return rm;
    }

    // Helper method to test AI connection
    public void testAIConnection() {
        System.out.println("🔍 Test de connexion au serveur AI...");
        AIEvaluationService aiService = new AIEvaluationService();

        try {
            String testCode = "def add(a, b):\n    return a + b\n\nprint('test')";
            EvaluationResponse response = aiService.evaluatePythonCode(testCode, "ADDITION");
            System.out.println("✅ Connexion AI réussie! Score: " + response.getScore() + "%, Result: " + response.getResult());
        } catch (Exception e) {
            System.err.println("❌ Échec de connexion AI: " + e.getMessage());
            System.out.println("💡 Vérifiez que le serveur Python est démarré: python app.py");
            System.out.println("💡 URL du serveur: http://127.0.0.1:5000");
        }
    }

    // Helper method to test database connection
    public void testDatabaseConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                System.out.println("✅ Connexion base de données OK");

                // Test table existence
                String[] tables = {"rendu_mission", "mission", "user"};
                for (String table : tables) {
                    if (tableExists(table)) {
                        System.out.println("   ✅ Table '" + table + "' existe");
                    } else {
                        System.out.println("   ⚠️ Table '" + table + "' n'existe pas");
                    }
                }
            } else {
                System.out.println("❌ Connexion base de données fermée");
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur testDatabaseConnection: " + e.getMessage());
        }
    }

    // Add this diagnostic method
    public void diagnosticCheck() {
        System.out.println("=".repeat(60));
        System.out.println("🔍 DIAGNOSTIC CHECK");
        System.out.println("=".repeat(60));

        try {
            // 1. Check connection
            if (connection != null && !connection.isClosed()) {
                System.out.println("✅ Database connection: OK");
            } else {
                System.out.println("❌ Database connection: FAILED");
                return;
            }

            // 2. Check if table exists and count records
            String countQuery = "SELECT COUNT(*) as total FROM rendu_mission";
            try (Statement st = connection.createStatement();
                 ResultSet rs = st.executeQuery(countQuery)) {

                if (rs.next()) {
                    int count = rs.getInt("total");
                    System.out.println("📊 Records in rendu_mission table: " + count);
                }
            }

            // 3. Show sample data
            String sampleQuery = "SELECT * FROM rendu_mission LIMIT 3";
            try (Statement st = connection.createStatement();
                 ResultSet rs = st.executeQuery(sampleQuery)) {

                System.out.println("\n📋 Sample data:");
                int rowNum = 0;
                while (rs.next()) {
                    rowNum++;
                    System.out.println("  Row " + rowNum + ":");
                    System.out.println("    id: " + rs.getInt("id"));
                    System.out.println("    score: " + rs.getInt("score"));
                    System.out.println("    resultat: " + rs.getString("resultat"));
                    System.out.println("    mission_id: " + rs.getInt("mission_id"));
                    System.out.println("    candidat_id: " + rs.getInt("candidat_id"));
                    System.out.println("    date_rendu: " + rs.getDate("date_rendu"));
                }

                if (rowNum == 0) {
                    System.out.println("  ⚠️ No data found in rendu_mission table");
                }
            }

            // 4. Check if the table structure matches
            System.out.println("\n📋 Table structure:");
            String structureQuery = "DESCRIBE rendu_mission";
            try (Statement st = connection.createStatement();
                 ResultSet rs = st.executeQuery(structureQuery)) {

                while (rs.next()) {
                    System.out.println("  " + rs.getString("Field") + " - " + rs.getString("Type"));
                }
            }

        } catch (SQLException e) {
            System.err.println("❌ Diagnostic error: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println("=".repeat(60));
    }
}