package services;

import entities.Certification;
import utils.MyDatabase;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class CertificationService implements ICertificationService {

    private Connection connection;

    public CertificationService() {
        connection = MyDatabase.getInstance().getConnection();
    }

    @Override
    public void ajouter(Certification c) throws SQLException {
        String sql = "INSERT INTO certification (candidat_id, cours_id, date_obtention) VALUES (?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, c.getCandidatId());
            ps.setInt(2, c.getCoursId());
            ps.setTimestamp(3, Timestamp.valueOf(c.getDateObtention()));
            ps.executeUpdate();
        }
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM certification WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    @Override
    public List<Certification> read() throws SQLException {
        List<Certification> list = new ArrayList<>();
        String sql = "SELECT * FROM certification";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Certification c = new Certification(
                        rs.getInt("id"),
                        rs.getInt("candidat_id"),
                        rs.getInt("cours_id"),
                        rs.getTimestamp("date_obtention").toLocalDateTime()
                );
                list.add(c);
            }
        }
        return list;
    }

    @Override
    public Certification readByCoursAndCandidat(int coursId, int candidatId) throws SQLException {
        String sql = "SELECT * FROM certification WHERE cours_id = ? AND candidat_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, coursId);
            ps.setInt(2, candidatId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new Certification(
                        rs.getInt("id"),
                        rs.getInt("candidat_id"),
                        rs.getInt("cours_id"),
                        rs.getTimestamp("date_obtention").toLocalDateTime()
                );
            }
        }
        return null;
    }
}
