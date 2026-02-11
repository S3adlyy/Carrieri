package services;

import entities.Module;
import utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ModuleService implements IModuleService {  // ← AJOUTER implements

    private Connection con = MyDatabase.getInstance().getConnection();

    @Override  // ← AJOUTER @Override
    public void ajouter(Module module) {
        String sql = "INSERT INTO module (titre, description, ordre, cours_id) VALUES (?, ?, ?, ?)";

        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, module.getTitre());
            ps.setString(2, module.getDescription());
            ps.setInt(3, module.getOrdre());
            ps.setInt(4, module.getCoursId());
            int rows = ps.executeUpdate();
            if (rows > 0) {
                System.out.println("✅ Module ajouté: " + module.getTitre());
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur ajout module: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void modifier(Module module) {
        String sql = "UPDATE module SET titre=?, description=?, ordre=?, cours_id=? WHERE id=?";

        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, module.getTitre());
            ps.setString(2, module.getDescription());
            ps.setInt(3, module.getOrdre());
            ps.setInt(4, module.getCoursId());
            ps.setInt(5, module.getId());
            int rows = ps.executeUpdate();
            if (rows > 0) {
                System.out.println("✅ Module modifié: " + module.getId());
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur modification module: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void supprimer(int id) {
        Connection conn = null;
        try {
            conn = MyDatabase.getInstance().getConnection();
            conn.setAutoCommit(false); // Transaction

            // 1. Supprimer les leçons du module
            String deleteLecons = "DELETE FROM lecon WHERE module_id=?";
            try (PreparedStatement ps1 = conn.prepareStatement(deleteLecons)) {
                ps1.setInt(1, id);
                int leconsSupprimees = ps1.executeUpdate();
                System.out.println("📚 Leçons supprimées du module " + id + ": " + leconsSupprimees);
            }

            // 2. Supprimer le module
            String deleteModule = "DELETE FROM module WHERE id=?";
            try (PreparedStatement ps2 = conn.prepareStatement(deleteModule)) {
                ps2.setInt(1, id);
                int rows = ps2.executeUpdate();
                if (rows > 0) {
                    System.out.println("✅ Module supprimé: " + id);
                }
            }

            conn.commit(); // Valider la transaction
            System.out.println("✅ Transaction validée");

        } catch (SQLException e) {
            System.err.println("❌ Erreur suppression module: " + e.getMessage());
            try {
                if (conn != null) conn.rollback(); // Annuler en cas d'erreur
                System.err.println("⏪ Transaction annulée");
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            e.printStackTrace();
        }
    }

    @Override
    public List<Module> getAll() {
        List<Module> list = new ArrayList<>();
        String sql = "SELECT * FROM module ORDER BY cours_id, ordre";

        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                list.add(new Module(
                        rs.getInt("id"),
                        rs.getString("titre"),
                        rs.getString("description"),
                        rs.getInt("ordre"),
                        rs.getInt("cours_id")
                ));
            }
            System.out.println("📚 Modules chargés: " + list.size());

        } catch (SQLException e) {
            System.err.println("❌ Erreur getAll modules: " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public List<Module> getModulesByCours(int coursId) {
        List<Module> list = new ArrayList<>();
        String sql = "SELECT * FROM module WHERE cours_id=? ORDER BY ordre";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, coursId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Module(
                            rs.getInt("id"),
                            rs.getString("titre"),
                            rs.getString("description"),
                            rs.getInt("ordre"),
                            rs.getInt("cours_id")
                    ));
                }
            }
            System.out.println("📚 Modules chargés pour cours " + coursId + ": " + list.size());

        } catch (SQLException e) {
            System.err.println("❌ Erreur getModulesByCours: " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }
}