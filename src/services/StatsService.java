package services;

import entities.Reclamation;
import entities.Feedback;
import java.sql.*;
import java.util.*;
import utils.MyDatabase;

public class StatsService {

    private Connection connection;
    private ReclamationService reclamationService = new ReclamationService();
    private FeedbackService feedbackService = new FeedbackService();

    public StatsService() {
        connection = MyDatabase.getInstance().getConnection();
    }

    // ============================================
    // STATISTIQUES GÉNÉRALES
    // ============================================

    public int getTotalReclamations() throws SQLException {
        String sql = "SELECT COUNT(*) as total FROM reclamation";
        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery(sql);
        if (rs.next()) {
            return rs.getInt("total");
        }
        return 0;
    }

    public int getTotalFeedbacks() throws SQLException {
        String sql = "SELECT COUNT(*) as total FROM feedback";
        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery(sql);
        if (rs.next()) {
            return rs.getInt("total");
        }
        return 0;
    }

    public int getTotalTraitements() throws SQLException {
        String sql = "SELECT COUNT(*) as total FROM traitement_reclamation";
        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery(sql);
        if (rs.next()) {
            return rs.getInt("total");
        }
        return 0;
    }

    // ============================================
    // STATISTIQUES PAR STATUT
    // ============================================

    public Map<String, Integer> getReclamationsParStatut() throws SQLException {
        Map<String, Integer> stats = new HashMap<>();
        String sql = "SELECT statut, COUNT(*) as count FROM reclamation GROUP BY statut";
        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery(sql);

        while (rs.next()) {
            stats.put(rs.getString("statut"), rs.getInt("count"));
        }

        // Ajouter les statuts manquants avec 0
        String[] statuts = {"Nouvelle", "En cours", "Résolue", "Fermée"};
        for (String statut : statuts) {
            stats.putIfAbsent(statut, 0);
        }

        return stats;
    }

    public Map<String, Integer> getReclamationsParPriorite() throws SQLException {
        Map<String, Integer> stats = new HashMap<>();
        String sql = "SELECT priorite, COUNT(*) as count FROM reclamation GROUP BY priorite";
        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery(sql);

        while (rs.next()) {
            stats.put(rs.getString("priorite"), rs.getInt("count"));
        }

        String[] priorites = {"Haute", "Moyenne", "Basse"};
        for (String priorite : priorites) {
            stats.putIfAbsent(priorite, 0);
        }

        return stats;
    }

    public Map<String, Integer> getReclamationsParCategorie() throws SQLException {
        Map<String, Integer> stats = new HashMap<>();
        String sql = "SELECT categorie, COUNT(*) as count FROM reclamation GROUP BY categorie";
        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery(sql);

        while (rs.next()) {
            stats.put(rs.getString("categorie"), rs.getInt("count"));
        }

        return stats;
    }

    // ============================================
    // STATISTIQUES DE FEEDBACK
    // ============================================

    public double getMoyenneNotes() throws SQLException {
        String sql = "SELECT AVG(note) as moyenne FROM feedback";
        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery(sql);
        if (rs.next()) {
            return rs.getDouble("moyenne");
        }
        return 0;
    }

    public Map<String, Integer> getFeedbacksParNote() throws SQLException {
        Map<String, Integer> stats = new HashMap<>();
        String sql = "SELECT " +
                "SUM(CASE WHEN note >= 80 THEN 1 ELSE 0 END) as excellent, " +
                "SUM(CASE WHEN note >= 60 AND note < 80 THEN 1 ELSE 0 END) as bon, " +
                "SUM(CASE WHEN note >= 40 AND note < 60 THEN 1 ELSE 0 END) as moyen, " +
                "SUM(CASE WHEN note < 40 THEN 1 ELSE 0 END) as faible " +
                "FROM feedback";

        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery(sql);

        if (rs.next()) {
            stats.put("Excellent (80-100)", rs.getInt("excellent"));
            stats.put("Bon (60-79)", rs.getInt("bon"));
            stats.put("Moyen (40-59)", rs.getInt("moyen"));
            stats.put("Faible (0-39)", rs.getInt("faible"));
        }

        return stats;
    }

