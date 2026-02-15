package services;

import entities.Mission;
import utils.MyDatabase;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MissionService implements IMissionService<Mission> {
    public Connection connection;

    public MissionService() {
        connection = MyDatabase.getInstance().getConnection();
        System.out.println("Connection to Carrieri Database is established without any probleme");
    }

    @Override
    public void ajouter(Mission mission) throws SQLException {
        // Updated SQL to include type
        String sql = "INSERT INTO mission (description, type, score_min, created_at, created_by_id) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            preparedStatement.setString(1, mission.getDescription());
            preparedStatement.setString(2, mission.getType());  // Add type
            preparedStatement.setInt(3, mission.getScore_min());
            preparedStatement.setTimestamp(4, Timestamp.valueOf(mission.getCreated_at()));
            preparedStatement.setObject(5, mission.getCreated_by_id(), Types.INTEGER);

            preparedStatement.executeUpdate();

            // Get generated ID
            ResultSet generatedKeys = preparedStatement.getGeneratedKeys();
            if (generatedKeys.next()) {
                mission.setId(generatedKeys.getInt(1));
            }

            System.out.println("Mission ajoutée avec succès - Type: " + mission.getType());
        }
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM mission WHERE id = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, id);
            preparedStatement.executeUpdate();
            System.out.println("Mission supprimée avec succès");
        }
    }

    @Override
    public void update(Mission mission) throws SQLException {
        // Updated SQL to include type
        String sql = "UPDATE mission SET description = ?, type = ?, score_min = ?, created_by_id = ? WHERE id = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, mission.getDescription());
            preparedStatement.setString(2, mission.getType());  // Add type
            preparedStatement.setInt(3, mission.getScore_min());
            preparedStatement.setObject(4, mission.getCreated_by_id(), Types.INTEGER);
            preparedStatement.setInt(5, mission.getId());

            preparedStatement.executeUpdate();
            System.out.println("Mission mise à jour avec succès - Type: " + mission.getType());
        }
    }

    @Override
    public List<Mission> read() throws SQLException {
        List<Mission> missions = new ArrayList<>();
        String sql = "SELECT * FROM mission";
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {

            while (resultSet.next()) {
                Mission mission = extractMissionFromResultSet(resultSet);
                missions.add(mission);
            }
        }
        return missions;
    }

    // Get mission by ID
    public Mission getById(int id) throws SQLException {
        String sql = "SELECT * FROM mission WHERE id = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, id);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return extractMissionFromResultSet(resultSet);
                }
            }
        }
        return null;
    }

    // Helper method to extract mission from ResultSet
    private Mission extractMissionFromResultSet(ResultSet resultSet) throws SQLException {
        Mission mission = new Mission();
        mission.setId(resultSet.getInt("id"));
        mission.setDescription(resultSet.getString("description"));

        // Handle type column (might be null in old records)
        try {
            mission.setType(resultSet.getString("type"));
        } catch (SQLException e) {
            mission.setType("ADDITION"); // Default type if column doesn't exist
        }

        mission.setScore_min(resultSet.getInt("score_min"));

        // Handle created_at
        Timestamp timestamp = resultSet.getTimestamp("created_at");
        if (timestamp != null) {
            mission.setCreated_at(timestamp.toLocalDateTime());
        }

        // Handle created_by_id
        mission.setCreated_by_id(resultSet.getObject("created_by_id", Integer.class));

        return mission;
    }

    // Initialize default missions if they don't exist
    public void initializeDefaultMissions() throws SQLException {
        List<Mission> existing = read();
        if (existing.isEmpty()) {
            System.out.println("📋 Creating default missions...");

            Mission mission1 = new Mission("Écrire une fonction qui additionne deux nombres", 60, 1);
            mission1.setType("ADDITION");
            ajouter(mission1);

            Mission mission2 = new Mission("Calculer la factorielle d'un nombre", 70, 1);
            mission2.setType("FACTORIAL");
            ajouter(mission2);

            Mission mission3 = new Mission("Calculer le n-ième nombre de Fibonacci", 70, 1);
            mission3.setType("FIBONACCI");
            ajouter(mission3);

            Mission mission4 = new Mission("Vérifier si un nombre est premier", 65, 1);
            mission4.setType("PRIME_CHECK");
            ajouter(mission4);

            System.out.println("✅ Default missions created!");
        } else {
            // Check if existing missions have type set, if not update them
            for (Mission mission : existing) {
                if (mission.getType() == null || mission.getType().isEmpty()) {
                    // Assign type based on description
                    String desc = mission.getDescription().toLowerCase();
                    if (desc.contains("addition") || desc.contains("add")) {
                        mission.setType("ADDITION");
                    } else if (desc.contains("factoriel") || desc.contains("factorial")) {
                        mission.setType("FACTORIAL");
                    } else if (desc.contains("fibonacci")) {
                        mission.setType("FIBONACCI");
                    } else if (desc.contains("premier") || desc.contains("prime")) {
                        mission.setType("PRIME_CHECK");
                    } else {
                        mission.setType("ADDITION"); // Default
                    }
                    update(mission);
                    System.out.println("✅ Updated mission " + mission.getId() + " with type: " + mission.getType());
                }
            }
        }
    }

    // Get missions by type
    public List<Mission> getByType(String type) throws SQLException {
        List<Mission> missions = new ArrayList<>();
        String sql = "SELECT * FROM mission WHERE type = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, type);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    missions.add(extractMissionFromResultSet(resultSet));
                }
            }
        }
        return missions;
    }

    // Check if a mission exists for a candidate (for accepted status)
    public boolean isMissionCompletedByCandidate(int missionId, int candidatId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM rendu_mission WHERE mission_id = ? AND candidat_id = ? AND resultat = 'ACCEPTED'";
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, missionId);
            preparedStatement.setInt(2, candidatId);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    public boolean existsByDescription(String description) throws SQLException {
        String sql = "SELECT COUNT(*) FROM mission WHERE description = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, description);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    public boolean existsByDescriptionIgnoreCase(String description) throws SQLException {
        String sql = "SELECT COUNT(*) FROM mission WHERE LOWER(description) = LOWER(?)";
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, description);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1) > 0;
                }
            }
        }
        return false;
    }

}