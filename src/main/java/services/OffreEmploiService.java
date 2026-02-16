package services;

import entities.OffreEmploi;
import utils.MyDatabase;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class OffreEmploiService implements IOffreEmploiService {

    private final Connection cnx;

    public OffreEmploiService() {
        cnx = MyDatabase.getInstance().getConnection();
    }

    //  CREATE
    @Override
    public void ajouter(OffreEmploi o) throws SQLException {
        // Vérifier l'unicité du titre
        if (existsByTitre(o.getTitre())) {
            throw new SQLException("Une offre avec le titre \"" + o.getTitre() + "\" existe déjà. Veuillez choisir un titre différent.");
        }

        String sql = "INSERT INTO offre_emploi " +
                "(titre, description, salaire, type_contrat, localisation, date_publication, date_expiration, " +
                "niveau_qualification, experience_requise, competences_requises, secteur_activite, entreprise, contact_recruteur) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, o.getTitre());
        ps.setString(2, o.getDescription());
        ps.setDouble(3, o.getSalaire());
        ps.setString(4, o.getTypeContrat());
        ps.setString(5, o.getLocalisation());

        // DATETIME => Timestamp
        ps.setTimestamp(6, o.getDatePublication() == null ? null : Timestamp.valueOf(o.getDatePublication()));
        ps.setTimestamp(7, o.getDateExpiration() == null ? null : Timestamp.valueOf(o.getDateExpiration()));

        ps.setString(8, o.getNiveauQualification());
        ps.setString(9, o.getExperienceRequise());
        ps.setString(10, o.getCompetencesRequises());
        ps.setString(11, o.getSecteurActivite());
        ps.setString(12, o.getEntreprise());
        ps.setString(13, o.getContactRecruteur());

        ps.executeUpdate();
    }

    // Méthode pour vérifier si une offre avec le même titre existe déjà
    public boolean existsByTitre(String titre) throws SQLException {
        String sql = "SELECT COUNT(*) FROM offre_emploi WHERE LOWER(titre) = LOWER(?)";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, titre.trim());
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            return rs.getInt(1) > 0;
        }
        return false;
    }

    //  READ (IService requires read())
    @Override
    public List<OffreEmploi> read() throws SQLException {
        List<OffreEmploi> list = new ArrayList<>();
        String sql = "SELECT * FROM offre_emploi";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);

        while (rs.next()) {
            list.add(map(rs));
        }
        return list;
    }

    // (optional) if you still want afficher() for old tests:
    public List<OffreEmploi> afficher() throws SQLException {
        return read();
    }

    //  READ by id
    @Override
    public OffreEmploi findById(int id) throws SQLException {
        String sql = "SELECT * FROM offre_emploi WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);

        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            return map(rs);
        }
        return null;
    }

    //  UPDATE
    @Override
    public void modifier(OffreEmploi o) throws SQLException {
        String sql = "UPDATE offre_emploi SET " +
                "titre=?, description=?, salaire=?, type_contrat=?, localisation=?, date_publication=?, date_expiration=?, " +
                "niveau_qualification=?, experience_requise=?, competences_requises=?, secteur_activite=?, entreprise=?, contact_recruteur=? " +
                "WHERE id=?";

        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, o.getTitre());
        ps.setString(2, o.getDescription());
        ps.setDouble(3, o.getSalaire());
        ps.setString(4, o.getTypeContrat());
        ps.setString(5, o.getLocalisation());

        // DATETIME => Timestamp
        ps.setTimestamp(6, o.getDatePublication() == null ? null : Timestamp.valueOf(o.getDatePublication()));
        ps.setTimestamp(7, o.getDateExpiration() == null ? null : Timestamp.valueOf(o.getDateExpiration()));

        ps.setString(8, o.getNiveauQualification());
        ps.setString(9, o.getExperienceRequise());
        ps.setString(10, o.getCompetencesRequises());
        ps.setString(11, o.getSecteurActivite());
        ps.setString(12, o.getEntreprise());
        ps.setString(13, o.getContactRecruteur());
        ps.setInt(14, o.getId());

        ps.executeUpdate();
    }

    //  DELETE
    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM offre_emploi WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    //  Mapper ResultSet -> OffreEmploi
    private OffreEmploi map(ResultSet rs) throws SQLException {

        int id = rs.getInt("id");
        String titre = rs.getString("titre");
        String description = rs.getString("description");
        double salaire = rs.getDouble("salaire");
        String typeContrat = rs.getString("type_contrat");
        String localisation = rs.getString("localisation");

        Timestamp tp = rs.getTimestamp("date_publication");
        Timestamp te = rs.getTimestamp("date_expiration");

        LocalDateTime datePublication = (tp != null) ? tp.toLocalDateTime() : null;
        LocalDateTime dateExpiration = (te != null) ? te.toLocalDateTime() : null;

        String niveauQualification = rs.getString("niveau_qualification");
        String experienceRequise = rs.getString("experience_requise");
        String competencesRequises = rs.getString("competences_requises");
        String secteurActivite = rs.getString("secteur_activite");
        String entreprise = rs.getString("entreprise");
        String contactRecruteur = rs.getString("contact_recruteur");

        return new OffreEmploi(
                id, titre, description, salaire, typeContrat, localisation,
                datePublication, dateExpiration,
                niveauQualification, experienceRequise, competencesRequises,
                secteurActivite, entreprise, contactRecruteur
        );
    }
}
