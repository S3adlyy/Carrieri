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
        String sql = "INSERT INTO mission (description, score_min, created_at, created_by_id) VALUES (?, ?, ?, ?)";
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, mission.getDescription());
            preparedStatement.setInt(2, mission.getScore_min());
            preparedStatement.setTimestamp(3, Timestamp.valueOf(mission.getCreated_at()));
            preparedStatement.setObject(4, mission.getCreated_by_id(), Types.INTEGER);

            preparedStatement.executeUpdate();
            System.out.println("Mission ajoutée avec succès");
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
        String sql = "UPDATE mission SET description = ?, score_min = ?, created_by_id = ? WHERE id = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, mission.getDescription());
            preparedStatement.setInt(2, mission.getScore_min());
            preparedStatement.setObject(3, mission.getCreated_by_id(), Types.INTEGER);
            preparedStatement.setInt(4, mission.getId());

            preparedStatement.executeUpdate();
            System.out.println("Mission mise à jour avec succès");
        }
    }

    @Override
    public List<Mission> read() throws SQLException {
        List<Mission> missions = new ArrayList<>();
        String sql = "SELECT * FROM mission";
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {

            while (resultSet.next()) {
                Mission mission = new Mission(
                        resultSet.getInt("id"),
                        resultSet.getString("description"),
                        resultSet.getInt("score_min"),
                        resultSet.getTimestamp("created_at").toLocalDateTime(),
                        resultSet.getObject("created_by_id", Integer.class)
                );
                missions.add(mission);
            }
        }
        return missions;
    }

    // Additional method to get mission by ID
    public Mission getById(int id) throws SQLException {
        String sql = "SELECT * FROM mission WHERE id = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, id);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return new Mission(
                            resultSet.getInt("id"),
                            resultSet.getString("description"),
                            resultSet.getInt("score_min"),
                            resultSet.getTimestamp("created_at").toLocalDateTime(),
                            resultSet.getObject("created_by_id", Integer.class)
                    );
                }
            }
        }
        return null;
    }
}