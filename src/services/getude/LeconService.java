package services.getude;

import entities.getude.Lecon;
import utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LeconService implements ILeconService {
    private Connection con = MyDatabase.getInstance().getConnection();

    @Override
    public void ajouter(Lecon l) {
        validateLecon(l);
        String sql = "INSERT INTO lecon (titre, contenu, video, ordre, module_id) VALUES (?, ?, ?, ?, ?)";

        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, l.getTitre().trim());
            ps.setString(2, l.getContenu() != null ? l.getContenu().trim() : null);
            ps.setBytes(3, l.getVideo());        // ✅ AUCUNE COMPRESSION !
            ps.setInt(4, l.getOrdre());
            ps.setInt(5, l.getModuleId());
            ps.executeUpdate();
            System.out.println("✅ Leçon ajoutée: " + l.getTitre());
        } catch (SQLException e) {
            System.err.println("❌ Erreur ajout leçon: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de l'ajout de la leçon: " + e.getMessage(), e);
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
                        rs.getBytes("video"),      // ✅ AUCUNE COMPRESSION !
                        rs.getInt("ordre"),
                        rs.getInt("module_id")
                );
                list.add(lecon);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public void modifier(Lecon l) {
        validateLecon(l);
        String sql = "UPDATE lecon SET titre=?, contenu=?, video=?, ordre=?, module_id=? WHERE id=?";

        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, l.getTitre().trim());
            ps.setString(2, l.getContenu() != null ? l.getContenu().trim() : null);
            ps.setBytes(3, l.getVideo());        // ✅ AUCUNE COMPRESSION !
            ps.setInt(4, l.getOrdre());
            ps.setInt(5, l.getModuleId());
            ps.setInt(6, l.getId());
            int rows = ps.executeUpdate();
            if (rows > 0) {
                System.out.println("✅ Leçon modifiée: " + l.getId());
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur modification leçon: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de la modification de la leçon: " + e.getMessage(), e);
        }
    }

    @Override
    public void supprimer(int id) {
        Connection conn = null;
        try {
            conn = MyDatabase.getInstance().getConnection();
            conn.setAutoCommit(false);

            // 1. Supprimer les progressions
            String deleteProgressions = "DELETE FROM progression_lecon WHERE lecon_id=?";
            try (PreparedStatement ps = conn.prepareStatement(deleteProgressions)) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }

            // 2. Supprimer la leçon
            String sql = "DELETE FROM lecon WHERE id=?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, id);
                int rows = ps.executeUpdate();
                if (rows > 0) {
                    System.out.println("✅ Leçon supprimée: " + id);
                }
            }

            conn.commit();
        } catch (SQLException e) {
            System.err.println("❌ Erreur suppression leçon: " + e.getMessage());
            try {
                if (conn != null) conn.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
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
                        rs.getBytes("video"),
                        rs.getInt("ordre"),
                        rs.getInt("module_id")
                );
                list.add(lecon);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // ============================================
    // VALIDATION
    // ============================================
    private void validateLecon(Lecon lecon) {
        // Validation du titre
        if (lecon.getTitre() == null || lecon.getTitre().trim().isEmpty()) {
            throw new IllegalArgumentException("Le titre de la leçon est obligatoire");
        }
        if (lecon.getTitre().trim().length() < 3) {
            throw new IllegalArgumentException("Le titre doit contenir au moins 3 caractères");
        }
        if (lecon.getTitre().length() > 200) {
            throw new IllegalArgumentException("Le titre ne peut pas dépasser 200 caractères");
        }
        
        // ✅ VALIDATION LOGIQUE: Le titre ne peut pas être composé uniquement de chiffres
        if (isOnlyDigits(lecon.getTitre().trim())) {
            throw new IllegalArgumentException("Le titre ne peut pas être composé uniquement de chiffres");
        }

        // ✅ VALIDATION D'UNICITÉ: Vérifier que le titre de la leçon est unique dans le module
        if (isTitreLeconExiste(lecon.getTitre().trim(), lecon.getModuleId(), lecon.getId())) {
            throw new IllegalArgumentException("Une leçon avec ce titre existe déjà dans ce module !");
        }

        // Validation du contenu
        if (lecon.getContenu() == null || lecon.getContenu().trim().isEmpty()) {
            throw new IllegalArgumentException("Le contenu de la leçon est obligatoire");
        }
        if (lecon.getContenu().trim().length() < 10) {
            throw new IllegalArgumentException("Le contenu doit contenir au moins 10 caractères");
        }
        if (lecon.getContenu().length() > 10000) {
            throw new IllegalArgumentException("Le contenu ne peut pas dépasser 10000 caractères");
        }
        
        // ✅ VALIDATION LOGIQUE: Le contenu ne peut pas être composé uniquement de chiffres
        if (isOnlyDigits(lecon.getContenu().trim())) {
            throw new IllegalArgumentException("Le contenu ne peut pas être composé uniquement de chiffres");
        }
        
        // ✅ VALIDATION LOGIQUE: Le contenu ne doit pas contenir trop de chiffres (max 30%)
        if (hasTooManyDigits(lecon.getContenu().trim(), 30)) {
            throw new IllegalArgumentException("Le contenu contient trop de chiffres (maximum 30% autorisé)");
        }

        // Validation de l'ordre
        if (lecon.getOrdre() <= 0) {
            throw new IllegalArgumentException("L'ordre doit être un nombre positif");
        }
        if (lecon.getOrdre() > 100) {
            throw new IllegalArgumentException("L'ordre ne peut pas dépasser 100");
        }

        // Validation du module ID
        if (lecon.getModuleId() <= 0) {
            throw new IllegalArgumentException("La leçon doit être associée à un module valide");
        }

        // Validation de la vidéo (taille maximale: 64 MB = limite MySQL max_allowed_packet)
        if (lecon.getVideo() != null && lecon.getVideo().length > 64 * 1024 * 1024) {
            throw new IllegalArgumentException("La vidéo ne peut pas dépasser 64 MB (limite MySQL)");
        }
    }
    
    // ✅ MÉTHODE POUR VÉRIFIER SI UN TEXTE EST COMPOSÉ UNIQUEMENT DE CHIFFRES
    private boolean isOnlyDigits(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        String textWithoutSpaces = text.replaceAll("\\s+", "");
        return textWithoutSpaces.matches("\\d+");
    }
    
    // ✅ MÉTHODE POUR VÉRIFIER SI UN TEXTE CONTIENT TROP DE CHIFFRES
    private boolean hasTooManyDigits(String text, int maxPercentage) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        
        int totalChars = 0;
        int digitCount = 0;
        
        for (char c : text.toCharArray()) {
            if (!Character.isWhitespace(c)) {
                totalChars++;
                if (Character.isDigit(c)) {
                    digitCount++;
                }
            }
        }
        
        if (totalChars == 0) {
            return false;
        }
        
        double digitPercentage = (digitCount * 100.0) / totalChars;
        return digitPercentage > maxPercentage;
    }

    // ✅ MÉTHODE POUR VÉRIFIER L'UNICITÉ DU TITRE DE LA LEÇON DANS UN MODULE
    private boolean isTitreLeconExiste(String titre, int moduleId, int leconIdExclu) {
        String sql = "SELECT COUNT(*) FROM lecon WHERE LOWER(titre) = LOWER(?) AND module_id = ? AND id != ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, titre);
            ps.setInt(2, moduleId);
            ps.setInt(3, leconIdExclu <= 0 ? -1 : leconIdExclu);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur vérification unicité titre leçon: " + e.getMessage());
        }
        return false;
    }
}
