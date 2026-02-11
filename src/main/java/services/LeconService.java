package services;

import entities.Lecon;
import utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LeconService implements ILeconService {  // ← AJOUTER implements
    private Connection con = MyDatabase.getInstance().getConnection();

    @Override  // ← AJOUTER @Override
    public void ajouter(Lecon l) {
        String sql = "INSERT INTO lecon (titre, contenu, video_url, ordre, module_id, type) VALUES (?, ?, ?, ?, ?, ?)";

        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, l.getTitre());
            ps.setString(2, l.getContenu());
            ps.setString(3, l.getVideoUrl());
            ps.setInt(4, l.getOrdre());
            ps.setInt(5, l.getModuleId());
            ps.setString(6, l.getType());
            ps.executeUpdate();
            System.out.println("✅ Leçon ajoutée: " + l.getTitre());
        } catch (Exception e) {
            System.err.println("❌ Erreur ajout leçon: " + e.getMessage());
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
                Lecon lecon = new Lecon(
                        rs.getInt("id"),
                        rs.getString("titre"),
                        rs.getString("contenu"),
                        rs.getString("video_url"),
                        rs.getInt("ordre"),
                        rs.getInt("module_id")
                );
                lecon.setType(rs.getString("type"));
                list.add(lecon);
            }
            System.out.println("📚 Leçons chargées pour module " + moduleId + ": " + list.size());
        } catch (Exception e) {
            System.err.println("❌ Erreur chargement leçons: " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public void modifier(Lecon l) {
        String sql = "UPDATE lecon SET titre=?, contenu=?, video_url=?, ordre=?, module_id=?, type=? WHERE id=?";

        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, l.getTitre());
            ps.setString(2, l.getContenu());
            ps.setString(3, l.getVideoUrl());
            ps.setInt(4, l.getOrdre());
            ps.setInt(5, l.getModuleId());
            ps.setString(6, l.getType());
            ps.setInt(7, l.getId());
            int rows = ps.executeUpdate();
            if (rows > 0) {
                System.out.println("✅ Leçon modifiée: " + l.getId());
            }
        } catch (Exception e) {
            System.err.println("❌ Erreur modification leçon: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void supprimer(int id) {
        String sql = "DELETE FROM lecon WHERE id=?";

        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, id);
            int rows = ps.executeUpdate();
            if (rows > 0) {
                System.out.println("✅ Leçon supprimée: " + id);
            } else {
                System.err.println("⚠️ Aucune leçon trouvée avec l'ID: " + id);
            }
        } catch (Exception e) {
            System.err.println("❌ Erreur suppression leçon: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public List<Lecon> getAll() {
        List<Lecon> list = new ArrayList<>();
        String sql = "SELECT * FROM lecon ORDER BY module_id, ordre";
        try {
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery(sql);
            while (rs.next()) {
                Lecon lecon = new Lecon(
                        rs.getInt("id"),
                        rs.getString("titre"),
                        rs.getString("contenu"),
                        rs.getString("video_url"),
                        rs.getInt("ordre"),
                        rs.getInt("module_id")
                );
                lecon.setType(rs.getString("type"));
                list.add(lecon);
            }
        } catch (Exception e) {
            System.err.println("❌ Erreur getAll leçons: " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }
}