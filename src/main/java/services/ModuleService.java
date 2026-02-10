package services;

import entities.Module;
import utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ModuleService implements IModuleService{
    private Connection con = MyDatabase.getInstance().getConnection();

    @Override
    public void ajouter(Module module) {
        String sql = "INSERT INTO module (titre, description, ordre, cours_id) VALUES (?, ?, ?, ?)";

        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, module.getTitre());
            ps.setString(2, module.getDescription());
            ps.setInt(3, module.getOrdre());
            ps.setInt(4, module.getCoursId());
            ps.executeUpdate();
        } catch (Exception e) {
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
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void supprimer(int id) {
        String sql = "DELETE FROM module WHERE id=?";

        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<Module> getAll() {
        List<Module> list = new ArrayList<>();
        String sql = "SELECT * FROM module";

        try {
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery(sql);

            while (rs.next()) {
                list.add(new Module(
                        rs.getInt("id"),
                        rs.getString("titre"),
                        rs.getString("description"),
                        rs.getInt("ordre"),
                        rs.getInt("cours_id")
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    @Override
    public List<Module> getModulesByCours(int coursId) {
        List<Module> list = new ArrayList<>();
        String sql = "SELECT * FROM module WHERE cours_id=? ORDER BY ordre";

        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, coursId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                list.add(new Module(
                        rs.getInt("id"),
                        rs.getString("titre"),
                        rs.getString("description"),
                        rs.getInt("ordre"),
                        rs.getInt("cours_id")
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }


}
