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
        validateModule(module);
        String sql = "INSERT INTO module (titre, description, ordre, cours_id) VALUES (?, ?, ?, ?)";

        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, module.getTitre().trim());
            ps.setString(2, module.getDescription().trim());
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
        validateModule(module);
        String sql = "UPDATE module SET titre=?, description=?, ordre=?, cours_id=? WHERE id=?";

        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, module.getTitre().trim());
            ps.setString(2, module.getDescription().trim());
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
    // Dans ModuleService.java, ajoutez cette méthode :

    public Module getModuleById(int id) {
        String sql = "SELECT * FROM module WHERE id = ?";
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new Module(
                        rs.getInt("id"),
                        rs.getString("titre"),
                        rs.getString("description"),
                        rs.getInt("ordre"),
                        rs.getInt("cours_id")
                );
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur getModuleById: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    // ============================================
    // VALIDATION
    // ============================================
    private void validateModule(Module module) {
        // Validation du titre
        if (module.getTitre() == null || module.getTitre().trim().isEmpty()) {
            throw new IllegalArgumentException("Le titre du module est obligatoire");
        }
        if (module.getTitre().trim().length() < 3) {
            throw new IllegalArgumentException("Le titre doit contenir au moins 3 caractères");
        }
        if (module.getTitre().length() > 200) {
            throw new IllegalArgumentException("Le titre ne peut pas dépasser 200 caractères");
        }

        // Validation de la description
        if (module.getDescription() == null || module.getDescription().trim().isEmpty()) {
            throw new IllegalArgumentException("La description est obligatoire");
        }
        if (module.getDescription().trim().length() < 10) {
            throw new IllegalArgumentException("La description doit contenir au moins 10 caractères");
        }
        if (module.getDescription().length() > 1000) {
            throw new IllegalArgumentException("La description ne peut pas dépasser 1000 caractères");
        }

        // Validation de l'ordre
        if (module.getOrdre() <= 0) {
            throw new IllegalArgumentException("L'ordre doit être un nombre positif");
        }
        if (module.getOrdre() > 100) {
            throw new IllegalArgumentException("L'ordre ne peut pas dépasser 100");
        }

        // Validation du cours ID
        if (module.getCoursId() <= 0) {
            throw new IllegalArgumentException("Le module doit être associé à un cours valide");
        }
    }
}

