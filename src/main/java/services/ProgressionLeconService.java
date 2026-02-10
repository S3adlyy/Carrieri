package services;

import utils.MyDatabase;
import java.sql.*;

public class ProgressionLeconService {

    Connection con = MyDatabase.getInstance().getConnection();

    public void marquerTerminee(int candidatId, int leconId) {
        String sql = "INSERT INTO progression_lecon (candidat_id, lecon_id, terminee) VALUES (?, ?, true)";
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, candidatId);
            ps.setInt(2, leconId);
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public double getProgressionCours(int candidatId, int coursId) {
        double progression = 0;
        try {
            String totalSql = """
                SELECT COUNT(*) FROM lecon l
                JOIN module m ON l.module_id = m.id
                WHERE m.cours_id = ?
            """;

            String doneSql = """
                SELECT COUNT(*) FROM progression_lecon p
                JOIN lecon l ON p.lecon_id = l.id
                JOIN module m ON l.module_id = m.id
                WHERE m.cours_id = ? AND p.candidat_id = ?
            """;

            PreparedStatement ps1 = con.prepareStatement(totalSql);
            ps1.setInt(1, coursId);
            ResultSet rs1 = ps1.executeQuery();
            rs1.next();
            int total = rs1.getInt(1);

            PreparedStatement ps2 = con.prepareStatement(doneSql);
            ps2.setInt(1, coursId);
            ps2.setInt(2, candidatId);
            ResultSet rs2 = ps2.executeQuery();
            rs2.next();
            int done = rs2.getInt(1);

            if (total > 0) {
                progression = (done * 100.0) / total;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return progression;
    }
}
