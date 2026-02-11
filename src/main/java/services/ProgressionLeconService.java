package services;

import entities.Lecon;
import utils.MyDatabase;
import java.sql.*;
import java.util.List;
import entities.Module;

public class ProgressionLeconService implements IProgressionLeconService {

    Connection con = MyDatabase.getInstance().getConnection();

    @Override
    public void marquerTerminee(int candidatId, int leconId) {
        System.out.println("SERVICE: marquerTerminee - candidat=" + candidatId + ", leçon=" + leconId);
        String sql = "INSERT INTO progression_lecon (candidat_id, lecon_id, termine) " +
                "VALUES (?, ?, 1) " +
                "ON DUPLICATE KEY UPDATE termine = 1, date_validation = CURRENT_TIMESTAMP";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, candidatId);
            ps.setInt(2, leconId);
            int rows = ps.executeUpdate();
            System.out.println("SERVICE: " + rows + " ligne(s) affectée(s)");
        } catch (SQLException e) {
            System.err.println("SERVICE ERROR marquerTerminee: " + e.getMessage());
            // Fallback...
        }
    }

    @Override
    public boolean isLeconTerminee(int candidatId, int leconId) {
        String sql = "SELECT termine FROM progression_lecon WHERE candidat_id = ? AND lecon_id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, candidatId);
            ps.setInt(2, leconId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getBoolean("termine");
            }
        } catch (SQLException e) {
            System.err.println("SERVICE ERROR isLeconTerminee: " + e.getMessage());
        }
        return false;
    }

    @Override
    public double getProgressionCours(int candidatId, int coursId) {
        System.out.println("SERVICE: Calcul progression cours " + coursId + " pour candidat " + candidatId);
        List<Module> modules = new ModuleService().getModulesByCours(coursId);
        int totalLecons = 0;
        int leconsTerminees = 0;

        for (Module m : modules) {
            List<Lecon> lecons = new LeconService().getLeconsByModule(m.getId());
            totalLecons += lecons.size();
            for (Lecon l : lecons) {
                if (isLeconTerminee(candidatId, l.getId())) {
                    leconsTerminees++;
                }
            }
        }

        if (totalLecons == 0) return 0;
        return ((double) leconsTerminees / totalLecons) * 100;
    }
}