package services;

import entities.Cours;
import utils.MyDatabase;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class CoursServices implements ICoursService<Cours> {
    public Connection connection;

    public CoursServices() {
        connection = MyDatabase.getInstance().getConnection();
        System.out.println("Connection to Carrieri Database is established without any probleme");
    }

    @Override
    public void ajouter(Cours cours) throws SQLException {
        // Vérification de toutes les colonnes NOT NULL
        if (cours.getTitre() == null || cours.getTitre().trim().isEmpty()) {
            throw new IllegalArgumentException("Le titre ne peut pas être vide");
        }
        if (cours.getDescription() == null || cours.getDescription().trim().isEmpty()) {
            throw new IllegalArgumentException("La description ne peut pas être vide");
        }
        if (cours.getNiveau() == null || cours.getNiveau().trim().isEmpty()) {
            throw new IllegalArgumentException("Le niveau ne peut pas être vide");
        }
        if (cours.getCompetences_visees() == null || cours.getCompetences_visees().trim().isEmpty()) {
            throw new IllegalArgumentException("Les compétences visées ne peuvent pas être vides");
        }

        String sql = "INSERT INTO cours (titre, description, duree, niveau, competences_visees, est_obligatoire, created_by) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, cours.getTitre().trim());
            preparedStatement.setString(2, cours.getDescription().trim());
            preparedStatement.setInt(3, cours.getDuree());
            preparedStatement.setString(4, cours.getNiveau().trim());
            preparedStatement.setString(5, cours.getCompetences_visees().trim());
            preparedStatement.setBoolean(6, cours.isEst_obligatoire());
            preparedStatement.setInt(7, cours.getCreatedBy());

            int rowsInserted = preparedStatement.executeUpdate();
            System.out.println("Rows inserted: " + rowsInserted);
        } catch (SQLException e) {
            System.err.println("Erreur lors de l'insertion : " + e.getMessage());
            throw e;
        }
    }


    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM cours WHERE id = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, id);
            preparedStatement.executeUpdate();
            System.out.println("Cours supprimé avec succès");
        }
    }

    @Override
    public void update(Cours cours) throws SQLException {
        String sql = "UPDATE cours SET titre = ?, description = ?, duree = ?, niveau = ?, competences_visees = ?, est_obligatoire = ? WHERE id = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, cours.getTitre());
            preparedStatement.setString(2, cours.getDescription());
            preparedStatement.setInt(3, cours.getDuree());
            preparedStatement.setString(4, cours.getNiveau());
            preparedStatement.setString(5, cours.getCompetences_visees());
            preparedStatement.setBoolean(6, cours.isEst_obligatoire());
            preparedStatement.setInt(7, cours.getId());

            preparedStatement.executeUpdate();
            System.out.println("Cours mis à jour avec succès");
        }
    }

    @Override
    public List<Cours> read() throws SQLException {
        List<Cours> coursList = new ArrayList<>();
        String sql = "SELECT * FROM cours";
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {

            while (resultSet.next()) {
                Cours cours = new Cours(
                        resultSet.getInt("id"),
                        resultSet.getString("titre"),
                        resultSet.getString("description"),
                        resultSet.getInt("duree"),
                        resultSet.getString("niveau"),
                        resultSet.getString("competences_visees"),
                        resultSet.getBoolean("est_obligatoire"),
                        resultSet.getInt("created_by")
                );
                coursList.add(cours);
            }
        }
        return coursList;
    }
}