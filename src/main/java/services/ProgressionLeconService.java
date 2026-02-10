package services;

import entities.Lecon;
import utils.MyDatabase;
import java.sql.*;
import java.util.List;
import entities.Module;

public class ProgressionLeconService {

    Connection con = MyDatabase.getInstance().getConnection();

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

            // Fallback si ON DUPLICATE KEY ne fonctionne pas
            try {
                // D'abord essayer UPDATE
                String updateSql = "UPDATE progression_lecon SET termine = 1 WHERE candidat_id = ? AND lecon_id = ?";
                PreparedStatement ps = con.prepareStatement(updateSql);
                ps.setInt(1, candidatId);
                ps.setInt(2, leconId);
                int updated = ps.executeUpdate();

                if (updated == 0) {
                    // Si aucune ligne mise à jour, faire INSERT
                    String insertSql = "INSERT INTO progression_lecon (candidat_id, lecon_id, termine) VALUES (?, ?, 1)";
                    PreparedStatement ps2 = con.prepareStatement(insertSql);
                    ps2.setInt(1, candidatId);
                    ps2.setInt(2, leconId);
                    ps2.executeUpdate();
                    System.out.println("SERVICE FALLBACK: INSERT réussi");
                    ps2.close();
                } else {
                    System.out.println("SERVICE FALLBACK: UPDATE réussi");
                }
                ps.close();
            } catch (SQLException e2) {
                System.err.println("SERVICE FALLBACK ERROR: " + e2.getMessage());
                e2.printStackTrace();
            }
        }
    }

    public boolean isLeconTerminee(int candidatId, int leconId) {
        String sql = "SELECT termine FROM progression_lecon WHERE candidat_id = ? AND lecon_id = ?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, candidatId);
            ps.setInt(2, leconId);

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                boolean termine = rs.getBoolean("termine");
                System.out.println("SERVICE: Leçon " + leconId + " termine = " + termine);
                return termine;
            }
        } catch (SQLException e) {
            System.err.println("SERVICE ERROR isLeconTerminee: " + e.getMessage());
        }
        System.out.println("SERVICE: Leçon " + leconId + " non trouvée dans progression_lecon");
        return false;
    }

    public double getProgressionCours(int candidatId, int coursId) {
        System.out.println("SERVICE: Calcul progression cours " + coursId + " pour candidat " + candidatId);

        List<Module> modules = new ModuleService().getModulesByCours(coursId);
        System.out.println("SERVICE: Nombre de modules: " + modules.size());

        int totalLecons = 0;
        int leconsTerminees = 0;

        for (Module m : modules) {
            List<Lecon> lecons = new LeconService().getLeconsByModule(m.getId());
            totalLecons += lecons.size();
            System.out.println("SERVICE: Module " + m.getId() + " a " + lecons.size() + " leçons");

            for (Lecon l : lecons) {
                if (isLeconTerminee(candidatId, l.getId())) {
                    leconsTerminees++;
                    System.out.println("SERVICE: Leçon " + l.getId() + " est terminée");
                }
            }
        }

        System.out.println("SERVICE: Total leçons: " + totalLecons + ", Terminées: " + leconsTerminees);

        if (totalLecons == 0) {
            System.out.println("SERVICE: Aucune leçon, progression = 0");
            return 0;
        }

        double progression = ((double) leconsTerminees / totalLecons) * 100;
        System.out.println("SERVICE: Progression calculée: " + progression + "%");
        return progression;
    }
}