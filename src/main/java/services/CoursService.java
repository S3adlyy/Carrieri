package services;

import entities.Cours;
import utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CoursService implements ICoursService<Cours> {
    public Connection connection;

    public CoursService() {
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

        String sql = "INSERT INTO cours (titre, description, duree, niveau, competences_visees, est_obligatoire, created_by, image_couverture) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, cours.getTitre().trim());
            ps.setString(2, cours.getDescription().trim());
            ps.setInt(3, cours.getDuree());
            ps.setString(4, cours.getNiveau().trim());
            ps.setString(5, cours.getCompetences_visees().trim());
            ps.setBoolean(6, cours.isEst_obligatoire());
            ps.setInt(7, cours.getCreatedBy());

            if (cours.getImageCouverture() != null) {
                ps.setBytes(8, cours.getImageCouverture());
            } else {
                ps.setNull(8, Types.BLOB);
            }

            int rowsInserted = ps.executeUpdate();
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
        String sql = "UPDATE cours SET titre = ?, description = ?, duree = ?, niveau = ?, competences_visees = ?, est_obligatoire = ?, image_couverture=? WHERE id = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, cours.getTitre());
            preparedStatement.setString(2, cours.getDescription());
            preparedStatement.setInt(3, cours.getDuree());
            preparedStatement.setString(4, cours.getNiveau());
            preparedStatement.setString(5, cours.getCompetences_visees());
            preparedStatement.setBoolean(6, cours.isEst_obligatoire());
            preparedStatement.setBytes(7, cours.getImageCouverture());
            preparedStatement.setInt(8, cours.getId());

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
                byte[] image = resultSet.getBytes("image_couverture");
                Cours cours = new Cours(
                        resultSet.getInt("id"),
                        resultSet.getString("titre"),
                        resultSet.getString("description"),
                        resultSet.getInt("duree"),
                        resultSet.getString("niveau"),
                        resultSet.getString("competences_visees"),
                        resultSet.getBoolean("est_obligatoire"),
                        resultSet.getInt("created_by"),
                        image // ajouter ici
                );
                coursList.add(cours);
            }
        }
        return coursList;
    }
    public List<Cours> readByAdmin(int userId) throws SQLException {
        List<Cours> coursList = new ArrayList<>();
        String sql = "SELECT * FROM cours WHERE created_by = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet resultSet = stmt.executeQuery();

            while (resultSet.next()) {
                byte[] image = resultSet.getBytes("image_couverture");
                Cours cours = new Cours(
                        resultSet.getInt("id"),
                        resultSet.getString("titre"),
                        resultSet.getString("description"),
                        resultSet.getInt("duree"),
                        resultSet.getString("niveau"),
                        resultSet.getString("competences_visees"),
                        resultSet.getBoolean("est_obligatoire"),
                        resultSet.getInt("created_by"),
                        image // ajouter ici
                );
                coursList.add(cours);
            }
        }
        return coursList;
    }

}