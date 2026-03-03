package services.goffre;

import entities.goffre.Postulation;
import session.SessionContext;
import utils.MyDatabase;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class PostulationService implements IPostulationService {

    private final Connection cnx;

    public PostulationService() {
        cnx = MyDatabase.getInstance().getConnection();
    }

    //  CREATE (métier)
    @Override
    public void postuler(Postulation p) throws SQLException {
        String sql = "INSERT INTO postulation (date_postulation, statut, motivation_candidature, cv_path, candidat_id, offre_id) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setTimestamp(1, p.getDatePostulation() == null ? null : Timestamp.valueOf(p.getDatePostulation()));
        ps.setString(2, p.getStatut());
        ps.setString(3, p.getMotivationCandidature());
        ps.setString(4, p.getCvPath());
        ps.setInt(5, p.getCandidatId());
        ps.setInt(6, p.getOffreId());

        ps.executeUpdate();
    }

    //  IService<Postulation> : ajouter() -> appelle postuler()
    @Override
    public void ajouter(Postulation p) throws SQLException {
        postuler(p);
    }

    //  READ ALL
    public List<Postulation> afficher() throws SQLException {
        List<Postulation> list = new ArrayList<>();

        String sql = "SELECT * FROM postulation WHERE candidat_id = ?";
        System.out.println("postulations candidat id afficher: " + SessionContext.getCurrentUser().getId());

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, SessionContext.getCurrentUser().getId());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        }

        return list;
    }
    public List<Postulation> afficherCandidates() throws SQLException {
        List<Postulation> list = new ArrayList<>();

        String sql = "SELECT * FROM postulation";
        System.out.println("postulations candidat id afficher: " + SessionContext.getCurrentUser().getId());

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        }

        return list;
    }


    //  IService<Postulation> : read()
    @Override
    public List<Postulation> read() throws SQLException {
        return afficher();
    }
    @Override
    public List<Postulation> readCandidates() throws SQLException {
        return afficherCandidates();
    }

    //  READ by offre
    @Override
    public List<Postulation> afficherParOffre(int offreId) throws SQLException {
        List<Postulation> list = new ArrayList<>();
        String sql = "SELECT * FROM postulation WHERE offre_id=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, offreId);

        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            list.add(map(rs));
        }
        return list;
    }

    //  READ by candidat
    @Override
    public List<Postulation> afficherParCandidat(int candidatId) throws SQLException {
        List<Postulation> list = new ArrayList<>();
        String sql = "SELECT * FROM postulation WHERE candidat_id=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, candidatId);

        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            list.add(map(rs));
        }
        return list;
    }

    //  UPDATE statut
    @Override
    public void changerStatut(int idPostulation, String nouveauStatut) throws SQLException {
        String sql = "UPDATE postulation SET statut=? WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, nouveauStatut);
        ps.setInt(2, idPostulation);
        ps.executeUpdate();
    }

    //  IService<Postulation> : modifier()
    // Ici on considère que "modifier" = changer statut (cas le plus utile côté recruteur)
    @Override
    public void modifier(Postulation p) throws SQLException {
        changerStatut(p.getId(), p.getStatut());
    }

    //  IService<Postulation> : supprimer()
    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM postulation WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    public int supprimerParOffre(int offreId) throws SQLException {
        String sql = "DELETE FROM postulation WHERE offre_id=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, offreId);
        return ps.executeUpdate(); // returns number of deleted rows
    }

    // helper mapper
    private Postulation map(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        Timestamp ts = rs.getTimestamp("date_postulation");
        LocalDateTime datePost = (ts != null) ? ts.toLocalDateTime() : null;

        String statut = rs.getString("statut");
        String motivation = rs.getString("motivation_candidature");
        String cvPath = rs.getString("cv_path");
        int candidatId = rs.getInt("candidat_id");
        int offreId = rs.getInt("offre_id");

        return new Postulation(id, candidatId, offreId, datePost, statut, motivation, cvPath);
    }
}
