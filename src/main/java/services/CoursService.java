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
    public void supprimer(int id) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM cours WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
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
        if (cours.getTitre() == null || cours.getTitre().trim().isEmpty())
            throw new IllegalArgumentException("Le titre est obligatoire");
        if (cours.getNiveau() == null || cours.getNiveau().trim().isEmpty())
            throw new IllegalArgumentException("Le niveau est obligatoire");
    }
}