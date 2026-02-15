package services;

import utils.MyDatabase;
import java.sql.*;

public class ProgressionCoursService implements IProgressionCoursService {

    Connection conn;

    public ProgressionCoursService() {
        conn = MyDatabase.getInstance().getConnection();
    }

    @Override
    public void ajouterOuUpdate(int candidatId, int coursId, int progression) throws SQLException {
        // Validation
        if (candidatId <= 0) {
            throw new IllegalArgumentException("ID candidat invalide");
        }
        if (coursId <= 0) {
            throw new IllegalArgumentException("ID cours invalide");
        }
        if (progression < 0 || progression > 100) {
            throw new IllegalArgumentException("La progression doit être entre 0 et 100");
        }

        String check = "SELECT * FROM progression_cours WHERE candidat_id=? AND cours_id=?";
        PreparedStatement ps = conn.prepareStatement(check);
        ps.setInt(1, candidatId);
        ps.setInt(2, coursId);
        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            String update = "UPDATE progression_cours SET progression=?, date_maj=NOW() WHERE candidat_id=? AND cours_id=?";
            PreparedStatement ps2 = conn.prepareStatement(update);
            ps2.setInt(1, progression);
            ps2.setInt(2, candidatId);
            ps2.setInt(3, coursId);
            ps2.executeUpdate();
        } else {
            String insert = "INSERT INTO progression_cours (candidat_id, cours_id, progression) VALUES (?, ?, ?)";
            PreparedStatement ps2 = conn.prepareStatement(insert);
            ps2.setInt(1, candidatId);
            ps2.setInt(2, coursId);
            ps2.setInt(3, progression);
            ps2.executeUpdate();
        }
    }

    @Override
    public boolean estCoursComplete(int candidatId, int coursId) throws SQLException {
        String sql = "SELECT progression FROM progression_cours WHERE candidat_id=? AND cours_id=?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, candidatId);
        ps.setInt(2, coursId);
        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            int prog = rs.getInt("progression");
            return prog >= 100;
        }
        return false;
    }
}