    // ============================================
    // STATISTIQUES TEMPORELLES
    // ============================================

    public Map<String, Integer> getReclamationsParMois(int annee) throws SQLException {
        Map<String, Integer> stats = new LinkedHashMap<>();
        String[] mois = {"Jan", "Fév", "Mar", "Avr", "Mai", "Jun",
                "Jul", "Aoû", "Sep", "Oct", "Nov", "Déc"};

        // Initialiser tous les mois à 0
        for (String m : mois) {
            stats.put(m, 0);
        }

        String sql = "SELECT MONTH(date_creation) as mois, COUNT(*) as count " +
                "FROM reclamation WHERE YEAR(date_creation) = ? " +
                "GROUP BY MONTH(date_creation) ORDER BY mois";

        PreparedStatement pstmt = connection.prepareStatement(sql);
        pstmt.setInt(1, annee);
        ResultSet rs = pstmt.executeQuery();

        while (rs.next()) {
            int index = rs.getInt("mois") - 1;
            stats.put(mois[index], rs.getInt("count"));
        }

        return stats;
    }

    public Map<String, Integer> getTraitementsParMois(int annee) throws SQLException {
        Map<String, Integer> stats = new LinkedHashMap<>();
        String[] mois = {"Jan", "Fév", "Mar", "Avr", "Mai", "Jun",
                "Jul", "Aoû", "Sep", "Oct", "Nov", "Déc"};

        for (String m : mois) {
            stats.put(m, 0);
        }

        String sql = "SELECT MONTH(date_traitement) as mois, COUNT(*) as count " +
                "FROM traitement_reclamation WHERE YEAR(date_traitement) = ? " +
                "GROUP BY MONTH(date_traitement) ORDER BY mois";

        PreparedStatement pstmt = connection.prepareStatement(sql);
        pstmt.setInt(1, annee);
        ResultSet rs = pstmt.executeQuery();

        while (rs.next()) {
            int index = rs.getInt("mois") - 1;
            stats.put(mois[index], rs.getInt("count"));
        }

        return stats;
    }

    // ============================================
    // STATISTIQUES DE PERFORMANCE
    // ============================================

    public double getTauxResolution() throws SQLException {
        String sql = "SELECT " +
                "(SELECT COUNT(*) FROM reclamation WHERE statut IN ('Résolue', 'Fermée')) * 100.0 / " +
                "NULLIF((SELECT COUNT(*) FROM reclamation), 0) as taux";

        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery(sql);
        if (rs.next()) {
            return rs.getDouble("taux");
        }
        return 0;
    }

    public double getTempsMoyenTraitement() throws SQLException {
        String sql = "SELECT AVG(TIMESTAMPDIFF(HOUR, r.date_creation, t.date_traitement)) as heures " +
                "FROM traitement_reclamation t " +
                "JOIN reclamation r ON t.reclamation_id = r.id " +
                "WHERE r.statut = 'Résolue'";

        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery(sql);
        if (rs.next()) {
            return rs.getDouble("heures");
        }
        return 0;
    }

    public List<Reclamation> getReclamationsUrgentes() throws SQLException {
        // Réclamations non traitées depuis plus de 7 jours
        String sql = "SELECT * FROM reclamation WHERE statut IN ('Nouvelle', 'En cours') " +
                "AND date_creation < DATE_SUB(NOW(), INTERVAL 7 DAY) " +
                "ORDER BY date_creation ASC";

        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery(sql);
        List<Reclamation> urgentes = new ArrayList<>();

        while (rs.next()) {
            Reclamation r = new Reclamation();
            r.setId(rs.getInt("id"));
            r.setObjet(rs.getString("objet"));
            r.setStatut(rs.getString("statut"));
            r.setPriorite(rs.getString("priorite"));
            r.setDateCreation(rs.getTimestamp("date_creation"));
            urgentes.add(r);
        }

        return urgentes;
    }
}