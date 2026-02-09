package services;

import entities.Lecon;
import utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LeconService implements ILeconService {
    private Connection con = MyDatabase.getInstance().getConnection();

    @Override
    public void ajouter(Lecon l) {
        String sql = "INSERT INTO lecon (titre, contenu, video_url, ordre, module_id) VALUES (?, ?, ?, ?, ?)";

        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, l.getTitre());
            ps.setString(2, l.getContenu());
            ps.setString(3, l.getVideoUrl());
            ps.setInt(4, l.getOrdre());
            ps.setInt(5, l.getModuleId());
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<Lecon> getLeconsByModule(int moduleId) {
        List<Lecon> list = new ArrayList<>();
        String sql = "SELECT * FROM lecon WHERE module_id=? ORDER BY ordre";

        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, moduleId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                list.add(new Lecon(
                        rs.getInt("id"),
                        rs.getString("titre"),
                        rs.getString("contenu"),
                        rs.getString("video_url"),
                        rs.getInt("ordre"),
                        rs.getInt("module_id")
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    @Override
    public void modifier(Lecon l) {}
    @Override
    public void supprimer(int id) {}
    @Override
    public List<Lecon> getAll() { return new ArrayList<>(); }
}
